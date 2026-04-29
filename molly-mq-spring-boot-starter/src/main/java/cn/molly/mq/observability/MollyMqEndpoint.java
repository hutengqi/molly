package cn.molly.mq.observability;

import cn.molly.mq.consumer.MessageListenerContainer;
import cn.molly.mq.consumer.MessageListenerContainerRegistry;
import cn.molly.mq.core.ProducerOperations;
import cn.molly.mq.properties.MollyMqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * /actuator/mollymq 端点：暴露 Provider / Consumer Container / 关键配置项快照
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@RequiredArgsConstructor
@Endpoint(id = "mollymq")
public class MollyMqEndpoint {

    private final ProducerOperations producer;
    private final MessageListenerContainerRegistry registry;
    private final MollyMqProperties properties;

    @ReadOperation
    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", properties.isEnabled());
        result.put("provider", properties.getProvider().name().toLowerCase());
        result.put("producer", producer == null ? null : Map.of("providerName", producer.providerName()));

        List<MessageListenerContainer> containers = registry == null
                ? List.of()
                : registry.getContainers();
        result.put("consumers", containers.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("topic", c.topic());
            m.put("group", c.group());
            m.put("running", c.isRunning());
            return m;
        }).collect(Collectors.toList()));

        Map<String, Object> reliability = new LinkedHashMap<>();
        reliability.put("outboxEnabled", properties.getReliability().isOutboxEnabled());
        reliability.put("tableName", properties.getReliability().getTableName());
        reliability.put("scanInterval", properties.getReliability().getScanInterval().toString());
        result.put("reliability", reliability);

        Map<String, Object> consumer = new LinkedHashMap<>();
        consumer.put("group", properties.getConsumer().getGroup());
        consumer.put("concurrency", properties.getConsumer().getConcurrency());
        consumer.put("maxRetries", properties.getConsumer().getMaxRetries());
        consumer.put("idempotencyStore", properties.getConsumer().getIdempotencyStore());
        result.put("consumerConfig", consumer);
        return result;
    }
}
