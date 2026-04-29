package cn.molly.mq.provider.rocketmq;

import cn.molly.mq.consumer.ListenerDispatcher;
import cn.molly.mq.consumer.MessageListenerEndpoint;
import cn.molly.mq.core.ConsumeContext;
import cn.molly.mq.core.ConsumeResult;
import cn.molly.mq.core.Message;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.MessageListener;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.consumer.MessageListenerContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RocketMQ 消费容器；封装 {@link DefaultMQPushConsumer}，将原生回调桥接到 {@link ListenerDispatcher}
 * <p>
 * 顺序：ordered=true 走 {@link MessageListenerOrderly}，按队列串行；
 * 并发：ordered=false 走 {@link MessageListenerConcurrently}，并发度受 consumer.concurrency 控制
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
public class RocketMQMessageListenerContainer implements MessageListenerContainer {

    private final MessageListenerEndpoint endpoint;
    private final MollyMqProperties properties;
    private final MessageConverter converter;
    private final ListenerDispatcher dispatcher;
    private final DefaultMQPushConsumer consumer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RocketMQMessageListenerContainer(MessageListenerEndpoint endpoint,
                                            MollyMqProperties properties,
                                            MessageConverter converter,
                                            ListenerDispatcher dispatcher) {
        this.endpoint = endpoint;
        this.properties = properties;
        this.converter = converter;
        this.dispatcher = dispatcher;
        this.consumer = buildConsumer();
    }

    private DefaultMQPushConsumer buildConsumer() {
        String group = StringUtils.hasText(endpoint.getGroup())
                ? endpoint.getGroup()
                : properties.getConsumer().getGroup();
        DefaultMQPushConsumer c = new DefaultMQPushConsumer(group);
        c.setNamesrvAddr(properties.getRocketmq().getNameServer());
        int concurrency = endpoint.getConcurrency() > 0
                ? endpoint.getConcurrency()
                : properties.getConsumer().getConcurrency();
        c.setConsumeThreadMin(Math.max(1, concurrency));
        c.setConsumeThreadMax(Math.max(1, concurrency));
        c.setPullBatchSize(properties.getConsumer().getBatchSize());
        // RocketMQ 本地重试次数；超过后由 dispatcher 决策是否投 DLQ
        c.setMaxReconsumeTimes(properties.getConsumer().getMaxRetries());
        try {
            c.subscribe(endpoint.getTopic(),
                    StringUtils.hasText(endpoint.getTag()) ? endpoint.getTag() : "*");
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ subscribe failed topic=" + endpoint.getTopic(), e);
        }
        if (endpoint.isOrdered()) {
            c.registerMessageListener((MessageListenerOrderly) this::consumeOrderly);
        } else {
            c.registerMessageListener((MessageListenerConcurrently) this::consumeConcurrently);
        }
        return c;
    }

    private ConsumeConcurrentlyStatus consumeConcurrently(List<MessageExt> msgs, ConsumeConcurrentlyContext ctx) {
        for (MessageExt ext : msgs) {
            ConsumeResult result = dispatchOne(ext);
            if (result == ConsumeResult.RETRY) {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private ConsumeOrderlyStatus consumeOrderly(List<MessageExt> msgs, ConsumeOrderlyContext ctx) {
        for (MessageExt ext : msgs) {
            ConsumeResult result = dispatchOne(ext);
            if (result == ConsumeResult.RETRY) {
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        }
        return ConsumeOrderlyStatus.SUCCESS;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ConsumeResult dispatchOne(MessageExt ext) {
        try {
            Object payload = endpoint.getPayloadType() == byte[].class
                    ? ext.getBody()
                    : converter.fromBytes(ext.getBody(), endpoint.getPayloadType());
            Message message = Message.builder()
                    .topic(ext.getTopic())
                    .tag(ext.getTags())
                    .bizKey(ext.getKeys())
                    .idempotencyKey(ext.getUserProperty("molly.mq.idempotency-key"))
                    .headers(ext.getProperties())
                    .payload(payload)
                    .build();
            ConsumeContext context = ConsumeContext.builder()
                    .messageId(ext.getMsgId())
                    .topic(ext.getTopic())
                    .partition(ext.getQueueId())
                    .offset(ext.getQueueOffset())
                    .deliveryCount(ext.getReconsumeTimes() + 1)
                    .brokerTimestamp(ext.getBornTimestamp())
                    .providerName("rocketmq")
                    .build();
            MessageListener listener = endpoint.getListener();
            return dispatcher.dispatch(listener, message, context, properties.getConsumer().getMaxRetries());
        } catch (Exception e) {
            log.error("[molly-mq][rocketmq] dispatch error msgId={}", ext.getMsgId(), e);
            return ConsumeResult.RETRY;
        }
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            try {
                consumer.start();
                log.info("[molly-mq][rocketmq] container started topic={} group={} ordered={}",
                        endpoint.getTopic(), endpoint.getGroup(), endpoint.isOrdered());
            } catch (Exception e) {
                running.set(false);
                throw new IllegalStateException("RocketMQ consumer 启动失败 topic=" + endpoint.getTopic(), e);
            }
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            consumer.shutdown();
            log.info("[molly-mq][rocketmq] container stopped topic={}", endpoint.getTopic());
        }
    }

    @Override
    public String topic() {
        return endpoint.getTopic();
    }

    @Override
    public String group() {
        return endpoint.getGroup();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
