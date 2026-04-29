package cn.molly.mq.provider;

/**
 * 支持的消息队列 Provider 枚举
 * <p>
 * 通过 {@code molly.mq.provider} 属性二选一启用，保证单进程内仅有一个 Provider 装配
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public enum MqProvider {

    /**
     * RocketMQ（默认）
     */
    ROCKETMQ,

    /**
     * Apache Kafka
     */
    KAFKA,

    /**
     * Apache Pulsar
     */
    PULSAR,

    /**
     * RabbitMQ
     */
    RABBIT
}
