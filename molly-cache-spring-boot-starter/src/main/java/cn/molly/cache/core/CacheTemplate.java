package cn.molly.cache.core;

import cn.molly.cache.properties.MollyCacheProperties;
import cn.molly.cache.sync.TransactionAwareCacheFlusher;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 缓存操作门面。
 * <p>
 * 对业务层提供以 {@code name + key} 语义表达的便捷 API，同时负责：
 * <ul>
 *     <li>调用 {@link CacheKeyGenerator} 拼接最终 key</li>
 *     <li>对 TTL 叠加防雪崩抖动</li>
 *     <li>空值占位透传与还原</li>
 *     <li>提供 afterCommit 变体，将失效挂入事务提交后</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
public class CacheTemplate {

    private final CacheOperations operations;

    private final CacheKeyGenerator keyGenerator;

    private final MollyCacheProperties properties;

    private final TransactionAwareCacheFlusher flusher;

    /**
     * 构造缓存模板。
     *
     * @param operations   底层 SPI
     * @param keyGenerator key 生成器
     * @param properties   组件配置
     * @param flusher      事务同步执行器
     */
    public CacheTemplate(CacheOperations operations,
                         CacheKeyGenerator keyGenerator,
                         MollyCacheProperties properties,
                         TransactionAwareCacheFlusher flusher) {
        this.operations = operations;
        this.keyGenerator = keyGenerator;
        this.properties = properties;
        this.flusher = flusher;
    }

    // ================== 单 key ==================

    /**
     * 读取单 key 的原始值。
     *
     * @param name 命名空间
     * @param key  业务 key
     * @return 原始缓存值；未命中返回 {@code null}；命中空值占位返回 {@link NullValue} 实例
     */
    public Object get(String name, Object key) {
        return operations.get(keyGenerator.build(name, key));
    }

    /**
     * 写入单 key。
     *
     * @param name  命名空间
     * @param key   业务 key
     * @param value 值
     * @param ttl   过期时间，空则使用默认 TTL
     */
    public void put(String name, Object key, Object value, Duration ttl) {
        operations.put(keyGenerator.build(name, key), wrap(value), jitter(resolveTtl(ttl)));
    }

    /**
     * 写入单 key 的空值占位。
     *
     * @param name 命名空间
     * @param key  业务 key
     */
    public void putNull(String name, Object key) {
        if (!properties.getNullValue().isEnabled()) {
            return;
        }
        operations.put(keyGenerator.build(name, key), NullValue.INSTANCE,
                jitter(properties.getNullValue().getTtl()));
    }

    /**
     * 立即删除单 key。
     *
     * @param name 命名空间
     * @param key  业务 key
     */
    public void evict(String name, Object key) {
        operations.evict(keyGenerator.build(name, key));
    }

    /**
     * 清空整个命名空间下的所有 key（基于 pattern 匹配）。
     *
     * @param name 命名空间
     */
    public void evictAll(String name) {
        operations.evictByPattern(keyGenerator.build(name, null) + "*");
    }

    /**
     * 清空整个命名空间（事务提交后执行）。
     *
     * @param name 命名空间
     */
    public void evictAllAfterCommit(String name) {
        flusher.runAfterCommit(() -> evictAll(name));
    }

    /**
     * 将删除动作挂入事务提交后；未开启事务时退化为立即执行。
     *
     * @param name 命名空间
     * @param key  业务 key
     */
    public void evictAfterCommit(String name, Object key) {
        flusher.runAfterCommit(() -> evict(name, key));
    }

    // ================== 多 key ==================

