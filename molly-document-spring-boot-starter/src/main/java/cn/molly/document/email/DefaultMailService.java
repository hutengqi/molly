package cn.molly.document.email;

import cn.molly.document.core.DocumentException;
import cn.molly.document.properties.MollyDocumentProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 默认邮件服务实现。
 * <p>
 * 提供模板渲染（Thymeleaf 可选）、附件、内联资源、异步发送、失败重试能力。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Slf4j
public class DefaultMailService implements MailService {

    private final JavaMailSender mailSender;
    private final MollyDocumentProperties.Email properties;
    /**
     * Thymeleaf 模板引擎，可选；为 {@code null} 时仅支持 {@code ${key}} 朴素占位符。
     */
    private final TemplateEngine templateEngine;
    private final Executor asyncExecutor;

    public DefaultMailService(JavaMailSender mailSender,
                              MollyDocumentProperties.Email properties,
                              TemplateEngine templateEngine,
                              Executor asyncExecutor) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.templateEngine = templateEngine;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public void send(MailRequest request) {
        MollyDocumentProperties.Retry retry = properties.getRetry();
        int attempts = retry.isEnabled() ? Math.max(1, retry.getMaxAttempts()) : 1;
        long backOffMs = retry.getBackOff() == null ? 0 : retry.getBackOff().toMillis();

        Throwable last = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                doSend(request);
                return;
            } catch (Exception e) {
                last = e;
                log.warn("邮件发送失败 ({}/{}): {}", i, attempts, e.getMessage());
                if (i < attempts && backOffMs > 0) {
                    try {
                        Thread.sleep(backOffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new DocumentException("邮件发送失败，已达最大重试次数", last);
    }

    @Override
    public CompletableFuture<Void> sendAsync(MailRequest request) {
        if (!properties.getAsync().isEnabled() || asyncExecutor == null) {
            try {
                send(request);
                return CompletableFuture.completedFuture(null);
            } catch (RuntimeException e) {
                CompletableFuture<Void> f = new CompletableFuture<>();
                f.completeExceptionally(e);
                return f;
            }
        }
        return CompletableFuture.runAsync(() -> send(request), asyncExecutor);
    }

    private void doSend(MailRequest request) throws Exception {
        if (request.getTo() == null || request.getTo().isEmpty()) {
            throw new DocumentException("收件人为空");
        }
        MimeMessage message = mailSender.createMimeMessage();
        boolean multipart = !request.getAttachments().isEmpty() || !request.getInlines().isEmpty();
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, StandardCharsets.UTF_8.name());

        String from = StringUtils.hasText(request.getFrom()) ? request.getFrom() : properties.getFrom();
        if (!StringUtils.hasText(from)) {
            throw new DocumentException("未配置发件人 (molly.document.email.from)");
        }
        helper.setFrom(from);
        helper.setTo(request.getTo().toArray(new String[0]));
        if (request.getCc() != null && !request.getCc().isEmpty()) {
            helper.setCc(request.getCc().toArray(new String[0]));
        }
        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            helper.setBcc(request.getBcc().toArray(new String[0]));
        }
        helper.setSubject(request.getSubject() == null ? "" : request.getSubject());

        String body = resolveBody(request);
        helper.setText(body, request.isHtml() || StringUtils.hasText(request.getTemplate()));

        for (MailRequest.Attachment att : request.getAttachments()) {
            helper.addAttachment(att.getFilename(), att.getFile());
        }
        for (MailRequest.Inline inline : request.getInlines()) {
            helper.addInline(inline.getContentId(), inline.getFile());
        }
        mailSender.send(message);
    }

    private String resolveBody(MailRequest request) {
        if (StringUtils.hasText(request.getTemplate())) {
            Map<String, Object> vars = request.getTemplateVariables() == null
                    ? new HashMap<>()
                    : request.getTemplateVariables();
            if (templateEngine != null) {
                Context ctx = new Context();
                ctx.setVariables(vars);
                return templateEngine.process(request.getTemplate(), ctx);
            }
            // 朴素占位符模式：以模板名作为正文，再替换 ${var}
            return replacePlaceholders(request.getTemplate(), vars);
        }
        return request.getContent() == null ? "" : request.getContent();
    }

    private static String replacePlaceholders(String template, Map<String, Object> vars) {
        if (vars == null || vars.isEmpty()) {
            return template;
        }
        String out = template;
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            out = out.replace("${" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue().toString());
        }
        return out;
    }
}
