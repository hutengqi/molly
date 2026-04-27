package cn.molly.cache.core;

import java.io.Serializable;

/**
 * 缓存空值占位符。
 * <p>
 * 在 {@code cacheNull} 开启时，对回源得到 {@code null} 的场景写入该占位值，
 * 防止同一 key 反复穿透到底层数据源。读取时若命中该占位即视为业务上的空结果。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
public final class NullValue implements Serializable {

    /**
     * 全局唯一的空值占位实例。
     */
    public static final NullValue INSTANCE = new NullValue();

    private static final long serialVersionUID = 1L;

    private NullValue() {
    }

    /**
     * 判断给定对象是否代表空值占位。
     *
     * @param value 任意缓存值
     * @return 若为空值占位返回 true
     */
    public static boolean isNull(Object value) {
        return value instanceof NullValue;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullValue;
    }

    @Override
    public int hashCode() {
        return NullValue.class.hashCode();
    }

    @Override
    public String toString() {
        return "NullValue";
    }
}
