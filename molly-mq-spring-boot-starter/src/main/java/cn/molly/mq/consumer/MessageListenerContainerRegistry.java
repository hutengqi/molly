package cn.molly.mq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 统一管理所有 Provider 消费容器的注册表；
 * <p>
 * 利用 {@link SmartLifecycle} 在 Spring 容器就绪后启动、销毁前停止
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
public class MessageListenerContainerRegistry implements SmartLifecycle, DisposableBean {

    private final List<MessageListenerContainer> containers = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;

    public void register(MessageListenerContainer container) {
        containers.add(container);
        if (running) {
            container.start();
        }
    }

    public List<MessageListenerContainer> getContainers() {
        return Collections.unmodifiableList(new ArrayList<>(containers));
    }

    @Override
    public void start() {
        if (!running) {
            running = true;
            containers.forEach(c -> {
                try {
                    c.start();
                } catch (Exception e) {
                    log.error("[molly-mq] container start failed topic={}", c.topic(), e);
                }
            });
            log.info("[molly-mq] registry started, containers={}", containers.size());
        }
    }

    @Override
    public void stop() {
        if (running) {
            running = false;
            containers.forEach(c -> {
                try {
                    c.stop();
                } catch (Exception e) {
                    log.warn("[molly-mq] container stop failed topic={}", c.topic(), e);
                }
            });
            log.info("[molly-mq] registry stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // 晚于 DataSource / RocketMQ producer；早于 WebServer 停止
        return Integer.MAX_VALUE - 1000;
    }

    @Override
    public void destroy() {
        stop();
    }
}
