package cn.molly.oss.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对象存储文件的元信息模型。
 * <p>
 * 封装了文件在对象存储中的核心属性，包括名称、大小、类型、哈希值等。
 * 用于上传完成后返回给调用方，或查询对象信息时使用。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OssObject {

    /**
     * 对象在存储桶中的名称（含路径前缀）。
     * 示例: 2026/04/23/550e8400-e29b.png
     */
    private String objectName;

    /**
     * 所属存储桶名称。
     */
    private String bucket;

    /**
     * 文件大小，单位字节。
     */
    private long size;

    /**
     * 文件 MIME 类型。
     * 示例: image/png, application/pdf
     */
    private String contentType;

    /**
     * 最后修改时间。
     */
    private LocalDateTime lastModified;

    /**
     * 文件 MD5 哈希值，用于文件去重校验。
     */
    private String hash;

    /**
     * 文件访问 URL。
     */
    private String url;

    /**
     * 原始文件名。
     */
    private String originalFilename;

    /**
     * 缩略图访问 URL（仅图片类型文件有值）。
     */
    private String thumbnailUrl;
}
