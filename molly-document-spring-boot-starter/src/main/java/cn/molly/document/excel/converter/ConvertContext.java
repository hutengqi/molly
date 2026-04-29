package cn.molly.document.excel.converter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 单元格转换上下文，承载字段声明类型与注解元数据。
 * <p>
 * 由 {@code ExcelClassMeta} 构建后传入 {@link CellConverter}，
 * 使内置转换器无需反射读取注解即可完成转换。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Data
@Builder
@AllArgsConstructor
public class ConvertContext {

    /**
     * 字段声明类型（如 {@code java.util.Date} / {@code MyEnum}）。
     */
    private final Class<?> targetType;

    /**
     * 时间格式 pattern（来自 {@code ExcelDateFormat}），可为 {@code null}。
     */
    private final String datePattern;

    /**
     * 枚举类型（来自 {@code ExcelEnum.enumType}），可为 {@code null}。
     */
    private final Class<? extends Enum<?>> enumType;

    /**
     * 枚举 label 方法名，可为 {@code null}。
     */
    private final String enumLabelMethod;

    /**
     * 枚举 code 方法名，可为 {@code null}。
     */
    private final String enumCodeMethod;

    /**
     * 字段为空时的默认字面量（来自 {@code ExcelColumn.defaultValue}）。
     */
    private final String defaultValue;
}
