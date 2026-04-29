package cn.molly.document.excel.converter;

import cn.molly.document.core.DocumentException;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * {@link Date} 字段转换器。
 * <p>
 * 导出：若配置 {@code pattern}，则返回格式化字符串；否则返回原 {@code Date}（由 POI 按单元格格式展示）。
 * 导入：兼容 POI 读出的 {@code Date} 直接返回；字符串按 {@code pattern} 解析。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class DateConverter implements CellConverter<Date> {

    @Override
    public Object toCellValue(Date fieldValue, ConvertContext ctx) {
        if (fieldValue == null) {
            return null;
        }
        String pattern = ctx == null ? null : ctx.getDatePattern();
        if (pattern == null || pattern.isEmpty()) {
            return fieldValue;
        }
        return new SimpleDateFormat(pattern).format(fieldValue);
    }

    @Override
    public Date fromCellValue(Object cellValue, ConvertContext ctx) {
        if (cellValue == null) {
            return null;
        }
        if (cellValue instanceof Date d) {
            return d;
        }
        String str = cellValue.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        String pattern = ctx == null || ctx.getDatePattern() == null ? "yyyy-MM-dd HH:mm:ss" : ctx.getDatePattern();
        try {
            return new SimpleDateFormat(pattern).parse(str);
        } catch (Exception e) {
            throw new DocumentException("无法按 pattern [" + pattern + "] 解析日期: " + str, e);
        }
    }
}
