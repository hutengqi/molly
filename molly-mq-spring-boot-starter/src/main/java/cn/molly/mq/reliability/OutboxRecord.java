package cn.molly.mq.reliability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 本地消息表条目
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxRecord {

    /** 主键 */
    private String id;

    /** 目标 Provider */
    private String provider;

    /** 业务主题 */
    private String topic;

    /** 业务标签 */
    private String tag;

    /** 业务唯一键 */
    private String bizKey;

    /** 顺序键 */
    private String shardingKey;

    /** 幂等键 */
    private String idempotencyKey;

    /** 消息头 JSON */
    private String headersJson;

    /** 消息体（已序列化） */
    private byte[] payload;

    /** 延迟投递绝对时间戳（毫秒） */
    private Long deliveryTimeMs;

    /** 状态 */
    private OutboxStatus status;

    /** 已尝试次数 */
    private int attempts;

    /** 最近一次错误 */
    private String lastError;

    /** 下次触发时间（毫秒，用于重试退避） */
    private long nextFireTimeMs;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
