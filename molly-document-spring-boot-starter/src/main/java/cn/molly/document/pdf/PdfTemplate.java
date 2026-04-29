package cn.molly.document.pdf;

import java.io.OutputStream;
import java.util.Map;

/**
 * PDF 模板填充器。
 * <p>
 * 基于 PDFBox AcroForm 表单字段，将 {@code Map} 中的键值对写入对应字段后输出。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public interface PdfTemplate {

    /**
     * 按名称渲染模板（从配置的 {@code templateLocation} 路径加载）。
     *
     * @param templateName 模板名（如 {@code invoice.pdf}）
     * @param fields       字段名 -&gt; 值的映射
     * @param out          输出流
     */
    void render(String templateName, Map<String, String> fields, OutputStream out);
}
