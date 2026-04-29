package cn.molly.mq.provider.rocketmq;

import cn.molly.mq.consumer.ListenerDispatcher;
import cn.molly.mq.consumer.MessageListenerContainer;
import cn.molly.mq.consumer.MessageListenerContainerFactory;
import cn.molly.mq.consumer.MessageListenerEndpoint;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.properties.MollyMqProperties;
import lombok.RequiredArgsConstructor;

/**
 * RocketMQ {@link MessageListenerContainerFactory} 实现
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@RequiredArgsConstructor
public class RocketMQMessageListenerContainerFactory implements MessageListenerContainerFactory {

    private final MollyMqProperties properties;
    private final MessageConverter converter;
    private final ListenerDispatcher dispatcher;

    @Override
    public MessageListenerContainer create(MessageListenerEndpoint endpoint) {
        return new RocketMQMessageListenerContainer(endpoint, properties, converter, dispatcher);
    }

    @Override
    public String providerName() {
        return "rocketmq";
    }
}
