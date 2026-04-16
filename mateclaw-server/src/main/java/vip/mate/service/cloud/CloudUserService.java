package vip.mate.service.cloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.client.*;

import java.util.Map;

/**
 * 云端用户服务
 * 封装用户相关的业务逻辑
 *
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudUserService {

    private final CloudUserClient userClient;
    private final CloudAuthService authService;

    /**
     * 获取当前用户信息
     */
    public CloudUserClient.UserInfo getCurrentUser() {
        String token = authService.getValidToken();
        ApiResult<CloudUserClient.UserInfo> result = userClient.getCurrentUser(token);

        if (!result.isSuccess()) {
            log.error("获取用户信息失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "GET_USER_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 更新用户信息
     */
    public CloudUserClient.UserInfo updateUser(String nickname, String email, String avatar) {
        String token = authService.getValidToken();

        CloudUserClient.UserUpdateRequest request = new CloudUserClient.UserUpdateRequest();
        request.setNickname(nickname);
        request.setEmail(email);
        request.setAvatar(avatar);

        ApiResult<CloudUserClient.UserInfo> result = userClient.updateUser(token, request);

        if (!result.isSuccess()) {
            log.error("更新用户信息失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "UPDATE_USER_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 修改密码
     */
    public void changePassword(String oldPassword, String newPassword) {
        String token = authService.getValidToken();

        CloudUserClient.ChangePasswordRequest request = new CloudUserClient.ChangePasswordRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);

        ApiResult<Void> result = userClient.changePassword(token, request);

        if (!result.isSuccess()) {
            log.error("修改密码失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "CHANGE_PASSWORD_FAILED", result.getMessage());
        }

        log.info("密码修改成功");
    }

    /**
     * 获取用户设置
     */
    public Map<String, Object> getSettings() {
        String token = authService.getValidToken();
        ApiResult<Map<String, Object>> result = userClient.getSettings(token);

        if (!result.isSuccess()) {
            log.error("获取用户设置失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "GET_SETTINGS_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 更新用户设置
     */
    public void updateSettings(Map<String, Object> settings) {
        String token = authService.getValidToken();
        ApiResult<Void> result = userClient.updateSettings(token, settings);

        if (!result.isSuccess()) {
            log.error("更新用户设置失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "UPDATE_SETTINGS_FAILED", result.getMessage());
        }

        log.info("用户设置更新成功");
    }

    /**
     * 上传头像
     */
    public String uploadAvatar(byte[] imageData, String fileName) {
        String token = authService.getValidToken();
        ApiResult<String> result = userClient.uploadAvatar(token, imageData, fileName);

        if (!result.isSuccess()) {
            log.error("上传头像失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "UPLOAD_AVATAR_FAILED", result.getMessage());
        }

        return result.getData();
    }
}
