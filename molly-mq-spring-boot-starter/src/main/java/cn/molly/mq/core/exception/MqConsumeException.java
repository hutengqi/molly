package cn.molly.mq.core.exception;

/**
 * 消费阶段异常；触发重试或 DLQ
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class MqConsumeException extends MqException {

    public MqConsumeException(String message) {
        super(message);
    }

    public MqConsumeException(String message, Throwable cause) {
        super(message, cause);
    }
}
