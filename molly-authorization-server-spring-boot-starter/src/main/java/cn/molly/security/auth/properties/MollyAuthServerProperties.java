package cn.molly.security.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * Molly authorization server properties.
 * </p>
 *
 * @author Ht7_Sincerity
 * @since 2025/8/7
 */
@ConfigurationProperties(prefix = "molly.security.auth")
public class MollyAuthServerProperties {

    /**
     * The issuer URI for the authorization server.
     * This is a mandatory field for OIDC compliance.
     * For example: http://auth-server:9000
     */
    private String issuerUri;

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }
}
