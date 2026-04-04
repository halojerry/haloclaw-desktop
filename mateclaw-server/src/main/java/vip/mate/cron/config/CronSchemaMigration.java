package vip.mate.cron.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * CronJob 表 Schema 迁移
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
public class CronSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("mate_cron_job", "timezone", "VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai'");
        addColumnIfMissing("mate_cron_job", "task_type", "VARCHAR(16) NOT NULL DEFAULT 'text'");
        addColumnIfMissing("mate_cron_job", "request_body", "TEXT");
        addColumnIfMissing("mate_cron_job", "next_run_time", "DATETIME");
        log.info("[CronSchemaMigration] mate_cron_job schema migration completed");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column + " " + definition);
        } catch (Exception e) {
            log.debug("[CronSchemaMigration] Column {} may already exist: {}", column, e.getMessage());
        }
    }
}
