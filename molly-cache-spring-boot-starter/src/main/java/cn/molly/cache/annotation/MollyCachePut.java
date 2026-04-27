package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 单 key 缓存写注解（始终写回）。
 * <p>
 * 不改变方法执行：方法正常返回后，把返回值按 {@code name + key} 写入缓存。
 * 适合"写库后主动更新缓存"的场景；若希望失效而非更新，请使用 {@link MollyCacheEvict}。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyCachePut {

    /**
     * 缓存命名空间。
     *
     * @return 命名空间
     */
    String name();

    /**
     * 业务 key 的 SpEL 表达式，通常引用 {@code #result.xxx}。
     *
     * @return SpEL 表达式
     */
    String key();

    /**
     * 写入值的 SpEL 表达式，默认为方法返回值 {@code #result}。
     *
     * @return SpEL 表达式
     */
    String value() default "#result";

    /**
     * 过期时间。
     *
     * @return TTL
     */
    String ttl() default "";

    /**
     * 执行条件。
     *
     * @return SpEL 表达式
     */
    String condition() default "";

    /**
     * 跳过写入的条件。
     *
     * @return SpEL 表达式
     */
    String unless() default "";

    /**
     * 是否缓存 {@code null} 值占位。
     *
     * @return true 启用
     */
    boolean cacheNull() default true;
}
