package cn.molly.security.authorization.config;

import cn.molly.security.authorization.properties.MollyAuthorizationProperties;
import cn.molly.security.authorization.rbac.url.DynamicUrlAuthorizationManager;
import cn.molly.security.authorization.resource.MollyJwtAuthenticationConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 资源服务器自动配置。
 * <p>
 * 提供：
 * <ul>
 *     <li>{@link JwtDecoder}：基于 issuer-uri 或 jwk-set-uri 构建，强制使用 {@link ConditionalOnMissingBean} 允许覆盖</li>
 *     <li>{@link MollyJwtAuthenticationConverter}：按配置读取 authorities claim</li>
 *     <li>默认 {@link SecurityFilterChain}：配置白名单、资源服务器模式、无状态 session、统一异常处理</li>
 * </ul>
 * 若使用者自行提供 {@code SecurityFilterChain}，本配置将自动退让。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@AutoConfiguration
@AutoConfigureAfter(MollyAuthorizationAutoConfiguration.class)
@ConditionalOnClass({JwtDecoder.class, SecurityFilterChain.class})
@EnableWebSecurity
public class MollyResourceServerConfiguration {

    /**
     * 依据配置构建 {@link JwtDecoder}。优先使用 {@code jwk-set-uri}，否则从 {@code issuer-uri} 推导。
     *
     * @param properties 组件配置
     * @return {@link JwtDecoder}
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(MollyAuthorizationProperties properties) {
        if (properties.getJwkSetUri() != null && !properties.getJwkSetUri().isBlank()) {
            return NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
        }
        if (properties.getIssuerUri() == null || properties.getIssuerUri().isBlank()) {
            throw new IllegalStateException("molly.security.authorization.issuer-uri 或 jwk-set-uri 至少需要配置一项");
        }
        return JwtDecoders.fromIssuerLocation(properties.getIssuerUri());
    }

    /**
     * 构建从 JWT 到 Authentication 的转换器，读取指定 authorities claim。
     *
     * @param properties 组件配置
     * @return 转换器
     */
    @Bean
    @ConditionalOnMissingBean(name = "mollyJwtAuthenticationConverter")
    public Converter<Jwt, AbstractAuthenticationToken> mollyJwtAuthenticationConverter(MollyAuthorizationProperties properties) {
        return new MollyJwtAuthenticationConverter(properties);
    }

    /**
     * 默认资源服务器安全过滤链：白名单放行 + JWT 资源服务器 + 统一 401/403。
     * <p>
     * 当动态 URL 鉴权开启时，自动装配 {@link DynamicUrlAuthorizationManager}。
     *
     * @param http                           HttpSecurity
     * @param properties                     组件配置
     * @param jwtAuthenticationConverter     JWT 转换器
     * @param authenticationEntryPoint       未认证入口
     * @param accessDeniedHandler            鉴权失败处理器
     * @param applicationContext             Spring 应用上下文，用于按需获取 DynamicUrlAuthorizationManager
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(100)
    @ConditionalOnMissingBean(name = "mollyResourceServerSecurityFilterChain")
    public SecurityFilterChain mollyResourceServerSecurityFilterChain(
            HttpSecurity http,
            MollyAuthorizationProperties properties,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler,
            ApplicationContext applicationContext
    ) throws Exception {
        String[] permitAll = properties.getPermitAll().toArray(new String[0]);

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    if (permitAll.length > 0) {
                        authorize.requestMatchers(permitAll).permitAll();
                    }
                    boolean dynamicUrlEnabled = properties.getRbac().isEnabled()
                            && properties.getRbac().getDynamicUrl().isEnabled();
                    if (dynamicUrlEnabled && applicationContext.getBeanNamesForType(DynamicUrlAuthorizationManager.class).length > 0) {
                        DynamicUrlAuthorizationManager manager = applicationContext.getBean(DynamicUrlAuthorizationManager.class);
                        authorize.anyRequest().access(manager);
                    } else {
                        authorize.anyRequest().authenticated();
                    }
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}
