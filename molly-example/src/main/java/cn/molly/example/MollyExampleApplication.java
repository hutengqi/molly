package cn.molly.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Molly 示例统一入口。
 * <p>
 * 通过 {@code spring.profiles.active} 激活不同子示例：
 * <ul>
 *     <li>{@code auth}：OAuth2 / OIDC 授权服务器（端口 9000）</li>
 *     <li>{@code resource}：资源服务器 + RBAC 鉴权（端口 9100）</li>
 *     <li>{@code document}：Word / Excel / PDF / Email 文档生成（端口 8081）</li>
 *     <li>{@code mq}：消息队列（RocketMQ / Kafka / Pulsar / Rabbit，端口 8082）</li>
 * </ul>
 * 启动命令：
 * <pre>
 *     mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=auth
 *     mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=resource
 *     mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=document
 *     mvn spring-boot:run -pl molly-example -Dspring-boot.run.profiles=mq
 * </pre>
 *
 * @author Ht7_Sincerity
 * @since 2026/4/29
 */
@SpringBootApplication
public class MollyExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MollyExampleApplication.class, args);
    }
}
