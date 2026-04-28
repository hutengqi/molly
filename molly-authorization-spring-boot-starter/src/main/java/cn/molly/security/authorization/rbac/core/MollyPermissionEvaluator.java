package cn.molly.security.authorization.rbac.core;

import cn.molly.security.authorization.rbac.cache.PermissionCache;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Set;

/**
 * Molly 权限评估器。
 * <p>
 * 实现 Spring Security 的 {@link PermissionEvaluator}，支持 SpEL 表达式
 * {@code hasPermission(null, 'user:read')} 形式的鉴权；同时被
 * {@code MollyPreAuthorizeAspect} 作为唯一决策入口复用，保证两条路径语义一致。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class MollyPermissionEvaluator implements PermissionEvaluator {

    private final MollyPermissionService permissionService;

    private final PermissionCache permissionCache;

    /**
     * 构造评估器。
     *
     * @param permissionService 权限 SPI 实现
     * @param permissionCache   权限缓存
     */
    public MollyPermissionEvaluator(MollyPermissionService permissionService, PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    /**
     * 根据权限码判定是否具备访问权。
     *
     * @param authentication 当前 Authentication
     * @param target         目标对象（不使用）
     * @param permission     权限码，通常为 {@code String}
     * @return 是否具备
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }
        Set<String> owned = loadPermissions(authentication);
        return owned.contains(String.valueOf(permission));
    }

    /**
     * 基于类型 + 标识符的鉴权形式，本实现忽略类型参数，直接走权限码比对。
     *
     * @param authentication   当前 Authentication
     * @param targetId         目标 ID
     * @param targetType       目标类型
     * @param permission       权限码
     * @return 是否具备
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }

    /**
     * 加载当前主体的权限集合，优先走缓存。
     *
     * @param authentication 当前 Authentication
     * @return 权限码集合
     */
    public Set<String> loadPermissions(Authentication authentication) {
        String principal = authentication.getName();
        return permissionCache.getOrLoad(principal, () -> {
            Set<String> loaded = permissionService.loadPermissions(authentication);
            return loaded == null ? Set.of() : loaded;
        });
    }
}
