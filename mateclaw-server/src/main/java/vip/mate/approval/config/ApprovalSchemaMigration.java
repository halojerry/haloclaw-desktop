package vip.mate.approval.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 审批表 Schema 迁移
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class ApprovalSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createToolApprovalTable();
    }

    private void createToolApprovalTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mate_tool_approval (
                    id                  BIGINT       NOT NULL PRIMARY KEY,
                    pending_id          VARCHAR(32)  NOT NULL UNIQUE,
                    conversation_id     VARCHAR(128) NOT NULL,
                    user_id             VARCHAR(64),
                    agent_id            VARCHAR(64),
                    channel_type        VARCHAR(32),
                    requester_name      VARCHAR(128),
                    reply_target        VARCHAR(512),
                    tool_name           VARCHAR(128) NOT NULL,
                    tool_arguments      TEXT,
                    tool_call_payload   TEXT,
                    tool_call_hash      VARCHAR(64),
                    sibling_tool_calls  TEXT,
                    summary             TEXT,
                    findings_json       TEXT,
                    max_severity        VARCHAR(16),
                    status              VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
                    resolved_by         VARCHAR(64),
                    created_at          DATETIME     NOT NULL,
                    resolved_at         DATETIME,
                    expire_at           DATETIME,
                    create_time         DATETIME     NOT NULL,
                    update_time         DATETIME     NOT NULL,
                    deleted             INT          NOT NULL DEFAULT 0
                )
                """);

            safeExecute("CREATE INDEX IF NOT EXISTS idx_tool_approval_conv ON mate_tool_approval(conversation_id)");
            safeExecute("CREATE INDEX IF NOT EXISTS idx_tool_approval_status ON mate_tool_approval(status)");
            safeExecute("CREATE INDEX IF NOT EXISTS idx_tool_approval_pending_id ON mate_tool_approval(pending_id)");

            log.info("[ApprovalSchemaMigration] mate_tool_approval table ready");
        } catch (Exception e) {
            log.warn("[ApprovalSchemaMigration] Failed to create mate_tool_approval: {}", e.getMessage());
        }
    }

    private void safeExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.debug("[ApprovalSchemaMigration] Index may already exist: {}", e.getMessage());
        }
    }
}
