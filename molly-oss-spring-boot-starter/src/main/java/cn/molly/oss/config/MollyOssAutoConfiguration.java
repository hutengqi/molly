package cn.molly.oss.config;

import cn.molly.oss.core.OssTemplate;
import cn.molly.oss.properties.MollyOssProperties;
import cn.molly.oss.support.aliyun.AliyunOssTemplate;
import cn.molly.oss.support.minio.MinioOssTemplate;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Molly 对象存储服务的自动配置类。
 * <p>
 * 根据 {@code molly.oss.provider} 属性自动激活对应的存储实现：
 * <ul>
 *     <li>{@code aliyun}（默认）— 使用阿里云 OSS SDK</li>
 *     <li>{@code minio} — 使用 MinIO Java SDK</li>
 * </ul>
 * 所有 Bean 均使用 {@code @ConditionalOnMissingBean}，允许使用者完全覆盖。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
@AutoConfiguration
@EnableConfigurationProperties(MollyOssProperties.class)
public class MollyOssAutoConfiguration {

    // ========================= 阿里云 OSS 配置 =========================

    /**
     * 阿里云 OSS 条件配置组。
     * <p>
     * 仅当 classpath 中存在阿里云 OSS SDK 且 provider 为 aliyun 时激活。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OSS.class)
    @ConditionalOnProperty(name = "molly.oss.provider", havingValue = "aliyun", matchIfMissing = true)
    static class AliyunOssConfiguration {

        /**
         * 创建阿里云 OSS 客户端。
         * <p>
         * 使用 V4 签名算法，通过配置属性中的 AccessKey 进行认证。
         *
         * @param properties 配置属性
         * @return 阿里云 OSS 客户端实例
         */
        @Bean(destroyMethod = "shutdown")
        @ConditionalOnMissingBean(OSS.class)
        public OSS ossClient(MollyOssProperties properties) {
            MollyOssProperties.Aliyun aliyun = properties.getAliyun();
            DefaultCredentialProvider credentialProvider = new DefaultCredentialProvider(
                    aliyun.getAccessKeyId(), aliyun.getAccessKeySecret());

            ClientBuilderConfiguration config = new ClientBuilderConfiguration();
            config.setSignatureVersion(SignVersion.V4);

            return OSSClientBuilder.create()
                    .endpoint(aliyun.getEndpoint())
                    .credentialsProvider(credentialProvider)
                    .clientConfiguration(config)
                    .build();
        }

        /**
         * 创建阿里云 OSS 操作模板。
         *
         * @param ossClient  阿里云 OSS 客户端
         * @param properties 配置属性
         * @return 阿里云 OssTemplate 实现
         */
        @Bean
        @ConditionalOnMissingBean(OssTemplate.class)
        public OssTemplate ossTemplate(OSS ossClient, MollyOssProperties properties) {
            return new AliyunOssTemplate(ossClient, properties);
        }
    }

    // ========================= MinIO 配置 =========================

    /**
     * MinIO 条件配置组。
     * <p>
     * 仅当 classpath 中存在 MinIO SDK 且 provider 为 minio 时激活。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MinioClient.class)
    @ConditionalOnProperty(name = "molly.oss.provider", havingValue = "minio")
    static class MinioOssConfiguration {

        /**
         * 创建 MinIO 客户端。
         *
         * @param properties 配置属性
         * @return MinIO 客户端实例
         */
        @Bean
        @ConditionalOnMissingBean(MinioClient.class)
        public MinioClient minioClient(MollyOssProperties properties) {
            MollyOssProperties.Minio minio = properties.getMinio();
            return MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();
        }

        /**
         * 创建 MinIO 操作模板。
         *
         * @param minioClient MinIO 客户端
         * @param properties  配置属性
         * @return MinIO OssTemplate 实现
         */
        @Bean
        @ConditionalOnMissingBean(OssTemplate.class)
        public OssTemplate ossTemplate(MinioClient minioClient, MollyOssProperties properties) {
            return new MinioOssTemplate(minioClient, properties);
        }
    }
}
