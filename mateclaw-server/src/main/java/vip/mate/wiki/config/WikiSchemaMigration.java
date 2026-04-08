package vip.mate.wiki.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Wiki 模块 Schema 迁移
 * <p>
 * 确保 Wiki 相关表存在（兼容已有部署）。
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@Order(202)
@RequiredArgsConstructor
public class WikiSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createKnowledgeBaseTable();
        createRawMaterialTable();
        createPageTable();
        migrateKnowledgeBaseColumns();
        log.info("[WikiSchemaMigration] Wiki schema migration completed");
    }

    private void createKnowledgeBaseTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mate_wiki_knowledge_base (
                    id              BIGINT       NOT NULL PRIMARY KEY,
                    name            VARCHAR(128) NOT NULL,
                    description     TEXT,
                    agent_id        BIGINT,
                    config_content  CLOB,
                    status          VARCHAR(32)  NOT NULL DEFAULT 'active',
                    page_count      INT          NOT NULL DEFAULT 0,
                    raw_count       INT          NOT NULL DEFAULT 0,
                    create_time     DATETIME     NOT NULL,
                    update_time     DATETIME     NOT NULL,
                    deleted         INT          NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_wiki_kb_agent ON mate_wiki_knowledge_base(agent_id)");
    }

    private void createRawMaterialTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mate_wiki_raw_material (
                    id                BIGINT       NOT NULL PRIMARY KEY,
                    kb_id             BIGINT       NOT NULL,
                    title             VARCHAR(256) NOT NULL,
                    source_type       VARCHAR(32)  NOT NULL DEFAULT 'text',
                    source_path       VARCHAR(512),
                    original_content  CLOB,
                    extracted_text    CLOB,
                    content_hash      VARCHAR(64),
                    file_size         BIGINT       NOT NULL DEFAULT 0,
                    processing_status VARCHAR(32)  NOT NULL DEFAULT 'pending',
                    last_processed_at DATETIME,
                    error_message     VARCHAR(512),
                    create_time       DATETIME     NOT NULL,
                    update_time       DATETIME     NOT NULL,
                    deleted           INT          NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_wiki_raw_kb ON mate_wiki_raw_material(kb_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_wiki_raw_status ON mate_wiki_raw_material(kb_id, processing_status)");
    }

    private void createPageTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mate_wiki_page (
                    id              BIGINT       NOT NULL PRIMARY KEY,
                    kb_id           BIGINT       NOT NULL,
                    slug            VARCHAR(256) NOT NULL,
                    title           VARCHAR(256) NOT NULL,
                    content         CLOB,
                    summary         VARCHAR(1024),
                    outgoing_links  CLOB,
                    source_raw_ids  CLOB,
                    version         INT          NOT NULL DEFAULT 1,
                    last_updated_by VARCHAR(32)  NOT NULL DEFAULT 'ai',
                    create_time     DATETIME     NOT NULL,
                    update_time     DATETIME     NOT NULL,
                    deleted         INT          NOT NULL DEFAULT 0,
                    CONSTRAINT uk_wiki_page_kb_slug UNIQUE (kb_id, slug)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_wiki_page_kb ON mate_wiki_page(kb_id)");
    }

    /**
     * 增量字段迁移（兼容已有部署）
     */
    private void migrateKnowledgeBaseColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE mate_wiki_knowledge_base ADD COLUMN IF NOT EXISTS source_directory VARCHAR(512)");
        } catch (Exception e) {
            // MySQL 不支持 ADD COLUMN IF NOT EXISTS，忽略已存在的错误
            if (!e.getMessage().contains("Duplicate column")) {
                log.warn("[WikiSchemaMigration] Failed to add source_directory column: {}", e.getMessage());
            }
        }
    }
}
