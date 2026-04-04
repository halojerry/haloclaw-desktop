package vip.mate.tool.mcp.runtime;

import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;

import java.io.File;

/**
 * 为 stdio MCP 子进程补充工作目录支持。
 * MCP SDK 1.0.0 的 ServerParameters 尚未暴露 cwd，这里通过覆写 ProcessBuilder 注入。
 */
public class CwdAwareStdioClientTransport extends StdioClientTransport {

    private final String cwd;

    public CwdAwareStdioClientTransport(ServerParameters params, McpJsonMapper jsonMapper, String cwd) {
        super(params, jsonMapper);
        this.cwd = cwd;
    }

    @Override
    protected ProcessBuilder getProcessBuilder() {
        ProcessBuilder builder = super.getProcessBuilder();
        if (cwd != null && !cwd.isBlank()) {
            builder.directory(new File(cwd));
        }
        return builder;
    }
}
