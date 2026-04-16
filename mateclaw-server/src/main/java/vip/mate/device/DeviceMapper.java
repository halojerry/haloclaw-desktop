package vip.mate.device;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备 Mapper
 * 
 * @author MateClaw Team
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    /**
     * 根据设备ID和用户ID查询设备
     */
    @Select("SELECT * FROM mate_device WHERE device_id = #{deviceId} AND user_id = #{userId} AND deleted = 0")
    Device findByDeviceIdAndUserId(@Param("deviceId") String deviceId, @Param("userId") Long userId);

    /**
     * 根据设备ID查询设备
     */
    @Select("SELECT * FROM mate_device WHERE device_id = #{deviceId} AND deleted = 0")
    Device findByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 查询用户的所有设备
     */
    @Select("SELECT * FROM mate_device WHERE user_id = #{userId} AND deleted = 0 ORDER BY last_heartbeat DESC")
    List<Device> findByUserId(@Param("userId") Long userId);

    /**
     * 查询用户设备数量
     */
    @Select("SELECT COUNT(*) FROM mate_device WHERE user_id = #{userId} AND deleted = 0")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 更新最后心跳时间
     */
    @Update("UPDATE mate_device SET last_heartbeat = #{lastHeartbeat} WHERE device_id = #{deviceId}")
    int updateHeartbeat(@Param("deviceId") String deviceId, @Param("lastHeartbeat") LocalDateTime lastHeartbeat);

    /**
     * 将设备标记为当前设备
     */
    @Update("UPDATE mate_device SET is_current = 0 WHERE user_id = #{userId}")
    int clearCurrentDevice(@Param("userId") Long userId);

    /**
     * 查询离线设备（超过30分钟未心跳）
     */
    @Select("SELECT * FROM mate_device WHERE last_heartbeat < #{threshold} AND deleted = 0")
    List<Device> findOfflineDevices(@Param("threshold") LocalDateTime threshold);
}
