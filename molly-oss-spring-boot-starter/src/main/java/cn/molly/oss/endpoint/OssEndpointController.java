package cn.molly.oss.endpoint;

import cn.molly.oss.core.OssException;
import cn.molly.oss.core.OssObject;
import cn.molly.oss.core.OssTemplate;
import cn.molly.oss.core.UploadProgress;
import cn.molly.oss.properties.MollyOssProperties;
import cn.molly.oss.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 对象存储 HTTP 端点控制器。
 * <p>
 * 提供文件上传（单个/批量）、下载、缩略图、存在性检查、删除等 RESTful 接口。
 * 上传时自动计算文件 MD5 进行去重检查，对图片类型文件自动生成缩略图。
 * <p>
 * 默认路径前缀为 {@code /oss}，所有端点均可通过 {@code molly.oss.endpoint-enabled=false} 关闭。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
@RestController
@RequestMapping("/oss")
public class OssEndpointController {

    private static final Logger log = LoggerFactory.getLogger(OssEndpointController.class);

    private final OssTemplate ossTemplate;
    private final MollyOssProperties properties;

    public OssEndpointController(OssTemplate ossTemplate, MollyOssProperties properties) {
        this.ossTemplate = ossTemplate;
        this.properties = properties;
    }

    /**
     * 单文件上传。
     * <p>
     * 接收 multipart/form-data 格式的文件，自动进行以下处理：
     * <ol>
     *     <li>文件类型白名单校验（若配置了 allowedTypes）</li>
     *     <li>文件大小限制校验</li>
     *     <li>计算 MD5 哈希进行去重检查（秒传）</li>
     *     <li>生成唯一对象名（日期/UUID.ext）</li>
     *     <li>对图片自动生成缩略图</li>
     * </ol>
     *
     * @param file 上传的文件
     * @return 上传后的对象元信息，校验失败返回 400，存储异常返回 500
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);
        } catch (OssException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        try {
            OssObject result = doUpload(file);
            return ResponseEntity.ok(result);
        } catch (OssException e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 批量文件上传。
     * <p>
     * 逐个文件处理，单个文件失败不影响其他文件。
     * 返回每个文件的处理结果（含成功/失败状态）。
     *
     * @param files 上传的文件列表
     * @return 所有文件的处理结果列表
     */
    @PostMapping("/upload/batch")
    public ResponseEntity<List<Map<String, Object>>> uploadBatch(@RequestParam("files") List<MultipartFile> files) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                validateFile(file);
                OssObject obj = doUpload(file);
                results.add(Map.of(
                        "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                        "success", true,
                        "object", obj
                ));
            } catch (OssException e) {
                log.warn("批量上传中单文件失败: {}", file.getOriginalFilename(), e);
                results.add(Map.of(
                        "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                        "success", false,
                        "error", e.getMessage()
                ));
            }
        }
        return ResponseEntity.ok(results);
    }

    /**
     * 文件下载。
     * <p>
     * 使用 {@link StreamingResponseBody} 实现流式下载，避免将整个文件加载到内存。
     *
     * @param objectName 对象名称（URL 编码后传入，支持路径分隔符 /）
     * @return 文件流响应
     */
    @GetMapping("/download/**")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestAttribute(value = "org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping",
                    required = false) String fullPath,
            jakarta.servlet.http.HttpServletRequest request) {
        // 从请求路径中提取完整的 objectName（支持含 / 的路径）
        String objectName = extractObjectName(request, "/oss/download/");
        String bucket = properties.getBucket();

        if (!ossTemplate.objectExists(bucket, objectName)) {
            return ResponseEntity.notFound().build();
        }

        OssObject meta = ossTemplate.statObject(bucket, objectName);
        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = ossTemplate.getObject(bucket, objectName)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        };

        // 从 objectName 中提取文件名用于 Content-Disposition
        String filename = objectName.contains("/")
                ? objectName.substring(objectName.lastIndexOf('/') + 1) : objectName;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, meta.getContentType() != null
                        ? meta.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(meta.getSize()))
                .body(body);
    }

    /**
     * 获取缩略图。
     *
     * @param request HTTP 请求
     * @return 缩略图 URL 或重定向
     */
    @GetMapping("/thumbnail/**")
    public ResponseEntity<Map<String, String>> thumbnail(jakarta.servlet.http.HttpServletRequest request) {
        String objectName = extractObjectName(request, "/oss/thumbnail/");
        String bucket = properties.getBucket();

        if (!ossTemplate.objectExists(bucket, objectName)) {
            return ResponseEntity.notFound().build();
        }

        MollyOssProperties.Thumbnail thumbConfig = properties.getThumbnail();
        String thumbnailUrl = ossTemplate.getThumbnailUrl(
                bucket, objectName, thumbConfig.getWidth(), thumbConfig.getHeight());

        return ResponseEntity.ok(Map.of("thumbnailUrl", thumbnailUrl));
    }

    /**
     * 文件存在性检查。
     *
     * @param request HTTP 请求
     * @return 200 表示存在，404 表示不存在
     */
    @RequestMapping(value = "/exists/**", method = RequestMethod.HEAD)
    public ResponseEntity<Void> exists(jakarta.servlet.http.HttpServletRequest request) {
        String objectName = extractObjectName(request, "/oss/exists/");
        String bucket = properties.getBucket();

        if (ossTemplate.objectExists(bucket, objectName)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 删除文件。
     *
     * @param request HTTP 请求
     * @return 200 表示删除成功
     */
    @DeleteMapping("/delete/**")
    public ResponseEntity<Void> delete(jakarta.servlet.http.HttpServletRequest request) {
        String objectName = extractObjectName(request, "/oss/delete/");
        String bucket = properties.getBucket();
        ossTemplate.removeObject(bucket, objectName);
        return ResponseEntity.ok().build();
    }

    // ============================= 内部方法 =============================

    /**
     * 执行单个文件上传的核心逻辑。
     * <p>
     * 包含 MD5 计算 -> 去重检查 -> 上传 -> 缩略图生成完整流程。
     */
    private OssObject doUpload(MultipartFile file) {
        try {
            String bucket = properties.getBucket();
            String originalFilename = FileUtil.sanitizeFilename(file.getOriginalFilename());
            String contentType = file.getContentType();
            byte[] fileBytes = file.getBytes();
            long size = fileBytes.length;

            // 计算 MD5 哈希用于去重
            String hash = FileUtil.calculateMd5(fileBytes);
            String objectName = FileUtil.generateObjectName(originalFilename);

            // 使用带进度回调的上传，记录最终进度
            AtomicReference<UploadProgress> lastProgress = new AtomicReference<>();

            // 去重上传：若已存在相同内容则秒传
            OssObject result = ossTemplate.putObjectIfAbsent(
                    bucket, objectName, new ByteArrayInputStream(fileBytes),
                    size, contentType, hash);
            result.setOriginalFilename(originalFilename);
            result.setHash(hash);

            // 对图片文件自动生成缩略图
            if (properties.getThumbnail().isEnabled() && FileUtil.isImage(contentType)) {
                generateAndUploadThumbnail(bucket, result.getObjectName(), fileBytes, contentType);
                result.setThumbnailUrl(ossTemplate.getThumbnailUrl(
                        bucket, result.getObjectName(),
                        properties.getThumbnail().getWidth(),
                        properties.getThumbnail().getHeight()));
            }

            return result;
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw new OssException("文件上传处理失败: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * 生成并上传缩略图（仅用于 MinIO 等不支持服务端图片处理的存储服务）。
     */
    private void generateAndUploadThumbnail(String bucket, String objectName,
                                            byte[] imageData, String contentType) {
        try {
            String thumbnailObjectName = FileUtil.getThumbnailObjectName(objectName);
            String format = FileUtil.getImageFormat(objectName);
            MollyOssProperties.Thumbnail thumbConfig = properties.getThumbnail();

            InputStream thumbnailStream = FileUtil.generateThumbnail(
                    imageData, thumbConfig.getWidth(), thumbConfig.getHeight(), format);

            // 读取缩略图字节以获取大小
            byte[] thumbnailBytes = thumbnailStream.readAllBytes();
            ossTemplate.putObject(bucket, thumbnailObjectName,
                    new ByteArrayInputStream(thumbnailBytes),
                    thumbnailBytes.length, contentType);
        } catch (Exception e) {
            // 缩略图生成失败不阻断主流程，记录警告日志
            log.warn("生成缩略图失败, bucket={}, objectName={}", bucket, objectName, e);
        }
    }

    /**
     * 校验上传文件的大小和类型。
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new OssException("上传文件不能为空");
        }

        // 文件大小校验
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new OssException("文件大小超过限制: " + properties.getMaxFileSize() + " 字节");
        }

        // 文件类型白名单校验
        List<String> allowedTypes = properties.getAllowedTypes();
        if (allowedTypes != null && !allowedTypes.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || !allowedTypes.contains(contentType)) {
                throw new OssException("不支持的文件类型: " + contentType);
            }
        }
    }

    /**
     * 从请求 URI 中提取 objectName（支持含 / 的多级路径）。
     */
    private String extractObjectName(jakarta.servlet.http.HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());
        if (path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }
        throw new OssException("无法从请求路径中提取对象名称: " + uri);
    }
}