    /**
     * 批量读取多 key。
     *
     * @param name 命名空间
     * @param keys 业务 key 集合
     * @return 业务 key -> 值 的映射（仅包含命中项，空值占位会被还原为 {@code null} 并保留在结果中）
     */
    public Map<Object, Object> multiGet(String name, Collection<?> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyMap();
        }
        Map<String, Object> fullKeyToBiz = new LinkedHashMap<>(keys.size());
        for (Object k : keys) {
            fullKeyToBiz.put(keyGenerator.build(name, k), null);
        }
        Map<String, Object> raw = operations.multiGet(fullKeyToBiz.keySet());
        Map<Object, Object> result = new LinkedHashMap<>(raw.size());
        for (Object k : keys) {
            String full = keyGenerator.build(name, k);
            if (raw.containsKey(full)) {
                result.put(k, unwrap(raw.get(full)));
            }
        }
        return result;
    }

    /**
     * 批量写入多 key。
     *
     * @param name    命名空间
     * @param entries 业务 key -> 值
     * @param ttl     TTL
     */
    public void multiPut(String name, Map<?, ?> entries, Duration ttl) {
        if (MapUtils.isEmpty(entries)) {
            return;
        }
        Map<String, Object> wrapped = new LinkedHashMap<>(entries.size());
        entries.forEach((k, v) -> wrapped.put(keyGenerator.build(name, k), wrap(v)));
        operations.multiPut(wrapped, jitter(resolveTtl(ttl)));
    }

    /**
     * 批量删除多 key。
     *
     * @param name 命名空间
     * @param keys 业务 key 集合
     */
    public void multiEvict(String name, Collection<?> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        LinkedHashMap<String, Object> fullKeys = new LinkedHashMap<>(keys.size());
        for (Object k : keys) {
            fullKeys.put(keyGenerator.build(name, k), null);
        }
        operations.evict(fullKeys.keySet());
    }

    /**
     * 批量删除多 key（事务提交后执行）。
     *
     * @param name 命名空间
     * @param keys 业务 key 集合
     */
    public void multiEvictAfterCommit(String name, Collection<?> keys) {
        flusher.runAfterCommit(() -> multiEvict(name, keys));
    }

    // ================== Hash ==================

    /**
     * 读取 Hash 中单个 field。
     *
     * @param name  命名空间
     * @param key   Hash 业务 key
     * @param field 子 key
     * @return 值
     */
    public Object hGet(String name, Object key, String field) {
        return operations.hGet(keyGenerator.build(name, key), field);
    }

    /**
     * 读取 Hash 中多个 field。
     *
     * @param name   命名空间
     * @param key    Hash 业务 key
     * @param fields 子 key 集合
     * @return field -> 值
     */
    public Map<String, Object> hMultiGet(String name, Object key, Collection<String> fields) {
        return operations.hMultiGet(keyGenerator.build(name, key), fields);
    }

    /**
     * 读取 Hash 的全部 field。
     *
     * @param name 命名空间
     * @param key  Hash 业务 key
     * @return field -> 值
     */
    public Map<String, Object> hGetAll(String name, Object key) {
        return operations.hGetAll(keyGenerator.build(name, key));
    }

    /**
     * 写入 Hash 单个 field。
     *
     * @param name  命名空间
     * @param key   Hash 业务 key
     * @param field 子 key
     * @param value 值
     * @param ttl   TTL
     */
    public void hPut(String name, Object key, String field, Object value, Duration ttl) {
        operations.hPut(keyGenerator.build(name, key), field, wrap(value), jitter(resolveTtl(ttl)));
    }

    /**
     * 批量写入 Hash。
     *
     * @param name    命名空间
     * @param key     Hash 业务 key
     * @param entries field -> 值
     * @param ttl     TTL
     */
    public void hPutAll(String name, Object key, Map<String, ?> entries, Duration ttl) {
        if (MapUtils.isEmpty(entries)) {
            return;
        }
        Map<String, Object> wrapped = new LinkedHashMap<>(entries.size());
        entries.forEach((f, v) -> wrapped.put(f, wrap(v)));
        operations.hPutAll(keyGenerator.build(name, key), wrapped, jitter(resolveTtl(ttl)));
    }

    /**
     * 删除 Hash 中指定 field。
     *
     * @param name   命名空间
     * @param key    Hash 业务 key
     * @param fields 子 key 集合
     */
    public void hEvict(String name, Object key, Collection<String> fields) {
        operations.hEvict(keyGenerator.build(name, key), fields);
    }

    /**
     * 删除 Hash 中指定 field（事务提交后执行）。
     *
     * @param name   命名空间
     * @param key    Hash 业务 key
     * @param fields 子 key 集合
     */
    public void hEvictAfterCommit(String name, Object key, Collection<String> fields) {
        flusher.runAfterCommit(() -> hEvict(name, key, fields));
    }

    /**
     * 删除整个 Hash key。
     *
     * @param name 命名空间
     * @param key  Hash 业务 key
     */
    public void hEvictAll(String name, Object key) {
        operations.evict(keyGenerator.build(name, key));
    }

    // ================== 锁 ==================

    /**
     * 以分布式锁保护执行操作。
     *
     * @param name      命名空间
     * @param key       锁粒度 key
     * @param waitTime  最大等待时间
     * @param leaseTime 锁租期
     * @param action    受保护动作
     * @param <T>       返回类型
     * @return 动作返回值
     */
    public <T> T executeWithLock(String name, Object key, Duration waitTime, Duration leaseTime, Supplier<T> action) {
        MollyCacheProperties.Lock lockCfg = properties.getLock();
        Duration wait = waitTime != null ? waitTime : lockCfg.getWaitTime();
        Duration lease = leaseTime != null ? leaseTime : lockCfg.getLeaseTime();
        String lockKey = keyGenerator.build(name, key) + ":lock";
        return operations.executeWithLock(lockKey, wait, lease, action);
    }

    // ================== 内部工具 ==================

    /**
     * 暴露底层 SPI，便于切面直接使用。
     *
     * @return 底层 SPI
     */
    public CacheOperations getOperations() {
        return operations;
    }

    /**
     * 暴露 key 生成器。
     *
     * @return key 生成器
     */
    public CacheKeyGenerator getKeyGenerator() {
        return keyGenerator;
    }

    /**
     * 暴露组件配置。
     *
     * @return 配置
     */
    public MollyCacheProperties getProperties() {
        return properties;
    }

    /**
     * 暴露事务同步执行器。
     *
     * @return 事务同步执行器
     */
    public TransactionAwareCacheFlusher getFlusher() {
        return flusher;
    }

    /**
     * 为原始 TTL 叠加抖动。
     *
     * @param ttl 原始 TTL
     * @return 抖动后的 TTL
     */
    public Duration jitter(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return ttl;
        }
        double jitter = properties.getTtlJitter();
        if (jitter <= 0) {
            return ttl;
        }
        double factor = 1.0 + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * jitter;
        long millis = Math.max(1L, (long) (ttl.toMillis() * factor));
        return Duration.ofMillis(millis);
    }

    /**
     * 解析最终 TTL，空值回退到默认 TTL。
     *
     * @param ttl 注解上指定的 TTL
     * @return 实际 TTL
     */
    public Duration resolveTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return properties.getDefaultTtl();
        }
        return ttl;
    }

    /**
     * 将业务 {@code null} 值包装为占位对象（若启用）。
     *
     * @param value 原始值
     * @return 包装后的值
     */
    public Object wrap(Object value) {
        if (value == null && properties.getNullValue().isEnabled()) {
            return NullValue.INSTANCE;
        }
        return value;
    }

    /**
     * 将占位对象还原为 {@code null}。
     *
     * @param value 原始值
     * @return 还原后的值
     */
    public Object unwrap(Object value) {
        return NullValue.isNull(value) ? null : value;
    }
}
