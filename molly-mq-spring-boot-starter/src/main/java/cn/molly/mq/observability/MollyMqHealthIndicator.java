package cn.molly.mq.observability;

import cn.molly.mq.consumer.MessageListenerContainer;
import cn.molly.mq.consumer.MessageListenerContainerRegistry;
import cn.molly.mq.core.ProducerOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Molly MQ HealthIndicator：检测 Producer 激活 Provider 以及所有消费容器是否运行中
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@RequiredArgsConstructor
public class MollyMqHealthIndicator implements HealthIndicator {

    private final ProducerOperations producer;
    private final MessageListenerContainerRegistry registry;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        builder.withDetail("provider", producer == null ? "none" : producer.providerName());

        List<MessageListenerContainer> containers = registry == null
                ? List.of()
                : registry.getContainers();
        builder.withDetail("consumers.total", containers.size());
        List<String> down = containers.stream()
                .filter(c -> !c.isRunning())
                .map(c -> c.topic() + "@" + c.group())
                .collect(Collectors.toList());
        if (!down.isEmpty()) {
            builder.down().withDetail("consumers.down", down);
        }
        return builder.build();
    }
}
