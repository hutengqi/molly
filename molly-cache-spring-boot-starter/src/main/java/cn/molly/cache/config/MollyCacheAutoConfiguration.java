package cn.molly.cache.config;

import cn.molly.cache.core.CacheKeyGenerator;
import cn.molly.cache.core.CacheOperations;
import cn.molly.cache.core.CacheTemplate;
import cn.molly.cache.core.SpelEvaluator;
import cn.molly.cache.properties.MollyCacheProperties;
import cn.molly.cache.support.redis.RedissonCacheOperations;
import cn.molly.cache.sync.TransactionAwareCacheFlusher;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Molly 缓存组件自动配置。
 * <p>
 * 默认基于 Redisson 构建 Redis 实现；所有 Bean 均使用 {@code @ConditionalOnMissingBean}，
 * 允许使用者通过声明同类型 Bean 完全覆盖。{@link RedissonClient} 也允许使用者自带，
 * 便于接入 Sentinel/Cluster 等复杂拓扑。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@AutoConfiguration
@EnableConfigurationProperties(MollyCacheProperties.class)
public class MollyCacheAutoConfiguration {

    /**
     * 创建事务同步执行器（无条件，逻辑上缓存组件必备）。
     *
     * @return 事务感知执行器
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionAwareCacheFlusher transactionAwareCacheFlusher() {
        return new TransactionAwareCacheFlusher();
    }

    /**
     * 创建 SpEL 求值器。
     *
     * @return SpEL 求值器
     */
    @Bean
    @ConditionalOnMissingBean
    public SpelEvaluator mollyCacheSpelEvaluator() {
        return new SpelEvaluator();
    }

    /**
     * 创建 Key 生成器。
     *
     * @param properties 配置属性
     * @return Key 生成器
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheKeyGenerator mollyCacheKeyGenerator(MollyCacheProperties properties) {
        return new CacheKeyGenerator(properties);
    }

    /**
     * 创建缓存门面（CacheTemplate）。
     *
     * @param operations   底层操作 SPI
     * @param keyGenerator Key 生成器
     * @param properties   配置属性
     * @param flusher      事务同步执行器
     * @return 缓存门面
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheTemplate mollyCacheTemplate(CacheOperations operations,
                                            CacheKeyGenerator keyGenerator,
                                            MollyCacheProperties properties,
                                            TransactionAwareCacheFlusher flusher) {
        return new CacheTemplate(operations, keyGenerator, properties, flusher);
    }

    /**
     * Redis 提供商条件配置。仅当 classpath 存在 Redisson 且 provider 为 redis 时激活。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnProperty(name = "molly.cache.provider", havingValue = "redis", matchIfMissing = true)
    static class RedisCacheConfiguration {

        /**
         * 当容器中不存在 {@link RedissonClient} 时，基于配置属性创建 Single 模式客户端。
         *
         * @param properties 配置属性
         * @return Redisson 客户端
         */
        @Bean(destroyMethod = "shutdown")
        @ConditionalOnMissingBean(RedissonClient.class)
        public RedissonClient mollyRedissonClient(MollyCacheProperties properties) {
            MollyCacheProperties.Redisson redisson = properties.getRedisson();
            Config config = new Config();
            SingleServerConfig server = config.useSingleServer()
                    .setAddress(redisson.getAddress())
                    .setDatabase(redisson.getDatabase())
                    .setConnectionMinimumIdleSize(redisson.getConnectionMinimumIdleSize())
                    .setConnectionPoolSize(redisson.getConnectionPoolSize());
            if (redisson.getPassword() != null && !redisson.getPassword().isEmpty()) {
                server.setPassword(redisson.getPassword());
            }
            return Redisson.create(config);
        }

        /**
         * 创建 Redisson 默认缓存操作实现。
         *
         * @param redissonClient Redisson 客户端
         * @return 默认实现
         */
        @Bean
        @ConditionalOnMissingBean(CacheOperations.class)
        public CacheOperations mollyRedissonCacheOperations(RedissonClient redissonClient) {
            return new RedissonCacheOperations(redissonClient);
        }
    }
}
