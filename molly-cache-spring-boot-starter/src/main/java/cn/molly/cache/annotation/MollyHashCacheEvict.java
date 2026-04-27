package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于 Hash 结构的缓存失效注解。
 * <p>
 * 支持三种失效模式：
 * <ul>
 *     <li>{@code field} 非空：删除单个 subkey</li>
 *     <li>{@code fields} 非空：批量删除多个 subkey</li>
 *     <li>{@link #allFields()} 为 true：删除整个 Hash key</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyHashCacheEvict {

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
     * 是否删除整个 Hash。
     *
     * @return true 删除整个 Hash
     */
    boolean allFields() default false;

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
