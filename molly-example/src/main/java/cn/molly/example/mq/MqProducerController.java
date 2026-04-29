package cn.molly.example.mq;

import cn.molly.mq.core.Message;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.core.SendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * mq profile 生产者示例：演示同步 / 顺序 / 事务化发送
 *
 * @author Ht7_Sincerity
 * @since 2026/4/29
 */
@Slf4j
@Profile("mq")
@RestController
@RequiredArgsConstructor
@RequestMapping("/mq")
public class MqProducerController {

    private final ProducerOperations producer;

    /**
     * 普通发送：GET /mq/send?topic=demo-topic&msg=hello
     */
    @GetMapping("/send")
    public Map<String, Object> send(@RequestParam(defaultValue = "demo-topic") String topic,
                                    @RequestParam(defaultValue = "hello molly-mq") String msg) {
        SendResult result = producer.send(Message.of(topic, msg.getBytes()));
        log.info("[example][mq] sent msgId={} status={}", result.getMessageId(), result.getStatus());
        return Map.of(
                "messageId", String.valueOf(result.getMessageId()),
                "status", result.getStatus().name(),
                "provider", producer.providerName()
        );
    }

    /**
     * 顺序发送：GET /mq/send-ordered?topic=demo-order&shardingKey=ORDER-001&msg=...
     */
    @GetMapping("/send-ordered")
    public Map<String, Object> sendOrdered(@RequestParam(defaultValue = "demo-order") String topic,
                                           @RequestParam(defaultValue = "ORDER-001") String shardingKey,
                                           @RequestParam(defaultValue = "ordered payload") String msg) {
        SendResult result = producer.send(Message.ordered(topic, shardingKey, msg.getBytes()));
        return Map.of(
                "messageId", String.valueOf(result.getMessageId()),
                "shardingKey", shardingKey,
                "partition", String.valueOf(result.getPartition())
        );
    }
}
