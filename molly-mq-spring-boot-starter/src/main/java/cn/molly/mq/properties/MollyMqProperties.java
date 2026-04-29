package cn.molly.mq.properties;

import cn.molly.mq.provider.MqProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Molly MQ 顶层配置
 * <p>
 * 配置前缀 {@code molly.mq}，严格单 Provider 语义：通过 {@link #provider} 决定启用哪个 Provider 子配置
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Data
@ConfigurationProperties(prefix = "molly.mq")
public class MollyMqProperties {

    /**
     * 是否启用 molly-mq，默认 true
     */
    private boolean enabled = true;

    /**
     * 当前激活的 Provider，默认 ROCKETMQ
     */
    private MqProvider provider = MqProvider.ROCKETMQ;

    /**
     * 生产者通用配置
     */
    private Producer producer = new Producer();

    /**
     * 消费者通用配置
     */
    private Consumer consumer = new Consumer();

    /**
     * 可靠性配置：本地消息表 / outbox
     */
    private Reliability reliability = new Reliability();

    /**
     * 可观测配置
     */
    private Observability observability = new Observability();

    /**
     * Provider 专属配置（按需填写对应分支）
     */
    private RocketMq rocketmq = new RocketMq();

    private Kafka kafka = new Kafka();

    private Pulsar pulsar = new Pulsar();

    private Rabbit rabbit = new Rabbit();

    @Data
    public static class Producer {
        /** 生产者分组 / clientId */
        private String group = "molly-mq-producer";
        /** 发送超时 */
        private Duration sendTimeout = Duration.ofSeconds(3);
        /** 同步发送失败本地重试次数（不涉及 outbox） */
        private int retryTimes = 2;
        /** 单条消息体积上限字节 */
        private int maxMessageSize = 4 * 1024 * 1024;
        /** 是否强制顺序（shardingKey 非空时始终顺序，此开关用于对所有消息强制串行） */
        private boolean forceOrdered = false;
    }

    @Data
    public static class Consumer {
        /** 消费者分组 */
        private String group = "molly-mq-consumer";
        /** 单次拉取消息数 */
        private int batchSize = 16;
        /** 消费并发度（顺序消费时退化为单分区单线程） */
        private int concurrency = 4;
        /** 最大重试次数，超出投 DLQ */
        private int maxRetries = 16;
        /** 重试初始退避 */
        private Duration retryInitialBackoff = Duration.ofSeconds(1);
        /** 重试最大退避 */
        private Duration retryMaxBackoff = Duration.ofMinutes(5);
        /** DLQ 主题后缀 */
        private String dlqSuffix = "-DLQ";
        /** 幂等窗口（LocalIdempotencyStore 过期时间） */
        private Duration idempotencyTtl = Duration.ofHours(24);
        /** 幂等存储类型：local / redis / none */
        private String idempotencyStore = "local";
    }

    @Data
    public static class Reliability {
        /** 是否启用本地消息表（outbox 事务消息） */
        private boolean outboxEnabled = false;
        /** outbox 物理表名 */
        private String tableName = "mq_outbox";
        /** Flusher 扫描周期 */
        private Duration scanInterval = Duration.ofSeconds(5);
        /** 单次扫描最大条数 */
        private int scanBatchSize = 200;
        /** 单条消息最大投递尝试次数，超出标记 DEAD */
        private int maxAttempts = 32;
        /** 扫描时认为消息已卡死需要重投的阈值（距上次更新） */
        private Duration stuckThreshold = Duration.ofMinutes(5);
    }

    @Data
    public static class Observability {
        /** Micrometer 指标前缀 */
        private String metricPrefix = "molly.mq";
        /** 是否启用 OpenTelemetry tracing */
        private boolean tracingEnabled = true;
        /** 是否注册 /actuator/mollymq Endpoint */
        private boolean endpointEnabled = true;
    }

    @Data
    public static class RocketMq {
        private String nameServer = "localhost:9876";
        private String namespace;
        private String accessKey;
        private String secretKey;
        /** VipChannel 开关 */
        private boolean vipChannelEnabled = false;
        private Map<String, String> extra = new HashMap<>();
    }

    @Data
    public static class Kafka {
        private String bootstrapServers = "localhost:9092";
        /** producer acks：all / 1 / 0；默认 all */
        private String acks = "all";
        /** 启用幂等 producer */
        private boolean idempotenceEnabled = true;
        private Map<String, String> extra = new HashMap<>();
    }

    @Data
    public static class Pulsar {
        private String serviceUrl = "pulsar://localhost:6650";
        private String authToken;
        private Map<String, String> extra = new HashMap<>();
    }

    @Data
    public static class Rabbit {
        private String host = "localhost";
        private int port = 5672;
        private String username = "guest";
        private String password = "guest";
        private String virtualHost = "/";
        /** 是否启用 publisher confirms */
        private boolean publisherConfirms = true;
        /** 是否启用 mandatory 路由失败回退 */
        private boolean mandatory = true;
        private Map<String, String> extra = new HashMap<>();
    }
}
