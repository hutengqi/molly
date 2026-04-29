package cn.molly.mq.core;

/**
 * 消费处理结果
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public enum ConsumeResult {

    /**
     * 消费成功，Provider 侧提交位点 / ack
     */
    SUCCESS,

    /**
     * 需要重试：交由 RetryAdvice 执行退避，超过 maxRetries 投递至 DLQ
     */
    RETRY,

    /**
     * 放弃消息：直接投递至 DLQ，不再走重试队列
     */
    REJECT
}
