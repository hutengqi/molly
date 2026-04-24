package cn.molly.oss.support.minio;

import cn.molly.oss.core.*;
import cn.molly.oss.properties.MollyOssProperties;
import cn.molly.oss.util.FileUtil;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MinIO 的 {@link OssTemplate} 实现。
 * <p>
 * 基于 MinIO Java SDK，实现文件上传（含进度回调）、下载、删除、
 * 去重检查、缩略图（服务端 ImageIO 生成）等功能。
 * <p>
 * MinIO SDK 的 putObject 方法原生支持分片上传：当文件大于 5MB 时自动启用分片，
 * 因此无需额外实现断点续传逻辑。
 * <p>
 * 缩略图功能通过 Java ImageIO 在服务端生成，并作为独立对象存储到 MinIO。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
public class MinioOssTemplate implements OssTemplate {

    private final MinioClient minioClient;
    private final MollyOssProperties properties;

    public MinioOssTemplate(MinioClient minioClient, MollyOssProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    // ============================= 桶操作 =============================

    @Override
    public boolean bucketExists(String bucket) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        } catch (Exception e) {
            throw new OssException("检查存储桶是否存在失败: " + bucket, e);
        }
    }

    @Override
    public void createBucket(String bucket) {
        try {
            if (!bucketExists(bucket)) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new OssException("创建存储桶失败: " + bucket, e);
        }
    }

    // ============================= 对象操作 =============================

    @Override
    public OssObject putObject(String bucket, String objectName, InputStream inputStream,
                               long size, String contentType) {
        return putObject(bucket, objectName, inputStream, size, contentType, null);
    }

    @Override
    public OssObject putObject(String bucket, String objectName, InputStream inputStream,
                               long size, String contentType, UploadProgressListener listener) {
        try {
            InputStream wrappedStream = inputStream;

            // 使用计数输入流包装器实现进度回调
            if (listener != null) {
                wrappedStream = new CountingInputStream(inputStream, size, listener);
            }

            // MinIO putObject：size > 5MB 时自动启用分片上传（断点续传）
            // partSize 设为 -1 表示由 SDK 自动决定分片大小
            ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(wrappedStream, size, -1)
                    .contentType(contentType)
                    .build());

            // 通知上传完成
            if (listener != null) {
                listener.onProgress(UploadProgress.of(size, size));
            }

            return OssObject.builder()
                    .objectName(objectName)
                    .bucket(bucket)
                    .size(size)
                    .contentType(contentType)
                    .hash(response.etag())
                    .url(getObjectUrl(bucket, objectName))
                    .lastModified(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            throw new OssException("上传文件失败: " + objectName, e);
        }
    }

    @Override
    public InputStream getObject(String bucket, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new OssException("下载文件失败: " + objectName, e);
        }
    }

    @Override
    public void removeObject(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new OssException("删除文件失败: " + objectName, e);
        }
    }

    @Override
    public String getObjectUrl(String bucket, String objectName) {
        String endpoint = properties.getMinio().getEndpoint();
        // MinIO URL 格式: {endpoint}/{bucket}/{objectName}
        return endpoint + "/" + bucket + "/" + objectName;
    }

    @Override
    public OssObject statObject(String bucket, String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            return OssObject.builder()
                    .objectName(objectName)
                    .bucket(bucket)
                    .size(stat.size())
                    .contentType(stat.contentType())
                    .hash(stat.etag())
                    .url(getObjectUrl(bucket, objectName))
                    .lastModified(stat.lastModified() != null
                            ? stat.lastModified().toLocalDateTime()
                            : null)
                    .build();
        } catch (Exception e) {
            throw new OssException("获取对象元信息失败: " + objectName, e);
        }
    }

    @Override
    public boolean objectExists(String bucket, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            // 对象不存在时 MinIO 返回 NoSuchKey 错误
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new OssException("检查对象是否存在失败: " + objectName, e);
        } catch (Exception e) {
            throw new OssException("检查对象是否存在失败: " + objectName, e);
        }
    }

    @Override
    public List<OssObject> listObjects(String bucket, String prefix) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .recursive(true)
                    .build());

            List<OssObject> objects = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                objects.add(OssObject.builder()
                        .objectName(item.objectName())
                        .bucket(bucket)
                        .size(item.size())
                        .url(getObjectUrl(bucket, item.objectName()))
                        .lastModified(item.lastModified() != null
                                ? item.lastModified().toLocalDateTime()
                                : null)
                        .build());
            }
            return objects;
        } catch (Exception e) {
            throw new OssException("列出对象失败, prefix: " + prefix, e);
        }
    }

    // ============================= 高级功能 =============================

    @Override
    public OssObject putObjectIfAbsent(String bucket, String objectName, InputStream inputStream,
                                       long size, String contentType, String hash) {
        try {
            // 基于哈希值构建去重路径
            String ext = FileUtil.getExtension(objectName);
            String deduplicatedName = "dedup/" + hash.substring(0, 2) + "/" + hash
                    + (ext.isEmpty() ? "" : "." + ext);

            // 检查是否已存在相同哈希的对象
            if (objectExists(bucket, deduplicatedName)) {
                OssObject existing = statObject(bucket, deduplicatedName);
                existing.setOriginalFilename(objectName);
                return existing;
            }

            // 不存在则上传到去重路径，并在用户自定义元数据中记录哈希
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("content-md5", hash);

            ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(deduplicatedName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .userMetadata(userMetadata)
                    .build());

            return OssObject.builder()
                    .objectName(deduplicatedName)
                    .bucket(bucket)
                    .size(size)
                    .contentType(contentType)
                    .hash(hash)
                    .url(getObjectUrl(bucket, deduplicatedName))
                    .lastModified(LocalDateTime.now())
                    .build();
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw new OssException("去重上传失败: " + objectName, e);
        }
    }

    @Override
    public String getThumbnailUrl(String bucket, String objectName, int width, int height) {
        // MinIO 不支持服务端图片处理，返回预生成的缩略图对象 URL
        String thumbnailName = FileUtil.getThumbnailObjectName(objectName);
        return getObjectUrl(bucket, thumbnailName);
    }

    // ============================= 内部工具 =============================

    /**
     * 计数输入流包装器，用于追踪读取进度并回调监听器。
     * <p>
     * 每次 read 操作后累计已传输字节数，并通过 {@link UploadProgressListener} 通知调用方。
     */
    private static class CountingInputStream extends FilterInputStream {

        private final long totalBytes;
        private final UploadProgressListener listener;
        private long transferredBytes = 0;

        CountingInputStream(InputStream in, long totalBytes, UploadProgressListener listener) {
            super(in);
            this.totalBytes = totalBytes;
            this.listener = listener;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                transferredBytes++;
                notifyProgress();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = super.read(b, off, len);
            if (bytesRead > 0) {
                transferredBytes += bytesRead;
                notifyProgress();
            }
            return bytesRead;
        }

        private void notifyProgress() {
            listener.onProgress(UploadProgress.of(totalBytes, transferredBytes));
        }
    }
}
