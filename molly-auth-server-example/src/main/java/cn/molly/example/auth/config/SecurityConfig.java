package cn.molly.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.time.Duration;
import java.util.UUID;

/**
 * 提供认证服务器运行所必需的 Bean。
 * <p>
 * 本配置类定义了客户端存储、用户详情服务以及应用级别的安全过滤链。
 * 在生产环境中，应将内存实现替换为数据库存储（如 JdbcRegisteredClientRepository）。
 *
 * @author Ht7_Sincerity
 * @since 2025/8/7
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置授权服务器的协议端点安全过滤链。
     * <p>
     * 此过滤链负责处理所有 OAuth2 和 OIDC 协议端点（如 /oauth2/authorize、/oauth2/token、/oauth2/jwks 等）。
     * 它会应用 Spring Authorization Server 的默认安全配置，启用 OIDC 支持，
     * 并将未认证请求重定向到登录页面。
     * <p>
     * 使用 {@code @Order} 确保此过滤链优先于应用的其他安全过滤链。
     *
     * @param http HttpSecurity 构建器
     * @return 授权服务器专用的 {@link SecurityFilterChain} 实例
     * @throws Exception 配置异常
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());
        http
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    /**
     * 配置应用级别的默认安全过滤链。
     * <p>
     * 此过滤链处理所有非 OAuth2 协议端点的请求，主要提供表单登录功能。
     * 所有请求都需要认证，未认证用户会被重定向到登录页面。
     *
     * @param http HttpSecurity 构建器
     * @return 默认的 {@link SecurityFilterChain} 实例
     * @throws Exception 配置异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    /**
     * 提供密码编码器 Bean。
     * <p>
     * 使用 BCrypt 算法对用户密码进行加密。
     * 使用 {@code @ConditionalOnMissingBean} 可被覆盖，但此处 example 项目直接提供。
     *
     * @return {@link PasswordEncoder} 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 提供客户端存储库的内存实现。
     * <p>
     * 在生产环境中，应将其替换为数据库实现（例如 JdbcRegisteredClientRepository）。
     *
     * @return RegisteredClientRepository 的 Bean
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        RegisteredClient testClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("test-client")
                .clientSecret(passwordEncoder.encode("secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc")
                .redirectUri("http://127.0.0.1:8080/authorized")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("message.read")
                .scope("message.write")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(testClient);
    }

    /**
     * 提供用户详细信息服务的内存实现。
     * <p>
     * 在生产环境中，应将其替换为从数据库加载用户的实现。
     *
     * @param passwordEncoder 密码编码器
     * @return UserDetailsService 的 Bean
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails userDetails = User.withUsername("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(userDetails);
    }
}
