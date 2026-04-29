package cn.molly.mq.reliability;

/**
 * 本地消息表条目状态机
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public enum OutboxStatus {

    /** 待投递 */
    PENDING,

    /** 已成功投递到 Broker */
    SENT,

    /** 临时失败，等待下次扫描 */
    FAILED,

    /** 超过最大重试次数，需人工介入 */
    DEAD
}
