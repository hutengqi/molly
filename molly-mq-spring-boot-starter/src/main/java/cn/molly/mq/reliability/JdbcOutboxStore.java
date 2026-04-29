package cn.molly.mq.reliability;

import cn.molly.mq.core.exception.MqException;
import cn.molly.mq.properties.MollyMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于 JdbcTemplate 的默认 OutboxStore 实现
 * <p>
 * fetchPending 采用"先 SELECT 后 UPDATE"的乐观并发策略：每条记录 update 时带 status+version 比对，
 * 同一时刻多实例扫描互不冲突
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcOutboxStore implements OutboxStore {

    private final JdbcTemplate jdbcTemplate;
    private final MollyMqProperties properties;

    private String table() {
        return properties.getReliability().getTableName();
    }

    @Override
    public void insert(OutboxRecord record) {
        if (record.getId() == null) {
            record.setId(UUID.randomUUID().toString());
        }
        Instant now = Instant.now();
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        if (record.getStatus() == null) {
            record.setStatus(OutboxStatus.PENDING);
        }
        String sql = "INSERT INTO " + table()
                + " (id, provider, topic, tag, biz_key, sharding_key, idempotency_key, headers_json, payload, "
                + "delivery_time_ms, status, attempts, last_error, next_fire_time_ms, created_at, updated_at) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql,
                record.getId(),
                record.getProvider(),
                record.getTopic(),
                record.getTag(),
                record.getBizKey(),
                record.getShardingKey(),
                record.getIdempotencyKey(),
                record.getHeadersJson(),
                record.getPayload(),
                record.getDeliveryTimeMs(),
                record.getStatus().name(),
                record.getAttempts(),
                record.getLastError(),
                record.getNextFireTimeMs(),
                Timestamp.from(now),
                Timestamp.from(now));
    }

    @Override
    public List<OutboxRecord> fetchPending(int batchSize, long now) {
        // 1) 查询候选
        String selectSql = "SELECT * FROM " + table()
                + " WHERE status IN ('PENDING','FAILED') AND next_fire_time_ms <= ? "
                + " ORDER BY next_fire_time_ms ASC LIMIT ?";
        List<OutboxRecord> candidates = jdbcTemplate.query(selectSql, ROW_MAPPER, now, batchSize);
        if (candidates.isEmpty()) {
            return candidates;
        }
        // 2) 逐条原子抢占（status + attempts 作为并发控制），避免多节点重复处理
        List<OutboxRecord> locked = new ArrayList<>(candidates.size());
        String lockSql = "UPDATE " + table()
                + " SET status = 'PENDING', attempts = attempts + 1, updated_at = ? "
                + " WHERE id = ? AND status IN ('PENDING','FAILED') AND attempts = ?";
        Timestamp ts = Timestamp.from(Instant.now());
        for (OutboxRecord r : candidates) {
            int affected = jdbcTemplate.update(lockSql, ts, r.getId(), r.getAttempts());
            if (affected == 1) {
                r.setAttempts(r.getAttempts() + 1);
                r.setStatus(OutboxStatus.PENDING);
                locked.add(r);
            }
        }
        return locked;
    }

    @Override
    public void markSent(String id) {
        jdbcTemplate.update("UPDATE " + table() + " SET status = 'SENT', updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now()), id);
    }

    @Override
    public void markFailed(String id, String error, long nextFireTimeMs) {
        jdbcTemplate.update("UPDATE " + table()
                        + " SET status = 'FAILED', last_error = ?, next_fire_time_ms = ?, updated_at = ? WHERE id = ?",
                truncate(error), nextFireTimeMs, Timestamp.from(Instant.now()), id);
    }

    @Override
    public void markDead(String id, String error) {
        jdbcTemplate.update("UPDATE " + table()
                        + " SET status = 'DEAD', last_error = ?, updated_at = ? WHERE id = ?",
                truncate(error), Timestamp.from(Instant.now()), id);
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }

    private static final RowMapper<OutboxRecord> ROW_MAPPER = (rs, rn) -> {
        OutboxRecord r = new OutboxRecord();
        r.setId(rs.getString("id"));
        r.setProvider(rs.getString("provider"));
        r.setTopic(rs.getString("topic"));
        r.setTag(rs.getString("tag"));
        r.setBizKey(rs.getString("biz_key"));
        r.setShardingKey(rs.getString("sharding_key"));
        r.setIdempotencyKey(rs.getString("idempotency_key"));
        r.setHeadersJson(rs.getString("headers_json"));
        r.setPayload(rs.getBytes("payload"));
        long dt = rs.getLong("delivery_time_ms");
        r.setDeliveryTimeMs(rs.wasNull() ? null : dt);
        r.setStatus(OutboxStatus.valueOf(rs.getString("status")));
        r.setAttempts(rs.getInt("attempts"));
        r.setLastError(rs.getString("last_error"));
        r.setNextFireTimeMs(rs.getLong("next_fire_time_ms"));
        Timestamp c = rs.getTimestamp("created_at");
        r.setCreatedAt(c == null ? null : c.toInstant());
        Timestamp u = rs.getTimestamp("updated_at");
        r.setUpdatedAt(u == null ? null : u.toInstant());
        return r;
    };
}
