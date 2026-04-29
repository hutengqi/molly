package cn.molly.mq.reliability;

import cn.molly.mq.core.Message;

/**
 * 本地消息表写入 SPI；由 outbox 模块实现，t5 提供 JDBC 默认实现
 * <p>
 * 调用 stage 必须运行在 Spring 事务上下文；未启用 outbox 时该 Bean 不存在，调用方应降级为普通 send
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public interface OutboxPublisher {

    /**
     * 将消息持久化到本地消息表，业务事务提交后由 Flusher 真投递
     *
     * @param message      业务消息
     * @param providerName 目标 Provider 名称
     */
    <T> void stage(Message<T> message, String providerName);
}
