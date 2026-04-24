package cn.molly.oss.core;

/**
 * 对象存储统一异常。
 * <p>
 * 封装各种存储服务的底层异常，为上层提供统一的异常处理入口。
 * 所有 {@link OssTemplate} 实现在遇到底层 SDK 异常时，应统一包装为此异常抛出。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
public class OssException extends RuntimeException {

    public OssException(String message) {
        super(message);
    }

    public OssException(String message, Throwable cause) {
        super(message, cause);
    }
}
