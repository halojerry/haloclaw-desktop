package vip.mate.tool.builtin;

/**
 * 工具执行上下文 — 通过 ThreadLocal 向 @Tool 方法传递执行环境信息
 * <p>
 * 在 ToolExecutionExecutor.executeSingleTool() 中 set，在 finally 中 clear。
 * 视频生成等需要知道 conversationId 的工具从此处获取。
 *
 * @author MateClaw Team
 */
public final class ToolExecutionContext {

    private static final ThreadLocal<String> CONVERSATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    private ToolExecutionContext() {}

    public static void set(String conversationId, String username) {
        CONVERSATION_ID.set(conversationId);
        USERNAME.set(username);
    }

    public static String conversationId() {
        return CONVERSATION_ID.get();
    }

    public static String username() {
        return USERNAME.get();
    }

    public static void clear() {
        CONVERSATION_ID.remove();
        USERNAME.remove();
    }
}
