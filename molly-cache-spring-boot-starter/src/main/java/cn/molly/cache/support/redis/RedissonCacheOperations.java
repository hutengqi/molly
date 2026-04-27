package cn.molly.cache.support.redis;

import cn.molly.cache.core.CacheException;
import cn.molly.cache.core.CacheOperations;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.redisson.api.RBatch;
import org.redisson.api.RBucketAsync;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redisson 的 {@link CacheOperations} 默认实现。
 * <p>
 * 单 key 使用 {@code RBucket}；Hash 使用 {@code RMap}；批量操作使用 {@code RBatch}
 * 在单次客户端交互中完成以保证高吞吐与一致性；锁使用 {@code RLock} 的可中断 tryLock。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
public class RedissonCacheOperations implements CacheOperations {

    private final RedissonClient redissonClient;

    /**
     * 构造 Redisson 缓存操作实现。
     *
     * @param redissonClient Redisson 客户端
     */
    public RedissonCacheOperations(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Object get(String key) {
        return redissonClient.getBucket(key).get();
    }

    @Override
    public Map<String, Object> multiGet(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyMap();
        }
        RBatch batch = redissonClient.createBatch();
        Map<String, RFuture<Object>> futures = new LinkedHashMap<>(keys.size());
        for (String key : keys) {
            RBucketAsync<Object> bucket = batch.getBucket(key);
            futures.put(key, bucket.getAsync());
        }
        batch.execute();
        Map<String, Object> result = new LinkedHashMap<>(keys.size());
        futures.forEach((k, f) -> {
            try {
                Object v = f.toCompletableFuture().get();
                if (v != null) {
                    result.put(k, v);
                }
            } catch (Exception e) {
                throw new CacheException("批量读取缓存失败: " + k, e);
            }
        });
        return result;
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (isPositive(ttl)) {
            redissonClient.getBucket(key).set(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            redissonClient.getBucket(key).set(value);
        }
    }

    @Override
    public void multiPut(Map<String, ?> entries, Duration ttl) {
        if (MapUtils.isEmpty(entries)) {
            return;
        }
        RBatch batch = redissonClient.createBatch();
        boolean hasTtl = isPositive(ttl);
        long millis = hasTtl ? ttl.toMillis() : 0L;
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            RBucketAsync<Object> bucket = batch.getBucket(entry.getKey());
            if (hasTtl) {
                bucket.setAsync(entry.getValue(), millis, TimeUnit.MILLISECONDS);
            } else {
                bucket.setAsync(entry.getValue());
            }
        }
        batch.execute();
    }

    @Override
    public boolean evict(String key) {
        return redissonClient.getBucket(key).delete();
    }

    @Override
    public long evict(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }
        return redissonClient.getKeys().delete(keys.toArray(new String[0]));
    }

    @Override
    public long evictByPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return 0L;
        }
        return redissonClient.getKeys().deleteByPattern(pattern);
    }

    @Override
    public Object hGet(String key, String field) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.get(field);
    }

    @Override
    public Map<String, Object> hMultiGet(String key, Collection<String> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return Collections.emptyMap();
        }
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.getAll(new java.util.HashSet<>(fields));
    }

    @Override
    public Map<String, Object> hGetAll(String key) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.readAllMap();
    }

    @Override
    public void hPut(String key, String field, Object value, Duration ttl) {
        RMap<String, Object> map = redissonClient.getMap(key);
        map.fastPut(field, value);
        applyTtl(map, ttl);
    }

    @Override
    public void hPutAll(String key, Map<String, ?> entries, Duration ttl) {
        if (MapUtils.isEmpty(entries)) {
            return;
        }
        RMap<String, Object> map = redissonClient.getMap(key);
        map.putAll(entries);
        applyTtl(map, ttl);
    }

    @Override
    public long hEvict(String key, Collection<String> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return 0L;
        }
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.fastRemove(fields.toArray(new String[0]));
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        long waitMs = waitTime != null ? waitTime.toMillis() : -1L;
        long leaseMs = leaseTime != null ? leaseTime.toMillis() : -1L;
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitMs, leaseMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new CacheException("获取分布式锁失败: " + lockKey);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CacheException("获取分布式锁被中断: " + lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 对 Hash key 应用过期时间（仅当 ttl 为正数）。
     *
     * @param map Hash
     * @param ttl 过期时间
     */
    private void applyTtl(RMap<?, ?> map, Duration ttl) {
        if (isPositive(ttl)) {
            map.expire(ttl);
        }
    }

    /**
     * 判断 Duration 是否为正数。
     *
     * @param ttl Duration
     * @return true 表示正数
     */
    private boolean isPositive(Duration ttl) {
        return ttl != null && !ttl.isZero() && !ttl.isNegative();
    }
}
