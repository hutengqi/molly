package cn.molly.cache.aop;

import cn.molly.cache.annotation.MollyCacheEvict;
import cn.molly.cache.annotation.MollyCachePut;
import cn.molly.cache.annotation.MollyCacheable;
import cn.molly.cache.annotation.MollyCaching;
import cn.molly.cache.annotation.MollyHashCacheEvict;
import cn.molly.cache.annotation.MollyHashCachePut;
import cn.molly.cache.annotation.MollyHashCacheable;
import cn.molly.cache.annotation.MollyMultiCacheEvict;
import cn.molly.cache.annotation.MollyMultiCacheable;
import cn.molly.cache.core.CacheException;
import cn.molly.cache.core.CacheTemplate;
import cn.molly.cache.core.NullValue;
import cn.molly.cache.core.SpelEvaluator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Molly 缓存注解统一切面。
 * <p>
 * 在一个 @Around 中按声明语义完整调度所有缓存注解：
 * 事前 evict → 读类注解（Cacheable / MultiCacheable / HashCacheable）→ proceed
 * → 事后 put / evict / Caching 组合。通过 {@link CacheOperationContext}
 * 统一 SpEL 求值与工具方法。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
public class MollyCacheAspect {

    private final CacheTemplate template;

    private final SpelEvaluator spel;

    /**
     * 构造缓存切面。
     *
     * @param template 缓存门面
     * @param spel     SpEL 求值器
     */
    public MollyCacheAspect(CacheTemplate template, SpelEvaluator spel) {
        this.template = template;
        this.spel = spel;
    }

    /**
     * 统一环绕匹配任一 Molly 缓存注解的方法。
     *
     * @param joinPoint 连接点
     * @return 方法返回值（可能来自缓存）
     * @throws Throwable 方法本身抛出的异常
     */
    @Around("@annotation(cn.molly.cache.annotation.MollyCacheable) || "
            + "@annotation(cn.molly.cache.annotation.MollyCachePut) || "
            + "@annotation(cn.molly.cache.annotation.MollyCacheEvict) || "
            + "@annotation(cn.molly.cache.annotation.MollyMultiCacheable) || "
            + "@annotation(cn.molly.cache.annotation.MollyMultiCacheEvict) || "
            + "@annotation(cn.molly.cache.annotation.MollyHashCacheable) || "
            + "@annotation(cn.molly.cache.annotation.MollyHashCachePut) || "
            + "@annotation(cn.molly.cache.annotation.MollyHashCacheEvict) || "
            + "@annotation(cn.molly.cache.annotation.MollyCaching)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        CacheOperationContext ctx = new CacheOperationContext(joinPoint, spel, template);
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        MollyCacheable cacheable = method.getAnnotation(MollyCacheable.class);
        MollyCachePut put = method.getAnnotation(MollyCachePut.class);
        MollyCacheEvict evict = method.getAnnotation(MollyCacheEvict.class);
        MollyMultiCacheable multiCacheable = method.getAnnotation(MollyMultiCacheable.class);
        MollyMultiCacheEvict multiEvict = method.getAnnotation(MollyMultiCacheEvict.class);
        MollyHashCacheable hashCacheable = method.getAnnotation(MollyHashCacheable.class);
        MollyHashCachePut hashPut = method.getAnnotation(MollyHashCachePut.class);
        MollyHashCacheEvict hashEvict = method.getAnnotation(MollyHashCacheEvict.class);
        MollyCaching caching = method.getAnnotation(MollyCaching.class);

        // 1. 前置失效
        applyBeforeEvicts(evict, multiEvict, hashEvict, caching, ctx);

        // 2. 读类注解（按优先级依次）或直接执行
        Object result;
        if (cacheable != null) {
            result = handleCacheable(joinPoint, cacheable, ctx);
        } else if (multiCacheable != null) {
            result = handleMultiCacheable(joinPoint, multiCacheable, ctx);
        } else if (hashCacheable != null) {
            result = handleHashCacheable(joinPoint, hashCacheable, ctx);
        } else {
            result = joinPoint.proceed();
        }
        // 统一写入 ctx.result，便于后置注解访问 #result（包括缓存命中场景）
        ctx.setResult(result);

        // 3. 后置操作（put / evict / @MollyCaching 组合）
        applyAfter(put, evict, multiEvict, hashPut, hashEvict, caching, ctx);

        return result;
    }

    // ========================= 读类注解 =========================

