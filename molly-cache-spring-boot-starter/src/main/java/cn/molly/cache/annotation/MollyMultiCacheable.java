package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多 key 批量缓存读注解。
 * <p>
 * 通过 {@code keys} 表达式解析出业务 key 集合，对应的缓存以独立 key 存储；
 * 命中项直接返回，缺失项由方法回源并根据 {@link #idExtractor()} 拆分回填。
 * 方法返回类型应为可迭代集合或 Map。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyMultiCacheable {

    /**
     * 缓存命名空间。
     *
     * @return 命名空间
     */
    String name();

    /**
     * 业务 key 集合的 SpEL 表达式，求值结果必须为 Collection。
     *
     * @return SpEL 表达式
     */
    String keys();

    /**
     * 从返回集合的单个元素中提取 key 的 SpEL 表达式，在元素上下文中求值；
     * 默认 {@code #this.id} 适用于带 id 字段的领域对象。
     *
     * @return SpEL 表达式
     */
    String idExtractor() default "#this.id";

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
     * 跳过回填的条件。
     *
     * @return SpEL 表达式
     */
    String unless() default "";

    /**
     * 是否为缺失 key 写入空值占位。
     *
     * @return true 启用
     */
    boolean cacheNull() default true;

    /**
     * 是否在 miss key 回源时加锁（命名空间粒度）。
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
