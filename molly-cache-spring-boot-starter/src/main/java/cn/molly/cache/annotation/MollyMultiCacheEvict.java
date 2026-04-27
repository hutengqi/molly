package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多 key 批量缓存失效注解。
 * <p>
 * 通过 {@code keys} 表达式解析出业务 key 集合，使用 Redisson 批量操作
 * 在一次交互中原子化删除。可通过 {@link #afterCommit()} 延后到事务提交后执行。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyMultiCacheEvict {

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
     * 执行条件。
     *
     * @return SpEL 表达式
     */
    String condition() default "";

    /**
     * 是否在方法执行前失效。
     *
     * @return true 启用
     */
    boolean beforeInvocation() default false;

    /**
     * 是否在事务提交后失效。
     *
     * @return true 启用
     */
    boolean afterCommit() default true;
}
