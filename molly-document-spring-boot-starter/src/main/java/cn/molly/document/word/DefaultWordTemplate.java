package cn.molly.document.word;

import cn.molly.document.core.DocumentException;
import cn.molly.document.core.TemplateLoader;
import com.deepoove.poi.XWPFTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 基于 poi-tl 的 Word 模板渲染默认实现。
 * <p>
 * 模板语法遵循 poi-tl 规范：{@code {{name}}} 文本、{@code {{?items}}} 循环、{@code {{@image}}} 图片等，
 * 具体请参考 <a href="http://deepoove.com/poi-tl/">poi-tl 官方文档</a>。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class DefaultWordTemplate implements WordTemplate {

    private final TemplateLoader templateLoader;

    public DefaultWordTemplate(TemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }

    @Override
    public byte[] render(String templateName, Map<String, Object> model) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(8192)) {
            render(templateName, model, out);
            return out.toByteArray();
        } catch (DocumentException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentException("Word 渲染失败: " + templateName, e);
        }
    }

    @Override
    public void render(String templateName, Map<String, Object> model, OutputStream out) {
        byte[] bytes = templateLoader.readBytes(templateName);
        try (InputStream in = new ByteArrayInputStream(bytes);
             XWPFTemplate template = XWPFTemplate.compile(in).render(model)) {
            template.write(out);
            out.flush();
        } catch (DocumentException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentException("Word 渲染失败: " + templateName, e);
        }
    }
}
