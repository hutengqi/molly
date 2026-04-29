package cn.molly.mq.consumer;

import cn.molly.mq.consumer.dlq.DlqDispatcher;
import cn.molly.mq.consumer.idempotency.IdempotencyStore;
import cn.molly.mq.core.ConsumeContext;
import cn.molly.mq.core.ConsumeResult;
import cn.molly.mq.core.Message;
import cn.molly.mq.core.MessageListener;
import cn.molly.mq.observability.MollyMqMetrics;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 统一消费路由：幂等 → 业务 listener → 结果翻译 → 超过 maxRetries 投 DLQ
 * <p>
 * 由各 Provider Container 调用，封装公共容错与可观测逻辑
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
@RequiredArgsConstructor
public class ListenerDispatcher {

    private final IdempotencyStore idempotencyStore;
    private final DlqDispatcher dlqDispatcher;

    /**
     * 可观测埋点；由 Observability 自动装配注入，null 时不记录指标
     */
    @Setter
    private MollyMqMetrics metrics;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> ConsumeResult dispatch(MessageListener listener,
                                      Message<T> message,
                                      ConsumeContext context,
                                      int maxRetries) {
        String idempKey = resolveIdempotencyKey(message, context);
        if (!idempotencyStore.tryAcquire(idempKey)) {
            log.debug("[molly-mq] skip duplicated messageId={} key={}", context.getMessageId(), idempKey);
            record(context, "duplicated", 0);
            return ConsumeResult.SUCCESS;
        }
        long startNs = System.nanoTime();
        try {
            ConsumeResult result = listener.onMessage(message, context);
            if (result == null) {
                result = ConsumeResult.SUCCESS;
            }
            if (result == ConsumeResult.SUCCESS) {
                record(context, "success", System.nanoTime() - startNs);
                return result;
            }
            // RETRY / REJECT 均释放幂等锁，让下次投递能再次处理
            idempotencyStore.release(idempKey);
            if (result == ConsumeResult.REJECT || context.getDeliveryCount() > maxRetries) {
                dlqDispatcher.dispatch(message, context, null);
                record(context, "dlq", System.nanoTime() - startNs);
                return ConsumeResult.SUCCESS; // 告知 broker 不再重试
            }
            record(context, "retry", System.nanoTime() - startNs);
            return ConsumeResult.RETRY;
        } catch (Exception ex) {
            idempotencyStore.release(idempKey);
            log.warn("[molly-mq] listener error messageId={} topic={} deliveryCount={}",
                    context.getMessageId(), context.getTopic(), context.getDeliveryCount(), ex);
            if (context.getDeliveryCount() > maxRetries) {
                dlqDispatcher.dispatch(message, context, ex);
                record(context, "dlq", System.nanoTime() - startNs);
                return ConsumeResult.SUCCESS;
            }
            record(context, "retry", System.nanoTime() - startNs);
            return ConsumeResult.RETRY;
        }
    }

    private void record(ConsumeContext context, String status, long elapsedNs) {
        if (metrics != null) {
            metrics.recordConsume(
                    context.getProviderName() == null ? "unknown" : context.getProviderName(),
                    context.getTopic() == null ? "unknown" : context.getTopic(),
                    status,
                    Duration.ofNanos(Math.max(0, elapsedNs)));
        }
    }

    private String resolveIdempotencyKey(Message<?> message, ConsumeContext ctx) {
        if (message.getIdempotencyKey() != null && !message.getIdempotencyKey().isEmpty()) {
            return message.getIdempotencyKey();
        }
        if (message.getBizKey() != null && !message.getBizKey().isEmpty()) {
            return "biz:" + message.getBizKey();
        }
        return "msg:" + ctx.getMessageId();
    }
}
