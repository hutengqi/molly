package cn.molly.document.excel.converter;

/**
 * 布尔值转换器：导出 {@code 是/否}，导入兼容 {@code 是/否、Y/N、true/false、1/0}。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class BooleanYesNoConverter implements CellConverter<Boolean> {

    @Override
    public Object toCellValue(Boolean fieldValue, ConvertContext ctx) {
        if (fieldValue == null) {
            return null;
        }
        return fieldValue ? "是" : "否";
    }

    @Override
    public Boolean fromCellValue(Object cellValue, ConvertContext ctx) {
        if (cellValue == null) {
            return null;
        }
        if (cellValue instanceof Boolean b) {
            return b;
        }
        if (cellValue instanceof Number n) {
            return n.intValue() != 0;
        }
        String s = cellValue.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        return switch (s.toLowerCase()) {
            case "是", "y", "yes", "true", "1", "t" -> Boolean.TRUE;
            case "否", "n", "no", "false", "0", "f" -> Boolean.FALSE;
            default -> Boolean.parseBoolean(s);
        };
    }
}
