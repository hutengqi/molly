package cn.molly.mq.config;

import cn.molly.mq.consumer.ListenerDispatcher;
import cn.molly.mq.consumer.MessageListenerContainerFactory;
import cn.molly.mq.consumer.MessageListenerContainerRegistry;
import cn.molly.mq.consumer.MollyMessageListenerBeanPostProcessor;
import cn.molly.mq.consumer.dlq.DlqDispatcher;
import cn.molly.mq.consumer.idempotency.IdempotencyStore;
import cn.molly.mq.consumer.idempotency.LocalIdempotencyStore;
import cn.molly.mq.consumer.idempotency.NoOpIdempotencyStore;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.properties.MollyMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 消费者自动配置：装配幂等存储 / DLQ / 统一消费路由 / 容器注册表 / BeanPostProcessor
 * <p>
 * Factory 由各 Provider 模块按需注入；未提供时 BPP 不注册，避免 NPE
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(MollyMqAutoConfiguration.class)
@ConditionalOnProperty(prefix = "molly.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MollyMqConsumerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyStore mollyMqIdempotencyStore(MollyMqProperties properties) {
        String type = properties.getConsumer().getIdempotencyStore();
        if ("none".equalsIgnoreCase(type)) {
            return new NoOpIdempotencyStore();
        }
        // redis 等分布式存储由使用方自行提供 @Bean 覆盖
        return new LocalIdempotencyStore(properties.getConsumer().getIdempotencyTtl());
    }

    @Bean
    @ConditionalOnMissingBean
    public DlqDispatcher mollyMqDlqDispatcher(ProducerOperations producer, MollyMqProperties properties) {
        return new DlqDispatcher(producer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ListenerDispatcher mollyMqListenerDispatcher(IdempotencyStore idempotencyStore,
                                                         DlqDispatcher dlqDispatcher) {
        return new ListenerDispatcher(idempotencyStore, dlqDispatcher);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageListenerContainerRegistry mollyMqContainerRegistry() {
        return new MessageListenerContainerRegistry();
    }

    @Bean
    @ConditionalOnBean(MessageListenerContainerFactory.class)
    @ConditionalOnMissingBean
    public MollyMessageListenerBeanPostProcessor mollyMqListenerBpp(MessageListenerContainerFactory factory,
                                                                    MessageListenerContainerRegistry registry,
                                                                    MollyMqProperties properties,
                                                                    Environment environment) {
        return new MollyMessageListenerBeanPostProcessor(factory, registry, properties, environment);
    }
}
