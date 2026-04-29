package cn.molly.mq.provider.rabbit;

import cn.molly.mq.core.Message;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.core.SendResult;
import cn.molly.mq.core.exception.MqSendException;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.reliability.OutboxPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageBuilderSupport;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * RabbitMQ 生产者实现
 * <p>
 * 顺序性：通过 routing-key 或 x-consistent-hash-exchange 达成分区一致性（由使用方部署决定，这里将 shardingKey 作为 routingKey 使用）；
 * 延迟：依赖 rabbitmq-delayed-message-exchange 插件，通过 x-delay header 传递；
 * 可靠性：依赖 publisher confirms + mandatory，由 RabbitTemplate 配置开启
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
public class RabbitProducerOperations implements ProducerOperations {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;
    private final MollyMqProperties properties;
    private final OutboxPublisher outboxPublisher;

    public RabbitProducerOperations(RabbitTemplate rabbitTemplate,
                                    MessageConverter converter,
                                    MollyMqProperties properties,
                                    OutboxPublisher outboxPublisher) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.properties = properties;
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public <T> SendResult send(Message<T> message) {
        try {
            org.springframework.amqp.core.Message amqp = toAmqp(message);
            // topic 用作 exchange 名称；routingKey 优先使用 shardingKey
            String routingKey = message.getShardingKey() != null ? message.getShardingKey()
                    : (message.getTag() != null ? message.getTag() : "");
            rabbitTemplate.send(message.getTopic(), routingKey, amqp);
            return SendResult.builder()
                    .messageId(amqp.getMessageProperties().getMessageId())
                    .topic(message.getTopic())
                    .timestamp(System.currentTimeMillis())
                    .status(SendResult.Status.SUCCESS)
                    .build();
        } catch (Exception e) {
            throw new MqSendException("RabbitMQ 同步发送失败 topic=" + message.getTopic(), e);
        }
    }

    @Override
    public <T> CompletableFuture<SendResult> sendAsync(Message<T> message) {
        return CompletableFuture.supplyAsync(() -> send(message));
    }

    @Override
    public <T> void sendOneWay(Message<T> message) {
        send(message);
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
        return "rabbit";
    }

    private <T> org.springframework.amqp.core.Message toAmqp(Message<T> message) {
        byte[] body = converter.toBytes(message.getPayload());
        if (body.length > properties.getProducer().getMaxMessageSize()) {
            throw new MqSendException("消息体超出上限");
        }
        MessageBuilderSupport<org.springframework.amqp.core.Message> builder = MessageBuilder.withBody(body)
                .setContentType(converter.contentType())
                .setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE);
        if (message.getBizKey() != null) {
            builder.setMessageId(message.getBizKey());
        }
        if (message.getTag() != null) {
            builder.setHeader("molly.mq.tag", message.getTag());
        }
        if (message.getIdempotencyKey() != null) {
            builder.setHeader("molly.mq.idempotency-key", message.getIdempotencyKey());
        }
        if (message.getDeliveryTimeMs() != null && message.getDeliveryTimeMs() > System.currentTimeMillis()) {
            long delay = message.getDeliveryTimeMs() - System.currentTimeMillis();
            // 依赖 rabbitmq-delayed-message-exchange 插件
            builder.setHeader("x-delay", (int) Math.min(delay, Integer.MAX_VALUE));
        }
        if (message.getHeaders() != null) {
            for (Map.Entry<String, String> h : message.getHeaders().entrySet()) {
                builder.setHeader(h.getKey(), h.getValue());
            }
        }
        return builder.build();
    }
}
