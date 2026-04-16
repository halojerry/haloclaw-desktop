package vip.mate.skill.cloud.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vip.mate.skill.cloud.*;

import java.util.*;

/**
 * 选品技能
 * 分析产品数据，筛选符合标准的产品
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSelectionSkill implements SkillExecutor {

    @Value("${ozonclaw.api.base-url:http://localhost:18088}")
    private String cloudApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getSkillType() {
        return "ozon_product_selection";
    }

    @Override
    public String getSkillName() {
        return "Ozon选品分析";
    }

    @Override
    public String getDescription() {
        return "分析产品数据，筛选符合利润率和销量标准的产品";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<Map<String, Object>> getParamRules() {
        return Arrays.asList(
                Map.of("name", "minMargin", "type", "number", "required", false, "description", "最低利润率(默认0.25)"),
                Map.of("name", "maxWeight", "type", "number", "required", false, "description", "最大重量kg(默认3)"),
                Map.of("name", "minSales", "type", "number", "required", false, "description", "最低月销量(默认100)"),
                Map.of("name", "excludeCategories", "type", "array", "required", false, "description", "排除类目")
        );
    }

    @Override
    public boolean validateParams(SkillContext context) {
        return context.getParams() != null && !context.getParams().isEmpty();
    }

    @Override
    public SkillResult execute(SkillContext context) {
        String requestId = context.getRequestId();
        long startTime = System.currentTimeMillis();

        log.info("开始选品分析: requestId={}", requestId);

        try {
            // 尝试调用云端MCP
            SkillResult cloudResult = callCloudMcp(context);
            if (cloudResult != null && cloudResult.getStatus() == SkillResult.Status.SUCCESS) {
                return cloudResult;
            }

            // 云端调用失败，返回提示
            log.warn("云端MCP调用失败，无法完成选品分析");
            return SkillResult.failed(getSkillType(), getSkillName(), "选品分析需要连接云端服务")
                    .setRequestId(requestId);

        } catch (Exception e) {
            log.error("选品分析失败", e);
            return SkillResult.failed(getSkillType(), getSkillName(), "选品分析失败: " + e.getMessage())
                    .setRequestId(requestId);
        }
    }

    /**
     * 调用云端MCP选品工具
     */
    private SkillResult callCloudMcp(SkillContext context) {
        try {
            String url = cloudApiUrl + "/mcp/tools/ozon_product_selection";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (context.getToken() != null) {
                headers.setBearerAuth(context.getToken());
            }

            Map<String, Object> body = new HashMap<>(context.getParams());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                return SkillResult.success(getSkillType(), getSkillName(), response.get("data"))
                        .setRequestId(context.getRequestId());
            }
            return null;
        } catch (Exception e) {
            log.warn("云端MCP调用异常: {}", e.getMessage());
            return null;
        }
    }
}
