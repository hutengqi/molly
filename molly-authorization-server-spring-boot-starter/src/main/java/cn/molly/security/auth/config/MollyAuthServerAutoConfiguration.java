package cn.molly.security.auth.config;

import cn.molly.security.auth.properties.MollyAuthServerProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Molly 认证授权服务器的自动配置类。
 * <p>
 * 该类是 `molly-authorization-server-spring-boot-starter` 的核心，负责为 Spring Boot 应用提供
 * Spring Authorization Server 所需的默认配置和核心组件。
 * <p>
 * 主要职责包括：
 * 1. 引入 Spring Authorization Server 的基础配置 ({@code OAuth2AuthorizationServerConfiguration})。
 * 2. 启用并绑定自定义的配置属性 ({@link MollyAuthServerProperties})。
 * 3. 提供可被使用者覆盖的默认 Bean，例如：
 * - {@link AuthorizationServerSettings}: 定义服务器的元数据。
 * - {@link JWKSource}: 提供 JWT 签名的密钥。
 * - {@link OAuth2TokenCustomizer}: 自定义令牌内容。
 * <p>
 * 使用者只需要在项目中引入此 starter，并提供 {@code UserDetailsService} 和 {@code RegisteredClientRepository} 的实现，
 * 即可快速搭建一个功能完备的 OAuth2 认证服务器。
 *
 * @author Ht7_Sincerity
 * @see org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
 * @see org.springframework.security.core.userdetails.UserDetailsService
 * @since 2025/8/7
 */
@AutoConfiguration
@EnableConfigurationProperties(MollyAuthServerProperties.class)
@Import(OAuth2AuthorizationServerConfiguration.class)
public class MollyAuthServerAutoConfiguration {

    /**
     * 配置授权服务器的核心设置。
     * <p>
     * 此 Bean 用于定义授权服务器的元数据，例如签发者地址 (issuer URI)。
     * 签发者地址是 OIDC (OpenID Connect) 规范中的一个重要组成部分，它向客户端表明了此授权服务器的身份。
     * 我们从 {@link MollyAuthServerProperties} 中读取配置的 `issuer-uri` 来构建此设置。
     * 使用 {@link ConditionalOnMissingBean} 注解，允许使用者在自己的配置中提供一个同名的 Bean 来覆盖默认设置，从而实现自定义。
     *
     * @param properties Molly 认证服务器的自定义配置属性
     * @return {@link AuthorizationServerSettings} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationServerSettings authorizationServerSettings(MollyAuthServerProperties properties) {
        return AuthorizationServerSettings.builder()
                .issuer(properties.getIssuerUri())
                .build();
    }

    /**
     * 提供用于签名 JWT (JSON Web Token) 的密钥源 (JWKSource)。
     * <p>
     * JWK (JSON Web Key) 是 OAuth 2.1 和 OIDC 标准中用于安全地签名和验证令牌的核心组件。
     * 此 Bean 默认会在内存中动态生成一个 2048 位的 RSA 密钥对，并将其作为 JWK 的来源。
     * 这样做的好处是开箱即用，使用者在开发阶段无需进行任何关于密钥的配置。
     * 在生产环境中，强烈建议使用者通过提供自己的 {@link JWKSource} Bean 来覆盖此默认实现，
     * 以便从更安全的地方（如密钥库文件、数据库或硬件安全模块 HSM）加载密钥。
     *
     * @return {@link JWKSource} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    /**
     * 自定义 JWT (Access Token) 的内容。
     * <p>
     * 此 Bean 允许我们在生成的 Access Token 中添加额外的声明 (claims)。
     * 默认实现会获取当前认证用户的权限信息 (authorities)，并将它们作为一个名为 "authorities" 的集合添加到 Access Token 中。
     * 这对于下游的资源服务器进行细粒度的权限控制非常有用。
     * 使用者同样可以通过提供自己的 {@link OAuth2TokenCustomizer} Bean 来实现更复杂的令牌定制逻辑，
     * 例如添加用户ID、部门信息等。
     *
     * @return {@link OAuth2TokenCustomizer} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            // 我们只对 access_token 进行自定义
            if (context.getTokenType().getValue().equals("access_token")) {
                Authentication principal = context.getPrincipal();
                // 获取用户的所有权限（例如 "ROLE_ADMIN", "SCOPE_message.read"）
                Set<String> authorities = principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());
                // 将权限信息添加到 "authorities" 这个 claim 中
                context.getClaims().claim("authorities", authorities);
            }
        };
    }

    /**
     * 生成一个 RSA 密钥实例。
     * <p>
     * 这是一个私有辅助方法，调用 {@link #generateRsaKey()} 生成密钥对，
     * 然后将其封装成 nimbus-jose-jwt 库中的 {@link RSAKey} 对象，并为其分配一个唯一的 Key ID。
     *
     * @return {@link RSAKey} 实例
     */
    private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    /**
     * 生成一个标准的 JCA (Java Cryptography Architecture) 密钥对。
     * <p>
     * 这是一个私有辅助方法，负责创建 2048 位的 RSA 密钥对。
     * 如果在生成过程中发生异常，会抛出 {@link IllegalStateException}。
     *
     * @return {@link KeyPair} 实例
     */
    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }
}

