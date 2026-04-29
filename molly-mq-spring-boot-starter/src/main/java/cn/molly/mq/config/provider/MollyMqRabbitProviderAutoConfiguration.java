package cn.molly.mq.config.provider;

import cn.molly.mq.config.MollyMqAutoConfiguration;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.provider.rabbit.RabbitProducerOperations;
import cn.molly.mq.reliability.OutboxPublisher;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ Provider 自动配置：装配 {@link ConnectionFactory} / {@link RabbitTemplate} / {@link RabbitProducerOperations}
 * <p>
 * 默认启用 publisher confirms + mandatory 提升可靠性
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@AutoConfiguration
@AutoConfigureAfter(MollyMqAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
@ConditionalOnProperty(prefix = "molly.mq", name = "provider", havingValue = "rabbit")
public class MollyMqRabbitProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConnectionFactory.class)
    public ConnectionFactory mollyMqRabbitConnectionFactory(MollyMqProperties properties) {
        MollyMqProperties.Rabbit rabbit = properties.getRabbit();
        CachingConnectionFactory cf = new CachingConnectionFactory(rabbit.getHost(), rabbit.getPort());
        cf.setUsername(rabbit.getUsername());
        cf.setPassword(rabbit.getPassword());
        cf.setVirtualHost(rabbit.getVirtualHost());
        if (rabbit.isPublisherConfirms()) {
            cf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        }
        cf.setPublisherReturns(rabbit.isMandatory());
        return cf;
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate mollyMqRabbitTemplate(ConnectionFactory cf, MollyMqProperties properties) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMandatory(properties.getRabbit().isMandatory());
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(ProducerOperations.class)
    public ProducerOperations mollyMqProducerOperations(RabbitTemplate template,
                                                        MessageConverter converter,
                                                        MollyMqProperties properties,
                                                        ObjectProvider<OutboxPublisher> outboxPublisherProvider) {
        return new RabbitProducerOperations(template, converter, properties,
                outboxPublisherProvider.getIfAvailable());
    }
}
