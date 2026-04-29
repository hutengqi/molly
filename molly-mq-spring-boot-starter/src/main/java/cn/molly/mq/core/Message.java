package cn.molly.mq.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一消息模型，屏蔽各 Provider 差异
 * <p>
 * 通过 shardingKey 表达分区顺序性意图；通过 deliveryTimeMs 表达延迟投递；
 * 通过 headers 透传业务标签与幂等键
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message<T> {

    /**
     * 业务主题
     */
    private String topic;

    /**
     * 业务标签；RocketMQ 原生 tag，其余 Provider 映射到 header
     */
    private String tag;

    /**
     * 业务唯一键；用于 Provider 侧去重检索（如 RocketMQ keys，Kafka header）
     */
    private String bizKey;

    /**
     * 顺序键；非空时走分区顺序路由，保证同 shardingKey 消息被同一分区/队列顺序消费
     */
    private String shardingKey;

    /**
     * 延迟投递绝对时间戳（毫秒），null 或 0 表示立即投递
     */
    private Long deliveryTimeMs;

    /**
     * 幂等键；消费端 IdempotencyStore 去重依据，未设置时退化使用 bizKey → messageId
     */
    private String idempotencyKey;

    /**
     * 透传头
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * 消息体，由 {@link MessageConverter} 负责序列化
     */
    private T payload;

    /**
     * 快速构建纯文本消息
     */
    public static <T> Message<T> of(String topic, T payload) {
        return Message.<T>builder().topic(topic).payload(payload).build();
    }

    /**
     * 快速构建顺序消息
     */
    public static <T> Message<T> ordered(String topic, String shardingKey, T payload) {
        return Message.<T>builder().topic(topic).shardingKey(shardingKey).payload(payload).build();
    }
}
