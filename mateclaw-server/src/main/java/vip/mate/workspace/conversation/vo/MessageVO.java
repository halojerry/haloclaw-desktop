package vip.mate.workspace.conversation.vo;

import lombok.Data;
import vip.mate.workspace.conversation.model.MessageContentPart;
import vip.mate.workspace.conversation.model.MessageEntity;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageVO {

    private Long id;

    private String conversationId;

    private String role;

    private String content;

    private String toolName;

    private String status;

    private String metadata;  // Agent 事件元数据（JSON）：toolCalls, plan, currentPhase 等

    /** Prompt tokens 消耗 */
    private Integer promptTokens;

    /** Completion tokens 消耗 */
    private Integer completionTokens;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<MessageContentPart> contentParts;

    public static MessageVO from(MessageEntity entity, List<MessageContentPart> contentParts, String renderedContent) {
        MessageVO vo = new MessageVO();
        vo.setId(entity.getId());
        vo.setConversationId(entity.getConversationId());
        vo.setRole(entity.getRole());
        vo.setContent(renderedContent);
        vo.setToolName(entity.getToolName());
        vo.setStatus(entity.getStatus());
        vo.setMetadata(entity.getMetadata());  // 包含元数据（toolCalls 等）
        vo.setPromptTokens(entity.getPromptTokens());
        vo.setCompletionTokens(entity.getCompletionTokens());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setContentParts(contentParts);
        return vo;
    }
}
