package cn.molly.oss.core;

import java.io.InputStream;
import java.util.List;

/**
 * 对象存储统一操作模板接口。
 * <p>
 * 定义了对象存储的核心操作契约，包括文件上传、下载、删除、去重检查、缩略图等。
 * 不同的存储服务（阿里云 OSS、MinIO 等）需实现此接口，
 * 通过 Spring Boot 自动配置根据 {@code molly.oss.provider} 属性注入对应实现。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
public interface OssTemplate {

    // ============================= 桶操作 =============================

    /**
     * 判断存储桶是否存在。
     *
     * @param bucket 存储桶名称
     * @return 存在返回 true
     * @throws OssException 操作失败时抛出
     */
    boolean bucketExists(String bucket);

    /**
     * 创建存储桶。若已存在则不做任何操作。
     *
     * @param bucket 存储桶名称
     * @throws OssException 操作失败时抛出
     */
    void createBucket(String bucket);

    // ============================= 对象操作 =============================

    /**
     * 上传对象。
     *
     * @param bucket      存储桶名称
     * @param objectName  对象名称（含路径前缀）
     * @param inputStream 文件输入流
     * @param size        文件大小（字节），未知时传 -1
     * @param contentType 文件 MIME 类型
     * @return 上传后的对象元信息
     * @throws OssException 操作失败时抛出
     */
    OssObject putObject(String bucket, String objectName, InputStream inputStream,
                        long size, String contentType);

    /**
     * 上传对象（带进度回调）。
     *
     * @param bucket      存储桶名称
     * @param objectName  对象名称（含路径前缀）
     * @param inputStream 文件输入流
     * @param size        文件大小（字节），未知时传 -1
     * @param contentType 文件 MIME 类型
     * @param listener    进度监听器
     * @return 上传后的对象元信息
     * @throws OssException 操作失败时抛出
     */
    OssObject putObject(String bucket, String objectName, InputStream inputStream,
                        long size, String contentType, UploadProgressListener listener);

    /**
     * 下载对象，返回输入流。
     * <p>
     * 调用方需负责关闭返回的流。
     *
     * @param bucket     存储桶名称
     * @param objectName 对象名称
     * @return 文件输入流
     * @throws OssException 操作失败时抛出
     */
    InputStream getObject(String bucket, String objectName);

    /**
     * 删除对象。
     *
     * @param bucket     存储桶名称
     * @param objectName 对象名称
     * @throws OssException 操作失败时抛出
     */
    void removeObject(String bucket, String objectName);

    /**
     * 获取对象的访问 URL。
     *
     * @param bucket     存储桶名称
     * @param objectName 对象名称
     * @return 访问 URL
     * @throws OssException 操作失败时抛出
     */
    String getObjectUrl(String bucket, String objectName);

    /**
     * 获取对象元信息。
     *
     * @param bucket     存储桶名称
     * @param objectName 对象名称
     * @return 对象元信息
     * @throws OssException 操作失败时抛出
     */
    OssObject statObject(String bucket, String objectName);

    /**
     * 判断对象是否存在。
     *
     * @param bucket     存储桶名称
     * @param objectName 对象名称
     * @return 存在返回 true
     * @throws OssException 操作失败时抛出
     */
    boolean objectExists(String bucket, String objectName);

    /**
     * 列出指定前缀下的对象。
     *
     * @param bucket 存储桶名称
     * @param prefix 对象名称前缀
     * @return 对象元信息列表
     * @throws OssException 操作失败时抛出
     */
    List<OssObject> listObjects(String bucket, String prefix);

    // ============================= 高级功能 =============================

    /**
     * 去重上传：根据文件 MD5 哈希判断是否已存在相同内容的文件。
     * <p>
     * 若已存在相同哈希的对象，则直接返回已有对象的信息（秒传）；
     * 否则执行正常上传。
     *
     * @param bucket      存储桶名称
     * @param objectName  对象名称
     * @param inputStream 文件输入流
     * @param size        文件大小（字节）
     * @param contentType 文件 MIME 类型
     * @param hash        文件 MD5 哈希值
     * @return 上传后（或已存在的）对象元信息
     * @throws OssException 操作失败时抛出
     */
    OssObject putObjectIfAbsent(String bucket, String objectName, InputStream inputStream,
                                long size, String contentType, String hash);

    /**
     * 获取缩略图 URL。
     * <p>
     * 对于支持服务端图片处理的存储服务（如阿里云 OSS），返回带处理参数的 URL；
     * 对于不支持的存储服务，返回预生成的缩略图对象 URL。
     *
     * @param bucket     存储桶名称
     * @param objectName 对象名称
     * @param width      缩略图宽度
     * @param height     缩略图高度
     * @return 缩略图 URL
     * @throws OssException 操作失败时抛出
     */
    String getThumbnailUrl(String bucket, String objectName, int width, int height);
}
