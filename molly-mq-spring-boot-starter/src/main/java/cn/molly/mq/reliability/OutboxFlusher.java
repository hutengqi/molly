package cn.molly.mq.reliability;

import cn.molly.mq.core.Message;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.properties.MollyMqProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Outbox 扫描与重投任务
 * <p>
 * 启动后由 {@link ScheduledExecutorService} 周期性拉取 PENDING/FAILED 记录并调用 {@link ProducerOperations#send} 投递；
 * 失败时根据退避策略（指数 capped at retry-max-backoff）更新 next_fire_time，超过最大尝试次数标记 DEAD
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
public class OutboxFlusher {

    private final OutboxStore store;
    private final ProducerOperations producer;
    private final MessageConverter converter;
    private final ObjectMapper objectMapper;
    private final MollyMqProperties properties;

    private ScheduledExecutorService scheduler;
    private ExecutorService immediateExecutor;

    public OutboxFlusher(OutboxStore store,
                         ProducerOperations producer,
                         MessageConverter converter,
                         ObjectMapper objectMapper,
                         MollyMqProperties properties) {
        this.store = store;
        this.producer = producer;
        this.converter = converter;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void start() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "molly-mq-outbox-flusher");
            t.setDaemon(true);
            return t;
        });
        this.immediateExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "molly-mq-outbox-immediate");
            t.setDaemon(true);
            return t;
        });
        long intervalMs = properties.getReliability().getScanInterval().toMillis();
        scheduler.scheduleWithFixedDelay(this::scanAndSend, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        log.info("[molly-mq] outbox flusher started, interval={}ms", intervalMs);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (immediateExecutor != null) {
            immediateExecutor.shutdownNow();
        }
    }

    /**
     * 事务提交后立即尝试投递，降低端到端延迟；避开阻塞调用线程
     */
    public void triggerImmediate(String id) {
        if (immediateExecutor == null) {
            return;
        }
        immediateExecutor.submit(this::scanAndSend);
    }

    private void scanAndSend() {
        try {
            int batch = properties.getReliability().getScanBatchSize();
            long now = System.currentTimeMillis();
            List<OutboxRecord> records = store.fetchPending(batch, now);
            if (records.isEmpty()) {
                return;
            }
            for (OutboxRecord r : records) {
                dispatch(r);
            }
        } catch (Exception e) {
            log.warn("[molly-mq] outbox scan error", e);
        }
    }

    private void dispatch(OutboxRecord r) {
        try {
            Message<byte[]> msg = Message.<byte[]>builder()
                    .topic(r.getTopic())
                    .tag(r.getTag())
                    .bizKey(r.getBizKey())
                    .shardingKey(r.getShardingKey())
                    .idempotencyKey(r.getIdempotencyKey())
                    .deliveryTimeMs(r.getDeliveryTimeMs())
                    .headers(parseHeaders(r.getHeadersJson()))
                    .payload(r.getPayload())
                    .build();
            producer.send(msg);
            store.markSent(r.getId());
            log.debug("[molly-mq] outbox sent id={} topic={}", r.getId(), r.getTopic());
        } catch (Exception ex) {
            onFailure(r, ex);
        }
    }

    private void onFailure(OutboxRecord r, Exception ex) {
        int maxAttempts = properties.getReliability().getMaxAttempts();
        if (r.getAttempts() >= maxAttempts) {
            store.markDead(r.getId(), ex.getMessage());
            log.error("[molly-mq] outbox DEAD id={} topic={} attempts={}", r.getId(), r.getTopic(), r.getAttempts(), ex);
            return;
        }
        long backoffMs = computeBackoff(r.getAttempts());
        long next = System.currentTimeMillis() + backoffMs;
        store.markFailed(r.getId(), ex.getMessage(), next);
        log.warn("[molly-mq] outbox retry id={} topic={} attempts={} nextInMs={}",
                r.getId(), r.getTopic(), r.getAttempts(), backoffMs);
    }

    private long computeBackoff(int attempts) {
        long initial = properties.getConsumer().getRetryInitialBackoff().toMillis();
        long max = properties.getConsumer().getRetryMaxBackoff().toMillis();
        long backoff = initial * (1L << Math.min(attempts, 20));
        return Math.min(backoff, max);
    }

    private Map<String, String> parseHeaders(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
