package cn.molly.mq.config.provider;

import cn.molly.mq.config.MollyMqAutoConfiguration;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.provider.pulsar.PulsarProducerOperations;
import cn.molly.mq.reliability.OutboxPublisher;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.util.StringUtils;

/**
 * Pulsar Provider 自动配置：装配 byte[] Schema 的 PulsarTemplate 与 {@link PulsarProducerOperations}
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@AutoConfiguration
@AutoConfigureAfter(MollyMqAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.pulsar.core.PulsarTemplate")
@ConditionalOnProperty(prefix = "molly.mq", name = "provider", havingValue = "pulsar")
public class MollyMqPulsarProviderAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public PulsarClient mollyMqPulsarClient(MollyMqProperties properties) throws PulsarClientException {
        MollyMqProperties.Pulsar pulsar = properties.getPulsar();
        org.apache.pulsar.client.api.ClientBuilder builder = PulsarClient.builder()
                .serviceUrl(pulsar.getServiceUrl());
        if (StringUtils.hasText(pulsar.getAuthToken())) {
            builder.authentication(
                    org.apache.pulsar.client.impl.auth.AuthenticationToken.class.getName(),
                    pulsar.getAuthToken());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public PulsarProducerFactory<byte[]> mollyMqPulsarProducerFactory(PulsarClient client) {
        return new DefaultPulsarProducerFactory<>(client);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mollyMqPulsarTemplate")
    public PulsarTemplate<byte[]> mollyMqPulsarTemplate(PulsarProducerFactory<byte[]> factory) {
        return new PulsarTemplate<>(factory);
    }

    @Bean
    @ConditionalOnMissingBean(ProducerOperations.class)
    public ProducerOperations mollyMqProducerOperations(PulsarTemplate<byte[]> template,
                                                        MessageConverter converter,
                                                        MollyMqProperties properties,
                                                        ObjectProvider<OutboxPublisher> outboxPublisherProvider) {
        return new PulsarProducerOperations(template, converter, properties,
                outboxPublisherProvider.getIfAvailable());
    }
}
