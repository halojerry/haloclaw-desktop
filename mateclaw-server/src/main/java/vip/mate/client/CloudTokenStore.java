package vip.mate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 云端Token本地存储
 * 使用H2数据库持久化Token信息
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudTokenStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String TABLE_NAME = "cloud_token";

    /**
     * 初始化Token表
     */
    public void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS cloud_token (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                tenant_id VARCHAR(100) NOT NULL,
                access_token TEXT,
                refresh_token TEXT,
                token_type VARCHAR(50),
                expires_in BIGINT,
                expires_at TIMESTAMP,
                user_id BIGINT,
                username VARCHAR(100),
                extra_data TEXT,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(tenant_id)
            )
            """;
        jdbcTemplate.execute(sql);
    }

    /**
     * 保存Token
     */
    public void saveToken(String tenantId, CloudTokenInfo tokenInfo) {
        String sql = """
            INSERT INTO cloud_token (tenant_id, access_token, refresh_token, token_type, 
                expires_in, expires_at, user_id, username, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                access_token = VALUES(access_token),
                refresh_token = VALUES(refresh_token),
                token_type = VALUES(token_type),
                expires_in = VALUES(expires_in),
                expires_at = VALUES(expires_at),
                user_id = VALUES(user_id),
                username = VALUES(username),
                update_time = VALUES(update_time)
            """;

        LocalDateTime expiresAt = tokenInfo.getExpiresAt();
        if (expiresAt == null && tokenInfo.getExpiresIn() > 0) {
            expiresAt = LocalDateTime.now().plusSeconds(tokenInfo.getExpiresIn());
        }

        jdbcTemplate.update(sql,
                tenantId,
                tokenInfo.getAccessToken(),
                tokenInfo.getRefreshToken(),
                tokenInfo.getTokenType(),
                tokenInfo.getExpiresIn(),
                expiresAt,
                tokenInfo.getUserId(),
                tokenInfo.getUsername(),
                LocalDateTime.now()
        );
        log.debug("Token已保存，租户: {}", tenantId);
    }

    /**
     * 获取Token
     */
    public Optional<CloudTokenInfo> getToken(String tenantId) {
        String sql = "SELECT * FROM cloud_token WHERE tenant_id = ?";
        try {
            CloudTokenInfo tokenInfo = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                return CloudTokenInfo.builder()
                        .accessToken(rs.getString("access_token"))
                        .refreshToken(rs.getString("refresh_token"))
                        .tokenType(rs.getString("token_type"))
                        .expiresIn(rs.getLong("expires_in"))
                        .expiresAt(rs.getTimestamp("expires_at") != null ?
                                rs.getTimestamp("expires_at").toLocalDateTime() : null)
                        .userId(rs.getObject("user_id", Long.class))
                        .username(rs.getString("username"))
                        .tenantId(rs.getString("tenant_id"))
                        .createTime(rs.getTimestamp("create_time") != null ?
                                rs.getTimestamp("create_time").toLocalDateTime() : null)
                        .build();
            }, tenantId);
            return Optional.ofNullable(tokenInfo);
        } catch (Exception e) {
            log.warn("获取Token失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 删除Token
     */
    public void deleteToken(String tenantId) {
        String sql = "DELETE FROM cloud_token WHERE tenant_id = ?";
        jdbcTemplate.update(sql, tenantId);
        log.debug("Token已删除，租户: {}", tenantId);
    }

    /**
     * 检查Token是否存在且有效
     */
    public boolean hasValidToken(String tenantId) {
        return getToken(tenantId)
                .map(CloudTokenInfo::isValid)
                .orElse(false);
    }

    /**
     * 更新Token（仅更新access_token和过期时间）
     */
    public void updateAccessToken(String tenantId, String accessToken, long expiresIn) {
        String sql = """
            UPDATE cloud_token 
            SET access_token = ?, expires_at = ?, update_time = ?
            WHERE tenant_id = ?
            """;
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
        jdbcTemplate.update(sql, accessToken, expiresAt, LocalDateTime.now(), tenantId);
    }
}
