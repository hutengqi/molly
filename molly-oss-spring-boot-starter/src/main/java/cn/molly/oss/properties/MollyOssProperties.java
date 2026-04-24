package cn.molly.oss.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Molly 对象存储服务的配置属性。
 * <p>
 * 绑定前缀为 {@code molly.oss}，支持通过 {@code provider} 属性切换底层存储实现。
 * 默认使用阿里云 OSS，可切换为 MinIO。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
@Data
@ConfigurationProperties(prefix = "molly.oss")
public class MollyOssProperties {

    /**
     * 存储提供商类型，默认 aliyun。
     * 可选值: aliyun, minio
     */
    private Provider provider = Provider.ALIYUN;

    /**
     * 默认存储桶名称。
     */
    private String bucket;

    /**
     * 是否启用内置 HTTP 端点（文件上传/下载接口），默认 true。
     */
    private boolean endpointEnabled = true;

    /**
     * 最大文件上传大小，单位字节，默认 100MB。
     */
    private long maxFileSize = 100 * 1024 * 1024L;

    /**
     * 允许上传的文件类型白名单（MIME 类型）。
     * 为空时不做限制。
     * 示例: ["image/png", "image/jpeg", "application/pdf"]
     */
    private List<String> allowedTypes;

    /**
     * 缩略图配置。
     */
    private Thumbnail thumbnail = new Thumbnail();

    /**
     * 阿里云 OSS 专属配置。
     */
    private Aliyun aliyun = new Aliyun();

    /**
     * MinIO 专属配置。
     */
    private Minio minio = new Minio();

    /**
     * 存储提供商枚举。
     */
    public enum Provider {
        /** 阿里云 OSS */
        ALIYUN,
        /** MinIO 自建对象存储 */
        MINIO
    }

    /**
     * 缩略图配置。
     */
    @Data
    public static class Thumbnail {

        /**
         * 是否启用缩略图自动生成，默认 true。
         */
        private boolean enabled = true;

        /**
         * 缩略图宽度，默认 200 像素。
         */
        private int width = 200;

        /**
         * 缩略图高度，默认 200 像素。
         */
        private int height = 200;
    }

    /**
     * 阿里云 OSS 连接配置。
     */
    @Data
    public static class Aliyun {

        /**
         * 阿里云 OSS Endpoint 地址。
         * 示例: https://oss-cn-hangzhou.aliyuncs.com
         */
        private String endpoint;

        /**
         * AccessKey ID。
         */
        private String accessKeyId;

        /**
         * AccessKey Secret。
         */
        private String accessKeySecret;
    }

    /**
     * MinIO 连接配置。
     */
    @Data
    public static class Minio {

        /**
         * MinIO 服务端点地址。
         * 示例: http://localhost:9000
         */
        private String endpoint;

        /**
         * 访问密钥（Access Key）。
         */
        private String accessKey;

        /**
         * 密钥（Secret Key）。
         */
        private String secretKey;
    }
}
