package vip.mate.wiki.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.wiki.WikiProperties;
import vip.mate.wiki.model.WikiKnowledgeBaseEntity;
import vip.mate.wiki.model.WikiPageEntity;

import java.util.List;

/**
 * Wiki 上下文服务
 * <p>
 * 为 Agent 对话构建 Wiki 知识库上下文，注入到系统提示词中。
 *
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiContextService {

    private final WikiKnowledgeBaseService kbService;
    private final WikiPageService pageService;
    private final WikiProperties properties;

    /**
     * 构建指定 Agent 关联的 Wiki 上下文
     *
     * @param agentId Agent ID
     * @return Wiki 上下文字符串，如果没有关联知识库或页面则返回空字符串
     */
    public String buildWikiContext(Long agentId) {
        if (!properties.isEnabled()) {
            return "";
        }

        List<WikiKnowledgeBaseEntity> kbs = kbService.listByAgentId(agentId);
        if (kbs.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## Wiki Knowledge Base\n\n");
        sb.append("You have access to structured wiki knowledge bases. ");
        sb.append("Use `wiki_read_page` tool to read full page content when needed.\n\n");

        int totalChars = 0;
        int maxChars = properties.getMaxContextChars();

        for (WikiKnowledgeBaseEntity kb : kbs) {
            List<WikiPageEntity> pages = pageService.listSummaries(kb.getId());
            if (pages.isEmpty()) continue;

            sb.append("### ").append(kb.getName());
            if (kb.getDescription() != null && !kb.getDescription().isBlank()) {
                sb.append(" — ").append(kb.getDescription());
            }
            sb.append("\n\n");
            sb.append("Knowledge Base ID: `").append(kb.getId()).append("`\n\n");
            sb.append("Available pages:\n");

            for (WikiPageEntity page : pages) {
                String line = "- **[[" + page.getTitle() + "]]** (`" + page.getSlug() + "`): "
                        + (page.getSummary() != null ? page.getSummary() : "No summary") + "\n";

                if (totalChars + line.length() > maxChars) {
                    sb.append("- ... and more pages (use `wiki_list_pages` to see all)\n");
                    break;
                }

                sb.append(line);
                totalChars += line.length();
            }

            sb.append("\n");
        }

        String result = sb.toString();
        if (result.contains("Available pages:")) {
            return result;
        }
        return "";
    }
}
