package cn.molly.document.pdf;

import cn.molly.document.core.DocumentException;
import cn.molly.document.core.TemplateLoader;
import cn.molly.document.properties.MollyDocumentProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * 基于 PDFBox 的默认 PDF 模板填充实现。
 * <p>
 * <strong>注意</strong>：模板需为包含 AcroForm 字段（如 {@code name}、{@code amount}）的 PDF；
 * 中文显示需模板本身已嵌入中文字体，否则表单字段将显示为空白或错位。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class DefaultPdfTemplate implements PdfTemplate {

    private final TemplateLoader templateLoader;
    private final MollyDocumentProperties.Pdf properties;

    public DefaultPdfTemplate(TemplateLoader templateLoader, MollyDocumentProperties.Pdf properties) {
        this.templateLoader = templateLoader;
        this.properties = properties;
    }

    @Override
    public void render(String templateName, Map<String, String> fields, OutputStream out) {
        byte[] bytes = templateLoader.readBytes(templateName);
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            if (form == null) {
                throw new DocumentException("PDF 模板不包含 AcroForm: " + templateName);
            }
            if (fields != null) {
                for (Map.Entry<String, String> e : fields.entrySet()) {
                    PDField field = form.getField(e.getKey());
                    if (field == null) {
                        continue;
                    }
                    try {
                        field.setValue(e.getValue() == null ? "" : e.getValue());
                    } catch (IOException ex) {
                        throw new DocumentException("PDF 字段赋值失败: " + e.getKey(), ex);
                    }
                }
            }
            if (properties.isFlatten()) {
                form.flatten();
            }
            doc.save(out);
            out.flush();
        } catch (IOException e) {
            throw new DocumentException("PDF 渲染失败: " + templateName, e);
        }
    }

}
