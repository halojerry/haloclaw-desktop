package vip.mate.skill.cloud;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能执行器接口
 * 所有业务技能需实现此接口
 *
 * @author MateClaw Team
 */
public interface SkillExecutor {

    /**
     * 获取技能类型
     */
    String getSkillType();

    /**
     * 获取技能名称
     */
    String getSkillName();

    /**
     * 获取技能描述
     */
    default String getDescription() {
        return "";
    }

    /**
     * 获取技能版本
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 获取参数规则
     */
    default List<Map<String, Object>> getParamRules() {
        return List.of();
    }

    /**
     * 验证参数
     */
    default boolean validateParams(SkillContext context) {
        return true;
    }

    /**
     * 执行技能
     */
    SkillResult execute(SkillContext context);

    /**
     * 技能注册表
     */
    class Registry {
        private static final Map<String, SkillExecutor> executors = new ConcurrentHashMap<>();

        public static void register(SkillExecutor executor) {
            executors.put(executor.getSkillType(), executor);
        }

        public static SkillExecutor get(String skillType) {
            return executors.get(skillType);
        }

        public static Map<String, SkillExecutor> getAll() {
            return new ConcurrentHashMap<>(executors);
        }

        public static void unregister(String skillType) {
            executors.remove(skillType);
        }

        public static void clear() {
            executors.clear();
        }
    }
}
