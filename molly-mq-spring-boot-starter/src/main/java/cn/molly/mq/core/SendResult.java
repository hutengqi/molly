package cn.molly.mq.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送结果抽象
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResult {

    /**
     * Provider 分配的消息唯一 id
     */
    private String messageId;

    /**
     * 最终投递主题
     */
    private String topic;

    /**
     * 分区/队列序号；RocketMQ 为 queueId，Kafka 为 partition，Pulsar/Rabbit 可为 null
     */
    private Integer partition;

    /**
     * 位点；Kafka offset、RocketMQ queueOffset，其它 Provider 可为 null
     */
    private Long offset;

    /**
     * Broker 记录时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 发送状态
     */
    private Status status;

    public enum Status {
        /** 成功并持久化 */
        SUCCESS,
        /** 成功写入但未刷盘 / 未同步从副本 */
        FLUSH_PENDING,
        /** 发送失败（已抛异常前填充，供回调使用） */
        FAILED
    }
}
