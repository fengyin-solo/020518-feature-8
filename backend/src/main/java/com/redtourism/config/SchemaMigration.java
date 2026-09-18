package com.redtourism.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 客服链路相关表结构的幂等迁移：
 * 全新部署由 schema.sql 建表；已有数据卷启动时在此补齐缺失的表与列。
 */
@Component
public class SchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS service_session (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id BIGINT NOT NULL," +
                    "stage VARCHAR(20) NOT NULL DEFAULT 'BOT'," +
                    "pending_faq_ids VARCHAR(500)," +
                    "resolved_faq_id BIGINT," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uk_user (user_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS service_flow_log (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id BIGINT NOT NULL," +
                    "step VARCHAR(20) NOT NULL," +
                    "detail TEXT," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_user (user_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            if (!columnExists("service_chat", "msg_type")) {
                jdbcTemplate.execute(
                        "ALTER TABLE service_chat ADD COLUMN msg_type VARCHAR(20) DEFAULT 'TEXT' AFTER sender");
                log.info("service_chat 已补充 msg_type 列");
            }
        } catch (Exception e) {
            log.warn("客服链路表结构迁移跳过：{}", e.getMessage());
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        return count != null && count > 0;
    }
}
