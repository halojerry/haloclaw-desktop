package vip.mate.channel.wecom;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import vip.mate.channel.AbstractChannelAdapter;
import vip.mate.channel.ChannelMessage;
import vip.mate.channel.ChannelMessageRouter;
import vip.mate.channel.ExponentialBackoff;
import vip.mate.channel.model.ChannelEntity;
import vip.mate.workspace.conversation.model.MessageContentPart;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业微信智能机器人渠道适配器 — WebSocket 长连接模式
 * <p>
 * 基于企业微信「智能机器人」API 长连接协议（wecom-aibot-python-sdk 逆向）：
 * <ul>
 *   <li>WebSocket 连接 wss://openws.work.weixin.qq.com</li>
 *   <li>bot_id + secret 认证（aibot_subscribe 帧）</li>
 *   <li>30 秒心跳（ping 帧）</li>
 *   <li>aibot_msg_callback / aibot_event_callback 消息推送</li>
 *   <li>reply_stream 流式回复（覆盖更新"思考中..."）</li>
 *   <li>send_message 主动推送</li>
 * </ul>
 * <p>
 * 用户在企业微信后台创建「智能机器人」→ 选择「API 模式 → 配置长连接」
 * → 获得 bot_id 和 secret → 填入 MateClaw → 启动即可对话。
 * 无需公网 IP，无需回调 URL。
 * <p>
 * 配置项（configJson）：
 * <ul>
 *   <li>bot_id: 机器人 ID</li>
 *   <li>secret: 机器人 Secret</li>
 *   <li>welcome_text: 欢迎消息（可选）</li>
 *   <li>media_download_enabled: 是否下载媒体文件（默认 false）</li>
 *   <li>media_dir: 媒体文件保存目录（默认 data/media）</li>
 * </ul>
 *
 * @author MateClaw Team
 */
@Slf4j
public class WeComChannelAdapter extends AbstractChannelAdapter {

    public static final String CHANNEL_TYPE = "wecom";

    /** 企业微信智能机器人 WebSocket 地址 */
    private static final String DEFAULT_WS_URL = "wss://openws.work.weixin.qq.com";

    /** 心跳间隔 30 秒 */
    private static final long HEARTBEAT_INTERVAL_MS = 30_000;

    /** 连续未收到 pong 的最大次数（超过则认为连接已死） */
    private static final int MAX_MISSED_PONG = 2;

    /** 回复 ACK 等待超时 5 秒 */
    private static final long REPLY_ACK_TIMEOUT_MS = 5_000;

    /** 消息去重：最大记录数 */
    private static final int PROCESSED_IDS_MAX = 2000;

    // ==================== WebSocket 命令常量 ====================

    private static final String CMD_SUBSCRIBE = "aibot_subscribe";
    private static final String CMD_HEARTBEAT = "ping";
    private static final String CMD_RESPONSE = "aibot_respond_msg";
    private static final String CMD_RESPONSE_WELCOME = "aibot_respond_welcome_msg";
    private static final String CMD_SEND_MSG = "aibot_send_msg";
    private static final String CMD_CALLBACK = "aibot_msg_callback";
    private static final String CMD_EVENT_CALLBACK = "aibot_event_callback";

    // ==================== 运行时状态 ====================

    private HttpClient httpClient;
    private volatile WebSocket webSocket;
    private volatile Thread wsThread;

    /** 心跳定时任务 */
    private volatile ScheduledFuture<?> heartbeatFuture;

    /** 连续未收到 pong 的计数 */
    private final AtomicInteger missedPongCount = new AtomicInteger(0);

    /** 消息去重集合 */
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();

    /** 回复 ACK 等待：reqId -> CompletableFuture */
    private final ConcurrentHashMap<String, CompletableFuture<Map<String, Object>>> pendingAcks = new ConcurrentHashMap<>();

    /** 回复队列：reqId -> 串行队列（保证同一 reqId 的回复按序发送） */
    private final ConcurrentHashMap<String, LinkedBlockingQueue<ReplyTask>> replyQueues = new ConcurrentHashMap<>();

