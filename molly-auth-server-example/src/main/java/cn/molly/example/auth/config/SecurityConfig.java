package cn.molly.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

/**
 * 提供认证服务器运行所必需的 Bean
 */
@Configuration
public class SecurityConfig {

    /**
     * 提供客户端存储库的内存实现。
     * 在生产环境中，您应该将其替换为数据库实现（例如 JdbcRegisteredClientRepository）。
     *
     * @return RegisteredClientRepository 的 Bean
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // 模拟一个客户端，用于密码模式和刷新令牌
        RegisteredClient testClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("test-client")
                // {noop} 表示密码是明文存储，仅用于测试
                .clientSecret("{noop}secret")
                // 客户端认证方式
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                // 支持的授权类型
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                // 授权成功后的重定向 URI
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc")
                .redirectUri("http://127.0.0.1:8080/authorized")
                // 客户端的权限范围
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("message.read")
                .scope("message.write")
                // 客户端设置，例如是否需要用户手动同意授权
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .refreshTokenTimeToLive(Duration.ofMinutes(1440))
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(testClient);
    }

    /**
     * 提供用户详细信息服务的内存实现。
     * 在生产环境中，您应该将其替换为从数据库加载用户的实现。
     *
     * @return UserDetailsService 的 Bean
     */
    @Bean
    public UserDetailsService userDetailsService() {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // 模拟一个用户
        UserDetails userDetails = User.withUsername("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(userDetails);
    }
}
