package cn.molly.mq.core;

import java.util.concurrent.CompletableFuture;

/**
 * 统一生产者接口；按语义而非 Provider API 暴露能力
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public interface ProducerOperations {

    /**
     * 同步发送；失败抛 {@link cn.molly.mq.core.exception.MqSendException}
     * <p>
     * 若消息的 shardingKey 非空，Provider 自动走分区顺序路由
     */
    <T> SendResult send(Message<T> message);

    /**
     * 异步发送；回调线程由 Provider 自行管理
     */
    <T> CompletableFuture<SendResult> sendAsync(Message<T> message);

    /**
     * One-way 发送；仅适用于日志类低可靠场景
     */
    <T> void sendOneWay(Message<T> message);

    /**
     * 事务化发送：入 outbox 本地消息表，业务事务提交后由 Flusher 真正投递到 Broker
     * <p>
     * 必须运行在 Spring 事务上下文内，否则抛 {@link IllegalStateException}
     */
    <T> void sendTransactional(Message<T> message);

    /**
     * 当前激活 Provider 名称
     */
    String providerName();
}
