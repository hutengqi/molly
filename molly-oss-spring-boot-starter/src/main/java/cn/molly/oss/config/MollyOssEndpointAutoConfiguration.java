package cn.molly.oss.config;

import cn.molly.oss.core.OssTemplate;
import cn.molly.oss.endpoint.OssEndpointController;
import cn.molly.oss.properties.MollyOssProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * 对象存储 HTTP 端点的自动配置类。
 * <p>
 * 当满足以下条件时自动注册 {@link OssEndpointController}：
 * <ul>
 *     <li>当前为 Web 应用环境</li>
 *     <li>{@code molly.oss.endpoint-enabled} 为 true（默认）</li>
 *     <li>容器中已存在 {@link OssTemplate} Bean</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
@AutoConfiguration(after = MollyOssAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "molly.oss.endpoint-enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(OssTemplate.class)
public class MollyOssEndpointAutoConfiguration {

    /**
     * 注册文件上传/下载 HTTP 端点控制器。
     *
     * @param ossTemplate 对象存储操作模板
     * @param properties  配置属性
     * @return 端点控制器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public OssEndpointController ossEndpointController(OssTemplate ossTemplate, MollyOssProperties properties) {
        return new OssEndpointController(ossTemplate, properties);
    }
}
