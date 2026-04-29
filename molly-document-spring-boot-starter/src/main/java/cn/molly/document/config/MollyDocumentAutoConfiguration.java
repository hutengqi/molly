package cn.molly.document.config;

import cn.molly.document.properties.MollyDocumentProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Molly 文档组件顶层自动配置。
 * <p>
 * 仅负责启用配置属性绑定。实际的 Bean 装配按子能力拆分在：
 * <ul>
 *     <li>{@link MollyWordAutoConfiguration} - Word 模板渲染</li>
 *     <li>{@link MollyExcelAutoConfiguration} - Excel 导入导出</li>
 *     <li>{@link MollyPdfAutoConfiguration} - PDF 模板填充</li>
 *     <li>{@link MollyEmailAutoConfiguration} - 邮件发送</li>
 * </ul>
 * 每个子配置均通过 {@code @ConditionalOnClass} 守门，仅当使用方显式引入对应依赖时才激活。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@AutoConfiguration
@EnableConfigurationProperties(MollyDocumentProperties.class)
public class MollyDocumentAutoConfiguration {
}
