package vip.mate.wiki.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.wiki.model.WikiPageEntity;

/**
 * Wiki 页面 Mapper
 *
 * @author MateClaw Team
 */
@Mapper
public interface WikiPageMapper extends BaseMapper<WikiPageEntity> {
}
