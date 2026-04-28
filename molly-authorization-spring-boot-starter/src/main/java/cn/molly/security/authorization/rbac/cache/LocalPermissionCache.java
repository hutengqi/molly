package cn.molly.security.authorization.rbac.cache;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * 进程内 TTL 权限缓存。
 * <p>
 * 采用 {@link ConcurrentHashMap} + 条目级过期时间戳实现，读写均摊 O(1)。
 * 仅适合单实例部署；多实例场景下请切换到基于 {@code molly-cache} 的分布式实现。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class LocalPermissionCache implements PermissionCache {

    /**
     * 缓存条目，包含权限集合与过期时间戳（毫秒）。
     */
    private record Entry(Set<String> value, long expireAt) {
    }

    private final ConcurrentMap<String, Entry> store = new ConcurrentHashMap<>();

    private final long ttlMillis;

    /**
     * 构造本地缓存。
     *
     * @param ttl 条目存活时间；为 null 或非正值时使用 5 分钟默认值
     */
    public LocalPermissionCache(Duration ttl) {
        this.ttlMillis = (ttl == null || ttl.isZero() || ttl.isNegative())
                ? Duration.ofMinutes(5).toMillis()
                : ttl.toMillis();
    }

    @Override
    public Set<String> getOrLoad(String principal, Supplier<Set<String>> loader) {
        long now = System.currentTimeMillis();
        Entry entry = store.get(principal);
        if (entry != null && entry.expireAt > now) {
            return entry.value;
        }
        Set<String> loaded = loader.get();
        store.put(principal, new Entry(loaded, now + ttlMillis));
        return loaded;
    }

    @Override
    public void evict(String principal) {
        store.remove(principal);
    }

    @Override
    public void clear() {
        store.clear();
    }
}
