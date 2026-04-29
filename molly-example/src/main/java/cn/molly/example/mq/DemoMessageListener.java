package cn.molly.example.mq;

import cn.molly.mq.consumer.MollyMessageListener;
import cn.molly.mq.core.ConsumeContext;
import cn.molly.mq.core.ConsumeResult;
import cn.molly.mq.core.Message;
import cn.molly.mq.core.MessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * mq profile 注解式消费示例：订阅 demo-topic
 *
 * @author Ht7_Sincerity
 * @since 2026/4/29
 */
@Slf4j
@Profile("mq")
@Component
@MollyMessageListener(topic = "demo-topic", tag = "*", group = "molly-example-consumer")
public class DemoMessageListener implements MessageListener<byte[]> {

    @Override
    public ConsumeResult onMessage(Message<byte[]> message, ConsumeContext context) {
        String body = message.getPayload() == null
                ? "" : new String(message.getPayload(), StandardCharsets.UTF_8);
        log.info("[example][mq] received topic={} msgId={} deliveryCount={} body={}",
                context.getTopic(), context.getMessageId(), context.getDeliveryCount(), body);
        return ConsumeResult.SUCCESS;
    }
}
