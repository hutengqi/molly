package cn.molly.security.authorization.rbac.core;

import cn.molly.security.authorization.rbac.cache.PermissionCache;

/**
 * 权限缓存刷新器。
 * <p>
 * 供业务侧在"角色 / 权限变更"后主动失效缓存，避免等待 TTL 过期。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class PermissionRefresher {

    private final PermissionCache permissionCache;

    /**
     * 构造刷新器。
     *
     * @param permissionCache 底层缓存
     */
    public PermissionRefresher(PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    /**
     * 使某一主体的权限缓存立即失效。
     *
     * @param principal 主体名
     */
    public void refresh(String principal) {
        permissionCache.evict(principal);
    }

    /**
     * 清空全部权限缓存。
     */
    public void refreshAll() {
        permissionCache.clear();
    }
}
