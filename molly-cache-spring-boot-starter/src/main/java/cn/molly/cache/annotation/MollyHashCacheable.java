package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于 Hash 结构的缓存读注解。
 * <p>
 * 支持三种读模式：
 * <ul>
 *     <li>{@code field} 非空：读取单个 subkey，方法返回值为对应值</li>
 *     <li>{@code fields} 非空：读取多个 subkey，方法返回值为 Map&lt;String, ?&gt;</li>
 *     <li>{@code field} 与 {@code fields} 均为空：读取整个 Hash，方法返回值为 Map&lt;String, ?&gt;</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyHashCacheable {

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
     * 单 subkey 的 SpEL 表达式；与 {@link #fields()} 二选一。
     *
     * @return SpEL 表达式
     */
    String field() default "";

    /**
     * 多 subkey 的 SpEL 表达式，求值结果须为 Collection&lt;String&gt;。
     *
     * @return SpEL 表达式
     */
    String fields() default "";

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
     * 跳过回填条件。
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

    /**
     * 是否启用分布式锁保护 miss。
     *
     * @return true 启用
     */
    boolean lock() default false;

    /**
     * 锁等待时间。
     *
     * @return Duration 字符串
     */
    String lockWaitTime() default "";

    /**
     * 锁租期。
     *
     * @return Duration 字符串
     */
    String lockLeaseTime() default "";
}
