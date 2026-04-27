package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 单 key 缓存失效注解。
 * <p>
 * 支持按 key 精确失效、按命名空间整体失效，以及事务提交后失效。
 * {@link #beforeInvocation()} 为 true 时在方法执行前失效；默认在方法返回后失效，
 * 且若处于活动事务中并开启 {@link #afterCommit()}，则注册到事务提交后阶段执行。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyCacheEvict {

    /**
     * 缓存命名空间。
     *
     * @return 命名空间
     */
    String name();

    /**
     * 业务 key 的 SpEL 表达式，当 {@link #allEntries()} 为 false 时必填。
     *
     * @return SpEL 表达式
     */
    String key() default "";

    /**
     * 是否清空整个命名空间（忽略 key）。
     *
     * @return true 清空整个命名空间
     */
    boolean allEntries() default false;

    /**
     * 执行条件。
     *
     * @return SpEL 表达式
     */
    String condition() default "";

    /**
     * 是否在方法执行前失效。
     *
     * @return true 则在方法执行前失效
     */
    boolean beforeInvocation() default false;

    /**
     * 是否在事务提交后失效（无事务时同步执行）。
     *
     * @return true 启用事务后置失效
     */
    boolean afterCommit() default true;
}
