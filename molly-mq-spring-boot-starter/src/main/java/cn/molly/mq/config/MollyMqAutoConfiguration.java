package cn.molly.mq.config;

import cn.molly.mq.core.JacksonMessageConverter;
import cn.molly.mq.core.MessageConverter;
import cn.molly.mq.properties.MollyMqProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Molly MQ 主自动配置，装配通用组件（MessageConverter / Properties）
 * <p>
 * 各 Provider 的具体生产/消费实现由独立的 Provider AutoConfiguration 按条件装配
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@AutoConfiguration
@EnableConfigurationProperties(MollyMqProperties.class)
@ConditionalOnProperty(prefix = "molly.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MollyMqAutoConfiguration {

    /**
     * 默认 Jackson 消息转换器；优先复用容器中的 ObjectMapper，缺省时 new 一个
     */
    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter mollyMqMessageConverter(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JacksonMessageConverter(objectMapper);
    }
}
