package cn.molly.mq.config.provider;

import cn.molly.mq.config.MollyMqAutoConfiguration;
import cn.molly.mq.consumer.ListenerDispatcher;
import cn.molly.mq.consumer.MessageListenerContainerFactory;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.provider.rocketmq.RocketMQMessageListenerContainerFactory;
import cn.molly.mq.provider.rocketmq.RocketMQProducerOperations;
import cn.molly.mq.reliability.OutboxPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * RocketMQ Provider 自动配置：装配 {@link DefaultMQProducer} 与 {@link RocketMQProducerOperations}
 * <p>
 * 激活条件：classpath 含 RocketMQ client 且 {@code molly.mq.provider=rocketmq}（默认）
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(MollyMqAutoConfiguration.class)
@ConditionalOnClass(name = "org.apache.rocketmq.client.producer.DefaultMQProducer")
@ConditionalOnProperty(prefix = "molly.mq", name = "provider", havingValue = "rocketmq", matchIfMissing = true)
public class MollyMqRocketProviderAutoConfiguration {

    /**
     * RocketMQ 原生生产者；通过 InitializingBean / DisposableBean 接口完成 start/shutdown
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public DefaultMQProducer mollyMqRocketProducer(MollyMqProperties properties) throws MQClientException {
        MollyMqProperties.RocketMq rocket = properties.getRocketmq();
        DefaultMQProducer producer;
        if (StringUtils.hasText(rocket.getNamespace())) {
            producer = new DefaultMQProducer(rocket.getNamespace(), properties.getProducer().getGroup());
        } else {
            producer = new DefaultMQProducer(properties.getProducer().getGroup());
        }
        producer.setNamesrvAddr(rocket.getNameServer());
        producer.setSendMsgTimeout((int) properties.getProducer().getSendTimeout().toMillis());
        producer.setRetryTimesWhenSendFailed(properties.getProducer().getRetryTimes());
        producer.setRetryTimesWhenSendAsyncFailed(properties.getProducer().getRetryTimes());
        producer.setMaxMessageSize(properties.getProducer().getMaxMessageSize());
        producer.setVipChannelEnabled(rocket.isVipChannelEnabled());
        log.info("[molly-mq] RocketMQ producer initialized, nameServer={}, group={}",
                rocket.getNameServer(), properties.getProducer().getGroup());
        return producer;
    }

    /**
     * 统一生产者实现
     */
    @Bean
    @ConditionalOnMissingBean(ProducerOperations.class)
    public ProducerOperations mollyMqProducerOperations(DefaultMQProducer producer,
                                                        MessageConverter converter,
                                                        MollyMqProperties properties,
                                                        ObjectProvider<OutboxPublisher> outboxPublisherProvider) {
        return new RocketMQProducerOperations(producer, converter, properties,
                outboxPublisherProvider.getIfAvailable());
    }

    /**
     * 消费容器工厂；供 {@link cn.molly.mq.consumer.MollyMessageListenerBeanPostProcessor} 使用
     */
    @Bean
    @ConditionalOnMissingBean(MessageListenerContainerFactory.class)
    public MessageListenerContainerFactory mollyMqRocketContainerFactory(MollyMqProperties properties,
                                                                         MessageConverter converter,
                                                                         ListenerDispatcher dispatcher) {
        return new RocketMQMessageListenerContainerFactory(properties, converter, dispatcher);
    }
}
