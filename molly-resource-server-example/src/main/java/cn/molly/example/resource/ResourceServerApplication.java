package cn.molly.example.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Molly 资源服务器示例应用。
 * <p>
 * 引入 {@code molly-authorization-spring-boot-starter} 即可获得：
 * <ul>
 *     <li>基于 JWT 的 Bearer Token 鉴权</li>
 *     <li>{@code @MollyPreAuthorize} 方法级权限控制</li>
 *     <li>统一的 401 / 403 JSON 响应</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@SpringBootApplication
public class ResourceServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceServerApplication.class, args);
    }
}
