package cn.molly.document.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明时间字段的格式化样式。
 * <p>
 * 作用于 {@code java.util.Date} / {@code java.time.LocalDate} / {@code LocalDateTime} / {@code LocalTime} 等类型。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelDateFormat {

    /**
     * 格式 pattern，默认 {@code yyyy-MM-dd HH:mm:ss}。
     */
    String pattern() default "yyyy-MM-dd HH:mm:ss";
}
