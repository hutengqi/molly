package cn.molly.security.authorization.rbac.core;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 兜底的 {@link MollyPermissionService} 实现。
 * <p>
 * 直接将 Authentication 中的 {@link GrantedAuthority} 作为权限码返回，适用于
 * 权限完全由 JWT 声明、无需外部数据源的简单场景。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class DefaultMollyPermissionService implements MollyPermissionService {

    /**
     * 将 Authentication 持有的权限转为权限码集合。
     *
     * @param authentication 当前 Authentication
     * @return 权限码集合
     */
    @Override
    public Set<String> loadPermissions(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
