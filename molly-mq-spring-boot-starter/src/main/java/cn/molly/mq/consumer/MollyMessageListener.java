package cn.molly.mq.consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消费监听器注解；标注在 {@link cn.molly.mq.core.MessageListener} 实现类上由 BPP 自动装配容器
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyMessageListener {

    /**
     * 订阅主题，支持占位符 {@code ${property}}
     */
    String topic();

    /**
     * 订阅标签过滤（RocketMQ tag，其它 Provider 忽略）；默认 * 表示不过滤
     */
    String tag() default "*";

    /**
     * 消费分组；为空时使用 {@code molly.mq.consumer.group}
     */
    String group() default "";

    /**
     * 消息体类型；默认 byte[]，设置具体 POJO 由 MessageConverter 反序列化
     */
    Class<?> payloadType() default byte[].class;

    /**
     * 是否顺序消费；true 时底层使用顺序消费容器（单分区单线程）
     */
    boolean ordered() default false;

    /**
     * 消费并发度；ordered=true 时被忽略
     */
    int concurrency() default -1;
}
