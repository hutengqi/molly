package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 单 key 缓存读注解（Cache-Aside）。
 * <p>
 * 在方法调用前根据 {@code name + key} 查询缓存：命中则直接返回；未命中则执行方法，
 * 若满足 {@code unless} 之外的条件，将方法返回值写入缓存。默认开启空值占位、
 * TTL 抖动与（配置开启时的）分布式锁单飞。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyCacheable {

    /**
     * 缓存命名空间，最终 key = {@code keyPrefix + name + separator + key}。
     *
     * @return 命名空间
     */
    String name();

    /**
     * 业务 key 的 SpEL 表达式。
     *
     * @return SpEL 表达式
     */
    String key();

    /**
     * 过期时间，Duration 字符串（如 {@code PT30M}）；为空表示使用全局默认 TTL。
     *
     * @return TTL
     */
    String ttl() default "";

    /**
     * 执行条件（SpEL），为空表示始终执行缓存逻辑。
     *
     * @return SpEL 表达式
     */
    String condition() default "";

    /**
     * 跳过缓存回填的条件（SpEL），求值为 true 时不写入缓存。
     *
     * @return SpEL 表达式
     */
    String unless() default "";

    /**
     * 是否为空结果写入占位防穿透，默认沿用全局配置。
     *
     * @return true 启用空值缓存
     */
    boolean cacheNull() default true;

    /**
     * 是否使用分布式锁保护 miss 时的回源，防止缓存击穿。
     *
     * @return true 启用
     */
    boolean lock() default false;

    /**
     * 锁等待时间（Duration 字符串），为空时取全局配置。
     *
     * @return 等待时间
     */
    String lockWaitTime() default "";

    /**
     * 锁租期（Duration 字符串），为空时取全局配置。
     *
     * @return 租期
     */
    String lockLeaseTime() default "";
}
