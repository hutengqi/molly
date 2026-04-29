package cn.molly.document.excel;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excel 读写模板。
 * <p>
 * 支持两种模式：
 * <ul>
 *     <li>普通模式：{@link #export(Class, List, OutputStream)} / {@link #importFrom(Class, InputStream)}，
 *     数据量较小时使用，基于 XSSF DOM。</li>
 *     <li>流式模式：{@link #exportStream(Class, Iterator, OutputStream)} / {@link #importStream(Class, InputStream, Consumer)}，
 *     大数据量时使用，基于 SXSSF 滑窗写入与 SAX 事件读取。</li>
 * </ul>
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public interface ExcelTemplate {

    /**
     * 将实体集合导出到流（DOM 模式，单 Sheet）。
     */
    <T> void export(Class<T> type, List<T> rows, OutputStream out);

    /**
     * 流式导出（SXSSF，单 Sheet）。
     *
     * @param rows 数据迭代器，可来自数据库游标或分页查询
     */
    <T> void exportStream(Class<T> type, Iterator<T> rows, OutputStream out);

    /**
     * 从流读取 Excel 转换为实体集合（DOM 模式，单 Sheet）。
     */
    <T> List<T> importFrom(Class<T> type, InputStream in);

    /**
     * 流式读取 Excel（逐行回调，单 Sheet）。
     */
    <T> void importStream(Class<T> type, InputStream in, Consumer<T> consumer);
}
