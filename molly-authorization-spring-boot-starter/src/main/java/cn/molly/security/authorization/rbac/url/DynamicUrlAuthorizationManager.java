package cn.molly.security.authorization.rbac.url;

import cn.molly.security.authorization.rbac.core.MollyPermissionEvaluator;
import cn.molly.security.authorization.rbac.core.MollyPermissionService;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 基于 {@link UrlPermissionRule} 的动态 URL 鉴权管理器。
 * <p>
 * 启用条件：{@code molly.security.authorization.rbac.dynamic-url.enabled=true}。
 * 每次请求时按规则顺序匹配第一条命中的规则并判定所需权限；若无任何规则命中，
 * 则允许通过（交由后续 {@code @MollyPreAuthorize} 或默认 {@code authenticated()} 兜底）。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class DynamicUrlAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final MollyPermissionService permissionService;

    private final MollyPermissionEvaluator permissionEvaluator;

    /**
     * 规则缓存：首次访问时初始化，使用者可通过 {@link #refresh()} 触发刷新。
     */
    private volatile List<CompiledRule> compiledRules;

    /**
     * 构造管理器。
     *
     * @param permissionService   规则数据源
     * @param permissionEvaluator 权限评估器（用于读取缓存化的主体权限）
     */
    public DynamicUrlAuthorizationManager(MollyPermissionService permissionService, MollyPermissionEvaluator permissionEvaluator) {
        this.permissionService = permissionService;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * 触发规则重新加载，供配置中心 / 管理端主动刷新。
     */
    public void refresh() {
        this.compiledRules = null;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        List<CompiledRule> rules = getOrInitRules();
        CompiledRule matched = null;
        for (CompiledRule rule : rules) {
            if (rule.matcher.matches(request)) {
                matched = rule;
                break;
            }
        }
        if (matched == null) {
            return new AuthorizationDecision(true);
        }
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        Set<String> owned = permissionEvaluator.loadPermissions(authentication);
        boolean ok = matched.requireAll
                ? matched.requiredPermissions.stream().allMatch(owned::contains)
                : matched.requiredPermissions.stream().anyMatch(owned::contains);
        return new AuthorizationDecision(ok);
    }

    /**
     * 双检锁初始化规则。
     *
     * @return 编译后的规则列表
     */
    private List<CompiledRule> getOrInitRules() {
        List<CompiledRule> local = this.compiledRules;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (this.compiledRules == null) {
                List<UrlPermissionRule> raw = permissionService.loadUrlPermissionRules();
                List<CompiledRule> compiled = new ArrayList<>(raw == null ? 0 : raw.size());
                if (raw != null) {
                    for (UrlPermissionRule rule : raw) {
                        compiled.add(compile(rule));
                    }
                }
                this.compiledRules = compiled;
            }
            return this.compiledRules;
        }
    }

    /**
     * 将规则编译为带 {@link RequestMatcher} 的运行时对象。
     *
     * @param rule 原始规则
     * @return 编译后的规则
     */
    private CompiledRule compile(UrlPermissionRule rule) {
        HttpMethod method = (rule.getHttpMethod() == null || rule.getHttpMethod().isBlank())
                ? null
                : HttpMethod.valueOf(rule.getHttpMethod().toUpperCase());
        AntPathRequestMatcher matcher = method == null
                ? new AntPathRequestMatcher(rule.getPattern())
                : new AntPathRequestMatcher(rule.getPattern(), method.name());
        Set<String> required = rule.getRequiredPermissions() == null ? Set.of() : rule.getRequiredPermissions();
        return new CompiledRule(matcher, required, rule.isRequireAll());
    }

    /**
     * 编译后的规则结构。
     */
    private record CompiledRule(RequestMatcher matcher, Set<String> requiredPermissions, boolean requireAll) {
    }
}
