package vip.mate.cron.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.cron.model.CronJobEntity;

/**
 * 定时任务 Mapper
 *
 * @author MateClaw Team
 */
@Mapper
public interface CronJobMapper extends BaseMapper<CronJobEntity> {
}
