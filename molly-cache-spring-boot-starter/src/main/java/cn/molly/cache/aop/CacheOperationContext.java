package cn.molly.cache.aop;

import cn.molly.cache.core.CacheException;
import cn.molly.cache.core.CacheTemplate;
import cn.molly.cache.core.SpelEvaluator;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 缓存切面共享上下文。
 * <p>
 * 封装 {@link ProceedingJoinPoint} 所需的方法反射元数据、参数、返回值以及
 * SpEL 求值上下文，供注解处理器按需懒求值；同时提供通用工具方法用于
 * 解析 Duration、解析集合型 SpEL 等。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
public class CacheOperationContext {

    private final Object target;

    private final Method method;

    private final Object[] args;

    private final SpelEvaluator spelEvaluator;

    private final CacheTemplate cacheTemplate;

    private Object result;

    private EvaluationContext evaluationContext;

    /**
     * 构造上下文。
     *
     * @param joinPoint     AOP 连接点
     * @param spelEvaluator SpEL 求值器
     * @param cacheTemplate 缓存门面
     */
    public CacheOperationContext(ProceedingJoinPoint joinPoint,
                                 SpelEvaluator spelEvaluator,
                                 CacheTemplate cacheTemplate) {
        this.target = joinPoint.getTarget();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        this.method = signature.getMethod();
        this.args = joinPoint.getArgs();
        this.spelEvaluator = spelEvaluator;
        this.cacheTemplate = cacheTemplate;
    }

    /**
     * 设置方法返回值并刷新 {@code #result} 变量。
     *
     * @param result 方法返回值
     */
    public void setResult(Object result) {
        this.result = result;
        this.evaluationContext = null;
    }

    /**
     * 获取或构造 SpEL 求值上下文。
     *
     * @return 求值上下文
     */
    public EvaluationContext getEvaluationContext() {
        if (evaluationContext == null) {
            evaluationContext = spelEvaluator.buildContext(target, method, args, result);
        }
        return evaluationContext;
    }

    /**
     * 求值条件型表达式，空表达式返回默认值。
     *
     * @param expression   SpEL
     * @param defaultValue 默认值
     * @return 布尔结果
     */
    public boolean evaluateBoolean(String expression, boolean defaultValue) {
        return spelEvaluator.evaluateBoolean(expression, getEvaluationContext(), defaultValue);
    }

    /**
     * 求值为 Object。
     *
     * @param expression SpEL
     * @return 结果
     */
    public Object evaluate(String expression) {
        return spelEvaluator.evaluate(expression, getEvaluationContext());
    }

    /**
     * 求值为指定类型。
     *
     * @param expression SpEL
     * @param type       目标类型
     * @param <T>        泛型
     * @return 结果
     */
    public <T> T evaluate(String expression, Class<T> type) {
        return spelEvaluator.evaluate(expression, getEvaluationContext(), type);
    }

    /**
     * 求值为 String 列表，支持 Collection/Array。
     *
     * @param expression SpEL
     * @return 字符串列表
     */
    public List<String> evaluateStringList(String expression) {
        Object value = evaluate(expression);
        if (value == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        if (value instanceof Collection<?> coll) {
            for (Object item : coll) {
                list.add(String.valueOf(item));
            }
        } else if (value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < len; i++) {
                Object item = java.lang.reflect.Array.get(value, i);
                list.add(String.valueOf(item));
            }
        } else {
            list.add(String.valueOf(value));
        }
        return list;
    }

    /**
     * 求值集合 SpEL。
     *
     * @param expression SpEL
     * @return 集合；未求值到集合则抛出异常
     */
    @SuppressWarnings("unchecked")
    public Collection<Object> evaluateCollection(String expression) {
        Object value = evaluate(expression);
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection<?>) {
            return (Collection<Object>) value;
        }
        if (value.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            for (Object item : (Object[]) value) {
                list.add(item);
            }
            return list;
        }
        throw new CacheException("SpEL 表达式期望求值为 Collection: " + expression);
    }

    /**
     * 解析 Duration 字符串，空值返回 null。
     *
     * @param text ISO-8601 Duration 字符串
     * @return Duration
     */
    public static Duration parseDuration(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return Duration.parse(text);
        } catch (Exception e) {
            throw new CacheException("无效的 Duration 表达式: " + text, e);
        }
    }

    /**
     * 获取被拦截方法。
     *
     * @return 方法
     */
    public Method getMethod() {
        return method;
    }

    /**
     * 获取目标对象。
     *
     * @return 目标对象
     */
    public Object getTarget() {
        return target;
    }

    /**
     * 获取方法实参。
     *
     * @return 实参数组
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * 获取方法返回值。
     *
     * @return 返回值
     */
    public Object getResult() {
        return result;
    }

    /**
     * 获取缓存模板。
     *
     * @return 缓存模板
     */
    public CacheTemplate getCacheTemplate() {
        return cacheTemplate;
    }
}
