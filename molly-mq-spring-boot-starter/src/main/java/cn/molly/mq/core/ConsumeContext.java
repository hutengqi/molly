package cn.molly.mq.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消费上下文，封装 Provider 交付时的元信息
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeContext {

    /**
     * Provider 分配的消息唯一 id
     */
    private String messageId;

    /**
     * 业务主题
     */
    private String topic;

    /**
     * 投递分区 / 队列
     */
    private Integer partition;

    /**
     * 位点
     */
    private Long offset;

    /**
     * 当前投递次数（首次=1）
     */
    private int deliveryCount;

    /**
     * Broker 时间戳
     */
    private Long brokerTimestamp;

    /**
     * 当前激活的 Provider 名称
     */
    private String providerName;
}
