package cn.molly.security.authorization.config;

import cn.molly.security.authorization.exception.MollyAccessDeniedHandler;
import cn.molly.security.authorization.exception.MollyAuthenticationEntryPoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 401/403 统一 JSON 响应相关 Bean 装配。
 * <p>
 * 使用者可通过提供同名 Bean 覆盖默认实现，以接入全站统一响应体结构。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@AutoConfiguration
public class MollyAuthorizationExceptionConfiguration {

    /**
     * 默认未认证入口：输出 401 JSON。
     *
     * @return {@link AuthenticationEntryPoint}
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEntryPoint mollyAuthenticationEntryPoint() {
        return new MollyAuthenticationEntryPoint();
    }

    /**
     * 默认鉴权失败处理器：输出 403 JSON。
     *
     * @return {@link AccessDeniedHandler}
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedHandler mollyAccessDeniedHandler() {
        return new MollyAccessDeniedHandler();
    }
}