    private Object handleCacheable(ProceedingJoinPoint pjp, MollyCacheable ann, CacheOperationContext ctx) throws Throwable {
        if (!ctx.evaluateBoolean(ann.condition(), true)) {
            return pjp.proceed();
        }
        Object key = requireKey(ctx.evaluate(ann.key()), "@MollyCacheable.key");
        String name = ann.name();
        Object hit = template.get(name, key);
        if (hit != null) {
            return NullValue.isNull(hit) ? null : hit;
        }
        Duration ttl = CacheOperationContext.parseDuration(ann.ttl());
        if (ann.lock()) {
            Duration wait = CacheOperationContext.parseDuration(ann.lockWaitTime());
            Duration lease = CacheOperationContext.parseDuration(ann.lockLeaseTime());
            try {
                return template.executeWithLock(name, key, wait, lease, () -> {
                    Object again = template.get(name, key);
                    if (again != null) {
                        return NullValue.isNull(again) ? null : again;
                    }
                    return proceedAndStore(pjp, ctx, name, key, ttl, ann.unless(), ann.cacheNull());
                });
            } catch (ProceedInvocationException e) {
                throw e.getCause();
            }
        }
        try {
            return proceedAndStore(pjp, ctx, name, key, ttl, ann.unless(), ann.cacheNull());
        } catch (ProceedInvocationException e) {
            throw e.getCause();
        }
    }

    private Object proceedAndStore(ProceedingJoinPoint pjp, CacheOperationContext ctx,
                                   String name, Object key, Duration ttl,
                                   String unless, boolean cacheNull) {
        try {
            Object result = pjp.proceed();
            ctx.setResult(result);
            if (!ctx.evaluateBoolean(unless, false)) {
                if (result == null) {
                    if (cacheNull) {
                        template.putNull(name, key);
                    }
                } else {
                    template.put(name, key, result, ttl);
                }
            }
            return result;
        } catch (Throwable t) {
            throw new ProceedInvocationException(t);
        }
    }

    private Object handleMultiCacheable(ProceedingJoinPoint pjp, MollyMultiCacheable ann, CacheOperationContext ctx) throws Throwable {
        if (!ctx.evaluateBoolean(ann.condition(), true)) {
            return pjp.proceed();
        }
        Collection<Object> keys = ctx.evaluateCollection(ann.keys());
        if (keys.isEmpty()) {
            return pjp.proceed();
        }
        Map<Object, Object> hits = template.multiGet(ann.name(), keys);
        if (hits.size() == keys.size()) {
            Object converted = convertMultiHits(ctx.getMethod(), hits);
            if (converted != null) {
                return converted;
            }
        }
        Object result = pjp.proceed();
        ctx.setResult(result);
        if (ctx.evaluateBoolean(ann.unless(), false)) {
            return result;
        }
        Duration ttl = CacheOperationContext.parseDuration(ann.ttl());
        writeBackMulti(ann, keys, result, ttl);
        return result;
    }

    /**
     * 根据方法返回类型尝试装配全命中时的返回值；无法装配返回 null 让切面继续 proceed。
     */
    private Object convertMultiHits(Method method, Map<Object, Object> hits) {
        Class<?> returnType = method.getReturnType();
        if (Map.class.isAssignableFrom(returnType)) {
            Map<Object, Object> result = new LinkedHashMap<>(hits.size());
            hits.forEach((k, v) -> result.put(k, NullValue.isNull(v) ? null : v));
            return result;
        }
        if (Collection.class.isAssignableFrom(returnType)) {
            java.util.ArrayList<Object> result = new java.util.ArrayList<>(hits.size());
            for (Object v : hits.values()) {
                result.add(NullValue.isNull(v) ? null : v);
            }
            return result;
        }
        return null;
    }

    private void writeBackMulti(MollyMultiCacheable ann, Collection<Object> keys, Object result, Duration ttl) {
        if (result == null) {
            return;
        }
        Map<Object, Object> toWrite = new LinkedHashMap<>();
        if (result instanceof Map<?, ?> resultMap) {
            resultMap.forEach(toWrite::put);
        } else if (result instanceof Collection<?> coll) {
            for (Object item : coll) {
                if (item == null) {
                    continue;
                }
                Object id = spel.evaluateOn(ann.idExtractor(), item);
                if (id != null) {
                    toWrite.put(id, item);
                }
            }
        } else {
            return;
        }
        if (ann.cacheNull()) {
            for (Object k : keys) {
                if (!toWrite.containsKey(k)) {
                    toWrite.put(k, NullValue.INSTANCE);
                }
            }
        }
        template.multiPut(ann.name(), toWrite, ttl);
    }

