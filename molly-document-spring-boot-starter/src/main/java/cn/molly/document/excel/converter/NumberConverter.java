package cn.molly.document.excel.converter;

import cn.molly.document.core.DocumentException;

import java.math.BigDecimal;

/**
 * 数值型转换器，支持 {@link BigDecimal} 与其它 {@link Number} 子类。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class NumberConverter implements CellConverter<Number> {

    @Override
    public Object toCellValue(Number fieldValue, ConvertContext ctx) {
        if (fieldValue == null) {
            return null;
        }
        // POI 写入 double 单元格更通用；BigDecimal 直接 toString 可避免精度展开
        if (fieldValue instanceof BigDecimal bd) {
            return bd.toPlainString();
        }
        return fieldValue;
    }

    @Override
    public Number fromCellValue(Object cellValue, ConvertContext ctx) {
        if (cellValue == null) {
            return null;
        }
        Class<?> type = ctx == null ? BigDecimal.class : ctx.getTargetType();
        String str;
        if (cellValue instanceof Number n) {
            str = n instanceof Double d ? stripDecimal(d) : n.toString();
        } else {
            str = cellValue.toString().trim();
        }
        if (str.isEmpty()) {
            return null;
        }
        try {
            if (type == Integer.class || type == int.class) {
                return Integer.valueOf(new BigDecimal(str).intValueExact());
            }
            if (type == Long.class || type == long.class) {
                return Long.valueOf(new BigDecimal(str).longValueExact());
            }
            if (type == Short.class || type == short.class) {
                return Short.valueOf((short) new BigDecimal(str).intValueExact());
            }
            if (type == Byte.class || type == byte.class) {
                return Byte.valueOf((byte) new BigDecimal(str).intValueExact());
            }
            if (type == Double.class || type == double.class) {
                return Double.valueOf(str);
            }
            if (type == Float.class || type == float.class) {
                return Float.valueOf(str);
            }
            return new BigDecimal(str);
        } catch (Exception e) {
            throw new DocumentException("无法将 [" + str + "] 解析为 " + (type == null ? "Number" : type.getSimpleName()), e);
        }
    }

    private static String stripDecimal(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }
}
