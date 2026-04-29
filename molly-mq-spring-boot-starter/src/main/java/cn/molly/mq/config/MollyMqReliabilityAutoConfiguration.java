package cn.molly.mq.config;

import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.properties.MollyMqProperties;
import cn.molly.mq.reliability.JdbcOutboxPublisher;
import cn.molly.mq.reliability.JdbcOutboxStore;
import cn.molly.mq.reliability.OutboxFlusher;
import cn.molly.mq.reliability.OutboxPublisher;
import cn.molly.mq.reliability.OutboxStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 可靠性自动配置：本地消息表 outbox
 * <p>
 * 激活条件：{@code molly.mq.reliability.outbox-enabled=true} 且 classpath 含 JdbcTemplate 且容器有 DataSource
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@AutoConfiguration
@AutoConfigureAfter(MollyMqAutoConfiguration.class)
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnProperty(prefix = "molly.mq.reliability", name = "outbox-enabled", havingValue = "true")
public class MollyMqReliabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JdbcTemplate mollyMqJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(OutboxStore.class)
    public OutboxStore mollyMqOutboxStore(JdbcTemplate jdbcTemplate, MollyMqProperties properties) {
        return new JdbcOutboxStore(jdbcTemplate, properties);
    }

    /**
     * Flusher 依赖 ProducerOperations，如未装配 Provider 则不开启
     */
    @Bean(destroyMethod = "stop")
    @ConditionalOnBean(ProducerOperations.class)
    @ConditionalOnMissingBean
    public OutboxFlusher mollyMqOutboxFlusher(OutboxStore store,
                                              ProducerOperations producer,
                                              MessageConverter converter,
                                              ObjectProvider<ObjectMapper> objectMapperProvider,
                                              MollyMqProperties properties) {
        OutboxFlusher flusher = new OutboxFlusher(store, producer, converter,
                objectMapperProvider.getIfAvailable(ObjectMapper::new), properties);
        flusher.start();
        return flusher;
    }

    @Bean
    @ConditionalOnBean({OutboxStore.class, OutboxFlusher.class})
    @ConditionalOnMissingBean(OutboxPublisher.class)
    public OutboxPublisher mollyMqOutboxPublisher(OutboxStore store,
                                                  MessageConverter converter,
                                                  ObjectProvider<ObjectMapper> objectMapperProvider,
                                                  OutboxFlusher flusher) {
        return new JdbcOutboxPublisher(store, converter,
                objectMapperProvider.getIfAvailable(ObjectMapper::new), flusher);
    }
}
