package cn.molly.mq.consumer.idempotency;

/**
 * 幂等存储 SPI；消费端依据 idempotencyKey 判断是否重复
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public interface IdempotencyStore {

    /**
     * 尝试占坑：true 表示首次，返回 false 代表重复消息应直接跳过
     */
    boolean tryAcquire(String key);

    /**
     * 回滚占坑；消费失败且应重投时调用，避免锁死
     */
    void release(String key);
}
