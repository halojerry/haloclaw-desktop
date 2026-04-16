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
 * 上架技能
 * 将产品上架到Ozon平台
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductPublishSkill implements SkillExecutor {

    @Value("${ozonclaw.api.base-url:http://localhost:18088}")
    private String cloudApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getSkillType() {
        return "ozon_product_publish";
    }

    @Override
    public String getSkillName() {
        return "Ozon产品上架";
    }

    @Override
    public String getDescription() {
        return "将产品信息提交到Ozon平台进行上架";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<Map<String, Object>> getParamRules() {
        return Arrays.asList(
                Map.of("name", "name", "type", "string", "required", true, "description", "产品名称"),
                Map.of("name", "sku", "type", "string", "required", true, "description", "SKU"),
                Map.of("name", "categoryId", "type", "number", "required", true, "description", "Ozon类目ID"),
                Map.of("name", "price", "type", "number", "required", true, "description", "售价(RUB)"),
                Map.of("name", "images", "type", "array", "required", true, "description", "图片URL列表"),
                Map.of("name", "attributes", "type", "object", "required", true, "description", "产品属性"),
                Map.of("name", "storeId", "type", "string", "required", false, "description", "店铺ID")
        );
    }

    @Override
    public boolean validateParams(SkillContext context) {
        String name = context.getStringParam("name");
        String sku = context.getStringParam("sku");
        Long categoryId = context.getLongParam("categoryId");
        Double price = context.getNumberParam("price");

        return name != null && !name.isEmpty() &&
               sku != null && !sku.isEmpty() &&
               categoryId != null && categoryId > 0 &&
               price != null && price > 0;
    }

    @Override
    public SkillResult execute(SkillContext context) {
        String requestId = context.getRequestId();
        long startTime = System.currentTimeMillis();

        log.info("开始产品上架: requestId={}, sku={}", requestId, context.getStringParam("sku"));

        try {
            // 调用云端MCP上架工具
            SkillResult cloudResult = callCloudMcp(context);
            if (cloudResult != null && cloudResult.getStatus() == SkillResult.Status.SUCCESS) {
                return cloudResult;
            }

            // 云端调用失败
            log.warn("云端MCP调用失败，无法完成产品上架");
            return SkillResult.failed(getSkillType(), getSkillName(), "产品上架需要连接云端服务")
                    .setRequestId(requestId);

        } catch (Exception e) {
            log.error("产品上架失败", e);
            return SkillResult.failed(getSkillType(), getSkillName(), "产品上架失败: " + e.getMessage())
                    .setRequestId(requestId);
        }
    }

    /**
     * 调用云端MCP上架工具
     */
    private SkillResult callCloudMcp(SkillContext context) {
        try {
            String url = cloudApiUrl + "/mcp/tools/ozon_listing";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (context.getToken() != null) {
                headers.setBearerAuth(context.getToken());
            }

            Map<String, Object> body = new HashMap<>();
            body.put("name", context.getStringParam("name"));
            body.put("sku", context.getStringParam("sku"));
            body.put("categoryId", context.getLongParam("categoryId"));
            body.put("price", context.getNumberParam("price"));
            body.put("images", context.getParam("images"));
            body.put("attributes", context.getParam("attributes"));
            body.put("storeId", context.getStringParam("storeId"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Map<String, Object> data = new HashMap<>();
                data.put("productId", response.get("productId"));
                data.put("status", "published");
                return SkillResult.success(getSkillType(), getSkillName(), data)
                        .setRequestId(context.getRequestId());
            }
            return null;
        } catch (Exception e) {
            log.warn("云端MCP调用异常: {}", e.getMessage());
            return null;
        }
    }
}
