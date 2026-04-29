package cn.molly.document.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注一个实体类为 Excel 数据载体。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelSheet {

    /**
     * Sheet 名称，默认类简单名。
     */
    String name() default "";

    /**
     * 表头起始行（0-based），导出时表头占 1~N 行（多级表头时自动计算总行数）。
     */
    int headerRowIndex() default 0;

    /**
     * 是否冻结表头。
     */
    boolean freezeHeader() default true;

    /**
     * 表头样式预设，取值 {@code default} / {@code plain} / {@code bordered}；为空表示使用全局配置。
     */
    String headerPreset() default "";
}
