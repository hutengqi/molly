package cn.molly.document.email;

import java.util.concurrent.CompletableFuture;

/**
 * 邮件发送服务。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public interface MailService {

    /**
     * 同步发送（阻塞）。失败将触发重试；达到最大尝试次数后抛出异常。
     */
    void send(MailRequest request);

    /**
     * 异步发送，返回可追踪的 {@link CompletableFuture}。
     * <p>
     * 当 {@code molly.document.email.async.enabled=false} 时，回退为同步执行。
     */
    CompletableFuture<Void> sendAsync(MailRequest request);
}
