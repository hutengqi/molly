package cn.molly.oss.support.aliyun;

import cn.molly.oss.core.*;
import cn.molly.oss.properties.MollyOssProperties;
import cn.molly.oss.util.FileUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云 OSS 的 {@link OssTemplate} 实现。
 * <p>
 * 基于阿里云 OSS Java SDK，实现文件上传（含进度回调）、下载、删除、
 * 去重检查、缩略图（服务端图片处理）等功能。
 * <p>
 * 缩略图功能利用阿里云 OSS 内置的图片处理服务，通过 URL 参数实现服务端缩放，
 * 无需额外存储缩略图文件。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
public class AliyunOssTemplate implements OssTemplate {

    private final OSS ossClient;
    private final MollyOssProperties properties;

    public AliyunOssTemplate(OSS ossClient, MollyOssProperties properties) {
        this.ossClient = ossClient;
        this.properties = properties;
    }

    // ============================= 桶操作 =============================

    @Override
    public boolean bucketExists(String bucket) {
        try {
            return ossClient.doesBucketExist(bucket);
        } catch (Exception e) {
            throw new OssException("检查存储桶是否存在失败: " + bucket, e);
        }
    }

    @Override
    public void createBucket(String bucket) {
        try {
            if (!ossClient.doesBucketExist(bucket)) {
                ossClient.createBucket(bucket);
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
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            if (size > 0) {
                metadata.setContentLength(size);
            }

            PutObjectRequest request = new PutObjectRequest(bucket, objectName, inputStream, metadata);

            // 设置进度回调：阿里云 ProgressEvent.getBytes() 返回的是增量值，需要累加
            if (listener != null) {
                final long totalBytes = size;
                final long[] bytesWritten = {0L};
                request.setProgressListener(progressEvent -> {
                    ProgressEventType type = progressEvent.getEventType();
                    if (type == ProgressEventType.TRANSFER_STARTED_EVENT) {
                        bytesWritten[0] = 0L;
                        listener.onProgress(UploadProgress.of(totalBytes, 0));
                    } else if (type == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) {
                        bytesWritten[0] += progressEvent.getBytes();
                        listener.onProgress(UploadProgress.of(totalBytes, bytesWritten[0]));
                    } else if (type == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                        listener.onProgress(UploadProgress.of(totalBytes, totalBytes));
                    }
                });
            }

            PutObjectResult result = ossClient.putObject(request);

            return OssObject.builder()
                    .objectName(objectName)
                    .bucket(bucket)
                    .size(size)
                    .contentType(contentType)
                    .hash(result.getETag())
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
            OSSObject ossObject = ossClient.getObject(bucket, objectName);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            throw new OssException("下载文件失败: " + objectName, e);
        }
    }

    @Override
    public void removeObject(String bucket, String objectName) {
        try {
            ossClient.deleteObject(bucket, objectName);
        } catch (Exception e) {
            throw new OssException("删除文件失败: " + objectName, e);
        }
    }

    @Override
    public String getObjectUrl(String bucket, String objectName) {
        String endpoint = properties.getAliyun().getEndpoint();
        // 拼接标准 OSS URL: https://{bucket}.{endpoint}/{objectName}
        String host = endpoint.replaceFirst("https?://", "");
        String scheme = endpoint.startsWith("https") ? "https" : "http";
        return scheme + "://" + bucket + "." + host + "/" + objectName;
    }

    @Override
    public OssObject statObject(String bucket, String objectName) {
        try {
            ObjectMetadata metadata = ossClient.getObjectMetadata(bucket, objectName);
            return OssObject.builder()
                    .objectName(objectName)
                    .bucket(bucket)
                    .size(metadata.getContentLength())
                    .contentType(metadata.getContentType())
                    .hash(metadata.getETag())
                    .url(getObjectUrl(bucket, objectName))
                    .lastModified(metadata.getLastModified() != null
                            ? metadata.getLastModified().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : null)
                    .build();
        } catch (Exception e) {
            throw new OssException("获取对象元信息失败: " + objectName, e);
        }
    }

    @Override
    public boolean objectExists(String bucket, String objectName) {
        try {
            return ossClient.doesObjectExist(bucket, objectName);
        } catch (Exception e) {
            throw new OssException("检查对象是否存在失败: " + objectName, e);
        }
    }

    @Override
    public List<OssObject> listObjects(String bucket, String prefix) {
        try {
            ListObjectsV2Request request = new ListObjectsV2Request(bucket);
            request.setPrefix(prefix);
            request.setMaxKeys(1000);

            ListObjectsV2Result result = ossClient.listObjectsV2(request);
            List<OssObject> objects = new ArrayList<>();
            for (OSSObjectSummary summary : result.getObjectSummaries()) {
                objects.add(OssObject.builder()
                        .objectName(summary.getKey())
                        .bucket(bucket)
                        .size(summary.getSize())
                        .hash(summary.getETag())
                        .url(getObjectUrl(bucket, summary.getKey()))
                        .lastModified(summary.getLastModified() != null
                                ? summary.getLastModified().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDateTime()
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
            // 基于哈希值构建去重路径: hash 前两位作为目录 + 完整 hash 作为文件名
            String ext = FileUtil.getExtension(objectName);
            String deduplicatedName = "dedup/" + hash.substring(0, 2) + "/" + hash
                    + (ext.isEmpty() ? "" : "." + ext);

            // 检查是否已存在相同哈希的对象
            if (ossClient.doesObjectExist(bucket, deduplicatedName)) {
                OssObject existing = statObject(bucket, deduplicatedName);
                existing.setOriginalFilename(objectName);
                return existing;
            }

            // 不存在则上传到去重路径
            return putObject(bucket, deduplicatedName, inputStream, size, contentType);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw new OssException("去重上传失败: " + objectName, e);
        }
    }

    @Override
    public String getThumbnailUrl(String bucket, String objectName, int width, int height) {
        // 阿里云 OSS 内置图片处理：通过 URL 参数实现服务端缩放
        return getObjectUrl(bucket, objectName)
                + "?x-oss-process=image/resize,m_lfit,w_" + width + ",h_" + height;
    }

    /**
     * 断点续传上传大文件。
     * <p>
     * 利用阿里云 SDK 的分片上传能力，支持断点续传。
     * 当文件大小超过指定阈值时，建议使用此方法代替普通上传。
     *
     * @param bucket     存储桶名称
     * @param objectName 对象名称
     * @param filePath   本地文件路径
     * @param partSize   分片大小（字节），建议 1MB~5MB
     * @return 上传后的对象元信息
     * @throws OssException 操作失败时抛出
     */
    public OssObject resumableUpload(String bucket, String objectName,
                                     String filePath, long partSize) {
        try {
            UploadFileRequest uploadRequest = new UploadFileRequest(bucket, objectName);
            uploadRequest.setUploadFile(filePath);
            uploadRequest.setPartSize(partSize);
            // 启用断点续传：开启 Checkpoint，SDK 会在本地记录上传进度
            uploadRequest.setEnableCheckpoint(true);
            uploadRequest.setTaskNum(5); // 并发上传线程数

            UploadFileResult result = ossClient.uploadFile(uploadRequest);

            return OssObject.builder()
                    .objectName(objectName)
                    .bucket(bucket)
                    .hash(result.getMultipartUploadResult().getETag())
                    .url(getObjectUrl(bucket, objectName))
                    .lastModified(LocalDateTime.now())
                    .build();
        } catch (Throwable e) {
            throw new OssException("断点续传上传失败: " + objectName, e);
        }
    }
}