    private Object handleHashCacheable(ProceedingJoinPoint pjp, MollyHashCacheable ann, CacheOperationContext ctx) throws Throwable {
        if (!ctx.evaluateBoolean(ann.condition(), true)) {
            return pjp.proceed();
        }
        Object key = requireKey(ctx.evaluate(ann.key()), "@MollyHashCacheable.key");
        String name = ann.name();
        Duration ttl = CacheOperationContext.parseDuration(ann.ttl());

        // 模式一：单 subkey
        if (!ann.field().isBlank()) {
            String field = String.valueOf(ctx.evaluate(ann.field()));
            Object hit = template.hGet(name, key, field);
            if (hit != null) {
                return NullValue.isNull(hit) ? null : hit;
            }
            Object result = pjp.proceed();
            ctx.setResult(result);
            if (!ctx.evaluateBoolean(ann.unless(), false)) {
                if (result == null) {
                    if (ann.cacheNull()) {
                        template.hPut(name, key, field, NullValue.INSTANCE, ttl);
                    }
                } else {
                    template.hPut(name, key, field, result, ttl);
                }
            }
            return result;
        }

        // 模式二：多 subkey
        if (!ann.fields().isBlank()) {
            List<String> fields = ctx.evaluateStringList(ann.fields());
            if (!fields.isEmpty()) {
                Map<String, Object> hits = template.hMultiGet(name, key, fields);
                if (hits.size() == fields.size()) {
                    return unwrapHashMap(hits);
                }
            }
            Object result = pjp.proceed();
            ctx.setResult(result);
            if (!ctx.evaluateBoolean(ann.unless(), false) && result instanceof Map<?, ?> resultMap) {
                template.hPutAll(name, key, toWritableMap(resultMap, ann.cacheNull()), ttl);
            }
            return result;
        }

        // 模式三：整表
        Map<String, Object> all = template.hGetAll(name, key);
        if (!all.isEmpty()) {
            return unwrapHashMap(all);
        }
        Object result = pjp.proceed();
        ctx.setResult(result);
        if (!ctx.evaluateBoolean(ann.unless(), false) && result instanceof Map<?, ?> resultMap) {
            template.hPutAll(name, key, toWritableMap(resultMap, ann.cacheNull()), ttl);
        }
        return result;
    }

