package vip.mate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 会话历史上下文窗口管理配置
 *
 * @author MateClaw Team
 */
@Data
@ConfigurationProperties(prefix = "mate.agent.conversation.window")
public class ConversationWindowProperties {

    /** 全局默认最大输入 token（上下文窗口） */
    private int defaultMaxInputTokens = 128000;

    /** 历史 token 占比达此阈值触发压缩（0-1） */
    private double compactTriggerRatio = 0.75;

    /** 压缩后保留最近 N 轮对话（user+assistant 算一轮） */
    private int preserveRecentPairs = 5;

    /** 摘要自身最大 token 数 */
    private int summaryMaxTokens = 800;
}
