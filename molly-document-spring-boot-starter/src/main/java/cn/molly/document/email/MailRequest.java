package cn.molly.document.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 邮件发送请求封装。
 * <p>
 * 支持收件人 / 抄送 / 密送 / HTML 或纯文本 / 模板渲染 / 附件 / 内联资源。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailRequest {

    /**
     * 发件人地址，未设置时使用配置 {@code molly.document.email.from}。
     */
    private String from;

    /**
     * 收件人列表（至少一位）。
     */
    private List<String> to;

    /**
     * 抄送列表。
     */
    private List<String> cc;

    /**
     * 密送列表。
     */
    private List<String> bcc;

    /**
     * 邮件主题。
     */
    private String subject;

    /**
     * 邮件正文（与 {@link #template} 互斥）。
     */
    private String content;

    /**
     * 是否 HTML。
     */
    private boolean html;

    /**
     * 模板名（相对 {@code templateLocation}），与 {@link #content} 互斥。
     */
    private String template;

    /**
     * 模板渲染参数。
     */
    private Map<String, Object> templateVariables;

    /**
     * 附件列表。
     */
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    /**
     * 内联图片（HTML 邮件中通过 {@code <img src="cid:xxx"/>} 引用）。
     */
    @Builder.Default
    private List<Inline> inlines = new ArrayList<>();

    /**
     * 附件定义。
     */
    @Data
    @AllArgsConstructor
    public static class Attachment {
        /**
         * 附件展示文件名。
         */
        private String filename;
        /**
         * 源文件。
         */
        private File file;
    }

    /**
     * 内联资源定义。
     */
    @Data
    @AllArgsConstructor
    public static class Inline {
        /**
         * Content-ID。
         */
        private String contentId;
        /**
         * 源文件。
         */
        private File file;
    }
}
