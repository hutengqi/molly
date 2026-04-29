package cn.molly.mq.consumer.retry;

import cn.molly.mq.properties.MollyMqProperties;

/**
 * 消费端重试退避策略；指数退避封顶
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class ExponentialBackoffRetryPolicy {

    private final long initialMs;
    private final long maxMs;
    private final int maxRetries;

    public ExponentialBackoffRetryPolicy(MollyMqProperties.Consumer consumer) {
        this.initialMs = consumer.getRetryInitialBackoff().toMillis();
        this.maxMs = consumer.getRetryMaxBackoff().toMillis();
        this.maxRetries = consumer.getMaxRetries();
    }

    /**
     * 是否应当继续重试
     */
    public boolean shouldRetry(int deliveryCount) {
        return deliveryCount <= maxRetries;
    }

    /**
     * 计算下次投递前应延迟的毫秒数
     */
    public long nextBackoffMs(int deliveryCount) {
        long backoff = initialMs * (1L << Math.min(deliveryCount, 20));
        return Math.min(backoff, maxMs);
    }
}
