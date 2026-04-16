package vip.mate.skill.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.skill.runtime.SkillFrontmatterParser.SkillDependencies;
import vip.mate.tool.model.ToolEntity;
import vip.mate.tool.repository.ToolMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 技能依赖检查器
 * 检查 commands / env / tools / platforms 依赖是否满足
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillDependencyChecker {

    private final ToolMapper toolMapper;

    private static final String CURRENT_OS = detectOS();

    /**
     * 检查依赖
     */
    public DependencyCheckResult check(SkillDependencies dependencies, List<String> platforms, String skillName) {
        List<String> missing = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean allSatisfied = true;

        // 1. 平台检查
        if (platforms != null && !platforms.isEmpty()) {
            boolean platformMatch = platforms.stream()
                .anyMatch(p -> p.equalsIgnoreCase(CURRENT_OS));
            if (!platformMatch) {
                missing.add("platform:" + CURRENT_OS + " (requires: " + String.join(", ", platforms) + ")");
                allSatisfied = false;
            }
        }

        if (dependencies == null || dependencies.isEmpty()) {
            return DependencyCheckResult.builder()
                .skillName(skillName)
                .satisfied(allSatisfied)
                .missing(missing)
                .warnings(warnings)
                .summary(allSatisfied ? "All dependencies satisfied" : buildSummary(missing))
                .build();
        }

        // 2. 命令检查
        for (String cmd : dependencies.getCommands()) {
            if (!isCommandAvailable(cmd)) {
                missing.add("command:" + cmd);
                allSatisfied = false;
            }
        }

        // 3. 环境变量检查
        for (String envVar : dependencies.getEnv()) {
            String value = System.getenv(envVar);
            if (value == null || value.isBlank()) {
                missing.add("env:" + envVar);
                allSatisfied = false;
            }
        }

        // 4. 内部工具检查
        for (String toolName : dependencies.getTools()) {
            if (!isToolAvailable(toolName)) {
                missing.add("tool:" + toolName);
                allSatisfied = false;
            }
        }

        return DependencyCheckResult.builder()
            .skillName(skillName)
            .satisfied(allSatisfied)
            .missing(missing)
            .warnings(warnings)
            .summary(allSatisfied ? "All dependencies satisfied" : buildSummary(missing))
            .build();
    }

    // ==================== 检查方法 ====================

    private boolean isCommandAvailable(String command) {
        try {
            String checkCmd = isWindows() ? "where" : "which";
            ProcessBuilder pb = new ProcessBuilder(checkCmd, command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 快速消耗输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) { /* drain */ }
            }

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Command check failed for '{}': {}", command, e.getMessage());
            return false;
        }
    }

    private boolean isToolAvailable(String toolName) {
        try {
            // 先按 name 精确匹配
            Long count = toolMapper.selectCount(new LambdaQueryWrapper<ToolEntity>()
                .eq(ToolEntity::getName, toolName)
                .eq(ToolEntity::getEnabled, true));
            if (count > 0) return true;

            // 再按 beanName 匹配（兼容 Spring Bean 名称）
            count = toolMapper.selectCount(new LambdaQueryWrapper<ToolEntity>()
                .eq(ToolEntity::getBeanName, toolName)
                .eq(ToolEntity::getEnabled, true));
            return count > 0;
        } catch (Exception e) {
            log.debug("Tool check failed for '{}': {}", toolName, e.getMessage());
            return false;
        }
    }

    private static boolean isWindows() {
        return CURRENT_OS.equals("windows");
    }

    private static String detectOS() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac") || os.contains("darwin")) return "macos";
        if (os.contains("win")) return "windows";
        if (os.contains("linux")) return "linux";
        return os;
    }

    private String buildSummary(List<String> missing) {
        if (missing.isEmpty()) return "All dependencies satisfied";
        return "Missing: " + String.join(", ", missing);
    }

    // ==================== 结果模型 ====================

    @Data
    @Builder
    public static class DependencyCheckResult {
        private String skillName;
        private boolean satisfied;
        @Builder.Default
        private List<String> missing = new ArrayList<>();
        @Builder.Default
        private List<String> warnings = new ArrayList<>();
        private String summary;
    }
}
