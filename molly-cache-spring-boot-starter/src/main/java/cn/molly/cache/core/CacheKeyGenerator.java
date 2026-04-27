package cn.molly.cache.core;

import cn.molly.cache.properties.MollyCacheProperties;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 缓存 Key 生成器。
 * <p>
 * 统一负责将注解上的 {@code name} 与 SpEL 求值得到的 key 片段，按
 * {@code keyPrefix + name + separator + key} 的规则拼接为最终落盘 key，
 * 避免各业务模块在不同地方重复拼接导致规则漂移。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
public class CacheKeyGenerator {

    private final String keyPrefix;

    private final String separator;

    /**
     * 基于配置属性构造 key 生成器。
     *
     * @param properties 缓存组件配置
     */
    public CacheKeyGenerator(MollyCacheProperties properties) {
        this.keyPrefix = Objects.requireNonNullElse(properties.getKeyPrefix(), "");
        this.separator = Objects.requireNonNullElse(properties.getSeparator(), ":");
    }

    /**
     * 生成完整缓存 key。
     *
     * @param name 命名空间，不可为空
     * @param key  业务 key，可为空，为空时返回 {@code keyPrefix + name}
     * @return 最终落盘 key
     */
    public String build(String name, Object key) {
        if (StringUtils.isBlank(name)) {
            throw new CacheException("缓存命名空间 name 不能为空");
        }
        String namespace = keyPrefix + name;
        if (key == null) {
            return namespace;
        }
        return namespace + separator + key;
    }

    /**
     * 仅生成命名空间级别的 key（不包含业务 key），常用于整 key 失效。
     *
     * @param name 命名空间
     * @return {@code keyPrefix + name}
     */
    public String buildNamespace(String name) {
        return build(name, null);
    }
}
