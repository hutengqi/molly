package cn.molly.cache.aop;

import cn.molly.cache.annotation.MollyCacheLock;
import cn.molly.cache.core.CacheException;
import cn.molly.cache.core.CacheTemplate;
import cn.molly.cache.core.SpelEvaluator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;

import java.time.Duration;

/**
 * {@link MollyCacheLock} 分布式锁切面。
 * <p>
 * 独立于主缓存切面并设置为最高优先级之一，使其位于事务切面之外，
 * 避免锁持有时间被事务扩展。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MollyCacheLockAspect {

    private final CacheTemplate cacheTemplate;

    private final SpelEvaluator spelEvaluator;

    /**
     * 构造锁切面。
     *
     * @param cacheTemplate 缓存门面
     * @param spelEvaluator SpEL 求值器
     */
    public MollyCacheLockAspect(CacheTemplate cacheTemplate, SpelEvaluator spelEvaluator) {
        this.cacheTemplate = cacheTemplate;
        this.spelEvaluator = spelEvaluator;
    }

    /**
     * 环绕 {@link MollyCacheLock} 注解方法，加锁后执行。
     *
     * @param joinPoint 连接点
     * @param lock      注解实例
     * @return 方法执行结果
     * @throws Throwable 方法本身抛出的异常
     */
    @Around("@annotation(lock)")
    public Object aroundLock(ProceedingJoinPoint joinPoint, MollyCacheLock lock) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        EvaluationContext ctx = spelEvaluator.buildContext(
                joinPoint.getTarget(), sig.getMethod(), joinPoint.getArgs(), null);
        Object key = spelEvaluator.evaluate(lock.key(), ctx);
        if (key == null) {
            throw new CacheException("@MollyCacheLock 的 key 表达式求值为空: " + lock.key());
        }
        Duration wait = CacheOperationContext.parseDuration(lock.waitTime());
        Duration lease = CacheOperationContext.parseDuration(lock.leaseTime());
        try {
            return cacheTemplate.executeWithLock(lock.name(), key, wait, lease, () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    throw new CacheLockInvocationException(e);
                }
            });
        } catch (CacheLockInvocationException e) {
            throw e.getCause();
        }
    }

    /**
     * 内部异常：将锁内方法抛出的受检异常/Error 包装为运行时异常，
     * 便于在 {@code Supplier} 语义中向上抛出。
     */
    static final class CacheLockInvocationException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        CacheLockInvocationException(Throwable cause) {
            super(cause);
        }
    }
}
