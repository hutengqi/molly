package cn.molly.mq.core.exception;

/**
 * 序列化/反序列化异常
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class MqSerializationException extends MqException {

    public MqSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
