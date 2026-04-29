-- molly-mq outbox 本地消息表 DDL
-- 支持 MySQL 8+ / PostgreSQL 14+（PG 请将 BIGINT AUTO_INCREMENT 改成 BIGSERIAL，BLOB 改成 BYTEA，TIMESTAMP 保持）
CREATE TABLE IF NOT EXISTS mq_outbox (
    id                 VARCHAR(64)   NOT NULL PRIMARY KEY COMMENT '主键',
    provider           VARCHAR(32)   NOT NULL COMMENT '目标 Provider 名',
    topic              VARCHAR(256)  NOT NULL COMMENT '业务主题',
    tag                VARCHAR(128)  NULL COMMENT '业务标签',
    biz_key            VARCHAR(128)  NULL COMMENT '业务唯一键',
    sharding_key       VARCHAR(128)  NULL COMMENT '顺序键',
    idempotency_key    VARCHAR(128)  NULL COMMENT '幂等键',
    headers_json       TEXT          NULL COMMENT '消息头 JSON',
    payload            BLOB          NOT NULL COMMENT '消息体字节',
    delivery_time_ms   BIGINT        NULL COMMENT '延迟投递绝对时间戳',
    status             VARCHAR(16)   NOT NULL COMMENT 'PENDING/SENT/FAILED/DEAD',
    attempts           INT           NOT NULL DEFAULT 0 COMMENT '已尝试次数',
    last_error         VARCHAR(1024) NULL COMMENT '最近一次错误',
    next_fire_time_ms  BIGINT        NOT NULL COMMENT '下次触发时间',
    created_at         TIMESTAMP     NOT NULL COMMENT '创建时间',
    updated_at         TIMESTAMP     NOT NULL COMMENT '更新时间',
    INDEX idx_status_fire (status, next_fire_time_ms),
    INDEX idx_biz_key (biz_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='molly-mq 本地消息表';
