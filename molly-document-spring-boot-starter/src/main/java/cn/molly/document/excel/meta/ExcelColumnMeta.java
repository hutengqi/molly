package cn.molly.document.excel.meta;

import cn.molly.document.excel.annotation.ExcelStyle;
import cn.molly.document.excel.converter.CellConverter;
import cn.molly.document.excel.converter.ConvertContext;
import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Excel 单列元数据：对应实体类上一个被 {@code @ExcelColumn} 标记的字段。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Getter
@Builder
public class ExcelColumnMeta {

    /**
     * 反射字段。
     */
    private final Field field;

    /**
     * 多级表头，至少 1 级。
     */
    private final List<String> headerPath;

    /**
     * 列顺序。
     */
    private final int order;

    /**
     * 列宽，&lt;=0 表示默认。
     */
    private final int width;

    /**
     * 是否允许为空。
     */
    private final boolean nullable;

    /**
     * 导入默认值。
     */
    private final String defaultValue;

    /**
     * 字段级样式注解，可能为 {@code null}。
     */
    private final ExcelStyle styleAnnotation;

    /**
     * 对应的 {@link CellConverter}，已就位。
     */
    private final CellConverter<Object> converter;

    /**
     * 固化的转换上下文，复用避免重复构建。
     */
    private final ConvertContext convertContext;

    /**
     * 导出时该列最终所在的列索引（0-based），由 {@code ExcelClassMeta} 构建后赋值。
     */
    private int columnIndex;

    /**
     * 取字段的最叶级表头（最后一段）。
     */
    public String getLeafHeader() {
        return headerPath.get(headerPath.size() - 1);
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    /**
     * 读取实体字段值。
     */
    public Object readFieldValue(Object bean) {
        try {
            return field.get(bean);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("读取字段失败: " + field, e);
        }
    }

    /**
     * 写回实体字段值。
     */
    public void writeFieldValue(Object bean, Object value) {
        try {
            field.set(bean, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("写入字段失败: " + field, e);
        }
    }
}
