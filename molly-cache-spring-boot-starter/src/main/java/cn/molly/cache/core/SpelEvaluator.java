package cn.molly.cache.core;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpEL 表达式求值器。
 * <p>
 * 所有缓存注解的 {@code key/keys/field/fields/condition/unless/idExtractor}
 * 均通过本求值器解析。求值上下文基于 {@link MethodBasedEvaluationContext}，
 * 支持访问方法参数、{@code #root.target}、{@code #root.args}、
 * {@code #result}（写回/失效阶段）等变量。表达式会按 key 缓存以降低开销。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
public class SpelEvaluator {

    /**
     * {@code #result} 变量名。
     */
    public static final String RESULT_VARIABLE = "result";

    private final ExpressionParser parser = new SpelExpressionParser();

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    /**
     * 构造一个绑定了当前方法调用上下文的 EvaluationContext。
     *
     * @param target 被代理对象
     * @param method 被拦截的方法
     * @param args   方法实参
     * @param result 方法返回值，可为空
     * @return 求值上下文
     */
    public EvaluationContext buildContext(Object target, Method method, Object[] args, Object result) {
        MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(
                target, method, args, parameterNameDiscoverer);
        ctx.setVariable(RESULT_VARIABLE, result);
        return ctx;
    }

    /**
     * 解析表达式为指定类型的值。
     *
     * @param expression SpEL 表达式；为空直接返回 {@code null}
     * @param context    求值上下文
     * @param type       期望返回类型
     * @param <T>        返回类型
     * @return 表达式求值结果，表达式为空时返回 {@code null}
     */
    public <T> T evaluate(String expression, EvaluationContext context, Class<T> type) {
        if (StringUtils.isBlank(expression)) {
            return null;
        }
        try {
            return getExpression(expression).getValue(context, type);
        } catch (Exception e) {
            throw new CacheException("解析 SpEL 表达式失败: " + expression, e);
        }
    }

    /**
     * 以 Object 形式解析表达式。
     *
     * @param expression SpEL 表达式
     * @param context    求值上下文
     * @return 表达式求值结果
     */
    public Object evaluate(String expression, EvaluationContext context) {
        return evaluate(expression, context, Object.class);
    }

    /**
     * 以布尔形式解析表达式，表达式为空时返回 {@code defaultValue}。
     *
     * @param expression   SpEL 表达式
     * @param context      求值上下文
     * @param defaultValue 默认值
     * @return 布尔求值结果
     */
    public boolean evaluateBoolean(String expression, EvaluationContext context, boolean defaultValue) {
        if (StringUtils.isBlank(expression)) {
            return defaultValue;
        }
        Boolean value = evaluate(expression, context, Boolean.class);
        return value != null ? value : defaultValue;
    }

    private Expression getExpression(String expression) {
        return expressionCache.computeIfAbsent(expression, parser::parseExpression);
    }

    /**
     * 在以 {@code rootObject} 为根的上下文中求值。
     * <p>
     * 主要用于批量注解中对单个结果元素执行 {@code idExtractor} 这类
     * 表达式，支持 {@code #this.id} 和 {@code #root.xxx}。
     *
     * @param expression SpEL
     * @param rootObject 根对象
     * @return 求值结果
     */
    public Object evaluateOn(String expression, Object rootObject) {
        if (StringUtils.isBlank(expression)) {
            return null;
        }
        try {
            StandardEvaluationContext ctx = new StandardEvaluationContext(rootObject);
            return getExpression(expression).getValue(ctx);
        } catch (Exception e) {
            throw new CacheException("解析 SpEL 表达式失败: " + expression, e);
        }
    }
}
