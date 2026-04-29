package cn.molly.document.excel.style;

import cn.molly.document.excel.annotation.ExcelStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.util.HashMap;
import java.util.Map;

/**
 * 样式解析与缓存器。
 * <p>
 * POI 中每个 {@code Workbook} 对应的 {@code CellStyle} 数量受限（最多 64000），
 * 必须在同一 Workbook 内对等价样式复用实例，这里以样式描述为 key 缓存。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class ExcelStyleResolver {

    private final Workbook workbook;
    private final Map<String, CellStyle> cache = new HashMap<>();
    /**
     * 是否支持 {@link XSSFColor}（xlsx 体系）。SXSSF 底层也是 xlsx。
     */
    private final boolean supportRgb;

    public ExcelStyleResolver(Workbook workbook) {
        this.workbook = workbook;
        this.supportRgb = workbook instanceof XSSFWorkbook || workbook instanceof SXSSFWorkbook;
    }

    /**
     * 按字段样式注解解析 CellStyle；注解为 {@code null} 时返回默认正文样式。
     */
    public CellStyle resolveBody(ExcelStyle styleAnn) {
        if (styleAnn == null) {
            return cache.computeIfAbsent("__body_default__", k -> DefaultStylePresets.defaultBody(workbook));
        }
        String key = keyOf("body", styleAnn);
        return cache.computeIfAbsent(key, k -> buildFromAnnotation(styleAnn));
    }

    /**
     * 解析表头样式，根据 preset 选择一套预设。
     */
    public CellStyle resolveHeader(String preset) {
        String p = preset == null || preset.isEmpty() ? "default" : preset;
        return cache.computeIfAbsent("__header_" + p, k -> DefaultStylePresets.header(workbook, p));
    }

    private String keyOf(String scope, ExcelStyle s) {
        return scope + "|" + s.fontName() + "|" + s.fontSize() + "|" + s.bold()
                + "|" + s.fontColor() + "|" + s.backgroundColor()
                + "|" + s.horizontalAlign() + "|" + s.verticalAlign() + "|" + s.border();
    }

    private CellStyle buildFromAnnotation(ExcelStyle s) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();

        if (!s.fontName().isEmpty()) {
            font.setFontName(s.fontName());
        }
        if (s.fontSize() > 0) {
            font.setFontHeightInPoints(s.fontSize());
        }
        if (s.bold()) {
            font.setBold(true);
        }
        if (!s.fontColor().isEmpty() && supportRgb && font instanceof XSSFFont xf) {
            xf.setColor(rgb(s.fontColor()));
        }
        style.setFont(font);

        if (!s.backgroundColor().isEmpty() && supportRgb && style instanceof XSSFCellStyle xs) {
            xs.setFillForegroundColor(rgb(s.backgroundColor()));
            xs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (!s.horizontalAlign().isEmpty()) {
            style.setAlignment(parseHorizontal(s.horizontalAlign()));
        }
        if (!s.verticalAlign().isEmpty()) {
            style.setVerticalAlignment(parseVertical(s.verticalAlign()));
        }
        if (s.border()) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
        return style;
    }

    private static HorizontalAlignment parseHorizontal(String v) {
        return switch (v.toLowerCase()) {
            case "left" -> HorizontalAlignment.LEFT;
            case "right" -> HorizontalAlignment.RIGHT;
            case "center" -> HorizontalAlignment.CENTER;
            default -> HorizontalAlignment.GENERAL;
        };
    }

    private static VerticalAlignment parseVertical(String v) {
        return switch (v.toLowerCase()) {
            case "top" -> VerticalAlignment.TOP;
            case "bottom" -> VerticalAlignment.BOTTOM;
            case "center" -> VerticalAlignment.CENTER;
            default -> VerticalAlignment.CENTER;
        };
    }

    static XSSFColor rgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 6) {
            throw new IllegalArgumentException("颜色必须为 6 位 HEX: " + hex);
        }
        byte[] rgb = new byte[]{
                (byte) Integer.parseInt(h.substring(0, 2), 16),
                (byte) Integer.parseInt(h.substring(2, 4), 16),
                (byte) Integer.parseInt(h.substring(4, 6), 16),
        };
        return new XSSFColor(rgb, null);
    }
}
