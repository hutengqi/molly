package cn.molly.security.authorization.rbac.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Molly 风格的方法级鉴权注解。
 * <p>
 * 用法示例：
 * <pre>
 * &#64;MollyPreAuthorize(perm = "user:read")
 * public User getUser(Long id) { ... }
 *
 * &#64;MollyPreAuthorize(anyPerm = {"user:read", "user:admin"})
 * public List&lt;User&gt; list() { ... }
 *
 * &#64;MollyPreAuthorize(allPerm = {"user:read", "user:export"})
 * public void export() { ... }
 * </pre>
 * 三个字段按 {@code perm > allPerm > anyPerm} 的优先级判定，命中任一即进入对应匹配模式。
 * 全部为空时视为不鉴权（直接放行），等价于不加注解。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyPreAuthorize {

    /**
     * 单权限码，命中即放行。
     */
    String perm() default "";

    /**
     * 任一权限命中即放行。
     */
    String[] anyPerm() default {};

    /**
     * 所有权限必须命中才放行。
     */
    String[] allPerm() default {};
}
