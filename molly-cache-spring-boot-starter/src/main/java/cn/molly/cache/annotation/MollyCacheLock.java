package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式互斥锁注解。
 * <p>
 * 在方法执行期间持有基于 Redisson 的分布式锁；若在 {@code waitTime} 内无法获取锁，
 * 抛出 {@link cn.molly.cache.core.CacheException}。锁粒度由 {@code name + key} 决定，
 * 可用于并发防抖、幂等控制等场景。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyCacheLock {

    /**
     * 锁命名空间。
     *
     * @return 命名空间
     */
    String name();

    /**
     * 锁 key 的 SpEL 表达式。
     *
     * @return SpEL 表达式
     */
    String key();

    /**
     * 最大等待时间。
     *
     * @return Duration 字符串
     */
    String waitTime() default "";

    /**
     * 锁租期。
     *
     * @return Duration 字符串
     */
    String leaseTime() default "";
}
