package vip.mate.tool.builtin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 内置工具：网页搜索
 * 通过 WebSearchService 动态读取系统设置，支持 Serper / Tavily 双 provider 与 fallback
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSearchTool {

    private final WebSearchService webSearchService;

    @Tool(description = "在互联网上搜索最新信息。当需要查询实时新闻、最新数据或不确定的事实时使用此工具。")
    public String search(String query) {
        return webSearchService.search(query);
    }
}
