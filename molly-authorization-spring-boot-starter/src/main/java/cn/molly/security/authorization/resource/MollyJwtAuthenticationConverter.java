package cn.molly.security.authorization.resource;

import cn.molly.security.authorization.properties.MollyAuthorizationProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Molly 资源服务器默认 JWT Authentication 转换器。
 * <p>
 * 从 JWT 指定 claim (默认 {@code authorities}) 中提取字符串数组，拼接权限前缀后包装为
 * {@link GrantedAuthority} 集合。若 claim 不存在或格式不是 {@code Collection<String>}，
 * 将返回空权限集合而不抛异常，交由后续 RBAC 环节决策。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class MollyJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final MollyAuthorizationProperties properties;

    /**
     * 构造转换器。
     *
     * @param properties 授权鉴权组件配置
     */
    public MollyJwtAuthenticationConverter(MollyAuthorizationProperties properties) {
        this.properties = properties;
    }

    /**
     * 将 JWT 转换为 {@link JwtAuthenticationToken}，并从指定 claim 读取权限列表。
     *
     * @param jwt 经过签名校验的 JWT
     * @return 包含权限信息的 Authentication
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(source -> authorities);
        return delegate.convert(jwt);
    }

    /**
     * 从 JWT claim 中解析权限集合。
     *
     * @param jwt 当前 JWT
     * @return 权限集合，永不为 null
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object claim = jwt.getClaim(properties.getAuthoritiesClaim());
        if (!(claim instanceof Collection<?> raw)) {
            return Collections.emptyList();
        }
        String prefix = properties.getAuthorityPrefix() == null ? "" : properties.getAuthorityPrefix();
        List<String> values = raw.stream()
                .filter(v -> v instanceof String)
                .map(v -> (String) v)
                .toList();
        return values.stream()
                .map(v -> new SimpleGrantedAuthority(prefix + v))
                .collect(Collectors.toList());
    }
}
