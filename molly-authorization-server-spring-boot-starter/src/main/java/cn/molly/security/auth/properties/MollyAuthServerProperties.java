package cn.molly.security.auth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * Molly authorization server properties.
 * </p>
 *
 * @author Ht7_Sincerity
 * @since 2025/8/7
 */
@Data
@ConfigurationProperties(prefix = "molly.security.auth")
public class MollyAuthServerProperties {

    /**
     * 授权服务器的颁发者 URI。
     * 这是 OIDC 合规的必填字段。
     * 例如：http://auth-server:9000
     */
    private String issuerUri;
}
