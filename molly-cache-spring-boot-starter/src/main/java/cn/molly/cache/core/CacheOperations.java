package cn.molly.cache.core;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 缓存底层操作 SPI。
 * <p>
 * 接口接受的是最终落盘 key（已包含前缀与命名空间），上层注解切面与
 * {@link CacheTemplate} 负责 key 拼接。默认 Redisson 实现位于
 * {@code cn.molly.cache.support.redis}，使用者可提供自定义实现以接入
 * Memcached、Tair 等其它底层存储。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
public interface CacheOperations {

    /**
     * 读取单 key 对应的值。
     *
     * @param key 最终缓存 key
     * @return 缓存值，未命中返回 {@code null}；若命中 {@link NullValue} 占位，原样返回该占位对象
     */
    Object get(String key);

    /**
     * 批量读取多个 key。
     *
     * @param keys key 集合
     * @return key -> value 映射，不包含未命中项
     */
    Map<String, Object> multiGet(Collection<String> keys);

    /**
     * 写入单 key。
     *
     * @param key   最终缓存 key
     * @param value 值，可为空值占位
     * @param ttl   过期时间，{@code null} 或非正数表示永不过期
     */
    void put(String key, Object value, Duration ttl);

    /**
     * 批量写入。
     *
     * @param entries key -> value 映射
     * @param ttl     统一过期时间
     */
    void multiPut(Map<String, ?> entries, Duration ttl);

    /**
     * 删除单 key。
     *
     * @param key 最终缓存 key
     * @return 是否删除成功
     */
    boolean evict(String key);

    /**
     * 批量删除。
     *
     * @param keys key 集合
     * @return 实际删除数量
     */
    long evict(Collection<String> keys);

    /**
     * 读取 Hash 中单个 field。
     *
     * @param key   Hash key
     * @param field 子 key
     * @return 对应值，未命中返回 {@code null}
     */
    Object hGet(String key, String field);

    /**
     * 批量读取 Hash 中多个 field。
     *
     * @param key    Hash key
     * @param fields field 集合
     * @return field -> value 映射
     */
    Map<String, Object> hMultiGet(String key, Collection<String> fields);

    /**
     * 读取 Hash 的全部 field。
     *
     * @param key Hash key
     * @return field -> value 映射
     */
    Map<String, Object> hGetAll(String key);

    /**
     * 写入 Hash 单个 field。
     *
     * @param key   Hash key
     * @param field 子 key
     * @param value 值
     * @param ttl   Hash 整体过期时间，{@code null} 表示沿用既有 TTL
     */
    void hPut(String key, String field, Object value, Duration ttl);

    /**
     * 批量写入 Hash 多个 field。
     *
     * @param key     Hash key
     * @param entries field -> value 映射
     * @param ttl     Hash 整体过期时间
     */
    void hPutAll(String key, Map<String, ?> entries, Duration ttl);

    /**
     * 按 Redis 风格 pattern 批量删除 key（如 {@code molly:user:*}）。
     * <p>
     * 实现一般基于 SCAN，谨慎在大数据量下使用。
     *
     * @param pattern Redis pattern
     * @return 实际删除数量
     */
    long evictByPattern(String pattern);

    /**
     * 删除 Hash 中指定 field。
     *
     * @param key    Hash key
     * @param fields field 集合
     * @return 实际删除数量
     */
    long hEvict(String key, Collection<String> fields);

    /**
     * 以分布式锁保护的方式执行给定动作。
     *
     * @param lockKey   锁 key
     * @param waitTime  最大等待时间
     * @param leaseTime 锁租期
     * @param action    受保护动作
     * @param <T>       返回类型
     * @return 动作返回值
     */
    <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> action);
}
