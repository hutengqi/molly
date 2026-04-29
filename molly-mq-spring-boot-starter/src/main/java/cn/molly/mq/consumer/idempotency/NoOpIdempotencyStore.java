package cn.molly.mq.consumer.idempotency;

/**
 * 关闭幂等校验；所有消息都视为首次
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class NoOpIdempotencyStore implements IdempotencyStore {

    @Override
    public boolean tryAcquire(String key) {
        return true;
    }

    @Override
    public void release(String key) {
        // no-op
    }
}
