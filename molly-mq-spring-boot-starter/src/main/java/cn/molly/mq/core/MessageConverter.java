package cn.molly.mq.core;

/**
 * 消息 payload 序列化 SPI
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public interface MessageConverter {

    /**
     * 将业务对象序列化为字节数组
     */
    byte[] toBytes(Object payload);

    /**
     * 将字节数组反序列化为业务对象
     */
    <T> T fromBytes(byte[] bytes, Class<T> type);

    /**
     * 转换器标识，用于写入消息头便于反序列化时选择
     */
    String contentType();
}
