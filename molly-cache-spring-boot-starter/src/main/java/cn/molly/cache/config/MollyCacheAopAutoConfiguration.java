package cn.molly.cache.config;

import cn.molly.cache.aop.MollyCacheAspect;
import cn.molly.cache.aop.MollyCacheLockAspect;
import cn.molly.cache.core.CacheTemplate;
import cn.molly.cache.core.SpelEvaluator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Molly 缓存切面自动配置。
 * <p>
 * 注册主缓存切面与独立的分布式锁切面；使用 AspectJ 动态代理（cglib 支持类代理），
 * 允许对未实现接口的类方法生效。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@AutoConfiguration
@AutoConfigureAfter(MollyCacheAutoConfiguration.class)
@ConditionalOnClass(ProceedingJoinPoint.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class MollyCacheAopAutoConfiguration {

    /**
     * 注册主缓存注解切面。
     *
     * @param template 缓存门面
     * @param spel     SpEL 求值器
     * @return 切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MollyCacheAspect mollyCacheAspect(CacheTemplate template, SpelEvaluator spel) {
        return new MollyCacheAspect(template, spel);
    }

    /**
     * 注册分布式锁切面。
     *
     * @param template 缓存门面
     * @param spel     SpEL 求值器
     * @return 切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MollyCacheLockAspect mollyCacheLockAspect(CacheTemplate template, SpelEvaluator spel) {
        return new MollyCacheLockAspect(template, spel);
    }
}
