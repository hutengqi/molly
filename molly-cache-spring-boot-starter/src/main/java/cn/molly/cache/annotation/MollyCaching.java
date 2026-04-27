package cn.molly.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 组合缓存注解。
 * <p>
 * 允许在同一方法上声明多个写/失效类注解，由切面在方法生命周期内按声明顺序原子触发，
 * 典型场景如：一次写库后同时失效单 key 与对应 Hash。注意：仅支持组合写与失效，
 * 读类注解的多重声明无明确语义。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyCaching {

    /**
     * 组合的单 key 写入操作。
     *
     * @return 注解数组
     */
    MollyCachePut[] put() default {};

    /**
     * 组合的单 key 失效操作。
     *
     * @return 注解数组
     */
    MollyCacheEvict[] evict() default {};

    /**
     * 组合的多 key 失效操作。
     *
     * @return 注解数组
     */
    MollyMultiCacheEvict[] multiEvict() default {};

    /**
     * 组合的 Hash 写入操作。
     *
     * @return 注解数组
     */
    MollyHashCachePut[] hashPut() default {};

    /**
     * 组合的 Hash 失效操作。
     *
     * @return 注解数组
     */
    MollyHashCacheEvict[] hashEvict() default {};
}
