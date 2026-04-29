package cn.molly.mq.reliability;

import cn.molly.mq.core.Message;
import cn.molly.mq.core.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * OutboxPublisher 默认实现：写入本地消息表 + 事务提交后触发立即投递（由 {@link OutboxFlusher} 注册回调）
 * <p>
 * 要求调用方运行在 Spring 事务上下文，以便与业务事务一致性提交
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcOutboxPublisher implements OutboxPublisher {

    private final OutboxStore outboxStore;
    private final MessageConverter messageConverter;
    private final ObjectMapper objectMapper;
    private final OutboxFlusher outboxFlusher;

    @Override
    public <T> void stage(Message<T> message, String providerName) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("sendTransactional 必须运行在 @Transactional 业务事务内");
        }
        OutboxRecord record = OutboxRecord.builder()
                .provider(providerName)
                .topic(message.getTopic())
                .tag(message.getTag())
                .bizKey(message.getBizKey())
                .shardingKey(message.getShardingKey())
                .idempotencyKey(message.getIdempotencyKey())
                .headersJson(toJson(message.getHeaders()))
                .payload(messageConverter.toBytes(message.getPayload()))
                .deliveryTimeMs(message.getDeliveryTimeMs())
                .status(OutboxStatus.PENDING)
                .nextFireTimeMs(message.getDeliveryTimeMs() != null
                        ? message.getDeliveryTimeMs() : System.currentTimeMillis())
                .build();
        outboxStore.insert(record);
        // 提交后立刻触发一次 flush，不等定时器，降低端到端延迟
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                outboxFlusher.triggerImmediate(record.getId());
            }
        });
        log.debug("[molly-mq] outbox staged id={} topic={} provider={}", record.getId(), message.getTopic(), providerName);
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[molly-mq] outbox headers 序列化失败", e);
            return null;
        }
    }
}
