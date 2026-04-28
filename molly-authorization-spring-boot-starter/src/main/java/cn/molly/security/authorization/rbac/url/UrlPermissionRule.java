package cn.molly.security.authorization.rbac.url;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 动态 URL 鉴权规则。
 * <p>
 * 描述一条"URL 模式 + HTTP 方法 -> 所需权限码"的映射关系，由
 * {@code MollyPermissionService#loadUrlPermissionRules()} 返回，用于构建
 * {@code DynamicUrlAuthorizationManager}。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlPermissionRule {

    /**
     * Ant 风格的 URL 模式，例如 {@code /api/users/**}。
     */
    private String pattern;

    /**
     * HTTP 方法，例如 {@code GET}、{@code POST}。为 {@code null} 或空字符串时表示任意方法。
     */
    private String httpMethod;

    /**
     * 访问此资源所需的权限码集合。
     */
    private Set<String> requiredPermissions;

    /**
     * 匹配策略：为 {@code true} 时要求权限码全部命中；为 {@code false} 时任一命中即可。
     */
    private boolean requireAll;
}
