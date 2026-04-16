package vip.mate.controller.ozonclaw;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ozon-Claw 客户端设备管理接口
 * 
 * 处理设备ID注册、验证等操作
 * 
 * @author Ozon-Claw Team
 */
@RestController
@RequestMapping("/api/v1/ozonclaw/device")
@RequiredArgsConstructor
@Tag(name = "Ozon-Claw 设备管理", description = "客户端设备标识管理")
public class DeviceController {

    /**
     * 客户端版本信息（从配置读取）
     */
    private static final String CLIENT_VERSION = "1.0.0";

    @PostMapping("/register")
    @Operation(summary = "注册设备", description = "注册或更新客户端设备标识")
    public Map<String, Object> registerDevice(
            @RequestParam String deviceId,
            @RequestParam(required = false, defaultValue = "unknown") String deviceInfo,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 生成会话Token（简化实现）
        String sessionToken = UUID.randomUUID().toString();
        
        response.put("success", true);
        response.put("deviceId", deviceId);
        response.put("sessionToken", sessionToken);
        response.put("serverTime", System.currentTimeMillis());
        response.put("clientVersion", CLIENT_VERSION);
        
        return response;
    }

    @GetMapping("/info")
    @Operation(summary = "获取设备信息", description = "获取客户端和服务端版本信息")
    public Map<String, Object> getDeviceInfo(
            @RequestParam(required = false) String deviceId) {
        
        Map<String, Object> info = new HashMap<>();
        info.put("clientVersion", CLIENT_VERSION);
        info.put("serverVersion", "1.0.418");
        info.put("deviceId", deviceId);
        info.put("serverTime", System.currentTimeMillis());
        info.put("features", new String[]{
            "ozon_product_import",
            "ozon_category_tree", 
            "ozon_image_upload",
            "source_matching",
            "auto_pricing"
        });
        
        return info;
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "设备心跳", description = "保持客户端连接活跃")
    public Map<String, Object> heartbeat(@RequestParam String deviceId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("serverTime", System.currentTimeMillis());
        response.put("nextHeartbeat", System.currentTimeMillis() + 60000);
        return response;
    }
}
