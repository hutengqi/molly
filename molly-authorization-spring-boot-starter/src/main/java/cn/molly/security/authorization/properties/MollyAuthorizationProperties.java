package cn.molly.security.authorization.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Molly 授权鉴权组件配置属性。
 * <p>
 * 对应配置前缀 {@code molly.security.authorization}，提供：
 * <ul>
 *     <li>资源服务器 JWT 校验基础参数 (issuer-uri / jwk-set-uri)</li>
 *     <li>Authentication 构建所用的 claim 名与权限前缀</li>
 *     <li>默认放行的 URL 白名单</li>
 *     <li>RBAC 子模块开关、动态 URL 鉴权开关、权限缓存 TTL 与类型</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Data
@ConfigurationProperties(prefix = "molly.security.authorization")
public class MollyAuthorizationProperties {

    /**
     * OIDC 令牌签发者 URI，与认证服务器 {@code molly.security.auth.issuer-uri} 对齐。
     * 若未显式配置 {@link #jwkSetUri}，则由 issuer-uri 推导。
     */
    private String issuerUri;

    /**
     * JWK Set URI，通常由 issuer-uri 自动推导；当认证服务器部署在内网且 discovery 地址不可达时，可显式指定。
     */
    private String jwkSetUri;

    /**
     * JWT 中承载权限列表的 claim 名称。默认为 {@code authorities}，与认证 Starter 默认行为对齐。
     */
    private String authoritiesClaim = "authorities";

    /**
     * 从 claim 中读取权限后追加的前缀。例如配置 {@code ROLE_} 时，{@code admin} 将被映射为 {@code ROLE_admin}。
     */
    private String authorityPrefix = "";

    /**
     * 资源服务器放行的 URL 白名单，支持 Ant 风格通配符。
     */
    private List<String> permitAll = new ArrayList<>();

    /**
     * RBAC 子模块配置。
     */
    private Rbac rbac = new Rbac();

    /**
     * RBAC 配置项。
     */
    @Data
    public static class Rbac {

        /**
         * 是否启用 RBAC 相关 Bean (PermissionEvaluator、@MollyPreAuthorize AOP 等)。
         */
        private boolean enabled = true;

        /**
         * 动态 URL 鉴权配置。
         */
        private DynamicUrl dynamicUrl = new DynamicUrl();

        /**
         * 权限缓存配置。
         */
        private Cache cache = new Cache();
    }

    /**
     * 动态 URL 鉴权配置。
     */
    @Data
    public static class DynamicUrl {

        /**
         * 是否启用基于 {@code MollyPermissionService#loadUrlPermissionRules()} 的动态 URL 鉴权。
         */
        private boolean enabled = false;
    }

    /**
     * 权限缓存配置。
     */
    @Data
    public static class Cache {

        /**
         * 权限缓存过期时间，默认 5 分钟。
         */
        private Duration ttl = Duration.ofMinutes(5);

        /**
         * 缓存类型：{@code local} 本地内存；{@code redis} 需引入 molly-cache-spring-boot-starter。
         */
        private String type = "local";
    }
}
