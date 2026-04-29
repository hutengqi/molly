package cn.molly.document.excel.converter;

import cn.molly.document.core.DocumentException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * JDK 8 时间字段转换器，支持 {@link LocalDateTime} / {@link LocalDate} / {@link LocalTime}。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class LocalDateTimeConverter implements CellConverter<Object> {

    @Override
    public Object toCellValue(Object fieldValue, ConvertContext ctx) {
        if (fieldValue == null) {
            return null;
        }
        String pattern = ctx == null ? null : ctx.getDatePattern();
        DateTimeFormatter fmt = pattern == null || pattern.isEmpty()
                ? defaultFormatter(fieldValue.getClass())
                : DateTimeFormatter.ofPattern(pattern);

        if (fieldValue instanceof LocalDateTime v) {
            return v.format(fmt);
        }
        if (fieldValue instanceof LocalDate v) {
            return v.format(fmt);
        }
        if (fieldValue instanceof LocalTime v) {
            return v.format(fmt);
        }
        return fieldValue.toString();
    }

    @Override
    public Object fromCellValue(Object cellValue, ConvertContext ctx) {
        if (cellValue == null) {
            return null;
        }
        Class<?> type = ctx == null ? LocalDateTime.class : ctx.getTargetType();
        // POI 可能直接读出 Date
        if (cellValue instanceof Date d) {
            return convertFromDate(d, type);
        }
        String str = cellValue.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        String pattern = ctx == null || ctx.getDatePattern() == null ? defaultPattern(type) : ctx.getDatePattern();
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
            if (type == LocalDate.class) {
                return LocalDate.parse(str, fmt);
            }
            if (type == LocalTime.class) {
                return LocalTime.parse(str, fmt);
            }
            return LocalDateTime.parse(str, fmt);
        } catch (Exception e) {
            throw new DocumentException("无法按 pattern [" + pattern + "] 解析时间: " + str, e);
        }
    }

    private static DateTimeFormatter defaultFormatter(Class<?> type) {
        return DateTimeFormatter.ofPattern(defaultPattern(type));
    }

    private static String defaultPattern(Class<?> type) {
        if (type == LocalDate.class) {
            return "yyyy-MM-dd";
        }
        if (type == LocalTime.class) {
            return "HH:mm:ss";
        }
        return "yyyy-MM-dd HH:mm:ss";
    }

    private static Object convertFromDate(Date d, Class<?> type) {
        LocalDateTime ldt = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        if (type == LocalDate.class) {
            return ldt.toLocalDate();
        }
        if (type == LocalTime.class) {
            return ldt.toLocalTime();
        }
        return ldt;
    }
}
