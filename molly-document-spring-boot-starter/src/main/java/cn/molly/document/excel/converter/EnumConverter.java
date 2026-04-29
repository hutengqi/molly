package cn.molly.document.excel.converter;

import cn.molly.document.core.DocumentException;

import java.lang.reflect.Method;

/**
 * 枚举字段转换器。
 * <p>
 * 导出：调用 {@code labelMethod}（若存在）；否则返回 {@code name()}。
 * 导入：按优先级逐一反查——{@code labelMethod}、{@code codeMethod}、{@code name}。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumConverter implements CellConverter<Enum> {

    @Override
    public Object toCellValue(Enum fieldValue, ConvertContext ctx) {
        if (fieldValue == null) {
            return null;
        }
        String labelMethod = ctx == null ? null : ctx.getEnumLabelMethod();
        if (labelMethod == null || labelMethod.isEmpty()) {
            return fieldValue.name();
        }
        try {
            Method m = fieldValue.getClass().getMethod(labelMethod);
            Object v = m.invoke(fieldValue);
            return v == null ? fieldValue.name() : v.toString();
        } catch (Exception e) {
            throw new DocumentException("枚举 label 读取失败: " + fieldValue.getDeclaringClass().getName() + "#" + labelMethod, e);
        }
    }

    @Override
    public Enum fromCellValue(Object cellValue, ConvertContext ctx) {
        if (cellValue == null) {
            return null;
        }
        String str = cellValue.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        if (ctx == null || ctx.getEnumType() == null) {
            throw new DocumentException("EnumConverter 使用时必须提供 enumType 上下文");
        }
        Class<? extends Enum> enumType = ctx.getEnumType();
        Enum<?>[] constants = enumType.getEnumConstants();

        // 1) 按 labelMethod 反查
        String labelMethod = ctx.getEnumLabelMethod();
        if (labelMethod != null && !labelMethod.isEmpty()) {
            Enum<?> hit = match(constants, labelMethod, str);
            if (hit != null) {
                return hit;
            }
        }
        // 2) 按 codeMethod 反查
        String codeMethod = ctx.getEnumCodeMethod();
        if (codeMethod != null && !codeMethod.isEmpty()) {
            Enum<?> hit = match(constants, codeMethod, str);
            if (hit != null) {
                return hit;
            }
        }
        // 3) 按 name() 反查
        for (Enum<?> e : constants) {
            if (e.name().equalsIgnoreCase(str)) {
                return e;
            }
        }
        throw new DocumentException("无法在枚举 " + enumType.getName() + " 中匹配值: " + str);
    }

    private static Enum<?> match(Enum<?>[] constants, String methodName, String target) {
        try {
            Method m = constants[0].getClass().getMethod(methodName);
            for (Enum<?> e : constants) {
                Object v = m.invoke(e);
                if (v != null && target.equals(v.toString())) {
                    return e;
                }
            }
        } catch (NoSuchMethodException ignore) {
            // 方法不存在则跳过此级反查
        } catch (Exception e) {
            throw new DocumentException("枚举反查失败: " + methodName, e);
        }
        return null;
    }
}
