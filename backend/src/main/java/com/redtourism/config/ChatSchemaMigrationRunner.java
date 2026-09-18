package com.redtourism.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 客服链路升级的幂等迁移：
 * schema.sql 仅在 MySQL 数据卷首次初始化时执行，对已存在的数据卷，
 * 这里在应用启动时检测并补齐 service_chat 新字段与 service_session 表，
 * 同时把历史消息（旧数据仅有 USER/ADMIN 两种发送方）补全类型与阶段，便于列表页展示。
 */
@Slf4j
@Component
public class ChatSchemaMigrationRunner implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureServiceChatColumns();
        ensureServiceSessionTable();
        backfillLegacyMessages();
    }

    private boolean columnExists(String table, String column) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COUNT(*) AS c FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                table, column);
        Number n = (Number) rows.get(0).get("c");
        return n != null && n.intValue() > 0;
    }

    private void ensureServiceChatColumns() {
        addColumnIfMissing("service_chat", "msg_type",
                "ALTER TABLE service_chat ADD COLUMN msg_type VARCHAR(16) NOT NULL DEFAULT 'CHAT' "
                        + "COMMENT 'CHAT/CLARIFY/ANSWER/FALLBACK/HANDOFF' AFTER sender");
        addColumnIfMissing("service_chat", "ref_ids",
                "ALTER TABLE service_chat ADD COLUMN ref_ids VARCHAR(255) NULL AFTER content");
        addColumnIfMissing("service_chat", "trace_no",
                "ALTER TABLE service_chat ADD COLUMN trace_no VARCHAR(32) NULL AFTER ref_ids");
        addColumnIfMissing("service_chat", "stage",
                "ALTER TABLE service_chat ADD COLUMN stage VARCHAR(16) NULL AFTER trace_no");
        try {
            jdbcTemplate.execute("ALTER TABLE service_chat ADD INDEX idx_trace (trace_no)");
        } catch (Exception e) {
            log.debug("idx_trace 已存在，跳过");
        }
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        if (!columnExists(table, column)) {
            log.info("执行表结构迁移：{} ADD COLUMN {}", table, column);
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureServiceSessionTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS service_session ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "user_id BIGINT NOT NULL UNIQUE, "
                        + "stage VARCHAR(16) NOT NULL DEFAULT 'ASK' COMMENT 'ASK/CLARIFY/ANSWER/WAITING_HUMAN/IN_HUMAN', "
                        + "pending_faq_ids VARCHAR(255) NULL, "
                        + "active_trace_no VARCHAR(32) NULL, "
                        + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP, "
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void backfillLegacyMessages() {
        try {
            // 旧的 ADMIN 消息：人工服务中；旧的 USER 消息：等待人工接入
            int admin = jdbcTemplate.update(
                    "UPDATE service_chat SET msg_type='CHAT', stage='IN_HUMAN' "
                            + "WHERE sender='ADMIN' AND (stage IS NULL OR stage='')");
            int user = jdbcTemplate.update(
                    "UPDATE service_chat SET msg_type='CHAT', stage='WAITING_HUMAN' "
                            + "WHERE sender='USER' AND (stage IS NULL OR stage='')");
            // 为这些老用户补建会话状态：出现过客服回复的记为人工服务中，否则等待人工
            int sessions = jdbcTemplate.update(
                    "INSERT INTO service_session (user_id, stage, create_time, update_time) "
                            + "SELECT sc.user_id, "
                            + "CASE WHEN EXISTS (SELECT 1 FROM service_chat a "
                            + "WHERE a.user_id = sc.user_id AND a.sender='ADMIN') "
                            + "THEN 'IN_HUMAN' ELSE 'WAITING_HUMAN' END, NOW(), NOW() "
                            + "FROM (SELECT DISTINCT user_id FROM service_chat) sc "
                            + "LEFT JOIN service_session ss ON ss.user_id = sc.user_id "
                            + "WHERE ss.user_id IS NULL");
            if (admin + user + sessions > 0) {
                log.info("历史客服数据回填完成：ADMIN 消息 {} 条，USER 消息 {} 条，补建会话 {} 个",
                        admin, user, sessions);
            }
        } catch (Exception e) {
            log.warn("历史客服消息回填跳过：{}", e.getMessage());
        }
    }
}
