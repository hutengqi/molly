package cn.molly.mq.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Molly MQ 监控埋点工具
 * <p>
 * 统一指标名：<br>
 * - {prefix}.consume.count - 消费计数（含 status tag: success/retry/reject/dlq）<br>
 * - {prefix}.consume.latency - 消费耗时<br>
 * - {prefix}.produce.count - 发送计数（含 status tag: success/failed）<br>
 * - {prefix}.outbox.pending - outbox 待处理条数（Gauge，外部注册）
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class MollyMqMetrics {

    private final MeterRegistry registry;
    private final String prefix;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public MollyMqMetrics(MeterRegistry registry, String prefix) {
        this.registry = registry;
        this.prefix = prefix;
    }

    public void recordConsume(String provider, String topic, String status, Duration latency) {
        if (registry == null) {
            return;
        }
        String cKey = prefix + ".consume.count:" + provider + "|" + topic + "|" + status;
        counters.computeIfAbsent(cKey, k -> Counter.builder(prefix + ".consume.count")
                .tags(Tags.of("provider", provider, "topic", topic, "status", status))
                .register(registry)).increment();
        String tKey = prefix + ".consume.latency:" + provider + "|" + topic;
        timers.computeIfAbsent(tKey, k -> Timer.builder(prefix + ".consume.latency")
                .tags(Tags.of("provider", provider, "topic", topic))
                .register(registry)).record(latency);
    }

    public void recordProduce(String provider, String topic, String status) {
        if (registry == null) {
            return;
        }
        String key = prefix + ".produce.count:" + provider + "|" + topic + "|" + status;
        counters.computeIfAbsent(key, k -> Counter.builder(prefix + ".produce.count")
                .tags(Tags.of("provider", provider, "topic", topic, "status", status))
                .register(registry)).increment();
    }

    public MeterRegistry getRegistry() {
        return registry;
    }
}
