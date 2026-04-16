package vip.mate.skill.cloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能调度服务
 * 负责技能执行、注册、调度和历史管理
 *
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillExecutionService {

    private final List<SkillExecutor> skillExecutors;

    @Value("${mateclaw.skill.timeout:60000}")
    private long defaultTimeout;

    @Value("${mateclaw.skill.retry-count:3}")
    private int defaultRetryCount;

    /** 执行中的任务 */
    private final Map<String, ExecutionTask> runningTasks = new ConcurrentHashMap<>();

    /** 执行历史 */
    private final List<SkillResult> executionHistory = Collections.synchronizedList(new ArrayList<>());

    /** 历史记录最大条数 */
    private static final int MAX_HISTORY_SIZE = 1000;

    @PostConstruct
    public void init() {
        // 注册所有技能
        for (SkillExecutor executor : skillExecutors) {
            SkillExecutor.Registry.register(executor);
        }
        log.info("技能调度服务初始化完成，已注册 {} 个技能", SkillExecutor.Registry.getAll().size());
    }

    /**
     * 执行技能
     */
    public SkillResult execute(SkillContext context) {
        String taskId = UUID.randomUUID().toString();
        context.setRequestId(taskId);

        log.info("开始执行技能: taskId={}, skillType={}, skillName={}",
                taskId, context.getSkillType(), context.getSkillName());

        // 创建执行任务
        ExecutionTask task = ExecutionTask.builder()
                .taskId(taskId)
                .skillType(context.getSkillType())
                .skillName(context.getSkillName())
                .status(ExecutionTask.Status.RUNNING)
                .startTime(LocalDateTime.now())
                .context(context)
                .build();

        runningTasks.put(taskId, task);

        try {
            // 获取技能执行器
            SkillExecutor executor = getExecutor(context.getSkillType());

            if (executor == null) {
                throw new IllegalArgumentException("未找到技能执行器: " + context.getSkillType());
            }

            // 验证参数
            if (!executor.validateParams(context)) {
                throw new IllegalArgumentException("参数验证失败");
            }

            // 执行技能
            SkillResult result = executeWithRetry(executor, context, defaultRetryCount);

            // 更新任务状态
            task.setStatus(result.getStatus() == SkillResult.Status.SUCCESS ?
                    ExecutionTask.Status.SUCCESS : ExecutionTask.Status.FAILED);
            task.setResult(result);
            task.setEndTime(LocalDateTime.now());

            // 添加到历史记录
            addToHistory(result);

            return result;

        } catch (Exception e) {
            log.error("技能执行失败: taskId={}", taskId, e);

            task.setStatus(ExecutionTask.Status.FAILED);
            task.setEndTime(LocalDateTime.now());

            SkillResult result = SkillResult.failed(
                    context.getSkillType(),
                    context.getSkillName(),
                    e.getMessage()
            );
            result.setRequestId(taskId);
            task.setResult(result);

            addToHistory(result);
            return result;

        } finally {
            // 从运行中移除
            runningTasks.remove(taskId);
        }
    }

    /**
     * 带重试的执行
     */
    private SkillResult executeWithRetry(SkillExecutor executor, SkillContext context, int maxRetries) {
        Exception lastException = null;

        for (int i = 0; i <= maxRetries; i++) {
            try {
                return executor.execute(context);
            } catch (Exception e) {
                lastException = e;
                log.warn("技能执行异常，重试中: attempt={}, error={}", i + 1, e.getMessage());

                if (i < maxRetries) {
                    try {
                        Thread.sleep(1000L * (i + 1)); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new RuntimeException("技能执行失败，已重试 " + maxRetries + " 次", lastException);
    }

    /**
     * 获取技能执行器
     */
    public SkillExecutor getExecutor(String skillType) {
        return SkillExecutor.Registry.get(skillType);
    }

    /**
     * 获取所有可用技能
     */
    public List<SkillInfo> getAvailableSkills() {
        List<SkillInfo> skills = new ArrayList<>();
        for (SkillExecutor executor : SkillExecutor.Registry.getAll().values()) {
            skills.add(SkillInfo.builder()
                    .skillType(executor.getSkillType())
                    .skillName(executor.getSkillName())
                    .description(executor.getDescription())
                    .version(executor.getVersion())
                    .paramRules(executor.getParamRules())
                    .build());
        }
        return skills;
    }

    /**
     * 获取执行历史
     * @param userId 用户ID（可选，暂不使用）
     * @param limit 限制条数
     */
    public List<SkillResult> getExecutionHistory(Long userId, int limit) {
        if (limit <= 0 || limit > executionHistory.size()) {
            return new ArrayList<>(executionHistory);
        }
        // 返回最近的N条记录
        int start = Math.max(0, executionHistory.size() - limit);
        return new ArrayList<>(executionHistory.subList(start, executionHistory.size()));
    }

    /**
     * 获取执行历史（无参数版本）
     */
    public List<SkillResult> getExecutionHistory() {
        return new ArrayList<>(executionHistory);
    }

    /**
     * 清空执行历史
     */
    public void clearHistory() {
        executionHistory.clear();
    }

    /**
     * 添加到历史记录
     */
    private void addToHistory(SkillResult result) {
        executionHistory.add(result);
        if (executionHistory.size() > MAX_HISTORY_SIZE) {
            executionHistory.remove(0);
        }
    }

    /**
     * 获取运行中的任务
     */
    public Map<String, ExecutionTask> getRunningTasks() {
        return new HashMap<>(runningTasks);
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        ExecutionTask task = runningTasks.get(taskId);
        if (task != null) {
            task.setStatus(ExecutionTask.Status.CANCELLED);
            runningTasks.remove(taskId);
            return true;
        }
        return false;
    }
}
