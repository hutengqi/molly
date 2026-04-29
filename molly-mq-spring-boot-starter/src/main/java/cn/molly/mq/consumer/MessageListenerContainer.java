package cn.molly.mq.consumer;

/**
 * Provider 侧消费容器抽象；由各 Provider 实现并托管生命周期
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public interface MessageListenerContainer {

    void start();

    void stop();

    String topic();

    String group();

    boolean isRunning();
}
