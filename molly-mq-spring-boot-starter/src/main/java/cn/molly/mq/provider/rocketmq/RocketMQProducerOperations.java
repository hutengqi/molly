package cn.molly.mq.provider.rocketmq;

import cn.molly.mq.core.Message;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.core.SendResult;
import cn.molly.mq.core.exception.MqSendException;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.reliability.OutboxPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.MessageQueue;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * RocketMQ 生产者实现
 * <p>
 * 顺序性：shardingKey 非空时走 {@link MessageQueueSelector} hash 路由到固定队列；
 * 延迟：RocketMQ 原生 delay level（若配置 deliveryTimeMs，转成 startDeliverTime 头传递）；
 * 事务：sendTransactional 写入本地消息表 outbox，提交后由 Flusher 真投递
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
public class RocketMQProducerOperations implements ProducerOperations {

    private final DefaultMQProducer producer;
    private final MessageConverter converter;
    private final MollyMqProperties properties;
    private final OutboxPublisher outboxPublisher;

    public RocketMQProducerOperations(DefaultMQProducer producer,
                                      MessageConverter converter,
                                      MollyMqProperties properties,
                                      OutboxPublisher outboxPublisher) {
        this.producer = producer;
        this.converter = converter;
        this.properties = properties;
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public <T> SendResult send(Message<T> message) {
        org.apache.rocketmq.common.message.Message rmq = toRocketMessage(message);
        try {
            org.apache.rocketmq.client.producer.SendResult result;
            if (isOrdered(message)) {
                result = producer.send(rmq, HASH_SELECTOR, message.getShardingKey());
            } else {
                result = producer.send(rmq);
            }
            return toSendResult(result);
        } catch (Exception e) {
            throw new MqSendException("RocketMQ 同步发送失败 topic=" + message.getTopic(), e);
        }
    }

    @Override
    public <T> CompletableFuture<SendResult> sendAsync(Message<T> message) {
        org.apache.rocketmq.common.message.Message rmq = toRocketMessage(message);
        CompletableFuture<SendResult> future = new CompletableFuture<>();
        SendCallback callback = new SendCallback() {
            @Override
            public void onSuccess(org.apache.rocketmq.client.producer.SendResult r) {
                future.complete(toSendResult(r));
            }

            @Override
            public void onException(Throwable e) {
                future.completeExceptionally(new MqSendException("RocketMQ 异步发送失败", e));
            }
        };
        try {
            if (isOrdered(message)) {
                producer.send(rmq, HASH_SELECTOR, message.getShardingKey(), callback);
            } else {
                producer.send(rmq, callback);
            }
        } catch (Exception e) {
            future.completeExceptionally(new MqSendException("RocketMQ 异步发送调度失败", e));
        }
        return future;
    }

    @Override
    public <T> void sendOneWay(Message<T> message) {
        org.apache.rocketmq.common.message.Message rmq = toRocketMessage(message);
        try {
            if (isOrdered(message)) {
                producer.sendOneway(rmq, HASH_SELECTOR, message.getShardingKey());
            } else {
                producer.sendOneway(rmq);
            }
        } catch (Exception e) {
            throw new MqSendException("RocketMQ oneway 发送失败", e);
        }
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
        return "rocketmq";
    }

    /* ======== 内部工具 ======== */

    private static final MessageQueueSelector HASH_SELECTOR = (mqs, msg, arg) -> {
        int idx = Math.floorMod(arg.hashCode(), mqs.size());
        return mqs.get(idx);
    };

    private boolean isOrdered(Message<?> msg) {
        return msg.getShardingKey() != null && !msg.getShardingKey().isEmpty();
    }

    private <T> org.apache.rocketmq.common.message.Message toRocketMessage(Message<T> message) {
        byte[] body = converter.toBytes(message.getPayload());
        if (body.length > properties.getProducer().getMaxMessageSize()) {
            throw new MqSendException("消息体超出上限 " + properties.getProducer().getMaxMessageSize());
        }
        org.apache.rocketmq.common.message.Message rmq =
                new org.apache.rocketmq.common.message.Message(message.getTopic(), body);
        if (message.getTag() != null) {
            rmq.setTags(message.getTag());
        }
        if (message.getBizKey() != null) {
            rmq.setKeys(message.getBizKey());
        }
        if (message.getDeliveryTimeMs() != null && message.getDeliveryTimeMs() > 0) {
            // RocketMQ 5.x 支持绝对时间延迟；4.x 仅支持 delayLevel。此处写入扩展属性，由 Broker 版本决定
            rmq.putUserProperty("__STARTDELIVERTIME", String.valueOf(message.getDeliveryTimeMs()));
        }
        if (message.getIdempotencyKey() != null) {
            rmq.putUserProperty("molly.mq.idempotency-key", message.getIdempotencyKey());
        }
        rmq.putUserProperty("molly.mq.content-type", converter.contentType());
        if (message.getHeaders() != null) {
            for (Map.Entry<String, String> entry : message.getHeaders().entrySet()) {
                rmq.putUserProperty(entry.getKey(), entry.getValue());
            }
        }
        return rmq;
    }

    private SendResult toSendResult(org.apache.rocketmq.client.producer.SendResult r) {
        SendResult.Status status = r.getSendStatus() == SendStatus.SEND_OK
                ? SendResult.Status.SUCCESS : SendResult.Status.FLUSH_PENDING;
        MessageQueue mq = r.getMessageQueue();
        return SendResult.builder()
                .messageId(r.getMsgId())
                .topic(mq != null ? mq.getTopic() : null)
                .partition(mq != null ? mq.getQueueId() : null)
                .offset(r.getQueueOffset())
                .timestamp(System.currentTimeMillis())
                .status(status)
                .build();
    }
}
