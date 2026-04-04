package vip.mate.common.result;

import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author MateClaw Team
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统内部错误"),
    PARAM_ERROR(400, "参数校验失败"),
    AGENT_NOT_FOUND(1001, "Agent不存在"),
    AGENT_BUSY(1002, "Agent正在执行任务，请稍后"),
    LLM_ERROR(2001, "大模型调用失败"),
    TOOL_NOT_FOUND(3001, "工具不存在"),
    CHANNEL_ERROR(4001, "渠道消息发送失败");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
