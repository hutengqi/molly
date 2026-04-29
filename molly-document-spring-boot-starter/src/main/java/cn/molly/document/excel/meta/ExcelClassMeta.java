package cn.molly.document.excel.meta;

import cn.molly.document.core.DocumentException;
import cn.molly.document.excel.annotation.ExcelColumn;
import cn.molly.document.excel.annotation.ExcelConverter;
import cn.molly.document.excel.annotation.ExcelDateFormat;
import cn.molly.document.excel.annotation.ExcelEnum;
import cn.molly.document.excel.annotation.ExcelSheet;
import cn.molly.document.excel.annotation.ExcelStyle;
import cn.molly.document.excel.converter.CellConverter;
import cn.molly.document.excel.converter.ConvertContext;
import cn.molly.document.excel.converter.ConverterRegistry;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Excel 实体类元数据，解析后缓存。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Getter
public class ExcelClassMeta {

    private static final ConcurrentMap<CacheKey, ExcelClassMeta> CACHE = new ConcurrentHashMap<>();

    private final Class<?> beanClass;
    private final String sheetName;
    private final int headerRowIndex;
    private final boolean freezeHeader;
    private final String headerPreset;
    private final List<ExcelColumnMeta> columns;
    /**
     * 多级表头的最大深度，>=1。
     */
    private final int headerDepth;

    private ExcelClassMeta(Class<?> beanClass, String sheetName, int headerRowIndex, boolean freezeHeader,
                           String headerPreset, List<ExcelColumnMeta> columns) {
        this.beanClass = beanClass;
        this.sheetName = sheetName;
        this.headerRowIndex = headerRowIndex;
        this.freezeHeader = freezeHeader;
        this.headerPreset = headerPreset;
        this.columns = Collections.unmodifiableList(columns);
        this.headerDepth = columns.stream().mapToInt(c -> c.getHeaderPath().size()).max().orElse(1);
    }

    /**
     * 解析并缓存给定类型的元数据。
     *
     * @param beanClass       实体类
     * @param registry        转换器注册中心
     * @param headerSeparator 多级表头分隔符（如 "."）
     */
    public static ExcelClassMeta of(Class<?> beanClass, ConverterRegistry registry, String headerSeparator) {
        CacheKey key = new CacheKey(beanClass, headerSeparator);
        return CACHE.computeIfAbsent(key, k -> parse(beanClass, registry, headerSeparator));
    }

    /**
     * 清空缓存（测试或热更新使用）。
     */
    public static void clearCache() {
        CACHE.clear();
    }

