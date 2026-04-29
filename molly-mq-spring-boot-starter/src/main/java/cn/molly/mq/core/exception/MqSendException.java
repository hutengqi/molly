package cn.molly.mq.core.exception;

/**
 * 发送失败异常
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class MqSendException extends MqException {

    public MqSendException(String message) {
        super(message);
    }

    public MqSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
