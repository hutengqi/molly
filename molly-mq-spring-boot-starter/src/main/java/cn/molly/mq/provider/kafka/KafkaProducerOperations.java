package cn.molly.mq.provider.kafka;

import cn.molly.mq.core.Message;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.core.SendResult;
import cn.molly.mq.core.exception.MqSendException;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.reliability.OutboxPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 生产者实现
 * <p>
 * 顺序性：shardingKey → ProducerRecord.key，Kafka 默认按 key.hashCode 路由到固定分区；
 * 延迟：Kafka 原生不支持消息级延迟，此处通过 header 写入，由消费端自行实现（或交由 outbox 定时投递）
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
public class KafkaProducerOperations implements ProducerOperations {

    private final KafkaTemplate<byte[], byte[]> kafkaTemplate;
    private final MessageConverter converter;
    private final MollyMqProperties properties;
    private final OutboxPublisher outboxPublisher;

    public KafkaProducerOperations(KafkaTemplate<byte[], byte[]> kafkaTemplate,
                                   MessageConverter converter,
                                   MollyMqProperties properties,
                                   OutboxPublisher outboxPublisher) {
        this.kafkaTemplate = kafkaTemplate;
        this.converter = converter;
        this.properties = properties;
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public <T> SendResult send(Message<T> message) {
        try {
            org.springframework.kafka.support.SendResult<byte[], byte[]> r =
                    kafkaTemplate.send(toRecord(message))
                            .get(properties.getProducer().getSendTimeout().toMillis(),
                                    java.util.concurrent.TimeUnit.MILLISECONDS);
            return toSendResult(r.getRecordMetadata());
        } catch (Exception e) {
            throw new MqSendException("Kafka 同步发送失败 topic=" + message.getTopic(), e);
        }
    }

    @Override
    public <T> CompletableFuture<SendResult> sendAsync(Message<T> message) {
        return kafkaTemplate.send(toRecord(message))
                .thenApply(r -> toSendResult(r.getRecordMetadata()));
    }

    @Override
    public <T> void sendOneWay(Message<T> message) {
        // Kafka 没有 fire-and-forget API；使用异步发送并忽略结果
        kafkaTemplate.send(toRecord(message));
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
        return "kafka";
    }

    private <T> ProducerRecord<byte[], byte[]> toRecord(Message<T> message) {
        byte[] body = converter.toBytes(message.getPayload());
        if (body.length > properties.getProducer().getMaxMessageSize()) {
            throw new MqSendException("消息体超出上限");
        }
        byte[] key = message.getShardingKey() != null
                ? message.getShardingKey().getBytes(StandardCharsets.UTF_8)
                : (message.getBizKey() != null ? message.getBizKey().getBytes(StandardCharsets.UTF_8) : null);
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(message.getTopic(), null, key, body);
        if (message.getTag() != null) {
            record.headers().add(new RecordHeader("molly.mq.tag", message.getTag().getBytes(StandardCharsets.UTF_8)));
        }
        if (message.getIdempotencyKey() != null) {
            record.headers().add(new RecordHeader("molly.mq.idempotency-key",
                    message.getIdempotencyKey().getBytes(StandardCharsets.UTF_8)));
        }
        if (message.getDeliveryTimeMs() != null) {
            record.headers().add(new RecordHeader("molly.mq.delivery-time",
                    String.valueOf(message.getDeliveryTimeMs()).getBytes(StandardCharsets.UTF_8)));
        }
        record.headers().add(new RecordHeader("molly.mq.content-type",
                converter.contentType().getBytes(StandardCharsets.UTF_8)));
        if (message.getHeaders() != null) {
            for (Map.Entry<String, String> h : message.getHeaders().entrySet()) {
                record.headers().add(new RecordHeader(h.getKey(), h.getValue().getBytes(StandardCharsets.UTF_8)));
            }
        }
        return record;
    }

    private SendResult toSendResult(RecordMetadata md) {
        return SendResult.builder()
                .messageId(md.topic() + "-" + md.partition() + "-" + md.offset())
                .topic(md.topic())
                .partition(md.partition())
                .offset(md.offset())
                .timestamp(md.timestamp())
                .status(SendResult.Status.SUCCESS)
                .build();
    }
}
