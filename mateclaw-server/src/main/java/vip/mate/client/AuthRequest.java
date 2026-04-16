package vip.mate.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 云端认证请求
 *
 * @author MateClaw Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    /** 用户名/邮箱/手机号 */
    private String username;

    /** 密码 */
    private String password;

    /** 租户ID */
    private String tenantId;

    /** 登录类型 */
    private String loginType;

    /** 验证码（可选） */
    private String captcha;

    /** 验证码KEY（可选） */
    private String captchaKey;
}
