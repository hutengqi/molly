package cn.molly.document.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明字段与 Excel 列的映射关系。
 * <p>
 * 支持多级表头：{@code header} 可使用分隔符拆分多层，如 {@code "联系方式.手机"} 默认以 {@code .} 分隔，
 * 分隔符可通过 {@code molly.document.excel.header-separator} 配置。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {

    /**
     * 列标题，支持多级表头（用分隔符拆分）。
     */
    String header();

    /**
     * 列顺序（从小到大排列）。
     */
    int order() default 0;

    /**
     * 列宽（单位：字符宽度），&lt;=0 表示使用默认。
     */
    int width() default 0;

    /**
     * 是否允许为空（导入校验）。
     */
    boolean nullable() default true;

    /**
     * 导入时，若单元格为空使用的默认值。
     */
    String defaultValue() default "";
}
