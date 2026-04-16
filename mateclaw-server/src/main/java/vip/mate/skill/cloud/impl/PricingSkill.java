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
 * 定价技能
 * 计算Ozon产品最优售价
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PricingSkill implements SkillExecutor {

    @Value("${ozonclaw.api.base-url:http://localhost:18088}")
    private String cloudApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getSkillType() {
        return "ozon_pricing";
    }

    @Override
    public String getSkillName() {
        return "Ozon定价计算";
    }

    @Override
    public String getDescription() {
        return "根据成本、重量、汇率等参数计算Ozon最优售价";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<Map<String, Object>> getParamRules() {
        return Arrays.asList(
                Map.of("name", "costCny", "type", "number", "required", true, "description", "成本价(CNY)"),
                Map.of("name", "weightKg", "type", "number", "required", true, "description", "重量(kg)"),
                Map.of("name", "category", "type", "string", "required", false, "description", "类目"),
                Map.of("name", "targetMargin", "type", "number", "required", false, "description", "目标利润率(默认0.25)")
        );
    }

    @Override
    public boolean validateParams(SkillContext context) {
        Double costCny = context.getNumberParam("costCny");
        Double weightKg = context.getNumberParam("weightKg");
        return costCny != null && costCny > 0 && weightKg != null && weightKg > 0;
    }

    @Override
    public SkillResult execute(SkillContext context) {
        String requestId = context.getRequestId();
        long startTime = System.currentTimeMillis();

        log.info("开始定价计算: requestId={}", requestId);

        try {
            // 尝试调用云端MCP
            SkillResult cloudResult = callCloudMcp(context);
            if (cloudResult != null && cloudResult.getStatus() == SkillResult.Status.SUCCESS) {
                return cloudResult;
            }

            // 云端调用失败，使用本地计算
            log.warn("云端MCP调用失败，使用本地定价计算");
            return calculateLocally(context, startTime);

        } catch (Exception e) {
            log.error("定价计算失败", e);
            return SkillResult.failed(getSkillType(), getSkillName(), "定价计算失败: " + e.getMessage())
                    .setRequestId(requestId);
        }
    }

    /**
     * 调用云端MCP定价工具
     */
    private SkillResult callCloudMcp(SkillContext context) {
        try {
            String url = cloudApiUrl + "/mcp/tools/ozon_pricing";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (context.getToken() != null) {
                headers.setBearerAuth(context.getToken());
            }

            Map<String, Object> body = new HashMap<>();
            body.put("costCny", context.getNumberParam("costCny"));
            body.put("weightKg", context.getNumberParam("weightKg"));
            body.put("category", context.getStringParam("category"));
            body.put("targetMargin", context.getNumberParam("targetMargin"));

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

    /**
     * 本地定价计算（简化版）
     */
    private SkillResult calculateLocally(SkillContext context, long startTime) {
        double costCny = context.getNumberParam("costCny");
        double weightKg = context.getNumberParam("weightKg");
        double targetMargin = context.getNumberParam("targetMargin") != null ?
                context.getNumberParam("targetMargin") : 0.25;

        // 简化定价公式（实际应调用云端获取完整公式）
        double cnyToRubRate = 12.5; // 默认汇率
        double logisticsPerKg = 500; // 物流成本 RUB/kg
        double commissionRate = 0.15; // Ozon佣金率

        double costRub = costCny * cnyToRubRate;
        double logisticsCost = weightKg * logisticsPerKg;
        double basePrice = costRub + logisticsCost;
        double priceWithMargin = basePrice / (1 - targetMargin - commissionRate);

        Map<String, Object> data = new HashMap<>();
        data.put("costCny", costCny);
        data.put("costRub", Math.round(costRub * 100) / 100.0);
        data.put("logisticsCost", logisticsCost);
        data.put("finalPriceRub", Math.round(priceWithMargin * 100) / 100.0);
        data.put("margin", targetMargin);
        data.put("source", "local");

        return SkillResult.success(getSkillType(), getSkillName(), data)
                .setRequestId(context.getRequestId());
    }
}
