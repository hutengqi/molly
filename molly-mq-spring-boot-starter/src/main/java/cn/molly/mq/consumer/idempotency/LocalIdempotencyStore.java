package cn.molly.mq.consumer.idempotency;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

/**
 * Caffeine 本地幂等存储；适合单机 / 低可靠场景（重启失效，生产建议接 Redis 实现）
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class LocalIdempotencyStore implements IdempotencyStore {

    private static final Object MARKER = new Object();
    private final Cache<String, Object> cache;

    public LocalIdempotencyStore(Duration ttl) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(1_000_000)
                .build();
    }

    @Override
    public boolean tryAcquire(String key) {
        if (key == null || key.isEmpty()) {
            return true;
        }
        boolean[] firstTime = {false};
        cache.get(key, k -> {
            firstTime[0] = true;
            return MARKER;
        });
        return firstTime[0];
    }

    @Override
    public void release(String key) {
        if (key != null) {
            cache.invalidate(key);
        }
    }
}
