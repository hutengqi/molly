package cn.molly.document.config;

import cn.molly.document.excel.DefaultExcelTemplate;
import cn.molly.document.excel.ExcelTemplate;
import cn.molly.document.excel.converter.ConverterRegistry;
import cn.molly.document.properties.MollyDocumentProperties;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Excel 子能力自动配置：仅当 classpath 存在 Apache POI 时生效。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@AutoConfiguration(after = MollyDocumentAutoConfiguration.class)
@ConditionalOnClass(Workbook.class)
public class MollyExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConverterRegistry excelConverterRegistry() {
        return new ConverterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelTemplate excelTemplate(ConverterRegistry excelConverterRegistry,
                                       MollyDocumentProperties properties) {
        return new DefaultExcelTemplate(excelConverterRegistry, properties.getExcel());
    }
}
