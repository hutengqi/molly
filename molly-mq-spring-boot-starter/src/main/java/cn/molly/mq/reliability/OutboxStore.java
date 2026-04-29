package cn.molly.mq.reliability;

import java.util.List;

/**
 * 本地消息表存储 SPI；默认 JDBC 实现，可自定义 Redis / Mongo 实现
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public interface OutboxStore {

    /**
     * 插入一条 PENDING 记录
     */
    void insert(OutboxRecord record);

    /**
     * 抢占式拉取待投递记录（按 next_fire_time 升序，同时避免多实例重复拉取）
     *
     * @param batchSize 单次拉取上限
     * @param now       当前时间戳
     * @return 已被锁定 / 原子更新为进行中的记录
     */
    List<OutboxRecord> fetchPending(int batchSize, long now);

    /**
     * 标记发送成功
     */
    void markSent(String id);

    /**
     * 标记本次尝试失败，下次按退避策略触发
     */
    void markFailed(String id, String error, long nextFireTimeMs);

    /**
     * 超过最大尝试次数，进入 DEAD 状态等待人工介入
     */
    void markDead(String id, String error);
}
