package vip.mate.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 云端Token信息封装
 *
 * @author MateClaw Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudTokenInfo {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 令牌类型 */
    private String tokenType;

    /** 过期时间（秒） */
    private long expiresIn;

    /** 过期时间点 */
    private LocalDateTime expiresAt;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 租户ID */
    private String tenantId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 是否有效 */
    public boolean isValid() {
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /** 是否即将过期（剩余时间小于5分钟） */
    public boolean isNearExpiry() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().plusMinutes(5).isAfter(expiresAt);
    }
}
