package vip.mate.device;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vip.mate.common.result.R;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 设备管理接口
 * 
 * 处理设备注册、认证、心跳和管理功能
 * 
 * @author MateClaw Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "客户端设备标识管理")
public class DeviceController {

    private final DeviceService deviceService;

    /** 客户端版本信息 */
    private static final String CLIENT_VERSION = "1.0.418";

    /**
     * 注册设备
     * 
     * 首次启动或新设备登录时调用
     */
    @PostMapping("/register")
    @Operation(summary = "注册设备", description = "注册或更新客户端设备标识")
    public R<DeviceAuthResponse> registerDevice(
            @RequestBody DeviceRegisterRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            HttpServletRequest httpRequest) {
        
        // 如果没有传递用户信息，使用默认值（适用于未登录状态）
        if (userId == null) {
            userId = 0L;
            username = "anonymous";
        }
        
        DeviceAuthResponse response = deviceService.registerDevice(request, userId, username, httpRequest);
        
        if (response.getSuccess()) {
            return R.ok(response);
        } else {
            return R.fail(response.getMessage());
        }
    }

    /**
     * 设备认证
     * 
     * 已注册设备登录时调用
     */
    @PostMapping("/authenticate")
    @Operation(summary = "设备认证", description = "验证设备是否已绑定当前用户")
    public R<DeviceAuthResponse> authenticateDevice(
            @RequestParam String deviceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Username", required = false) String username) {
        
        if (userId == null || userId == 0L) {
            return R.fail("用户未登录");
        }
        
        DeviceAuthResponse response = deviceService.authenticateDevice(deviceId, userId, username);
        
        if (response.getSuccess()) {
            return R.ok(response);
        } else {
            return R.fail(response.getMessage());
        }
    }

    /**
     * 设备心跳
     * 
     * 每5分钟调用一次，保持设备在线状态
     */
    @PostMapping("/heartbeat")
    @Operation(summary = "设备心跳", description = "保持客户端连接活跃，标记设备在线状态")
    public R<DeviceAuthResponse> heartbeat(
            @RequestParam String deviceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        if (userId == null || userId == 0L) {
            return R.fail("用户未登录");
        }
        
        DeviceAuthResponse response = deviceService.heartbeat(deviceId, userId);
        
        if (response.getSuccess()) {
            return R.ok(response);
        } else {
            return R.fail(response.getMessage());
        }
    }

    /**
     * 获取设备列表
     * 
     * 获取当前用户绑定的所有设备
     */
    @GetMapping("/list")
    @Operation(summary = "获取设备列表", description = "获取当前用户绑定的所有设备")
    public R<List<DeviceVO>> getDeviceList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        if (userId == null || userId == 0L) {
            return R.fail("用户未登录");
        }
        
        List<DeviceVO> devices = deviceService.getUserDevices(userId);
        return R.ok(devices);
    }

    /**
     * 解绑设备
     * 
     * 用户主动解绑设备
     */
    @DeleteMapping("/unbind")
    @Operation(summary = "解绑设备", description = "解绑指定设备，解绑后需要重新注册")
    public R<Void> unbindDevice(
            @RequestParam String deviceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        if (userId == null || userId == 0L) {
            return R.fail("用户未登录");
        }
        
        boolean success = deviceService.unbindDevice(deviceId, userId);
        
        if (success) {
            return R.ok();
        } else {
            return R.fail("设备不存在或无权解绑");
        }
    }

    /**
     * 获取设备信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取设备信息", description = "获取指定设备的详细信息")
    public R<DeviceVO> getDeviceInfo(@RequestParam String deviceId) {
        DeviceVO device = deviceService.getDeviceInfo(deviceId);
        
        if (device != null) {
            return R.ok(device);
        } else {
            return R.fail("设备不存在");
        }
    }

    /**
     * 获取服务端版本信息
     */
    @GetMapping("/version")
    @Operation(summary = "获取版本信息", description = "获取客户端和服务端版本信息")
    public R<DeviceVersionInfo> getVersionInfo() {
        DeviceVersionInfo info = new DeviceVersionInfo();
        info.setClientVersion(CLIENT_VERSION);
        info.setServerVersion(CLIENT_VERSION);
        info.setServerTime(System.currentTimeMillis());
        info.setFeatures(new String[]{
            "device_registration",
            "device_authentication",
            "device_heartbeat",
            "multi_device_management"
        });
        return R.ok(info);
    }

    /**
     * 设备版本信息内部类
     */
    private static class DeviceVersionInfo {
        private String clientVersion;
        private String serverVersion;
        private Long serverTime;
        private String[] features;

        public String getClientVersion() { return clientVersion; }
        public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
        public String getServerVersion() { return serverVersion; }
        public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }
        public Long getServerTime() { return serverTime; }
        public void setServerTime(Long serverTime) { this.serverTime = serverTime; }
        public String[] getFeatures() { return features; }
        public void setFeatures(String[] features) { this.features = features; }
    }
}
