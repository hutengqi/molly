package cn.molly.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Molly 缓存组件配置属性。
 * <p>
 * 绑定前缀为 {@code molly.cache}，支持通过 {@code provider} 切换底层缓存实现，
 * 默认使用 Redis（基于 Redisson 客户端）。配置项覆盖以下维度：
 * <ul>
 *     <li>命名与 TTL（含防雪崩抖动）</li>
 *     <li>空值缓存（防穿透）</li>
 *     <li>分布式锁（防击穿）</li>
 *     <li>事务后置失效</li>
 *     <li>Redisson 连接参数</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
@Data
@ConfigurationProperties(prefix = "molly.cache")
public class MollyCacheProperties {

    /**
     * 缓存提供商，默认 REDIS。
     */
    private Provider provider = Provider.REDIS;

    /**
     * 全局 key 前缀，最终落盘 key = {@code keyPrefix + name + separator + key}。
     */
    private String keyPrefix = "molly:";

    /**
     * 命名空间与 key 之间的分隔符。
     */
    private String separator = ":";

    /**
     * 注解未显式声明 TTL 时采用的默认值。
     */
    private Duration defaultTtl = Duration.ofMinutes(30);

    /**
     * TTL 抖动系数（0~1），落盘时叠加 [-jitter, +jitter] 的随机偏移防雪崩，
     * 设置为 0 关闭抖动。
     */
    private double ttlJitter = 0.1;

    /**
     * 失效/更新类注解的默认事务后置策略。
     */
    private boolean afterCommit = true;

    /**
     * 空值缓存（防穿透）配置。
     */
    private NullValue nullValue = new NullValue();

    /**
     * 分布式锁（防击穿）配置。
     */
    private Lock lock = new Lock();

    /**
     * Redisson 连接配置，仅当容器内不存在 RedissonClient Bean 时生效。
     */
    private Redisson redisson = new Redisson();

    /**
     * 缓存提供商枚举。
     */
    public enum Provider {
        /** 基于 Redisson 的 Redis 缓存实现 */
        REDIS
    }

    /**
     * 空值缓存配置。
     */
    @Data
    public static class NullValue {

        /**
         * 是否启用空值占位缓存。
         */
        private boolean enabled = true;

        /**
         * 空值占位 TTL。
         */
        private Duration ttl = Duration.ofMinutes(1);
    }

    /**
     * 分布式锁配置。
     */
    @Data
    public static class Lock {

        /**
         * 是否启用基于 Redisson 的分布式锁能力。
         */
        private boolean enabled = true;

        /**
         * 获取锁的最大等待时间。
         */
        private Duration waitTime = Duration.ofSeconds(3);

        /**
         * 锁自动释放时间（租期）。
         */
        private Duration leaseTime = Duration.ofSeconds(10);
    }

    /**
     * Redisson 连接配置，支持 Single 模式快速配置；
     * 高级拓扑（Sentinel/Cluster）请自定义 {@code RedissonClient} Bean。
     */
    @Data
    public static class Redisson {

        /**
         * Redis 连接地址，例如 redis://127.0.0.1:6379。
         */
        private String address = "redis://127.0.0.1:6379";

        /**
         * 数据库索引。
         */
        private int database = 0;

        /**
         * 认证密码，未启用认证时留空。
         */
        private String password;

        /**
         * 连接池最小空闲连接数。
         */
        private int connectionMinimumIdleSize = 8;

        /**
         * 连接池最大连接数。
         */
        private int connectionPoolSize = 32;
    }
}
