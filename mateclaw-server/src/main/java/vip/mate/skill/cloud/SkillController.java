package vip.mate.skill.cloud;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vip.mate.common.result.R;

import java.util.List;
import java.util.Map;

/**
 * 技能执行控制器
 * 提供技能执行、列表查询、历史记录等接口
 *
 * @author MateClaw Team
 */
@Slf4j
@RestController
@RequestMapping("/api/skill")
@RequiredArgsConstructor
@Tag(name = "技能执行", description = "云端技能执行接口")
public class SkillController {

    private final SkillExecutionService skillExecutionService;

    /**
     * 执行技能
     */
    @Operation(summary = "执行技能", description = "执行指定的业务技能，返回执行结果")
    @PostMapping("/execute")
    public R<SkillResult> execute(@RequestBody SkillExecuteRequest request) {
        log.info("收到技能执行请求: skillType={}, userId={}",
                request.getSkillType(), request.getContext().getUserId());

        // 构建执行上下文
        SkillContext context = SkillContext.builder()
                .token(request.getToken())
                .userId(request.getContext().getUserId())
                .username(request.getContext().getUsername())
                .storeId(request.getContext().getStoreId())
                .storeName(request.getContext().getStoreName())
                .storePlatform(request.getContext().getStorePlatform())
                .skillType(request.getSkillType())
                .skillName(request.getSkillName())
                .params(request.getParams())
                .newApiKey(request.getContext().getNewApiKey())
                .language(request.getContext().getLanguage())
                .build();

        // 执行技能
        SkillResult result = skillExecutionService.execute(context);

        return R.ok(result);
    }

    /**
     * 获取可用技能列表
     */
    @Operation(summary = "获取可用技能列表", description = "获取所有已注册的技能信息")
    @GetMapping("/list")
    public R<List<SkillInfo>> listSkills() {
        List<SkillInfo> skills = skillExecutionService.getAvailableSkills();
        return R.ok(skills);
    }

    /**
     * 获取技能详情
     */
    @Operation(summary = "获取技能详情", description = "获取指定技能的详细信息")
    @GetMapping("/detail/{skillType}")
    public R<SkillInfo> getSkillDetail(@PathVariable String skillType) {
        SkillInfo skill = skillExecutionService.getAvailableSkills().stream()
                .filter(s -> s.getSkillType().equals(skillType))
                .findFirst()
                .orElse(null);

        if (skill == null) {
            return R.fail("未找到技能: " + skillType);
        }

        return R.ok(skill);
    }

    /**
     * 获取执行历史
     */
    @Operation(summary = "获取执行历史", description = "获取技能执行历史记录")
    @GetMapping("/history")
    public R<List<SkillResult>> getHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "50") int limit) {

        List<SkillResult> history = skillExecutionService.getExecutionHistory(userId, limit);
        return R.ok(history);
    }

    /**
     * 获取运行中的任务
     */
    @Operation(summary = "获取运行中的任务", description = "获取当前正在执行的技能任务")
    @GetMapping("/running")
    public R<List<ExecutionTask>> getRunningTasks() {
        Map<String, ExecutionTask> taskMap = skillExecutionService.getRunningTasks();
        return R.ok(new java.util.ArrayList<>(taskMap.values()));
    }

    /**
     * 获取任务状态
     */
    @Operation(summary = "获取任务状态", description = "获取指定任务ID的执行状态")
    @GetMapping("/task/{taskId}")
    public R<ExecutionTask> getTaskStatus(@PathVariable String taskId) {
        Map<String, ExecutionTask> taskMap = skillExecutionService.getRunningTasks();
        ExecutionTask task = taskMap.get(taskId);

        if (task == null) {
            return R.fail("未找到任务: " + taskId);
        }

        return R.ok(task);
    }

    /**
     * 技能执行请求
     */
    @lombok.Data
    public static class SkillExecuteRequest {
        /** 技能类型 */
        private String skillType;

        /** 技能名称(可选) */
        private String skillName;

        /** 用户Token */
        private String token;

        /** 执行上下文 */
        private ExecuteContext context;

        /** 执行参数 */
        private Map<String, Object> params;
    }

    /**
     * 执行上下文
     */
    @lombok.Data
    public static class ExecuteContext {
        /** 用户ID */
        private Long userId;

        /** 用户名 */
        private String username;

        /** 店铺ID */
        private Long storeId;

        /** 店铺名称 */
        private String storeName;

        /** 店铺平台 */
        private String storePlatform;

        /** New API Key */
        private String newApiKey;

        /** 语言设置 */
        private String language;
    }
}
