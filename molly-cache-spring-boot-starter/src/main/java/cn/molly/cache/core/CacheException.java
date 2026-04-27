package cn.molly.cache.core;

/**
 * 缓存组件内部统一异常。
 * <p>
 * 用于包装底层客户端异常、SpEL 解析失败、序列化失败等缓存层故障，
 * 使上层调用者仅需捕获本异常即可处理缓存子系统的所有错误。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
public class CacheException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 基于消息构造异常。
     *
     * @param message 异常描述
     */
    public CacheException(String message) {
        super(message);
    }

    /**
     * 基于消息与根因构造异常。
     *
     * @param message 异常描述
     * @param cause   根因
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
