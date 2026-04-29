package cn.molly.mq.consumer;

/**
 * 消费容器工厂 SPI；每个 Provider 提供一个实现
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public interface MessageListenerContainerFactory {

    /**
     * 创建并返回容器实例；工厂不负责 start()，由调用方或 registry 统一管理
     */
    MessageListenerContainer create(MessageListenerEndpoint endpoint);

    /**
     * 工厂归属 Provider；用于多 Provider 场景路由
     */
    String providerName();
}
