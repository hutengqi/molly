package cn.molly.document.excel.converter;

/**
 * 兜底的字符串转换器：导出调用 {@code toString}，导入保留原字符串。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class StringConverter implements CellConverter<String> {

    @Override
    public Object toCellValue(String fieldValue, ConvertContext ctx) {
        return fieldValue;
    }

    @Override
    public String fromCellValue(Object cellValue, ConvertContext ctx) {
        if (cellValue == null) {
            return ctx != null ? emptyToNull(ctx.getDefaultValue()) : null;
        }
        if (cellValue instanceof String s) {
            return s;
        }
        if (cellValue instanceof Double d && d == Math.floor(d) && !Double.isInfinite(d)) {
            // POI 将整数读作 Double，此处去除末尾 .0
            return Long.toString(d.longValue());
        }
        return cellValue.toString();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
