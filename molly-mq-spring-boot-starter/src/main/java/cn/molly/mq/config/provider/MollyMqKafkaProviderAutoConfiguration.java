package cn.molly.mq.config.provider;

import cn.molly.mq.config.MollyMqAutoConfiguration;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.provider.kafka.KafkaProducerOperations;
import cn.molly.mq.reliability.OutboxPublisher;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Provider 自动配置：装配 byte[] 模式的 KafkaTemplate 与 {@link KafkaProducerOperations}
 * <p>
 * 默认启用 acks=all + enable.idempotence 保证至少一次 + 幂等写入
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@AutoConfiguration
@AutoConfigureAfter(MollyMqAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
@ConditionalOnProperty(prefix = "molly.mq", name = "provider", havingValue = "kafka")
public class MollyMqKafkaProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<byte[], byte[]> mollyMqKafkaProducerFactory(MollyMqProperties properties) {
        MollyMqProperties.Kafka kafka = properties.getKafka();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        cfg.put(ProducerConfig.ACKS_CONFIG, kafka.getAcks());
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, kafka.isIdempotenceEnabled());
        cfg.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getProducer().getGroup());
        cfg.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, properties.getProducer().getMaxMessageSize());
        cfg.putAll(kafka.getExtra());
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mollyMqKafkaTemplate")
    public KafkaTemplate<byte[], byte[]> mollyMqKafkaTemplate(ProducerFactory<byte[], byte[]> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    @ConditionalOnMissingBean(ProducerOperations.class)
    public ProducerOperations mollyMqProducerOperations(KafkaTemplate<byte[], byte[]> template,
                                                        MessageConverter converter,
                                                        MollyMqProperties properties,
                                                        ObjectProvider<OutboxPublisher> outboxPublisherProvider) {
        return new KafkaProducerOperations(template, converter, properties,
                outboxPublisherProvider.getIfAvailable());
    }
}
