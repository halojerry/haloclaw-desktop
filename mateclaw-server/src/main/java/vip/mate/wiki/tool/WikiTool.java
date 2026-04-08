package vip.mate.wiki.tool;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vip.mate.wiki.model.WikiKnowledgeBaseEntity;
import vip.mate.wiki.model.WikiPageEntity;
import vip.mate.wiki.service.WikiKnowledgeBaseService;
import vip.mate.wiki.service.WikiPageService;

import java.util.List;

/**
 * Wiki 知识库工具
 * <p>
 * 供 Agent 在对话中按需读取 Wiki 页面内容。
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiTool {

    private final WikiPageService pageService;
    private final WikiKnowledgeBaseService kbService;

    @Tool(description = """
            读取 Wiki 知识库中指定页面的完整内容。
            当系统提示词中的 Wiki 页面摘要不够详细时，使用此工具获取完整内容。
            返回 Markdown 格式的页面内容，包含 [[双向链接]]。
            """)
    public String wiki_read_page(
            @ToolParam(description = "当前 Agent 的 ID") Long agentId,
            @ToolParam(description = "知识库 ID") Long kbId,
            @ToolParam(description = "页面标识符 (slug)") String slug) {

        if (kbId == null || slug == null || slug.isBlank()) {
            return error("kbId and slug are required");
        }

        String accessError = checkAccess(agentId, kbId);
        if (accessError != null) return accessError;

        WikiPageEntity page = pageService.getBySlug(kbId, slug);
        if (page == null) {
            return error("Page not found: " + slug);
        }

        JSONObject result = JSONUtil.createObj()
                .set("title", page.getTitle())
                .set("slug", page.getSlug())
                .set("version", page.getVersion())
                .set("lastUpdatedBy", page.getLastUpdatedBy())
                .set("content", page.getContent());
        return result.toString();
    }

    @Tool(description = """
            列出 Wiki 知识库中的所有页面。
            返回页面列表，包含标题、slug 和摘要。
            """)
    public String wiki_list_pages(
            @ToolParam(description = "当前 Agent 的 ID") Long agentId,
            @ToolParam(description = "知识库 ID") Long kbId) {

        if (kbId == null) {
            return error("kbId is required");
        }

        String accessError = checkAccess(agentId, kbId);
        if (accessError != null) return accessError;

        List<WikiPageEntity> pages = pageService.listSummaries(kbId);
        JSONArray arr = new JSONArray();
        for (WikiPageEntity page : pages) {
            arr.add(JSONUtil.createObj()
                    .set("title", page.getTitle())
                    .set("slug", page.getSlug())
                    .set("summary", page.getSummary()));
        }

        return JSONUtil.createObj()
                .set("kbId", kbId)
                .set("pageCount", pages.size())
                .set("pages", arr)
                .toString();
    }

    @Tool(description = """
            在 Wiki 知识库中搜索页面。
            按关键词搜索页面标题和摘要，返回匹配的页面列表。
            """)
    public String wiki_search_pages(
            @ToolParam(description = "当前 Agent 的 ID") Long agentId,
            @ToolParam(description = "知识库 ID") Long kbId,
            @ToolParam(description = "搜索关键词") String query) {

        if (kbId == null || query == null || query.isBlank()) {
            return error("kbId and query are required");
        }

        String accessError = checkAccess(agentId, kbId);
        if (accessError != null) return accessError;

        String queryLower = query.toLowerCase();
        List<WikiPageEntity> pages = pageService.listSummaries(kbId);
        List<WikiPageEntity> matched = pages.stream()
                .filter(p -> (p.getTitle() != null && p.getTitle().toLowerCase().contains(queryLower))
                        || (p.getSummary() != null && p.getSummary().toLowerCase().contains(queryLower)))
                .toList();

        JSONArray arr = new JSONArray();
        for (WikiPageEntity page : matched) {
            arr.add(JSONUtil.createObj()
                    .set("title", page.getTitle())
                    .set("slug", page.getSlug())
                    .set("summary", page.getSummary()));
        }

        return JSONUtil.createObj()
                .set("kbId", kbId)
                .set("query", query)
                .set("matchCount", matched.size())
                .set("pages", arr)
                .toString();
    }

    /**
     * 校验 Agent 是否有权访问指定知识库
     */
    private String checkAccess(Long agentId, Long kbId) {
        WikiKnowledgeBaseEntity kb = kbService.getById(kbId);
        if (kb == null) {
            return error("Knowledge base not found: " + kbId);
        }
        // KB 绑定了 agent 时，必须提供匹配的 agentId
        if (kb.getAgentId() != null) {
            if (agentId == null) {
                return error("Access denied: agentId is required for this knowledge base");
            }
            if (!kb.getAgentId().equals(agentId)) {
                return error("Access denied: knowledge base is not associated with this agent");
            }
        }
        return null;
    }

    private String error(String message) {
        return JSONUtil.createObj().set("error", message).toString();
    }
}
