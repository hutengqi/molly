package cn.molly.security.authorization.config;

import cn.molly.security.authorization.properties.MollyAuthorizationProperties;
import cn.molly.security.authorization.rbac.aop.MollyPreAuthorizeAspect;
import cn.molly.security.authorization.rbac.cache.LocalPermissionCache;
import cn.molly.security.authorization.rbac.cache.PermissionCache;
import cn.molly.security.authorization.rbac.core.DefaultMollyPermissionService;
import cn.molly.security.authorization.rbac.core.MollyPermissionEvaluator;
import cn.molly.security.authorization.rbac.core.MollyPermissionService;
import cn.molly.security.authorization.rbac.core.PermissionRefresher;
import cn.molly.security.authorization.rbac.url.DynamicUrlAuthorizationManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * RBAC 相关 Bean 自动装配。
 * <p>
 * 默认启用；可通过 {@code molly.security.authorization.rbac.enabled=false} 关闭。
 * 启用 {@link EnableMethodSecurity} 以支持原生 {@code @PreAuthorize("hasPermission(...)")} 与本 Starter
 * 的 {@link cn.molly.security.authorization.rbac.annotation.MollyPreAuthorize} 双栈。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@AutoConfiguration
@AutoConfigureAfter(MollyAuthorizationAutoConfiguration.class)
@ConditionalOnProperty(prefix = "molly.security.authorization.rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MollyRbacConfiguration {

    /**
     * 默认权限 SPI 实现：使用 JWT claim 中的 authorities 作为权限码。
     *
     * @return {@link MollyPermissionService}
     */
    @Bean
    @ConditionalOnMissingBean
    public MollyPermissionService mollyPermissionService() {
        return new DefaultMollyPermissionService();
    }

    /**
     * 默认权限缓存：进程内 TTL 缓存。
     *
     * @param properties 组件配置
     * @return {@link PermissionCache}
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionCache permissionCache(MollyAuthorizationProperties properties) {
        return new LocalPermissionCache(properties.getRbac().getCache().getTtl());
    }

    /**
     * 权限评估器。
     *
     * @param permissionService 权限 SPI
     * @param permissionCache   缓存
     * @return {@link MollyPermissionEvaluator}
     */
    @Bean
    @ConditionalOnMissingBean
    public MollyPermissionEvaluator mollyPermissionEvaluator(MollyPermissionService permissionService,
                                                             PermissionCache permissionCache) {
        return new MollyPermissionEvaluator(permissionService, permissionCache);
    }

    /**
     * 将 {@link PermissionEvaluator} 注入 Spring Security 的 SpEL 解析环境，
     * 使 {@code hasPermission(null, 'user:read')} 可用。
     *
     * @param permissionEvaluator 评估器
     * @return {@link MethodSecurityExpressionHandler}
     */
    @Bean
    @ConditionalOnMissingBean
    public MethodSecurityExpressionHandler mollyMethodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    /**
     * {@code @MollyPreAuthorize} 注解切面。
     *
     * @param permissionEvaluator 评估器
     * @return 切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MollyPreAuthorizeAspect mollyPreAuthorizeAspect(MollyPermissionEvaluator permissionEvaluator) {
        return new MollyPreAuthorizeAspect(permissionEvaluator);
    }

    /**
     * 权限缓存刷新器。
     *
     * @param permissionCache 缓存
     * @return {@link PermissionRefresher}
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionRefresher permissionRefresher(PermissionCache permissionCache) {
        return new PermissionRefresher(permissionCache);
    }

    /**
     * 动态 URL 鉴权管理器，仅在 {@code molly.security.authorization.rbac.dynamic-url.enabled=true} 时注册。
     *
     * @param permissionService   权限 SPI
     * @param permissionEvaluator 评估器
     * @return {@link DynamicUrlAuthorizationManager}
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "molly.security.authorization.rbac.dynamic-url", name = "enabled", havingValue = "true")
    public DynamicUrlAuthorizationManager dynamicUrlAuthorizationManager(MollyPermissionService permissionService,
                                                                          MollyPermissionEvaluator permissionEvaluator) {
        return new DynamicUrlAuthorizationManager(permissionService, permissionEvaluator);
    }
}
