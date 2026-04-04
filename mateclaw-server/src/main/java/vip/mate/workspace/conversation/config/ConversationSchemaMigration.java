package vip.mate.workspace.conversation.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        ensureColumn("mate_message", "content_parts", "TEXT");
        ensureColumn("mate_message", "prompt_tokens", "INT DEFAULT 0");
        ensureColumn("mate_message", "completion_tokens", "INT DEFAULT 0");
        ensureColumn("mate_message", "runtime_model", "VARCHAR(128)");
        ensureColumn("mate_message", "runtime_provider", "VARCHAR(64)");
        ensureColumn("mate_conversation", "stream_status", "VARCHAR(16) NOT NULL DEFAULT 'idle'");
        // 启动时重置孤儿状态：上次意外关机可能残留 running 状态
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE mate_conversation SET stream_status = 'idle' WHERE stream_status = 'running'");
            if (updated > 0) {
                log.info("Reset {} orphaned 'running' conversations to 'idle'", updated);
            }
        } catch (DataAccessException e) {
            log.debug("Skipping orphan reset (table may not exist yet): {}", e.getMessage());
        }

        normalizeSharedChannelConversationOwners();
    }

    private void normalizeSharedChannelConversationOwners() {
        String sql = """
                UPDATE mate_conversation
                SET username = 'system'
                WHERE username <> 'system'
                  AND (
                      conversation_id LIKE 'feishu:%'
                   OR conversation_id LIKE 'dingtalk:%'
                   OR conversation_id LIKE 'telegram:%'
                   OR conversation_id LIKE 'discord:%'
                   OR conversation_id LIKE 'wecom:%'
                   OR conversation_id LIKE 'qq:%'
                   OR conversation_id LIKE 'weixin:%'
                  )
                  AND deleted = 0
                """;
        try {
            int updated = jdbcTemplate.update(sql);
            if (updated > 0) {
                log.info("Normalized {} shared channel conversation owner(s) to system", updated);
            }
        } catch (DataAccessException e) {
            log.debug("Skipping shared channel owner normalization: {}", e.getMessage());
        }
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " " + ddl;
        try {
            jdbcTemplate.execute(sql);
            log.info("Ensured schema column exists: {}.{}", tableName, columnName);
        } catch (DataAccessException e) {
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (message.contains("duplicate column") || message.contains("already exists")
                    || message.contains("not found")) {
                log.info("Schema migration skipped for {}.{}: {}", tableName, columnName,
                        message.contains("not found") ? "table not yet created" : "column already exists");
                return;
            }
            throw e;
        }
    }
}
