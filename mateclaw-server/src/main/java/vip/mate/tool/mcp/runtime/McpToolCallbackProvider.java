package vip.mate.tool.mcp.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * MCP 工具回调提供者
 * <p>
 * 将所有 active MCP clients 暴露的 tools 统一为 ToolCallbackProvider，
 * 供 ToolRegistry 收集并注入 AgentToolSet。
 * <p>
 * 每次调用 getToolCallbacks() 都会从 McpClientManager 获取最新的 active tools，
 * 因此新增/删除 MCP server 后无需重启即可生效。
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolCallbackProvider implements ToolCallbackProvider {

    private final McpClientManager mcpClientManager;

    @Override
    public ToolCallback[] getToolCallbacks() {
        try {
            var callbacks = mcpClientManager.getAllToolCallbacks();
            if (!callbacks.isEmpty()) {
                log.debug("McpToolCallbackProvider providing {} tools from {} active MCP servers",
                        callbacks.size(), mcpClientManager.getActiveCount());
            }
            return callbacks.toArray(new ToolCallback[0]);
        } catch (Exception e) {
            log.warn("Failed to collect MCP tool callbacks: {}", e.getMessage());
            return new ToolCallback[0];
        }
    }
}