    private static ExcelClassMeta parse(Class<?> beanClass, ConverterRegistry registry, String headerSeparator) {
        ExcelSheet sheet = beanClass.getAnnotation(ExcelSheet.class);
        String sheetName = sheet != null && !sheet.name().isEmpty() ? sheet.name() : beanClass.getSimpleName();
        int headerRowIndex = sheet != null ? sheet.headerRowIndex() : 0;
        boolean freeze = sheet == null || sheet.freezeHeader();
        String preset = sheet != null ? sheet.headerPreset() : "";

        List<ExcelColumnMeta> columns = new ArrayList<>();
        for (Field field : listAllFields(beanClass)) {
            ExcelColumn col = field.getAnnotation(ExcelColumn.class);
            if (col == null) {
                continue;
            }
            field.setAccessible(true);

            List<String> headerPath = splitHeader(col.header(), headerSeparator);
            CellConverter<Object> converter = resolveConverter(field, registry);
            ConvertContext ctx = buildContext(field, col);

            ExcelColumnMeta meta = ExcelColumnMeta.builder()
                    .field(field)
                    .headerPath(headerPath)
                    .order(col.order())
                    .width(col.width())
                    .nullable(col.nullable())
                    .defaultValue(col.defaultValue())
                    .styleAnnotation(field.getAnnotation(ExcelStyle.class))
                    .converter(converter)
                    .convertContext(ctx)
                    .build();
            columns.add(meta);
        }
        if (columns.isEmpty()) {
            throw new DocumentException("类 " + beanClass.getName() + " 上没有任何 @ExcelColumn 字段");
        }
        columns.sort(Comparator.comparingInt(ExcelColumnMeta::getOrder));
        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).setColumnIndex(i);
        }
        return new ExcelClassMeta(beanClass, sheetName, headerRowIndex, freeze, preset, columns);
    }

    private static List<Field> listAllFields(Class<?> type) {
        List<Field> list = new ArrayList<>();
        Class<?> c = type;
        while (c != null && c != Object.class) {
            Collections.addAll(list, c.getDeclaredFields());
            c = c.getSuperclass();
        }
        return list;
    }

    private static List<String> splitHeader(String raw, String separator) {
        if (raw == null || raw.isEmpty()) {
            return List.of("");
        }
        if (separator == null || separator.isEmpty()) {
            return List.of(raw);
        }
        // 使用纯字符串分隔，避免正则语义
        List<String> parts = new ArrayList<>();
        int from = 0;
        while (true) {
            int idx = raw.indexOf(separator, from);
            if (idx < 0) {
                parts.add(raw.substring(from));
                break;
            }
            parts.add(raw.substring(from, idx));
            from = idx + separator.length();
        }
        return parts;
    }

    @SuppressWarnings("unchecked")
    private static CellConverter<Object> resolveConverter(Field field, ConverterRegistry registry) {
        ExcelConverter ec = field.getAnnotation(ExcelConverter.class);
        if (ec != null) {
            try {
                Class<? extends CellConverter<?>> clazz = ec.value();
                Constructor<? extends CellConverter<?>> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                return (CellConverter<Object>) ctor.newInstance();
            } catch (Exception e) {
                throw new DocumentException("实例化 ExcelConverter 失败: " + ec.value(), e);
            }
        }
        return (CellConverter<Object>) registry.resolve(field.getType());
    }

    private static ConvertContext buildContext(Field field, ExcelColumn col) {
        ExcelDateFormat dateFmt = field.getAnnotation(ExcelDateFormat.class);
        ExcelEnum enumAnn = field.getAnnotation(ExcelEnum.class);

        Class<? extends Enum<?>> enumType = null;
        String labelMethod = null;
        String codeMethod = null;
        if (enumAnn != null) {
            if (enumAnn.enumType() == Enum.class) {
                enumType = castEnum(field.getType());
            } else {
                enumType = castEnum(enumAnn.enumType());
            }
            labelMethod = nullIfEmpty(enumAnn.labelMethod());
            codeMethod = nullIfEmpty(enumAnn.codeMethod());
        } else if (Enum.class.isAssignableFrom(field.getType())) {
            enumType = castEnum(field.getType());
        }

        return ConvertContext.builder()
                .targetType(field.getType())
                .datePattern(dateFmt != null ? dateFmt.pattern() : null)
                .enumType(enumType)
                .enumLabelMethod(labelMethod)
                .enumCodeMethod(codeMethod)
                .defaultValue(col.defaultValue())
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<? extends Enum<?>> castEnum(Class<?> type) {
        if (Enum.class.isAssignableFrom(type)) {
            return (Class) type;
        }
        return null;
    }

    private static String nullIfEmpty(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    /**
     * 根据导入时识别到的表头行，构建表头文本 -&gt; 列元数据的映射。
     * <p>
     * 优先匹配最叶级 {@code leafHeader}；未命中时尝试匹配多级拼接。
     */
    public Map<String, ExcelColumnMeta> buildHeaderIndex(String headerSeparator) {
        Map<String, ExcelColumnMeta> map = new HashMap<>();
        for (ExcelColumnMeta c : columns) {
            map.put(c.getLeafHeader(), c);
            map.put(String.join(headerSeparator, c.getHeaderPath()), c);
        }
        return map;
    }

    private record CacheKey(Class<?> beanClass, String headerSeparator) {
        CacheKey {
            // 确保 hash 一致
            Arrays.hashCode(new Object[]{beanClass, headerSeparator});
        }
    }
}
