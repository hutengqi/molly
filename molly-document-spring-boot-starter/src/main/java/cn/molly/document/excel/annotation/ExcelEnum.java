package cn.molly.document.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明枚举字段的 label / code 双向映射。
 * <p>
 * 导出时调用 {@link #labelMethod()} 得到展示值；导入时：
 * <ol>
 *     <li>先尝试按 {@link #labelMethod()} 反查；</li>
 *     <li>再按 {@link #codeMethod()} 反查；</li>
 *     <li>最后按枚举名称反查。</li>
 * </ol>
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelEnum {

    /**
     * 枚举类型；省略时使用字段声明类型。
     */
    @SuppressWarnings("rawtypes")
    Class<? extends Enum> enumType() default Enum.class;

    /**
     * 取 label 的无参方法名（如 {@code getLabel}）；为空则使用 {@link Enum#name()}。
     */
    String labelMethod() default "";

    /**
     * 取 code 的无参方法名（如 {@code getCode}）；为空则不使用 code 反查。
     */
    String codeMethod() default "";
}
