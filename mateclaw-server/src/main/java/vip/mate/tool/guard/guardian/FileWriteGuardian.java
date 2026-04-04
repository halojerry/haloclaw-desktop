package vip.mate.tool.guard.guardian;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.mate.tool.guard.model.*;

import java.util.List;
import java.util.Set;

/**
 * 文件写入守卫
 * <p>
 * 标记写文件/编辑文件操作为 MEDIUM 风险。
 * 最终是否需要审批由 ToolPolicyResolver 决定。
 */
@Slf4j
@Component
public class FileWriteGuardian implements ToolGuardGuardian {

    private static final Set<String> FILE_WRITE_TOOL_NAMES = Set.of(
            "write_file", "edit_file"
    );

    @Override
    public boolean supports(ToolInvocationContext context) {
        return context.toolName() != null && FILE_WRITE_TOOL_NAMES.contains(context.toolName());
    }

    @Override
    public int priority() {
        return 150;
    }

    @Override
    public List<GuardFinding> evaluate(ToolInvocationContext context) {
        return List.of(new GuardFinding(
                "FILE_WRITE_OPERATION",
                GuardSeverity.MEDIUM,
                GuardCategory.COMMAND_INJECTION,
                "文件写入操作",
                "检测到文件写入/编辑操作，需要用户确认",
                "请确认文件内容和目标路径",
                context.toolName(),
                null,
                "file_write_tool_default",
                null
        ));
    }
}
