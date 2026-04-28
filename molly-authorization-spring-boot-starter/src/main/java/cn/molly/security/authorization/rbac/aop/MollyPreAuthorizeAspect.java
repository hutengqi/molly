package cn.molly.security.authorization.rbac.aop;

import cn.molly.security.authorization.rbac.annotation.MollyPreAuthorize;
import cn.molly.security.authorization.rbac.core.MollyPermissionEvaluator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * {@link MollyPreAuthorize} 注解的切面实现。
 * <p>
 * 在方法执行前根据注解语义判定当前主体是否具备所需权限，未通过则抛出
 * {@link AccessDeniedException}，由 {@code MollyAccessDeniedHandler} 统一返回 403。
 * 方法级注解优先于类级注解。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Aspect
public class MollyPreAuthorizeAspect {

    private final MollyPermissionEvaluator permissionEvaluator;

    /**
     * 构造切面。
     *
     * @param permissionEvaluator 权限评估器
     */
    public MollyPreAuthorizeAspect(MollyPermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * 拦截所有标注了 {@link MollyPreAuthorize} 的方法或所属类的方法。
     *
     * @param pjp 连接点
     * @return 原方法返回值
     * @throws Throwable 原方法抛出的异常
     */
    @Around("@annotation(cn.molly.security.authorization.rbac.annotation.MollyPreAuthorize) "
            + "|| @within(cn.molly.security.authorization.rbac.annotation.MollyPreAuthorize)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MollyPreAuthorize annotation = resolveAnnotation(pjp);
        if (annotation == null) {
            return pjp.proceed();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("未认证或会话已失效");
        }
        if (!checkPermission(authentication, annotation)) {
            throw new AccessDeniedException("无访问权限");
        }
        return pjp.proceed();
    }

    /**
     * 从连接点解析注解：优先方法级，其次类级。
     *
     * @param pjp 连接点
     * @return 解析到的注解；若不存在则返回 null
     */
    private MollyPreAuthorize resolveAnnotation(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        MollyPreAuthorize onMethod = AnnotationUtils.findAnnotation(method, MollyPreAuthorize.class);
        if (onMethod != null) {
            return onMethod;
        }
        return AnnotationUtils.findAnnotation(method.getDeclaringClass(), MollyPreAuthorize.class);
    }

    /**
     * 按 {@code perm > allPerm > anyPerm} 优先级判定权限。
     *
     * @param authentication 当前主体
     * @param annotation     注解实例
     * @return 是否通过
     */
    private boolean checkPermission(Authentication authentication, MollyPreAuthorize annotation) {
        String single = annotation.perm();
        String[] allPerm = annotation.allPerm();
        String[] anyPerm = annotation.anyPerm();

        boolean hasAny = (single != null && !single.isEmpty()) || allPerm.length > 0 || anyPerm.length > 0;
        if (!hasAny) {
            return true;
        }
        Set<String> owned = permissionEvaluator.loadPermissions(authentication);
        if (single != null && !single.isEmpty()) {
            return owned.contains(single);
        }
        if (allPerm.length > 0) {
            return Arrays.stream(allPerm).allMatch(owned::contains);
        }
        return Arrays.stream(anyPerm).anyMatch(owned::contains);
    }
}
