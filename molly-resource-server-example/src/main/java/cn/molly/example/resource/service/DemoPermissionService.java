package cn.molly.example.resource.service;

import cn.molly.security.authorization.rbac.core.MollyPermissionService;
import cn.molly.security.authorization.rbac.url.UrlPermissionRule;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 示例权限 SPI 实现。
 * <p>
 * 使用内存数据模拟权限数据源：为 user / admin 两个示例账号分别配置不同权限码。
 * 实际项目中应替换为基于数据库或远程服务的实现。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Service
public class DemoPermissionService implements MollyPermissionService {

    /**
     * 演示用户 -> 权限码集合映射。
     */
    private static final Map<String, Set<String>> DATA = Map.of(
            "user", Set.of("user:read"),
            "admin", Set.of("user:read", "user:write", "user:export")
    );

    @Override
    public Set<String> loadPermissions(Authentication authentication) {
        String principal = authentication == null ? null : authentication.getName();
        return DATA.getOrDefault(principal, Set.of());
    }

    /**
     * 动态 URL 鉴权规则示例。仅当配置开启动态 URL 时生效。
     *
     * @return 规则列表
     */
    @Override
    public List<UrlPermissionRule> loadUrlPermissionRules() {
        return List.of(
                new UrlPermissionRule("/api/users/**", "GET", Set.of("user:read"), false),
                new UrlPermissionRule("/api/users/**", "POST", Set.of("user:write"), false)
        );
    }
}
