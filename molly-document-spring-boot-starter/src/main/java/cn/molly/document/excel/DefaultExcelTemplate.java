package cn.molly.document.excel;

import cn.molly.document.core.DocumentException;
import cn.molly.document.excel.converter.CellConverter;
import cn.molly.document.excel.converter.ConvertContext;
import cn.molly.document.excel.converter.ConverterRegistry;
import cn.molly.document.excel.meta.ExcelClassMeta;
import cn.molly.document.excel.meta.ExcelColumnMeta;
import cn.molly.document.excel.style.ExcelStyleResolver;
import cn.molly.document.properties.MollyDocumentProperties;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 默认 {@link ExcelTemplate} 实现，基于 Apache POI。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class DefaultExcelTemplate implements ExcelTemplate {

    private final ConverterRegistry converterRegistry;
    private final MollyDocumentProperties.Excel properties;

    public DefaultExcelTemplate(ConverterRegistry converterRegistry, MollyDocumentProperties.Excel properties) {
        this.converterRegistry = converterRegistry;
        this.properties = properties;
    }

    @Override
    public <T> void export(Class<T> type, List<T> rows, OutputStream out) {
        ExcelClassMeta meta = ExcelClassMeta.of(type, converterRegistry, properties.getHeaderSeparator());
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeWorkbook(wb, meta, rows.iterator());
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new DocumentException("Excel 导出失败", e);
        }
    }

    @Override
    public <T> void exportStream(Class<T> type, Iterator<T> rows, OutputStream out) {
        ExcelClassMeta meta = ExcelClassMeta.of(type, converterRegistry, properties.getHeaderSeparator());
        SXSSFWorkbook wb = new SXSSFWorkbook(properties.getStreamWindowSize());
        wb.setCompressTempFiles(properties.isCompressTempFiles());
        try {
            writeWorkbook(wb, meta, rows);
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new DocumentException("Excel 流式导出失败", e);
        } finally {
            wb.dispose();
            try {
                wb.close();
            } catch (IOException ignore) {
                // ignore
            }
        }
    }

    @Override
    public <T> List<T> importFrom(Class<T> type, InputStream in) {
        List<T> list = new ArrayList<>();
        importStream(type, in, list::add);
        return list;
    }

    @Override
    public <T> void importStream(Class<T> type, InputStream in, Consumer<T> consumer) {
        ExcelClassMeta meta = ExcelClassMeta.of(type, converterRegistry, properties.getHeaderSeparator());
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            int headerRow = meta.getHeaderRowIndex() + meta.getHeaderDepth() - 1;
            Row header = sheet.getRow(headerRow);
            if (header == null) {
                throw new DocumentException("Excel 找不到表头行: " + headerRow);
            }
            Map<Integer, ExcelColumnMeta> indexByColumn = buildImportIndex(meta, header);
            DataFormatter formatter = new DataFormatter();

            int last = sheet.getLastRowNum();
            for (int r = headerRow + 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmpty(row, formatter)) {
                    continue;
                }
                T bean = newInstance(type);
                for (Map.Entry<Integer, ExcelColumnMeta> e : indexByColumn.entrySet()) {
                    Cell cell = row.getCell(e.getKey());
                    ExcelColumnMeta col = e.getValue();
                    Object raw = readCell(cell, formatter);
                    if (raw == null && !col.isNullable() && (col.getDefaultValue() == null || col.getDefaultValue().isEmpty())) {
                        throw new DocumentException("列 [" + col.getLeafHeader() + "] 在第 " + (r + 1) + " 行为空");
                    }
                    if (raw == null && col.getDefaultValue() != null && !col.getDefaultValue().isEmpty()) {
                        raw = col.getDefaultValue();
                    }
                    Object value = col.getConverter().fromCellValue(raw, col.getConvertContext());
                    col.writeFieldValue(bean, value);
                }
                consumer.accept(bean);
            }
        } catch (IOException e) {
            throw new DocumentException("Excel 导入失败", e);
        }
    }

    // ============================ 内部写入实现 ============================

    private void writeWorkbook(Workbook wb, ExcelClassMeta meta, Iterator<?> rows) {
        Sheet sheet = wb.createSheet(meta.getSheetName());
        ExcelStyleResolver styleResolver = new ExcelStyleResolver(wb);

        writeHeader(sheet, meta, styleResolver);
        int dataStart = meta.getHeaderRowIndex() + meta.getHeaderDepth();
        int r = dataStart;
        while (rows.hasNext()) {
            Object bean = rows.next();
            Row row = sheet.createRow(r++);
            writeRow(row, bean, meta, styleResolver);
        }
        if (meta.isFreezeHeader()) {
            sheet.createFreezePane(0, dataStart);
        }
        if (properties.isAutoSizeColumn() && !(wb instanceof SXSSFWorkbook)) {
            for (ExcelColumnMeta col : meta.getColumns()) {
                sheet.autoSizeColumn(col.getColumnIndex());
            }
        }
        for (ExcelColumnMeta col : meta.getColumns()) {
            if (col.getWidth() > 0) {
                sheet.setColumnWidth(col.getColumnIndex(), col.getWidth() * 256);
            }
        }
    }

    private void writeHeader(Sheet sheet, ExcelClassMeta meta, ExcelStyleResolver styleResolver) {
        int depth = meta.getHeaderDepth();
        int base = meta.getHeaderRowIndex();
        String preset = !meta.getHeaderPreset().isEmpty() ? meta.getHeaderPreset() : properties.getHeaderPreset();
        CellStyle headerStyle = styleResolver.resolveHeader(preset);

        // 1) 先铺满每行文本（按列补齐）
        String[][] grid = new String[depth][meta.getColumns().size()];
        for (int c = 0; c < meta.getColumns().size(); c++) {
            List<String> path = meta.getColumns().get(c).getHeaderPath();
            String tail = path.get(path.size() - 1);
            for (int r = 0; r < depth; r++) {
                grid[r][c] = r < path.size() ? path.get(r) : tail;
            }
        }
        // 2) 写入单元格
        for (int r = 0; r < depth; r++) {
            Row row = sheet.createRow(base + r);
            for (int c = 0; c < grid[r].length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(grid[r][c]);
                cell.setCellStyle(headerStyle);
            }
        }
        // 3) 合并：同一行内，相邻列当前及以上各行文本完全相同则合并
        for (int r = 0; r < depth; r++) {
            int c = 0;
            while (c < grid[r].length) {
                int next = c + 1;
                while (next < grid[r].length && sameColumnPrefix(grid, r, c, next)) {
                    next++;
                }
                if (next - c > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(base + r, base + r, c, next - 1));
                }
                c = next;
            }
        }
        // 4) 同列不同行相同（说明该列表头较浅，竖向合并到底）
        for (int c = 0; c < meta.getColumns().size(); c++) {
            int r = 0;
            while (r < depth) {
                int next = r + 1;
                while (next < depth && grid[next][c].equals(grid[r][c])) {
                    next++;
                }
                if (next - r > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(base + r, base + next - 1, c, c));
                }
                r = next;
            }
        }
    }

    private static boolean sameColumnPrefix(String[][] grid, int row, int c1, int c2) {
        for (int r = 0; r <= row; r++) {
            if (!grid[r][c1].equals(grid[r][c2])) {
                return false;
            }
        }
        return true;
    }

    private void writeRow(Row row, Object bean, ExcelClassMeta meta, ExcelStyleResolver styleResolver) {
        for (ExcelColumnMeta col : meta.getColumns()) {
            Cell cell = row.createCell(col.getColumnIndex());
            cell.setCellStyle(styleResolver.resolveBody(col.getStyleAnnotation()));
            Object raw = col.readFieldValue(bean);
            Object cellValue = col.getConverter().toCellValue(raw, col.getConvertContext());
            setCellValue(cell, cellValue);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof String s) {
            cell.setCellValue(s);
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b);
        } else if (value instanceof Date d) {
            cell.setCellValue(d);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    // ============================ 内部读取实现 ============================

    private Map<Integer, ExcelColumnMeta> buildImportIndex(ExcelClassMeta meta, Row header) {
        Map<String, ExcelColumnMeta> byName = meta.buildHeaderIndex(properties.getHeaderSeparator());
        Map<Integer, ExcelColumnMeta> map = new HashMap<>();
        for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            if (cell == null) {
                continue;
            }
            String name = cell.getStringCellValue();
            if (name == null) {
                continue;
            }
            ExcelColumnMeta col = byName.get(name.trim());
            if (col != null) {
                map.put(c, col);
            }
        }
        return map;
    }

    private static Object readCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return switch (type) {
            case STRING -> {
                String s = cell.getStringCellValue();
                yield s == null || s.isEmpty() ? null : s;
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
            case BLANK, _NONE, ERROR -> null;
            default -> formatter.formatCellValue(cell);
        };
    }

    private static boolean isEmpty(Row row, DataFormatter formatter) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                continue;
            }
            String v = formatter.formatCellValue(cell);
            if (v != null && !v.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new DocumentException("实例化失败: " + type.getName(), e);
        }
    }
}
