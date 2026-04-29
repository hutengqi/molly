package cn.molly.document.excel.converter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link CellConverter} 注册中心。
 * <p>
 * 按字段类型查找默认转换器；使用方可通过 {@link #register(Class, CellConverter)}
 * 覆盖默认实现，或直接在字段上使用 {@code @ExcelConverter} 指定。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class ConverterRegistry {

    private final ConcurrentMap<Class<?>, CellConverter<?>> converters = new ConcurrentHashMap<>();

    /**
     * 通用兜底转换器（未命中任何类型时使用）。
     */
    private final CellConverter<?> fallback = new StringConverter();

    public ConverterRegistry() {
        registerDefaults();
    }

    /**
     * 注册或覆盖 {@code type} 对应的默认转换器。
     */
    public <T> void register(Class<T> type, CellConverter<T> converter) {
        converters.put(type, converter);
    }

    /**
     * 查找字段类型对应的转换器，未命中时返回 {@link StringConverter}。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CellConverter<?> resolve(Class<?> type) {
        if (type == null) {
            return fallback;
        }
        CellConverter<?> c = converters.get(type);
        if (c != null) {
            return c;
        }
        if (Enum.class.isAssignableFrom(type)) {
            return converters.get(Enum.class);
        }
        if (Number.class.isAssignableFrom(type)) {
            return converters.get(Number.class);
        }
        return fallback;
    }

    private void registerDefaults() {
        StringConverter stringConverter = new StringConverter();
        converters.put(String.class, stringConverter);

        DateConverter dateConverter = new DateConverter();
        converters.put(Date.class, dateConverter);
        converters.put(java.sql.Date.class, dateConverter);
        converters.put(java.sql.Timestamp.class, dateConverter);

        LocalDateTimeConverter ldtConverter = new LocalDateTimeConverter();
        converters.put(LocalDateTime.class, ldtConverter);
        converters.put(LocalDate.class, ldtConverter);
        converters.put(LocalTime.class, ldtConverter);

        EnumConverter enumConverter = new EnumConverter();
        converters.put(Enum.class, enumConverter);

        BooleanYesNoConverter booleanConverter = new BooleanYesNoConverter();
        converters.put(Boolean.class, booleanConverter);
        converters.put(boolean.class, booleanConverter);

        NumberConverter numberConverter = new NumberConverter();
        converters.put(Number.class, numberConverter);
        converters.put(BigDecimal.class, numberConverter);
        converters.put(BigInteger.class, numberConverter);
        converters.put(Integer.class, numberConverter);
        converters.put(int.class, numberConverter);
        converters.put(Long.class, numberConverter);
        converters.put(long.class, numberConverter);
        converters.put(Short.class, numberConverter);
        converters.put(short.class, numberConverter);
        converters.put(Byte.class, numberConverter);
        converters.put(byte.class, numberConverter);
        converters.put(Double.class, numberConverter);
        converters.put(double.class, numberConverter);
        converters.put(Float.class, numberConverter);
        converters.put(float.class, numberConverter);
    }
}
