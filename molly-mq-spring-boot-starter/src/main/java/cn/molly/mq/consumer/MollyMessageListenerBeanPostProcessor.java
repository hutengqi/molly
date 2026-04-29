package cn.molly.mq.consumer;

import cn.molly.mq.core.MessageListener;
import cn.molly.mq.properties.MollyMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * 扫描 {@link MollyMessageListener} 注解的 {@link MessageListener} 实现，
 * 通过 {@link MessageListenerContainerFactory} 创建容器并注册到 {@link MessageListenerContainerRegistry}
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
@RequiredArgsConstructor
public class MollyMessageListenerBeanPostProcessor implements BeanPostProcessor {

    private final MessageListenerContainerFactory factory;
    private final MessageListenerContainerRegistry registry;
    private final MollyMqProperties properties;
    private final Environment environment;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof MessageListener<?> listener)) {
            return bean;
        }
        MollyMessageListener annotation = AnnotationUtils.findAnnotation(bean.getClass(), MollyMessageListener.class);
        if (annotation == null) {
            return bean;
        }
        String topic = environment.resolvePlaceholders(annotation.topic());
        String tag = environment.resolvePlaceholders(annotation.tag());
        String group = StringUtils.hasText(annotation.group())
                ? environment.resolvePlaceholders(annotation.group())
                : properties.getConsumer().getGroup();
        int concurrency = annotation.concurrency() > 0
                ? annotation.concurrency()
                : properties.getConsumer().getConcurrency();
        MessageListenerEndpoint endpoint = MessageListenerEndpoint.builder()
                .topic(topic)
                .tag(tag)
                .group(group)
                .payloadType(annotation.payloadType())
                .ordered(annotation.ordered())
                .concurrency(concurrency)
                .listener(listener)
                .build();
        MessageListenerContainer container = factory.create(endpoint);
        registry.register(container);
        log.info("[molly-mq] registered listener bean={} topic={} ordered={}",
                beanName, topic, annotation.ordered());
        return bean;
    }
}
