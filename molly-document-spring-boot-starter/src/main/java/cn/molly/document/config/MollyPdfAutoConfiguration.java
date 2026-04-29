package cn.molly.document.config;

import cn.molly.document.core.TemplateLoader;
import cn.molly.document.pdf.DefaultPdfTemplate;
import cn.molly.document.pdf.PdfTemplate;
import cn.molly.document.properties.MollyDocumentProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * PDF 子能力自动配置：仅当 classpath 存在 PDFBox 时生效。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@AutoConfiguration(after = MollyDocumentAutoConfiguration.class)
@ConditionalOnClass(PDDocument.class)
public class MollyPdfAutoConfiguration {

    @Bean(name = "pdfTemplateLoader")
    @ConditionalOnMissingBean(name = "pdfTemplateLoader")
    public TemplateLoader pdfTemplateLoader(MollyDocumentProperties properties) {
        MollyDocumentProperties.Pdf pdf = properties.getPdf();
        return new TemplateLoader(pdf.getTemplateLocation(), true);
    }

    @Bean
    @ConditionalOnMissingBean
    public PdfTemplate pdfTemplate(@Qualifier("pdfTemplateLoader") TemplateLoader pdfTemplateLoader,
                                   MollyDocumentProperties properties) {
        return new DefaultPdfTemplate(pdfTemplateLoader, properties.getPdf());
    }
}
