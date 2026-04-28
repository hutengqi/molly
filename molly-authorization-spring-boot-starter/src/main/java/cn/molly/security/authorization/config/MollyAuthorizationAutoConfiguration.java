package cn.molly.security.authorization.config;

import cn.molly.security.authorization.properties.MollyAuthorizationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * Molly 授权鉴权组件的顶层自动配置。
 * <p>
 * 仅负责激活 {@link MollyAuthorizationProperties}，具体 Bean 装配拆分到：
 * <ul>
 *     <li>{@link MollyResourceServerConfiguration} - 资源服务器（JWT 验签、FilterChain）</li>
 *     <li>{@link MollyRbacConfiguration} - RBAC 相关 Bean（PermissionEvaluator、AOP、缓存）</li>
 *     <li>{@link MollyAuthorizationExceptionConfiguration} - 401/403 统一响应</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE - 100)
@EnableConfigurationProperties(MollyAuthorizationProperties.class)
public class MollyAuthorizationAutoConfiguration {
}
