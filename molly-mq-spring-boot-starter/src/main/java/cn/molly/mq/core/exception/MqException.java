package cn.molly.mq.core.exception;

/**
 * Molly MQ 异常根类
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class MqException extends RuntimeException {

    public MqException(String message) {
        super(message);
    }

    public MqException(String message, Throwable cause) {
        super(message, cause);
    }
}
