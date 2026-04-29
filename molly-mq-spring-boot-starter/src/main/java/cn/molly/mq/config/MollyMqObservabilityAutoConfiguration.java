package cn.molly.mq.config;

import cn.molly.mq.consumer.ListenerDispatcher;
import cn.molly.mq.consumer.MessageListenerContainerRegistry;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.observability.MollyMqEndpoint;
import cn.molly.mq.observability.MollyMqHealthIndicator;
import cn.molly.mq.observability.MollyMqMetrics;
import cn.molly.mq.properties.MollyMqProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 可观测自动配置：装配 Micrometer 指标、HealthIndicator、/actuator/mollymq 端点
 * <p>
 * Micrometer / Actuator 类缺失时整组装配跳过，避免非 web 场景报错
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter({MollyMqAutoConfiguration.class, MollyMqConsumerAutoConfiguration.class})
@ConditionalOnProperty(prefix = "molly.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MollyMqObservabilityAutoConfiguration {

    /**
     * Micrometer 指标封装；MeterRegistry 不存在时传 null，内部空操作
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnMissingBean
    public MollyMqMetrics mollyMqMetrics(ObjectProvider<MeterRegistry> registryProvider,
                                         MollyMqProperties properties) {
        MeterRegistry registry = registryProvider.getIfAvailable();
        return new MollyMqMetrics(registry, properties.getObservability().getMetricPrefix());
    }

    /**
     * 将 Metrics 注入到 ListenerDispatcher，完成埋点闭环
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    public ListenerDispatcherMetricsBinder mollyMqMetricsBinder(ListenerDispatcher dispatcher,
                                                                 ObjectProvider<MollyMqMetrics> metricsProvider) {
        return new ListenerDispatcherMetricsBinder(dispatcher, metricsProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @ConditionalOnMissingBean(name = "mollyMqHealthIndicator")
    public MollyMqHealthIndicator mollyMqHealthIndicator(ObjectProvider<ProducerOperations> producerProvider,
                                                         ObjectProvider<MessageListenerContainerRegistry> registryProvider) {
        return new MollyMqHealthIndicator(producerProvider.getIfAvailable(), registryProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnAvailableEndpoint
    @ConditionalOnProperty(prefix = "molly.mq.observability", name = "endpoint-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MollyMqEndpoint mollyMqEndpoint(ObjectProvider<ProducerOperations> producerProvider,
                                           ObjectProvider<MessageListenerContainerRegistry> registryProvider,
                                           MollyMqProperties properties) {
        return new MollyMqEndpoint(producerProvider.getIfAvailable(),
                registryProvider.getIfAvailable(), properties);
    }

    /**
     * 内部小组件：构造时将 metrics 注入 dispatcher
     */
    public static class ListenerDispatcherMetricsBinder {
        public ListenerDispatcherMetricsBinder(ListenerDispatcher dispatcher, MollyMqMetrics metrics) {
            if (dispatcher != null && metrics != null) {
                dispatcher.setMetrics(metrics);
            }
        }
    }
}
