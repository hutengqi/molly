package cn.molly.example.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * molly-document-spring-boot-starter 示例应用。
 * <p>
 * 端口：8081，按需启动邮件、Word、Excel、PDF 能力。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@SpringBootApplication
public class DocumentExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentExampleApplication.class, args);
    }
}
