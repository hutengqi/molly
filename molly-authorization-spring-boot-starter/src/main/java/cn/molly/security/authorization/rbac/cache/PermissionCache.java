package cn.molly.security.authorization.rbac.cache;

import java.util.Set;
import java.util.function.Supplier;

/**
 * 权限缓存抽象。
 * <p>
 * 默认实现为进程内 TTL 缓存；当引入 molly-cache-spring-boot-starter 并启用 redis 模式时，
 * 可由分布式实现覆盖。key 通常为 {@code Authentication#getName()}。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public interface PermissionCache {

    /**
     * 读取缓存中的权限集合；未命中时调用 loader 加载并写回缓存。
     *
     * @param principal 缓存键（主体名）
     * @param loader    权限加载器
     * @return 权限码集合
     */
    Set<String> getOrLoad(String principal, Supplier<Set<String>> loader);

    /**
     * 使指定主体的权限缓存失效。
     *
     * @param principal 主体名
     */
    void evict(String principal);

    /**
     * 清空全部缓存。
     */
    void clear();
}
