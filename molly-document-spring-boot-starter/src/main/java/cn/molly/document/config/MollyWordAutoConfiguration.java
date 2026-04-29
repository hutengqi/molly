package cn.molly.document.config;

import cn.molly.document.core.TemplateLoader;
import cn.molly.document.properties.MollyDocumentProperties;
import cn.molly.document.word.DefaultWordTemplate;
import cn.molly.document.word.WordTemplate;
import com.deepoove.poi.XWPFTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Word 子能力自动配置：仅当 classpath 存在 {@link XWPFTemplate} 时生效。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@AutoConfiguration(after = MollyDocumentAutoConfiguration.class)
@ConditionalOnClass(XWPFTemplate.class)
public class MollyWordAutoConfiguration {

    @Bean(name = "wordTemplateLoader")
    @ConditionalOnMissingBean(name = "wordTemplateLoader")
    public TemplateLoader wordTemplateLoader(MollyDocumentProperties properties) {
        MollyDocumentProperties.Word word = properties.getWord();
        return new TemplateLoader(word.getTemplateLocation(), word.isCacheTemplates());
    }

    @Bean
    @ConditionalOnMissingBean
    public WordTemplate wordTemplate(@Qualifier("wordTemplateLoader") TemplateLoader wordTemplateLoader) {
        return new DefaultWordTemplate(wordTemplateLoader);
    }
}
