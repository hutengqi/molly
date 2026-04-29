package cn.molly.example.resource.service;

import cn.molly.security.authorization.rbac.core.MollyPermissionService;
import cn.molly.security.authorization.rbac.url.UrlPermissionRule;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * resource profile 下的示例权限 SPI 实现，使用内存数据模拟数据源。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/29
 */
@Profile("resource")
@Service
public class DemoPermissionService implements MollyPermissionService {

    private static final Map<String, Set<String>> DATA = Map.of(
            "user", Set.of("user:read"),
            "admin", Set.of("user:read", "user:write", "user:export")
    );

    @Override
    public Set<String> loadPermissions(Authentication authentication) {
        String principal = authentication == null ? null : authentication.getName();
        return DATA.getOrDefault(principal, Set.of());
    }

    @Override
    public List<UrlPermissionRule> loadUrlPermissionRules() {
        return List.of(
                new UrlPermissionRule("/api/users/**", "GET", Set.of("user:read"), false),
                new UrlPermissionRule("/api/users/**", "POST", Set.of("user:write"), false)
        );
    }
}
