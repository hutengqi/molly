package cn.molly.mq.core.exception;

/**
 * 未找到匹配的 Provider（SDK 缺失或 provider 配置错误）
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class MqProviderNotFoundException extends MqException {

    public MqProviderNotFoundException(String message) {
        super(message);
    }
}
