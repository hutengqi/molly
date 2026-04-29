package cn.molly.document.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明字段级样式覆盖。未配置的字段使用全局预设样式。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelStyle {

    /**
     * 字体名称。
     */
    String fontName() default "";

    /**
     * 字号，&lt;=0 表示使用默认。
     */
    short fontSize() default 0;

    /**
     * 是否加粗。
     */
    boolean bold() default false;

    /**
     * 字体颜色 RGB（十六进制，如 {@code #FF0000}），为空使用默认。
     */
    String fontColor() default "";

    /**
     * 背景色 RGB，为空表示无填充。
     */
    String backgroundColor() default "";

    /**
     * 水平对齐：{@code left} / {@code center} / {@code right}。
     */
    String horizontalAlign() default "";

    /**
     * 垂直对齐：{@code top} / {@code center} / {@code bottom}。
     */
    String verticalAlign() default "";

    /**
     * 是否描边。
     */
    boolean border() default false;
}
