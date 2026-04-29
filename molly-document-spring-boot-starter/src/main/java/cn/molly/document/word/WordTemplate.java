package cn.molly.document.word;

import java.io.OutputStream;
import java.util.Map;

/**
 * Word 文档渲染门面。
 * <p>
 * 基于模板 + 数据模型生成最终 docx 文档，默认实现基于 poi-tl。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public interface WordTemplate {

    /**
     * 渲染模板并返回字节数组。
     *
     * @param templateName 模板资源名（相对于 {@code molly.document.word.template-location}）
     * @param model        渲染数据模型
     * @return 渲染后的 docx 字节
     */
    byte[] render(String templateName, Map<String, Object> model);

    /**
     * 渲染模板并写入输出流。调用方负责关闭流。
     *
     * @param templateName 模板资源名
     * @param model        渲染数据模型
     * @param out          目标输出流
     */
    void render(String templateName, Map<String, Object> model, OutputStream out);
}
