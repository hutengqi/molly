package cn.molly.mq.core;

/**
 * 统一消费监听器
 * <p>
 * 实现方直接抛出异常等价于返回 {@link ConsumeResult#RETRY}
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@FunctionalInterface
public interface MessageListener<T> {

    /**
     * 处理单条消息
     *
     * @param message 反序列化后的消息
     * @param context 消费上下文
     * @return 消费结果
     */
    ConsumeResult onMessage(Message<T> message, ConsumeContext context);
}
