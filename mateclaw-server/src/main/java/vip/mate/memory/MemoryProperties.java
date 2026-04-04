package vip.mate.memory;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 记忆自动更新配置
 *
 * @author MateClaw Team
 */
@Data
@ConfigurationProperties(prefix = "mate.memory")
public class MemoryProperties {

    /** 启用对话后自动记忆提取 */
    private boolean autoSummarizeEnabled = true;

    /** 触发记忆提取的最小消息数 */
    private int minMessagesForSummarize = 4;

    /** 触发记忆提取的最小用户消息长度 */
    private int minUserMessageLength = 10;

    /** 跳过 cron 触发的对话（避免递归写入） */
    private boolean skipCronConversations = true;

    /** 记忆摘要的最大输出 token 数 */
    private int summaryMaxTokens = 1000;

    /** 启用定期记忆整合（daily notes → MEMORY.md） */
    private boolean emergenceEnabled = true;

    /** 记忆整合扫描的天数范围 */
    private int emergenceDayRange = 7;

    /** 同一 Agent 记忆提取的冷却时间（分钟） */
    private int cooldownMinutes = 5;

    /** 构建对话 transcript 时的最大消息数（防止过长） */
    private int maxTranscriptMessages = 30;
}
