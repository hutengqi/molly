package cn.molly.document.core;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 模板资源加载器。
 * <p>
 * 基于 Spring 的 {@link ResourcePatternResolver} 从 {@code classpath:} / {@code file:} 等位置按名称加载模板，
 * 当底层能力（如 poi-tl、PDFBox）需要一次性读取模板字节时提供字节数组缓存。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class TemplateLoader {

    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     * 已缓存的模板字节，避免重复 IO。
     */
    private final ConcurrentMap<String, byte[]> cache = new ConcurrentHashMap<>();

    private final String baseLocation;

    private final boolean cacheEnabled;

    public TemplateLoader(String baseLocation, boolean cacheEnabled) {
        this.baseLocation = Objects.requireNonNull(baseLocation, "baseLocation");
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * 按相对名称获取模板资源。
     *
     * @param name 相对于 {@code baseLocation} 的资源名，如 {@code contract.docx}
     * @return Spring Resource
     */
    public Resource getResource(String name) {
        String location = baseLocation.endsWith("/") ? baseLocation + name : baseLocation + "/" + name;
        Resource resource = resolver.getResource(location);
        if (!resource.exists()) {
            throw new DocumentException("模板资源不存在: " + location);
        }
        return resource;
    }

    /**
     * 按名称获取模板输入流。
     *
     * @param name 资源名
     * @return 输入流 (调用方负责关闭)
     */
    public InputStream openStream(String name) {
        try {
            return getResource(name).getInputStream();
        } catch (IOException e) {
            throw new DocumentException("打开模板失败: " + name, e);
        }
    }

    /**
     * 按名称获取模板字节（可缓存）。
     *
     * @param name 资源名
     * @return 字节数组
     */
    public byte[] readBytes(String name) {
        if (cacheEnabled) {
            return cache.computeIfAbsent(name, this::loadBytes);
        }
        return loadBytes(name);
    }

    private byte[] loadBytes(String name) {
        try (InputStream in = openStream(name)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new DocumentException("读取模板失败: " + name, e);
        }
    }

    /**
     * 清空缓存。
     */
    public void clearCache() {
        cache.clear();
    }
}
