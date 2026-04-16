package vip.mate.device;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 设备服务
 * 
 * 处理设备注册、认证、心跳和管理功能
 * 
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;

    /** 设备数量限制（默认5台） */
    @Value("${mateclaw.device.limit:5}")
    private int deviceLimit;

    /** 心跳超时时间（默认30分钟） */
    @Value("${mateclaw.device.heartbeat-timeout:1800}")
    private int heartbeatTimeout;

    /** JWT密钥 */
    @Value("${mateclaw.jwt.secret:MateClaw-Secret-Key-2024-Very-Long-String}")
    private String jwtSecret;

    /**
     * 设备注册
     */
    @Transactional
    public DeviceAuthResponse registerDevice(DeviceRegisterRequest request, Long userId, String username, 
                                               HttpServletRequest httpRequest) {
        DeviceAuthResponse response = new DeviceAuthResponse();
        
        // 检查设备是否已存在
        Device existingDevice = deviceMapper.findByDeviceId(request.getDeviceId());
        
        if (existingDevice != null) {
            // 设备已注册，检查是否属于当前用户
            if (!existingDevice.getUserId().equals(userId)) {
                response.setSuccess(false);
                response.setMessage("该设备已被其他用户绑定");
                response.setServerTime(System.currentTimeMillis());
                return response;
            }
            
            // 更新设备信息
            existingDevice.setDeviceName(request.getDeviceName());
            existingDevice.setDeviceInfo(request.getDeviceInfo());
            existingDevice.setLastHeartbeat(LocalDateTime.now());
            existingDevice.setIpAddress(getClientIp(httpRequest));
            existingDevice.setStatus("online");
            existingDevice.setIsCurrent(true);
            deviceMapper.updateById(existingDevice);
            
            // 清除其他设备的当前标记
            deviceMapper.clearCurrentDevice(userId);
            existingDevice.setIsCurrent(true);
            deviceMapper.updateById(existingDevice);
            
            log.info("设备已重新连接: deviceId={}, userId={}", request.getDeviceId(), userId);
        } else {
            // 检查用户设备数量是否超限
            int currentCount = deviceMapper.countByUserId(userId);
            if (currentCount >= deviceLimit) {
                response.setSuccess(false);
                response.setMessage("设备数量已达到上限(" + deviceLimit + "台)，请先解绑其他设备");
                response.setDeviceLimit(deviceLimit);
                response.setCurrentDeviceCount(currentCount);
                response.setServerTime(System.currentTimeMillis());
                return response;
            }
            
            // 创建新设备
            Device device = new Device();
            device.setDeviceId(request.getDeviceId());
            device.setDeviceName(request.getDeviceName());
            device.setDeviceInfo(request.getDeviceInfo());
            device.setDeviceType(request.getDeviceType() != null ? request.getDeviceType() : "desktop");
            device.setUserId(userId);
            device.setUsername(username);
            device.setStatus("online");
            device.setLastHeartbeat(LocalDateTime.now());
            device.setRegisteredAt(LocalDateTime.now());
            device.setIsCurrent(true);
            device.setIpAddress(getClientIp(httpRequest));
            device.setAppVersion(request.getDeviceInfo());
            
            deviceMapper.insert(device);
            
            // 清除其他设备的当前标记
            deviceMapper.clearCurrentDevice(userId);
            
            log.info("新设备注册: deviceId={}, userId={}", request.getDeviceId(), userId);
        }
        
        // 生成会话Token
        String sessionToken = UUID.randomUUID().toString();
        
        // 构建响应
        List<Device> userDevices = deviceMapper.findByUserId(userId);
        response.setSuccess(true);
        response.setMessage("设备注册成功");
        response.setSessionToken(sessionToken);
        response.setDeviceId(request.getDeviceId());
        response.setIsNewDevice(existingDevice == null);
        response.setBoundDevices(userDevices.stream().map(DeviceVO::fromEntity).collect(Collectors.toList()));
        response.setDeviceLimit(deviceLimit);
        response.setCurrentDeviceCount(userDevices.size());
        response.setServerTime(System.currentTimeMillis());
        
        return response;
    }

    /**
     * 设备认证
     */
    public DeviceAuthResponse authenticateDevice(String deviceId, Long userId, String username) {
        DeviceAuthResponse response = new DeviceAuthResponse();
        
        Device device = deviceMapper.findByDeviceIdAndUserId(deviceId, userId);
        
        if (device == null) {
            // 设备未注册，需要先注册
            response.setSuccess(false);
            response.setMessage("设备未注册，请先注册设备");
            response.setIsNewDevice(true);
            response.setServerTime(System.currentTimeMillis());
            return response;
        }
        
        if ("banned".equals(device.getStatus())) {
            response.setSuccess(false);
            response.setMessage("设备已被禁用");
            response.setServerTime(System.currentTimeMillis());
            return response;
        }
        
        // 更新心跳时间
        deviceMapper.updateHeartbeat(deviceId, LocalDateTime.now());
        
        // 生成会话Token
        String sessionToken = UUID.randomUUID().toString();
        
        // 获取用户所有设备
        List<Device> userDevices = deviceMapper.findByUserId(userId);
        
        response.setSuccess(true);
        response.setMessage("设备认证成功");
        response.setSessionToken(sessionToken);
        response.setDeviceId(deviceId);
        response.setIsNewDevice(false);
        response.setBoundDevices(userDevices.stream().map(DeviceVO::fromEntity).collect(Collectors.toList()));
        response.setDeviceLimit(deviceLimit);
        response.setCurrentDeviceCount(userDevices.size());
        response.setServerTime(System.currentTimeMillis());
        
        return response;
    }

    /**
     * 设备心跳
     */
    public DeviceAuthResponse heartbeat(String deviceId, Long userId) {
        DeviceAuthResponse response = new DeviceAuthResponse();
        
        Device device = deviceMapper.findByDeviceIdAndUserId(deviceId, userId);
        
        if (device == null) {
            response.setSuccess(false);
            response.setMessage("设备未注册");
            response.setServerTime(System.currentTimeMillis());
            return response;
        }
        
        if ("banned".equals(device.getStatus())) {
            response.setSuccess(false);
            response.setMessage("设备已被禁用");
            response.setServerTime(System.currentTimeMillis());
            return response;
        }
        
        // 更新心跳时间
        device.setLastHeartbeat(LocalDateTime.now());
        device.setStatus("online");
        deviceMapper.updateById(device);
        
        response.setSuccess(true);
        response.setServerTime(System.currentTimeMillis());
        response.setNextHeartbeat(System.currentTimeMillis() + 300000L); // 5分钟后
        response.setDeviceId(deviceId);
        
        return response;
    }

    /**
     * 获取用户设备列表
     */
    public List<DeviceVO> getUserDevices(Long userId) {
        List<Device> devices = deviceMapper.findByUserId(userId);
        return devices.stream().map(DeviceVO::fromEntity).collect(Collectors.toList());
    }

    /**
     * 解绑设备
     */
    @Transactional
    public boolean unbindDevice(String deviceId, Long userId) {
        Device device = deviceMapper.findByDeviceIdAndUserId(deviceId, userId);
        
        if (device == null) {
            return false;
        }
        
        // 不允许删除，标记为已解绑状态
        device.setStatus("unbound");
        device.setIsCurrent(false);
        deviceMapper.updateById(device);
        
        log.info("设备已解绑: deviceId={}, userId={}", deviceId, userId);
        return true;
    }

    /**
     * 解绑设备（管理员）
     */
    @Transactional
    public boolean adminUnbindDevice(String deviceId, Long targetUserId) {
        Device device = deviceMapper.findByDeviceIdAndUserId(deviceId, targetUserId);
        
        if (device == null) {
            return false;
        }
        
        device.setStatus("unbound");
        device.setIsCurrent(false);
        deviceMapper.updateById(device);
        
        log.info("管理员解绑设备: deviceId={}, targetUserId={}", deviceId, targetUserId);
        return true;
    }

    /**
     * 获取设备信息
     */
    public DeviceVO getDeviceInfo(String deviceId) {
        Device device = deviceMapper.findByDeviceId(deviceId);
        return device != null ? DeviceVO.fromEntity(device) : null;
    }

    /**
     * 标记设备为离线（定时任务调用）
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    @Transactional
    public void markOfflineDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(heartbeatTimeout / 60);
        List<Device> offlineDevices = deviceMapper.findOfflineDevices(threshold);
        
        for (Device device : offlineDevices) {
            if (!"offline".equals(device.getStatus())) {
                device.setStatus("offline");
                deviceMapper.updateById(device);
                log.debug("设备已标记为离线: deviceId={}", device.getDeviceId());
            }
        }
        
        if (!offlineDevices.isEmpty()) {
            log.info("本次标记 {} 台设备为离线", offlineDevices.size());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
