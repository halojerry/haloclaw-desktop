package vip.mate.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 云端认证响应
 *
 * @author MateClaw Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 令牌类型 */
    private String tokenType;

    /** 过期时间（秒） */
    private long expiresIn;

    /** 用户信息 */
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String avatar;
        private String role;
    }

    /** 转换为TokenInfo */
    public CloudTokenInfo toTokenInfo() {
        return CloudTokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType != null ? tokenType : "Bearer")
                .expiresIn(expiresIn)
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : null)
                .createTime(java.time.LocalDateTime.now())
                .build();
    }
}