    /** 回复队列处理线程池 */
    private final ExecutorService replyExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "wecom-reply");
        t.setDaemon(true);
        return t;
    });

    /** WebSocket 消息碎片缓冲区 */
    private final StringBuilder wsBuffer = new StringBuilder();

    /** 请求 ID 计数器 */
    private final AtomicInteger reqIdCounter = new AtomicInteger(0);

    /** 记录消息中 reqId -> frame 的映射，用于 reply_stream 回复 */
    private final ConcurrentHashMap<String, Map<String, Object>> pendingFrames = new ConcurrentHashMap<>();

    public WeComChannelAdapter(ChannelEntity channelEntity,
                               ChannelMessageRouter messageRouter,
                               ObjectMapper objectMapper) {
        super(channelEntity, messageRouter, objectMapper);
        int maxAttempts = -1;
        Object val = config.get("max_reconnect_attempts");
        if (val instanceof Number n) {
            maxAttempts = n.intValue();
        } else if (val instanceof String s) {
            try { maxAttempts = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        this.backoff = new ExponentialBackoff(2000, 30000, 2.0, maxAttempts);
    }

    // ==================== 生命周期 ====================

    @Override
    protected void doStart() {
        String botId = getConfigString("bot_id");
        String secret = getConfigString("secret");

        if (botId == null || botId.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalStateException("WeCom bot channel requires bot_id and secret in configJson");
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        connectWebSocket(botId, secret);

        log.info("[wecom] WeCom bot channel initialized: botId={}, maxReconnectAttempts={}",
                botId.length() > 12 ? botId.substring(0, 12) + "..." : botId, backoff.getMaxAttempts());
    }

    @Override
    protected void doStop() {
        // 停止心跳
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }

        // 关闭 WebSocket
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Manual disconnect")
                        .orTimeout(3, TimeUnit.SECONDS)
                        .exceptionally(ex -> null)
                        .join();
            } catch (Exception e) {
                log.debug("[wecom] Error closing WebSocket: {}", e.getMessage());
            }
            webSocket = null;
        }

        // 等待 WS 线程结束
        if (wsThread != null) {
            wsThread.interrupt();
            try {
                wsThread.join(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            wsThread = null;
        }

        // 清理挂起的 ACK
        pendingAcks.forEach((k, f) -> f.completeExceptionally(new RuntimeException("Channel stopped")));
        pendingAcks.clear();
        replyQueues.clear();
        pendingFrames.clear();
        processedMessageIds.clear();

        this.httpClient = null;
        log.info("[wecom] WeCom bot channel stopped");
    }

    @Override
    protected void doReconnect() {
        log.info("[wecom] Reconnecting WebSocket...");
        // 清理旧连接
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
        if (webSocket != null) {
            try { webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Reconnecting"); } catch (Exception ignored) {}
            webSocket = null;
        }
        if (wsThread != null) {
            wsThread.interrupt();
            try { wsThread.join(3000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            wsThread = null;
        }
        pendingAcks.forEach((k, f) -> f.completeExceptionally(new RuntimeException("Reconnecting")));
        pendingAcks.clear();
        replyQueues.clear();
        pendingFrames.clear();
        missedPongCount.set(0);

        if (this.httpClient == null) {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }

        String botId = getConfigString("bot_id");
        String secret = getConfigString("secret");
        connectWebSocket(botId, secret);
    }

    // ==================== WebSocket 连接 ====================

    /**
     * 在守护线程中建立 WebSocket 连接
     */
    private void connectWebSocket(String botId, String secret) {
        wsThread = new Thread(() -> {
            try {
                log.info("[wecom] WebSocket connecting to {}...", DEFAULT_WS_URL);

                CompletableFuture<WebSocket> wsFuture = httpClient.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .buildAsync(URI.create(DEFAULT_WS_URL), new WeComWebSocketListener());

                webSocket = wsFuture.get(20, TimeUnit.SECONDS);
                log.info("[wecom] WebSocket connected, sending auth...");

                // 发送认证帧
                sendAuth(botId, secret);

            } catch (Exception e) {
                log.error("[wecom] WebSocket connection failed: {}", e.getMessage(), e);
                if (running.get()) {
                    onDisconnected("WebSocket connection failed: " + e.getMessage());
                }
            }
        }, "wecom-ws-" + channelEntity.getId());
        wsThread.setDaemon(true);
        wsThread.start();
    }

    /**
     * WebSocket 监听器：接收消息帧并分发处理
     */
    private class WeComWebSocketListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            log.debug("[wecom] WebSocket onOpen");
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            wsBuffer.append(data);
            if (last) {
                String fullMessage = wsBuffer.toString();
                wsBuffer.setLength(0);
                handleWebSocketFrame(fullMessage);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            wsBuffer.append(new String(bytes));
            if (last) {
                String fullMessage = wsBuffer.toString();
                wsBuffer.setLength(0);
                handleWebSocketFrame(fullMessage);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("[wecom] WebSocket closed: code={}, reason={}", statusCode, reason);
            if (running.get()) {
                onDisconnected("WebSocket closed: code=" + statusCode + ", reason=" + reason);
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("[wecom] WebSocket error: {}", error.getMessage());
            if (running.get()) {
                onDisconnected("WebSocket error: " + error.getMessage());
            }
        }
    }

    // ==================== 帧处理 ====================

    /**
     * 处理收到的 WebSocket JSON 帧
     */
    @SuppressWarnings("unchecked")
    private void handleWebSocketFrame(String jsonStr) {
        try {
            Map<String, Object> frame = objectMapper.readValue(jsonStr, Map.class);
            String cmd = (String) frame.get("cmd");

            // 消息推送
            if (CMD_CALLBACK.equals(cmd)) {
                handleMessageCallback(frame);
                return;
            }

            // 事件推送
            if (CMD_EVENT_CALLBACK.equals(cmd)) {
                handleEventCallback(frame);
                return;
            }

            // 无 cmd 的帧：认证响应、心跳响应或回复 ACK
            Map<String, Object> headers = (Map<String, Object>) frame.getOrDefault("headers", Map.of());
            String reqId = (String) headers.getOrDefault("req_id", "");

            // 检查是否是回复消息的 ACK
            CompletableFuture<Map<String, Object>> ackFuture = pendingAcks.remove(reqId);
            if (ackFuture != null) {
                Integer errcode = frame.get("errcode") instanceof Number n ? n.intValue() : null;
                if (errcode != null && errcode != 0) {
                    ackFuture.completeExceptionally(new RuntimeException(
                            "Reply ACK error: errcode=" + errcode + ", errmsg=" + frame.get("errmsg")));
                } else {
                    ackFuture.complete(frame);
                }
                return;
            }

            // 认证响应
            if (reqId.startsWith(CMD_SUBSCRIBE)) {
                Integer errcode = frame.get("errcode") instanceof Number n ? n.intValue() : null;
                if (errcode != null && errcode != 0) {
                    log.error("[wecom] Authentication failed: errcode={}, errmsg={}", errcode, frame.get("errmsg"));
                    lastError = "Authentication failed: " + frame.get("errmsg");
                    return;
                }
                log.info("[wecom] Authentication successful");
                missedPongCount.set(0);
                startHeartbeat();
                return;
            }

            // 心跳响应
            if (reqId.startsWith(CMD_HEARTBEAT)) {
                Integer errcode = frame.get("errcode") instanceof Number n ? n.intValue() : null;
                if (errcode != null && errcode != 0) {
                    log.warn("[wecom] Heartbeat ACK error: errcode={}", errcode);
                    return;
                }
                missedPongCount.set(0);
                log.debug("[wecom] Heartbeat ACK received");
                return;
            }

            log.debug("[wecom] Received unknown frame: {}", jsonStr.length() > 200 ? jsonStr.substring(0, 200) : jsonStr);

        } catch (Exception e) {
            log.error("[wecom] Failed to handle WebSocket frame: {}", e.getMessage(), e);
        }
    }

    // ==================== 认证 & 心跳 ====================

    private void sendAuth(String botId, String secret) {
        String reqId = generateReqId(CMD_SUBSCRIBE);
        Map<String, Object> frame = Map.of(
                "cmd", CMD_SUBSCRIBE,
                "headers", Map.of("req_id", reqId),
                "body", Map.of("bot_id", botId, "secret", secret)
        );
        sendFrame(frame);
        log.info("[wecom] Auth frame sent");
    }

    private void startHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
        }
        heartbeatFuture = ensureReconnectScheduler().scheduleAtFixedRate(() -> {
            if (!running.get()) return;
            try {
                sendHeartbeat();
            } catch (Exception e) {
                log.warn("[wecom] Heartbeat send failed: {}", e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        log.debug("[wecom] Heartbeat started (interval={}ms)", HEARTBEAT_INTERVAL_MS);
    }

    private void sendHeartbeat() {
        if (missedPongCount.get() >= MAX_MISSED_PONG) {
            log.warn("[wecom] No heartbeat ACK for {} consecutive pings, connection considered dead",
                    missedPongCount.get());
            if (heartbeatFuture != null) {
                heartbeatFuture.cancel(false);
                heartbeatFuture = null;
            }
            if (running.get()) {
                onDisconnected("Heartbeat timeout: " + missedPongCount.get() + " missed pongs");
            }
            return;
        }

        missedPongCount.incrementAndGet();
        String reqId = generateReqId(CMD_HEARTBEAT);
        sendFrame(Map.of(
                "cmd", CMD_HEARTBEAT,
                "headers", Map.of("req_id", reqId)
        ));
        log.debug("[wecom] Heartbeat sent (missed={})", missedPongCount.get());
    }

    // ==================== 消息接收 ====================

    /**
     * 处理消息推送回调 (aibot_msg_callback)
     */
    @SuppressWarnings("unchecked")
    private void handleMessageCallback(Map<String, Object> frame) {
        try {
            Map<String, Object> body = (Map<String, Object>) frame.getOrDefault("body", Map.of());
            Map<String, Object> headers = (Map<String, Object>) frame.getOrDefault("headers", Map.of());
            String frameReqId = (String) headers.getOrDefault("req_id", "");

            String msgType = (String) body.getOrDefault("msgtype", "");
            Map<String, Object> fromMap = (Map<String, Object>) body.getOrDefault("from", Map.of());
            String senderId = (String) fromMap.getOrDefault("userid", "");
            String chatId = (String) body.getOrDefault("chatid", "");
            String chatType = (String) body.getOrDefault("chattype", "single");
            String msgId = (String) body.getOrDefault("msgid", "");

            // 补充 msgId（如果为空则用 senderId + send_time 合成）
            if (msgId.isBlank()) {
                msgId = senderId + "_" + body.getOrDefault("send_time", System.currentTimeMillis());
            }

            // 消息去重
            if (!msgId.isBlank() && !processedMessageIds.add(msgId)) {
                log.debug("[wecom] Duplicate msgId: {}, skipping", msgId);
                return;
            }
            // 去重集合超限清理
            if (processedMessageIds.size() > PROCESSED_IDS_MAX) {
                int toRemove = processedMessageIds.size() / 2;
                var it = processedMessageIds.iterator();
                while (it.hasNext() && toRemove > 0) { it.next(); it.remove(); toRemove--; }
            }

            // 保存 frame 用于 reply_stream
            pendingFrames.put(frameReqId, frame);

            List<MessageContentPart> contentParts = new ArrayList<>();
            String textContent = null;

            switch (msgType) {
                case "text" -> {
                    Map<String, Object> textBody = (Map<String, Object>) body.getOrDefault("text", Map.of());
                    textContent = ((String) textBody.getOrDefault("content", "")).trim();
                    if (!textContent.isBlank()) {
                        contentParts.add(MessageContentPart.text(textContent));
                    }
                }
                case "image" -> {
                    Map<String, Object> imgBody = (Map<String, Object>) body.getOrDefault("image", Map.of());
                    String url = (String) imgBody.getOrDefault("url", "");
                    String aesKey = (String) imgBody.getOrDefault("aeskey", "");
                    if (getConfigBoolean("media_download_enabled", false) && !url.isBlank()) {
                        String localPath = downloadAndDecryptMedia(url, aesKey, msgId, "image.jpg");
                        if (localPath != null) {
                            contentParts.add(MessageContentPart.image(localPath, url));
                        } else {
                            contentParts.add(MessageContentPart.image(url, url));
                        }
                    } else if (!url.isBlank()) {
                        contentParts.add(MessageContentPart.image(url, url));
                    }
                    textContent = "[图片]";
                }
                case "voice" -> {
                    Map<String, Object> voiceBody = (Map<String, Object>) body.getOrDefault("voice", Map.of());
                    String asrText = ((String) voiceBody.getOrDefault("content", "")).trim();
                    if (!asrText.isBlank()) {
                        contentParts.add(MessageContentPart.text(asrText));
                        textContent = asrText;
                    } else {
                        textContent = "[语音消息]";
                    }
                }
                case "file" -> {
                    Map<String, Object> fileBody = (Map<String, Object>) body.getOrDefault("file", Map.of());
                    String url = (String) fileBody.getOrDefault("url", "");
                    String aesKey = (String) fileBody.getOrDefault("aeskey", "");
                    String filename = (String) fileBody.getOrDefault("filename", "file.bin");
                    if (getConfigBoolean("media_download_enabled", false) && !url.isBlank()) {
                        String localPath = downloadAndDecryptMedia(url, aesKey, msgId, filename);
                        if (localPath != null) {
                            contentParts.add(MessageContentPart.file(localPath, filename, null));
                        }
                    }
                    textContent = "[文件: " + filename + "]";
                }
                case "mixed" -> {
                    Map<String, Object> mixedBody = (Map<String, Object>) body.getOrDefault("mixed", Map.of());
                    List<Map<String, Object>> items = (List<Map<String, Object>>) mixedBody.getOrDefault("msg_item", List.of());
                    StringBuilder textBuilder = new StringBuilder();
                    for (Map<String, Object> item : items) {
                        String itemType = (String) item.getOrDefault("msgtype", "");
                        if ("text".equals(itemType)) {
                            Map<String, Object> t = (Map<String, Object>) item.getOrDefault("text", Map.of());
                            String txt = ((String) t.getOrDefault("content", "")).trim();
                            if (!txt.isBlank()) {
                                textBuilder.append(txt).append('\n');
                            }
                        } else if ("image".equals(itemType)) {
                            Map<String, Object> img = (Map<String, Object>) item.getOrDefault("image", Map.of());
                            String url = (String) img.getOrDefault("url", "");
                            if (!url.isBlank()) {
                                contentParts.add(MessageContentPart.image(url, url));
                            }
                        }
                    }
                    textContent = textBuilder.toString().trim();
                    if (!textContent.isBlank()) {
                        contentParts.add(0, MessageContentPart.text(textContent));
                    }
                }
                default -> {
                    log.debug("[wecom] Ignoring unsupported message type: {}", msgType);
                    return;
                }
            }

            if (contentParts.isEmpty()) {
                if (textContent != null && !textContent.isBlank()) {
                    contentParts.add(MessageContentPart.text(textContent));
                } else {
                    return;
                }
            }

            // 发送"🤔 思考中..."处理指示器
            String processingStreamId = "";
            if (textContent != null && !textContent.isBlank()) {
                processingStreamId = generateReqId("stream");
                try {
                    replyStream(frameReqId, processingStreamId, "🤔 思考中...", false);
                } catch (Exception e) {
                    log.debug("[wecom] Failed to send processing indicator: {}", e.getMessage());
                }
            }

            boolean isGroup = "group".equals(chatType);
            String effectiveChatId = isGroup ? chatId : null;

            // conversationId 格式：wecom:{userid} 或 wecom:group:{chatid}
            // 由 ChannelMessageRouter.buildConversationId() 根据 channelType + chatId/senderId 构建

            ChannelMessage channelMessage = ChannelMessage.builder()
                    .messageId(msgId)
                    .channelType(CHANNEL_TYPE)
                    .senderId(senderId)
                    .senderName(senderId)
                    .chatId(effectiveChatId)
                    .content(textContent != null ? textContent.trim() : "")
                    .contentType(msgType)
                    .contentParts(contentParts)
                    .timestamp(LocalDateTime.now())
                    .replyToken(isGroup ? chatId : senderId)
                    .rawPayload(Map.of(
                            "wecom_frame_req_id", frameReqId,
                            "wecom_processing_stream_id", processingStreamId,
                            "wecom_chat_type", chatType,
                            "wecom_chatid", chatId
                    ))
                    .build();

            log.info("[wecom] Received message: sender={}, chatType={}, msgType={}, textLen={}",
                    senderId.length() > 20 ? senderId.substring(0, 20) : senderId,
                    chatType, msgType,
                    textContent != null ? textContent.length() : 0);

            onMessage(channelMessage);

        } catch (Exception e) {
            log.error("[wecom] Failed to handle message callback: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理事件推送回调 (aibot_event_callback)
     */
    @SuppressWarnings("unchecked")
    private void handleEventCallback(Map<String, Object> frame) {
        try {
            Map<String, Object> body = (Map<String, Object>) frame.getOrDefault("body", Map.of());
            Map<String, Object> event = body.get("event") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
            String eventType = (String) event.getOrDefault("eventtype", "");

            if ("enter_chat".equals(eventType)) {
                String welcomeText = getConfigString("welcome_text", "");
                if (!welcomeText.isBlank()) {
                    try {
                        Map<String, Object> headers = (Map<String, Object>) frame.getOrDefault("headers", Map.of());
                        String reqId = (String) headers.getOrDefault("req_id", "");
                        replyWelcome(reqId, welcomeText);
                        log.info("[wecom] Welcome message sent");
                    } catch (Exception e) {
                        log.warn("[wecom] Failed to send welcome message: {}", e.getMessage());
                    }
                }
                return;
            }

            log.debug("[wecom] Ignoring event type: {}", eventType);
        } catch (Exception e) {
            log.error("[wecom] Failed to handle event callback: {}", e.getMessage(), e);
        }
    }

    // ==================== 消息发送 ====================

    @Override
    public void sendMessage(String targetId, String content) {
        if (webSocket == null) {
            log.warn("[wecom] Channel not started, cannot send message");
            return;
        }

        // 检查是否有 pending frame（用于 reply_stream 覆盖"思考中..."）
        // sendMessage 被 renderAndSend 调用时，尝试用 reply_stream 覆盖
        // 但由于 rawPayload 信息在 ChannelMessageRouter 层已丢失，
        // 这里走 send_message 主动推送路径
        sendMessageToChat(targetId, content);
    }

    /**
     * 通过 WebSocket send_message 命令主动推送消息
     */
    private void sendMessageToChat(String chatId, String content) {
        if (webSocket == null || content == null || content.isBlank()) return;
        try {
            String reqId = generateReqId(CMD_SEND_MSG);
            Map<String, Object> frame = Map.of(
                    "cmd", CMD_SEND_MSG,
                    "headers", Map.of("req_id", reqId),
                    "body", Map.of(
                            "chatid", chatId,
                            "msgtype", "markdown",
                            "markdown", Map.of("content", content)
                    )
            );
            sendFrameWithAck(reqId, frame);
        } catch (Exception e) {
            log.error("[wecom] Failed to send message to {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * 覆写 renderAndSend：如果有 processing_stream_id 则用 reply_stream 覆盖"思考中..."
     */
    @Override
    public void renderAndSend(String targetId, String content) {
        // 尝试查找匹配的 pending frame（通过 target 反查）
        // renderAndSend 在 ChannelMessageRouter.processMessage() 中被调用
        // 此时 targetId 是 replyToken（userId 或 chatId）

        // 先进行正常的内容渲染（过滤 thinking、分割长文本）
        boolean filterThinking = getConfigBoolean("filter_thinking", true);
        boolean filterToolMessages = getConfigBoolean("filter_tool_messages", true);
        String format = getConfigString("message_format", "auto");
        int maxLen = vip.mate.channel.ChannelMessageRenderer.PLATFORM_LIMITS.getOrDefault(getChannelType(), 2048);

        List<String> segments = vip.mate.channel.ChannelMessageRenderer.renderForChannel(
                content, filterThinking, filterToolMessages, format, maxLen);

        for (String segment : segments) {
            sendMessage(targetId, segment);
        }
    }

    @Override
    public void sendContentParts(String targetId, List<MessageContentPart> parts) {
        for (MessageContentPart part : parts) {
            if (part == null) continue;
            switch (part.getType()) {
                case "text" -> { if (part.getText() != null) sendMessage(targetId, part.getText()); }
                case "image" -> {
                    String imgUrl = part.getFileUrl() != null ? part.getFileUrl() : part.getMediaId();
                    if (imgUrl != null) {
                        sendMessage(targetId, "![image](" + imgUrl + ")");
                    }
                }
                case "file" -> {
                    String fileName = part.getFileName() != null ? part.getFileName() : "file";
                    sendMessage(targetId, "[文件: " + fileName + "]");
                }
                default -> { if (part.getText() != null) sendMessage(targetId, part.getText()); }
            }
        }
    }

    // ==================== reply_stream 协议实现 ====================

    /**
     * 发送流式回复（reply_stream）
     * <p>
     * 通过 WebSocket 回复通道，使用相同 stream_id 可以覆盖更新已发送的消息。
     *
     * @param originalReqId 原始消息的 reqId（用于路由回复）
     * @param streamId      流式消息 ID（相同 ID 会覆盖之前的消息）
     * @param content       回复内容（支持 Markdown）
     * @param finish        是否结束流式消息
     */
    private void replyStream(String originalReqId, String streamId, String content, boolean finish) {
        Map<String, Object> streamBody = new LinkedHashMap<>();
        streamBody.put("id", streamId);
        streamBody.put("finish", finish);
        streamBody.put("content", content);

        Map<String, Object> body = Map.of(
                "msgtype", "stream",
                "stream", streamBody
        );

        Map<String, Object> frame = Map.of(
                "cmd", CMD_RESPONSE,
                "headers", Map.of("req_id", originalReqId),
                "body", body
        );

        sendFrameWithAck(originalReqId, frame);
    }

    /**
     * 发送欢迎消息
     */
    private void replyWelcome(String reqId, String text) {
        Map<String, Object> body = Map.of(
                "msgtype", "text",
                "text", Map.of("content", text)
        );
        Map<String, Object> frame = Map.of(
                "cmd", CMD_RESPONSE_WELCOME,
                "headers", Map.of("req_id", reqId),
                "body", body
        );
        sendFrameWithAck(reqId, frame);
    }

    // ==================== 帧发送基础设施 ====================

    /**
     * 发送 WebSocket 帧（fire and forget）
     */
    private void sendFrame(Map<String, Object> frame) {
        WebSocket ws = this.webSocket;
        if (ws == null) {
            log.warn("[wecom] WebSocket not connected, cannot send frame");
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(frame);
            ws.sendText(json, true);
        } catch (Exception e) {
            log.error("[wecom] Failed to send frame: {}", e.getMessage(), e);
        }
    }

    /**
     * 串行队列发送帧，等待 ACK（带超时）
     * <p>
     * 同一 reqId 的消息按顺序发送，每条等待 ACK 后再发下一条。
     */
    private void sendFrameWithAck(String reqId, Map<String, Object> frame) {
        CompletableFuture<Map<String, Object>> ackFuture = new CompletableFuture<>();

        // 注册 ACK 等待
        pendingAcks.put(reqId, ackFuture);

        // 发送帧
        sendFrame(frame);

        // 等待 ACK（超时 5 秒，不阻塞当前线程 — fire and forget）
        ackFuture.orTimeout(REPLY_ACK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .whenComplete((result, ex) -> {
                    pendingAcks.remove(reqId);
                    if (ex != null) {
                        log.debug("[wecom] Reply ACK timeout or error for reqId={}: {}", reqId, ex.getMessage());
                    }
                });
    }

    // ==================== 媒体文件下载与 AES 解密 ====================

    /**
     * 下载并解密企业微信媒体文件
     * <p>
     * AES-256-CBC 解密：base64 decode aesKey → IV = 前 16 字节 → PKCS#7 去填充
     *
     * @param url          文件下载 URL
     * @param aesKey       Base64 编码的 AES-256 密钥
     * @param msgId        消息 ID（用于生成文件名）
     * @param fileNameHint 文件名提示
     * @return 本地文件路径，失败返回 null
     */
    private String downloadAndDecryptMedia(String url, String aesKey, String msgId, String fileNameHint) {
        try {
            String mediaDir = getConfigString("media_dir", "data/media");
            Path mediaDirPath = Path.of(mediaDir);
            Files.createDirectories(mediaDirPath);

            // 1. HTTP GET 下载文件
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            byte[] encryptedData = response.body().readAllBytes();

            byte[] fileData;
            // 2. AES 解密（如果提供了 aesKey）
            if (aesKey != null && !aesKey.isBlank()) {
                fileData = decryptAes256Cbc(encryptedData, aesKey);
            } else {
                fileData = encryptedData;
            }

            // 3. 保存到本地
            String urlHash = md5Hex(url).substring(0, 8);
            String safeName = fileNameHint.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (safeName.isBlank()) safeName = "media";
            Path filePath = mediaDirPath.resolve("wecom_" + urlHash + "_" + safeName);
            Files.write(filePath, fileData);

            log.info("[wecom] Media downloaded: {} ({} bytes)", filePath, fileData.length);
            return filePath.toAbsolutePath().toString();

        } catch (Exception e) {
            log.error("[wecom] Failed to download media: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * AES-256-CBC 解密（对齐 wecom-aibot-python-sdk crypto_utils.py）
     * <p>
     * 1. Base64 decode aesKey（自动补齐 padding）
     * 2. IV = decoded key 前 16 字节
     * 3. AES-256-CBC 解密
     * 4. PKCS#7 去填充
     */
    private byte[] decryptAes256Cbc(byte[] encryptedData, String aesKeyBase64) throws Exception {
        // 补齐 Base64 padding
        int padCount = (4 - aesKeyBase64.length() % 4) % 4;
        String padded = aesKeyBase64 + "=".repeat(padCount);
        byte[] keyBytes = Base64.getDecoder().decode(padded);

        // IV = 前 16 字节
        byte[] iv = Arrays.copyOf(keyBytes, 16);

        // 确保数据是 16 字节的倍数
        int blockSize = 16;
        int remainder = encryptedData.length % blockSize;
        if (remainder != 0) {
            encryptedData = Arrays.copyOf(encryptedData, encryptedData.length + (blockSize - remainder));
        }

        // AES-256-CBC 解密（NoPadding — 手动去 PKCS#7）
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(encryptedData);

        // PKCS#7 去填充
        int padLen = decrypted[decrypted.length - 1] & 0xFF;
        if (padLen < 1 || padLen > 32 || padLen > decrypted.length) {
            throw new IllegalArgumentException("Invalid PKCS#7 padding value: " + padLen);
        }
        for (int i = decrypted.length - padLen; i < decrypted.length; i++) {
            if ((decrypted[i] & 0xFF) != padLen) {
                throw new IllegalArgumentException("Invalid PKCS#7 padding: bytes mismatch");
            }
        }
        return Arrays.copyOf(decrypted, decrypted.length - padLen);
    }

    // ==================== 主动推送 ====================

    @Override
    public boolean supportsProactiveSend() {
        return true;
    }

    @Override
    public void proactiveSend(String targetId, String content) {
        sendMessageToChat(targetId, content);
    }

    @Override
    public String getChannelType() {
        return CHANNEL_TYPE;
    }

    // ==================== 工具方法 ====================

    /**
     * 生成唯一请求 ID：{prefix}_{timestamp}_{counter}
     */
    private String generateReqId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + reqIdCounter.incrementAndGet();
    }

    /**
     * MD5 哈希（hex 字符串）
     */
    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    // ==================== 回复队列内部类 ====================

    private record ReplyTask(Map<String, Object> frame, CompletableFuture<Map<String, Object>> future) {}
}
