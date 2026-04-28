package cn.molly.security.authorization.rbac.core;

import cn.molly.security.authorization.rbac.url.UrlPermissionRule;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;

/**
 * 权限数据 SPI，由使用者实现以对接真实的权限存储（DB / 缓存 / 远端服务）。
 * <p>
 * 本 Starter 提供 {@code DefaultMollyPermissionService} 作为兜底实现，
 * 直接使用 JWT 中 {@code authorities} claim 的值作为权限码集合，以保证
 * 不接入权限系统时也能基于 Token 做 {@code @MollyPreAuthorize} 鉴权。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public interface MollyPermissionService {

    /**
     * 加载主体对应的权限码集合。
     * <p>
     * 实现方应返回纯权限码（例如 {@code user:read}），无需包含前缀。
     * 该集合将被 {@code MollyPermissionEvaluator} 与注解比对。
     *
     * @param authentication 当前 Spring Security Authentication
     * @return 权限码集合，允许为空但不应为 null
     */
    Set<String> loadPermissions(Authentication authentication);

    /**
     * 加载动态 URL 鉴权规则。仅当 {@code molly.security.authorization.rbac.dynamic-url.enabled=true}
     * 时被调用。默认返回空集合，表示未启用动态 URL 鉴权。
     *
     * @return URL 鉴权规则列表
     */
    default List<UrlPermissionRule> loadUrlPermissionRules() {
        return List.of();
    }
}
