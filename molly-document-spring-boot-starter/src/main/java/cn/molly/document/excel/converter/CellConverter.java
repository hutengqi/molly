package cn.molly.document.excel.converter;

/**
 * Excel 单元格 - 实体字段双向转换 SPI。
 * <p>
 * 实现类需具备公共无参构造，可通过 {@code @ExcelConverter} 指定到字段上，
 * 或通过 {@link ConverterRegistry#register(Class, CellConverter)} 注册为类型默认转换器。
 *
 * @param <T> 实体字段类型
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public interface CellConverter<T> {

    /**
     * 实体字段值 -&gt; 单元格写入值。返回 {@code null} 时写入空白单元格。
     * <p>
     * 推荐返回基础类型：{@code String} / {@code Number} / {@code Boolean} / {@code java.util.Date}。
     *
     * @param fieldValue 字段原值，可能为 {@code null}
     * @param ctx        转换上下文
     * @return 单元格可写入值
     */
    Object toCellValue(T fieldValue, ConvertContext ctx);

    /**
     * 单元格读取值 -&gt; 实体字段值。
     *
     * @param cellValue 单元格原值（可能是 {@code String} / {@code Double} / {@code Date} 等）
     * @param ctx       转换上下文
     * @return 字段值
     */
    T fromCellValue(Object cellValue, ConvertContext ctx);
}
