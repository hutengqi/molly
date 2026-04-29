package cn.molly.mq.consumer.dlq;

import cn.molly.mq.core.ConsumeContext;
import cn.molly.mq.core.Message;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.properties.MollyMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 死信转发器：将超过最大重试次数的消息投递至 {@code topic + dlqSuffix} 主题
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
@RequiredArgsConstructor
public class DlqDispatcher {

    private final ProducerOperations producer;
    private final MollyMqProperties properties;

    public <T> void dispatch(Message<T> original, ConsumeContext ctx, Throwable cause) {
        String dlqTopic = original.getTopic() + properties.getConsumer().getDlqSuffix();
        original.getHeaders().put("molly.mq.dlq.origin-topic", original.getTopic());
        original.getHeaders().put("molly.mq.dlq.delivery-count", String.valueOf(ctx.getDeliveryCount()));
        if (cause != null && cause.getMessage() != null) {
            String reason = cause.getMessage();
            original.getHeaders().put("molly.mq.dlq.reason",
                    reason.length() > 512 ? reason.substring(0, 512) : reason);
        }
        original.setTopic(dlqTopic);
        try {
            producer.send(original);
            log.warn("[molly-mq] DLQ dispatched messageId={} topic={}", ctx.getMessageId(), dlqTopic);
        } catch (Exception e) {
            log.error("[molly-mq] DLQ dispatch FAILED messageId={} topic={}", ctx.getMessageId(), dlqTopic, e);
            // 投递 DLQ 都失败时不再抛出，避免无限递归；日志即为最终保底
        }
    }
}
