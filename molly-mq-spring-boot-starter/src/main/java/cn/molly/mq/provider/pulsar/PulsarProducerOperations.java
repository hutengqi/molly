package cn.molly.mq.provider.pulsar;

import cn.molly.mq.core.Message;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.core.SendResult;
import cn.molly.mq.core.exception.MqSendException;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.reliability.OutboxPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Schema;
import org.springframework.pulsar.core.PulsarTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Pulsar 生产者实现
 * <p>
 * 顺序性：shardingKey → TypedMessageBuilder.key + Key_Shared 订阅模式；
 * 延迟：deliverAt 绝对时间戳
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
public class PulsarProducerOperations implements ProducerOperations {

    private final PulsarTemplate<byte[]> pulsarTemplate;
    private final MessageConverter converter;
    private final MollyMqProperties properties;
    private final OutboxPublisher outboxPublisher;

    public PulsarProducerOperations(PulsarTemplate<byte[]> pulsarTemplate,
                                    MessageConverter converter,
                                    MollyMqProperties properties,
                                    OutboxPublisher outboxPublisher) {
        this.pulsarTemplate = pulsarTemplate;
        this.converter = converter;
        this.properties = properties;
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public <T> SendResult send(Message<T> message) {
        try {
            MessageId id = pulsarTemplate.newMessage(converter.toBytes(message.getPayload()))
                    .withTopic(message.getTopic())
                    .withSchema(Schema.BYTES)
                    .withMessageCustomizer(tmb -> applyCustomizations(tmb, message))
                    .send();
            return SendResult.builder()
                    .messageId(id.toString())
                    .topic(message.getTopic())
                    .timestamp(System.currentTimeMillis())
                    .status(SendResult.Status.SUCCESS)
                    .build();
        } catch (Exception e) {
            throw new MqSendException("Pulsar 同步发送失败 topic=" + message.getTopic(), e);
        }
    }

    @Override
    public <T> CompletableFuture<SendResult> sendAsync(Message<T> message) {
        return pulsarTemplate.newMessage(converter.toBytes(message.getPayload()))
                .withTopic(message.getTopic())
                .withSchema(Schema.BYTES)
                .withMessageCustomizer(tmb -> applyCustomizations(tmb, message))
                .sendAsync()
                .thenApply(id -> SendResult.builder()
                        .messageId(id.toString())
                        .topic(message.getTopic())
                        .timestamp(System.currentTimeMillis())
                        .status(SendResult.Status.SUCCESS)
                        .build());
    }

    @Override
    public <T> void sendOneWay(Message<T> message) {
        sendAsync(message);
    }

    @Override
    public <T> void sendTransactional(Message<T> message) {
        if (outboxPublisher == null) {
            throw new IllegalStateException("事务化发送需要启用 molly.mq.reliability.outbox-enabled=true");
        }
        outboxPublisher.stage(message, providerName());
    }

    @Override
    public String providerName() {
        return "pulsar";
    }

    private <T> void applyCustomizations(org.apache.pulsar.client.api.TypedMessageBuilder<byte[]> tmb,
                                         Message<T> message) {
        if (message.getShardingKey() != null) {
            tmb.key(message.getShardingKey());
        } else if (message.getBizKey() != null) {
            tmb.key(message.getBizKey());
        }
        if (message.getTag() != null) {
            tmb.property("molly.mq.tag", message.getTag());
        }
        if (message.getIdempotencyKey() != null) {
            tmb.property("molly.mq.idempotency-key", message.getIdempotencyKey());
        }
        if (message.getDeliveryTimeMs() != null && message.getDeliveryTimeMs() > System.currentTimeMillis()) {
            tmb.deliverAt(message.getDeliveryTimeMs());
        }
        tmb.property("molly.mq.content-type", converter.contentType());
        if (message.getHeaders() != null) {
            for (Map.Entry<String, String> h : message.getHeaders().entrySet()) {
                tmb.property(h.getKey(), h.getValue());
            }
        }
    }
}
