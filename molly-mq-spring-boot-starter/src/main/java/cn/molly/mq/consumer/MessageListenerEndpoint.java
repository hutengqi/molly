package cn.molly.mq.consumer;

import cn.molly.mq.core.MessageListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 消费监听器元信息；由 BPP 或 Factory 构造，传入具体 Provider 的 Container 工厂创建底层容器
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Data
@Builder
@AllArgsConstructor
public class MessageListenerEndpoint {

    /** 订阅主题 */
    private final String topic;

    /** 订阅标签过滤 */
    private final String tag;

    /** 消费分组 */
    private final String group;

    /** payload 类型 */
    private final Class<?> payloadType;

    /** 是否顺序 */
    private final boolean ordered;

    /** 并发度（顺序时忽略） */
    private final int concurrency;

    /** 业务 listener 实现 */
    private final MessageListener<?> listener;
}