    private Map<String, Object> unwrapHashMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>(source.size());
        source.forEach((f, v) -> result.put(f, NullValue.isNull(v) ? null : v));
        return result;
    }

    private Map<String, Object> toWritableMap(Map<?, ?> source, boolean cacheNull) {
        Map<String, Object> target = new LinkedHashMap<>(source.size());
        source.forEach((f, v) -> {
            if (f == null) {
                return;
            }
            if (v == null) {
                if (cacheNull) {
                    target.put(String.valueOf(f), NullValue.INSTANCE);
                }
            } else {
                target.put(String.valueOf(f), v);
            }
        });
        return target;
    }

    // ========================= 写类/失效类注解 =========================

    private void applyBeforeEvicts(MollyCacheEvict evict, MollyMultiCacheEvict multiEvict,
                                   MollyHashCacheEvict hashEvict, MollyCaching caching,
                                   CacheOperationContext ctx) {
        if (evict != null && evict.beforeInvocation()) {
            applyCacheEvict(evict, ctx, true);
        }
        if (multiEvict != null && multiEvict.beforeInvocation()) {
            applyMultiEvict(multiEvict, ctx, true);
        }
        if (hashEvict != null && hashEvict.beforeInvocation()) {
            applyHashEvict(hashEvict, ctx, true);
        }
        if (caching != null) {
            for (MollyCacheEvict e : caching.evict()) {
                if (e.beforeInvocation()) {
                    applyCacheEvict(e, ctx, true);
                }
            }
            for (MollyMultiCacheEvict e : caching.multiEvict()) {
                if (e.beforeInvocation()) {
                    applyMultiEvict(e, ctx, true);
                }
            }
            for (MollyHashCacheEvict e : caching.hashEvict()) {
                if (e.beforeInvocation()) {
                    applyHashEvict(e, ctx, true);
                }
            }
        }
    }

    private void applyAfter(MollyCachePut put, MollyCacheEvict evict, MollyMultiCacheEvict multiEvict,
                            MollyHashCachePut hashPut, MollyHashCacheEvict hashEvict, MollyCaching caching,
                            CacheOperationContext ctx) {
        if (put != null) {
            applyCachePut(put, ctx);
        }
        if (hashPut != null) {
            applyHashPut(hashPut, ctx);
        }
        if (evict != null && !evict.beforeInvocation()) {
            applyCacheEvict(evict, ctx, false);
        }
        if (multiEvict != null && !multiEvict.beforeInvocation()) {
            applyMultiEvict(multiEvict, ctx, false);
        }
        if (hashEvict != null && !hashEvict.beforeInvocation()) {
            applyHashEvict(hashEvict, ctx, false);
        }
        if (caching != null) {
            for (MollyCachePut p : caching.put()) {
                applyCachePut(p, ctx);
            }
            for (MollyHashCachePut p : caching.hashPut()) {
                applyHashPut(p, ctx);
            }
            for (MollyCacheEvict e : caching.evict()) {
                if (!e.beforeInvocation()) {
                    applyCacheEvict(e, ctx, false);
                }
            }
            for (MollyMultiCacheEvict e : caching.multiEvict()) {
                if (!e.beforeInvocation()) {
                    applyMultiEvict(e, ctx, false);
                }
            }
            for (MollyHashCacheEvict e : caching.hashEvict()) {
                if (!e.beforeInvocation()) {
                    applyHashEvict(e, ctx, false);
                }
            }
        }
    }

    private void applyCachePut(MollyCachePut ann, CacheOperationContext ctx) {
        if (!ctx.evaluateBoolean(ann.condition(), true)) {
            return;
        }
        if (ctx.evaluateBoolean(ann.unless(), false)) {
            return;
        }
        Object key = requireKey(ctx.evaluate(ann.key()), "@MollyCachePut.key");
        Object value = ctx.evaluate(ann.value());
        Duration ttl = CacheOperationContext.parseDuration(ann.ttl());
        if (value == null) {
            if (ann.cacheNull()) {
                template.putNull(ann.name(), key);
            }
        } else {
            template.put(ann.name(), key, value, ttl);
        }
    }

    private void applyCacheEvict(MollyCacheEvict ann, CacheOperationContext ctx, boolean beforePhase) {
        if (!ctx.evaluateBoolean(ann.condition(), true)) {
            return;
        }
        Runnable action = ann.allEntries()
                ? () -> template.evictAll(ann.name())
                : () -> template.evict(ann.name(), requireKey(ctx.evaluate(ann.key()), "@MollyCacheEvict.key"));
        dispatch(action, beforePhase, ann.afterCommit());
    }

    private void applyMultiEvict(MollyMultiCacheEvict ann, CacheOperationContext ctx, boolean beforePhase) {
        if (!ctx.evaluateBoolean(ann.condition(), true)) {
            return;
        }
        Runnable action = () -> template.multiEvict(ann.name(), ctx.evaluateCollection(ann.keys()));
        dispatch(action, beforePhase, ann.afterCommit());
    }

    private void applyHashPut(MollyHashCachePut ann, CacheOperationContext ctx) {
        if (!ctx.evaluateBoolean(ann.condition(), true)) {
            return;
        }
        if (ctx.evaluateBoolean(ann.unless(), false)) {
            return;
        }
        Object key = requireKey(ctx.evaluate(ann.key()), "@MollyHashCachePut.key");
        Duration ttl = CacheOperationContext.parseDuration(ann.ttl());
        Object value = ctx.evaluate(ann.value());

        if (!ann.field().isBlank()) {
            String field = String.valueOf(ctx.evaluate(ann.field()));
            if (value == null) {
                if (ann.cacheNull()) {
                    template.hPut(ann.name(), key, field, NullValue.INSTANCE, ttl);
                }
            } else {
                template.hPut(ann.name(), key, field, value, ttl);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            template.hPutAll(ann.name(), key, toWritableMap(map, ann.cacheNull()), ttl);
        }
    }

    private void applyHashEvict(MollyHashCacheEvict ann, CacheOperationContext ctx, boolean beforePhase) {
        if (!ctx.evaluateBoolean(ann.condition(), true)) {
            return;
        }
        Runnable action;
        if (ann.allFields()) {
            action = () -> template.hEvictAll(ann.name(), requireKey(ctx.evaluate(ann.key()), "@MollyHashCacheEvict.key"));
        } else if (!ann.field().isBlank()) {
            action = () -> template.hEvict(ann.name(),
                    requireKey(ctx.evaluate(ann.key()), "@MollyHashCacheEvict.key"),
                    List.of(String.valueOf(ctx.evaluate(ann.field()))));
        } else if (!ann.fields().isBlank()) {
            action = () -> template.hEvict(ann.name(),
                    requireKey(ctx.evaluate(ann.key()), "@MollyHashCacheEvict.key"),
                    ctx.evaluateStringList(ann.fields()));
        } else {
            throw new CacheException("@MollyHashCacheEvict 必须指定 field/fields 之一或 allFields=true");
        }
        dispatch(action, beforePhase, ann.afterCommit());
    }

    private void dispatch(Runnable action, boolean beforePhase, boolean afterCommit) {
        if (!beforePhase && afterCommit) {
            template.getFlusher().runAfterCommit(action);
        } else {
            action.run();
        }
    }

    private static Object requireKey(Object key, String hint) {
        if (key == null) {
            throw new CacheException(hint + " 求值为空，请检查 SpEL 表达式或参数");
        }
        return key;
    }

    /**
     * 内部异常：在 Supplier 语义下包装 proceed 抛出的异常。
     */
    static final class ProceedInvocationException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        ProceedInvocationException(Throwable cause) {
            super(cause);
        }
    }
}
