package cn.molly.document.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Molly 文档组件配置属性。
 * <p>
 * 对应配置前缀 {@code molly.document}，四类子能力（Word / Excel / PDF / Email）共用同一属性根。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Data
@ConfigurationProperties(prefix = "molly.document")
public class MollyDocumentProperties {

    /**
     * Word 子能力配置。
     */
    private Word word = new Word();

    /**
     * Excel 子能力配置。
     */
    private Excel excel = new Excel();

    /**
     * PDF 子能力配置。
     */
    private Pdf pdf = new Pdf();

    /**
     * Email 子能力配置。
     */
    private Email email = new Email();

    /**
     * Word 配置项。
     */
    @Data
    public static class Word {

        /**
         * 模板根路径，支持 {@code classpath:} / {@code file:} 前缀。
         */
        private String templateLocation = "classpath:templates/word/";

        /**
         * 是否缓存已加载的模板资源（按路径缓存字节数组）。
         */
        private boolean cacheTemplates = true;
    }

    /**
     * Excel 配置项。
     */
    @Data
    public static class Excel {

        /**
         * 模板根路径（保留，供后续 Excel 模板导出扩展使用）。
         */
        private String templateLocation = "classpath:templates/excel/";

        /**
         * SXSSF 流式导出滑窗大小，写入超出窗口的行将被刷新到磁盘。
         */
        private int streamWindowSize = 100;

        /**
         * 是否自动列宽（开启后对大数据集有性能损耗）。
         */
        private boolean autoSizeColumn = false;

        /**
         * SXSSF 是否压缩磁盘临时文件。
         */
        private boolean compressTempFiles = true;

        /**
         * 默认时间格式，未显式声明 {@code @ExcelDateFormat} 时使用。
         */
        private String datePattern = "yyyy-MM-dd HH:mm:ss";

        /**
         * 表头样式预设名，取值：{@code default} / {@code plain} / {@code bordered}。
         */
        private String headerPreset = "default";

        /**
         * 多级表头分隔符。
         */
        private String headerSeparator = ".";
    }

    /**
     * PDF 配置项。
     */
    @Data
    public static class Pdf {

        /**
         * 模板根路径。
         */
        private String templateLocation = "classpath:templates/pdf/";

        /**
         * 填充后是否 flatten (将表单字段转换为静态内容，不可再编辑)。
         */
        private boolean flatten = true;

        /**
         * 字体配置。
         */
        private Font font = new Font();
    }

    /**
     * PDF 字体配置。
     */
    @Data
    public static class Font {

        /**
         * 中文字体资源路径，支持 {@code classpath:} / {@code file:} 前缀。
         * 若未配置，使用 PDFBox 内置字体（可能无法正确渲染中文）。
         */
        private String chinese;
    }

    /**
     * Email 配置项。
     */
    @Data
    public static class Email {

        /**
         * 默认发件人地址。
         */
        private String from;

        /**
         * 模板根路径（Thymeleaf 前缀）。
         */
        private String templateLocation = "classpath:/templates/mail/";

        /**
         * 模板后缀。
         */
        private String templateSuffix = ".html";

        /**
         * 异步发送配置。
         */
        private Async async = new Async();

        /**
         * 重试配置。
         */
        private Retry retry = new Retry();
    }

    /**
     * 邮件异步发送配置。
     */
    @Data
    public static class Async {

        /**
         * 是否启用异步发送。
         */
        private boolean enabled = true;

        /**
         * 线程池核心线程数。
         */
        private int corePoolSize = 4;

        /**
         * 线程池最大线程数。
         */
        private int maxPoolSize = 16;

        /**
         * 队列容量。
         */
        private int queueCapacity = 200;

        /**
         * 线程名前缀。
         */
        private String threadNamePrefix = "molly-mail-";
    }

    /**
     * 邮件发送重试配置。
     */
    @Data
    public static class Retry {

        /**
         * 是否启用重试。
         */
        private boolean enabled = true;

        /**
         * 最大尝试次数（含首次发送）。
         */
        private int maxAttempts = 3;

        /**
         * 退避时间（失败后等待多久再次尝试）。
         */
        private Duration backOff = Duration.ofSeconds(2);
    }
}
