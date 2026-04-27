package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于 Hash 结构的缓存写注解。
 * <p>
 * 支持三种写模式：
 * <ul>
 *     <li>{@code field} 非空：写入单个 subkey，默认值为 {@code #result}</li>
 *     <li>{@code fields} 非空且返回值为 Map：批量写入多个 subkey</li>
 *     <li>两者均为空且返回值为 Map：整表覆盖写入</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyHashCachePut {

    /**
     * 缓存命名空间。
     *
     * @return 命名空间
     */
    String name();

    /**
     * Hash 业务 key 的 SpEL 表达式。
     *
     * @return SpEL 表达式
     */
    String key();

    /**
     * 单 subkey 的 SpEL 表达式。
     *
     * @return SpEL 表达式
     */
    String field() default "";

    /**
     * 多 subkey 的 SpEL 表达式。
     *
     * @return SpEL 表达式
     */
    String fields() default "";

    /**
     * 写入值的 SpEL 表达式，默认 {@code #result}。
     *
     * @return SpEL 表达式
     */
    String value() default "#result";

    /**
     * TTL。
     *
     * @return Duration 字符串
     */
    String ttl() default "";

    /**
     * 执行条件。
     *
     * @return SpEL 表达式
     */
    String condition() default "";

    /**
     * 跳过写入条件。
     *
     * @return SpEL 表达式
     */
    String unless() default "";

    /**
     * 是否缓存空值占位。
     *
     * @return true 启用
     */
    boolean cacheNull() default true;
}
