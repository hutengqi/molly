package cn.molly.oss.util;

import cn.molly.oss.core.OssException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 文件操作工具类。
 * <p>
 * 提供 MD5 哈希计算、唯一对象名生成、缩略图生成、文件类型判断等通用能力，
 * 供 {@link cn.molly.oss.core.OssTemplate} 各实现及 HTTP 端点使用。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
public final class FileUtil {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/tiff"
    );

    private FileUtil() {
    }

    /**
     * 计算输入流的 MD5 哈希值。
     * <p>
     * 注意：此方法会读取整个流，调用后流将被消耗完毕。
     * 若后续还需要使用流内容，应提前缓存为字节数组。
     *
     * @param inputStream 文件输入流
     * @return 32 位小写十六进制 MD5 字符串
     * @throws OssException 计算失败时抛出
     */
    public static String calculateMd5(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new OssException("计算文件 MD5 失败", e);
        }
    }

    /**
     * 计算字节数组的 MD5 哈希值。
     *
     * @param data 字节数据
     * @return 32 位小写十六进制 MD5 字符串
     * @throws OssException 计算失败时抛出
     */
    public static String calculateMd5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return bytesToHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new OssException("计算文件 MD5 失败", e);
        }
    }

    /**
     * 生成唯一的对象名称。
     * <p>
     * 格式: {@code yyyy/MM/dd/uuid.ext}
     * 示例: {@code 2026/04/23/550e8400-e29b-41d4-a716-446655440000.png}
     *
     * @param originalFilename 原始文件名
     * @return 唯一对象名称
     */
    public static String generateObjectName(String originalFilename) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String uuid = UUID.randomUUID().toString();
        String ext = getExtension(originalFilename);
        return datePath + "/" + uuid + (ext.isEmpty() ? "" : "." + ext);
    }

    /**
     * 生成缩略图对象名称。
     * <p>
     * 在原始对象名的扩展名前插入 {@code _thumb} 后缀。
     * 示例: {@code 2026/04/23/uuid.png} -> {@code 2026/04/23/uuid_thumb.png}
     *
     * @param objectName 原始对象名称
     * @return 缩略图对象名称
     */
    public static String getThumbnailObjectName(String objectName) {
        int dotIndex = objectName.lastIndexOf('.');
        if (dotIndex > 0) {
            return objectName.substring(0, dotIndex) + "_thumb" + objectName.substring(dotIndex);
        }
        return objectName + "_thumb";
    }

    /**
     * 生成缩略图。
     * <p>
     * 使用 Java ImageIO 读取原图并等比缩放到指定尺寸，保持图片质量。
     * 输出格式与原图相同，默认 PNG。
     *
     * @param imageData 原始图片字节数据
     * @param width     缩略图宽度
     * @param height    缩略图高度
     * @param format    输出格式（如 "png", "jpg"）
     * @return 缩略图的字节数组输入流
     * @throws OssException 生成失败时抛出
     */
    public static InputStream generateThumbnail(byte[] imageData, int width, int height, String format) {
        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (originalImage == null) {
                throw new OssException("无法读取图片数据，可能不是有效的图片格式");
            }

            // 等比缩放：计算目标尺寸，保持宽高比
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            double ratio = Math.min((double) width / originalWidth, (double) height / originalHeight);
            int targetWidth = (int) (originalWidth * ratio);
            int targetHeight = (int) (originalHeight * ratio);

            // 使用高质量缩放算法
            BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String outputFormat = (format == null || format.isEmpty()) ? "png" : format;
            ImageIO.write(thumbnail, outputFormat, baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            throw new OssException("生成缩略图失败", e);
        }
    }

    /**
     * 判断给定的 MIME 类型是否为图片。
     *
     * @param contentType MIME 类型
     * @return 是图片返回 true
     */
    public static boolean isImage(String contentType) {
        return contentType != null && IMAGE_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * 提取文件扩展名（不含点号）。
     *
     * @param filename 文件名
     * @return 扩展名，无扩展名时返回空字符串
     */
    public static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1).toLowerCase() : "";
    }

    /**
     * 根据文件扩展名推断图片输出格式。
     *
     * @param filename 文件名
     * @return 图片格式（如 "png", "jpg"），默认 "png"
     */
    public static String getImageFormat(String filename) {
        String ext = getExtension(filename);
        return switch (ext) {
            case "jpg", "jpeg" -> "jpg";
            case "gif" -> "gif";
            case "bmp" -> "bmp";
            case "webp" -> "webp";
            default -> "png";
        };
    }

    /**
     * 验证文件名安全性，过滤路径穿越字符。
     *
     * @param filename 文件名
     * @return 安全的文件名
     * @throws OssException 包含非法字符时抛出
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new OssException("文件名不能为空");
        }
        // 过滤路径穿越和特殊字符
        String sanitized = filename.replace("..", "")
                .replace("/", "")
                .replace("\\", "")
                .replace("\0", "");
        if (sanitized.isEmpty()) {
            throw new OssException("文件名包含非法字符: " + filename);
        }
        return sanitized;
    }

    /**
     * 字节数组转十六进制字符串。
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}
