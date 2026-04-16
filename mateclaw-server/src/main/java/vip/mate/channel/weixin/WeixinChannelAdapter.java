package vip.mate.channel.weixin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import vip.mate.channel.AbstractChannelAdapter;
import vip.mate.channel.ChannelMessage;
import vip.mate.channel.ChannelMessageRouter;
import vip.mate.channel.model.ChannelEntity;
import vip.mate.workspace.conversation.model.MessageContentPart;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 微信个人号渠道适配器 — 基于 iLink Bot HTTP API
 * <p>
 * 微信个人号渠道实现（基于 iLink Bot HTTP API）：
 * <ul>
 *   <li>HTTP 长轮询接收消息（getupdates，服务端最长 35s）</li>
 *   <li>HTTP POST 发送消息（sendmessage）</li>
 *   <li>Bearer Token 认证（可通过 QR 码扫码登录获取）</li>
 *   <li>支持 text(1), image(2), voice/ASR(3), file(4), video(5) 消息类型</li>
 *   <li>基于 context_token 的消息去重和主动推送</li>
 * </ul>
 * <p>
 * 会话 ID 规则：
 * <ul>
 *   <li>私聊：weixin:{fromUserId}</li>
 *   <li>群聊：weixin:group:{groupId}</li>
 * </ul>
 * <p>
 * configJson 配置项：
 * <ul>
 *   <li>bot_token: iLink Bot Token（扫码登录获取）</li>
 *   <li>base_url: API 基础地址（默认 https://ilinkai.weixin.qq.com）</li>
 *   <li>media_download_enabled: 是否下载媒体文件（默认 true）</li>
 *   <li>media_dir: 媒体文件保存目录（默认 data/media）</li>
 * </ul>
 *
 * @author MateClaw Team
 */
@Slf4j
public class WeixinChannelAdapter extends AbstractChannelAdapter {

    public static final String CHANNEL_TYPE = "weixin";

    /** 消息去重最大记录数 */
    private static final int PROCESSED_IDS_MAX = 2000;

    // ==================== 运行时状态 ====================

    private ILinkClient client;

    /** 长轮询线程 */
    private volatile Thread pollThread;

    /** 停止信号 */
    private final AtomicBoolean stopSignal = new AtomicBoolean(false);

    /** 长轮询游标 */
    private volatile String cursor = "";

    /** 消息去重集合（LRU） */
    private final LinkedHashMap<String, Boolean> processedIds = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > PROCESSED_IDS_MAX;
        }
    };

    /** 用户最新 context_token 缓存（用于主动推送） */
    private final ConcurrentHashMap<String, String> userContextTokens = new ConcurrentHashMap<>();

    // ==================== 输入中提示 ====================

    /** 输入提示 ticket 缓存：userId -> (ticket, expireTime) */
    private final ConcurrentHashMap<String, TypingTicketEntry> typingTickets = new ConcurrentHashMap<>();

    /** 输入提示刷新任务：userId -> ScheduledFuture */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> typingTasks = new ConcurrentHashMap<>();

    /** 输入提示调度器 */
    private ScheduledExecutorService typingScheduler;

    /** Typing ticket 缓存 24 小时 */
    private static final long TYPING_TICKET_TTL_MS = 24 * 60 * 60 * 1000L;

    /** 输入提示刷新间隔 5 秒 */
    private static final long TYPING_REFRESH_INTERVAL_MS = 5_000;

    private record TypingTicketEntry(String ticket, long expireAt) {
        boolean isValid() { return !ticket.isBlank() && System.currentTimeMillis() < expireAt; }
    }

    // ==================== 文件上传 ====================

    /** 用于文件 URL 下载的 HttpClient */
    private HttpClient uploadHttpClient;

    public WeixinChannelAdapter(ChannelEntity channelEntity,
                                ChannelMessageRouter messageRouter,
                                ObjectMapper objectMapper) {
        super(channelEntity, messageRouter, objectMapper);
    }

    @Override
    public String getChannelType() {
        return CHANNEL_TYPE;
    }

    // ==================== 生命周期 ====================

    @Override
    protected void doStart() {
        String botToken = getConfigString("bot_token", "");
        String baseUrl = getConfigString("base_url", ILinkClient.DEFAULT_BASE_URL);

        if (botToken.isBlank()) {
            throw new RuntimeException("weixin: bot_token is required. Please scan QR code to obtain one.");
        }

        client = new ILinkClient(botToken, baseUrl, objectMapper);
        uploadHttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        typingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "weixin-typing-" + channelEntity.getId());
            t.setDaemon(true);
            return t;
        });

        // 启动长轮询线程
        stopSignal.set(false);
        cursor = "";
        pollThread = new Thread(this::pollLoop, "weixin-poll-" + channelEntity.getId());
        pollThread.setDaemon(true);
        pollThread.start();

        log.info("[weixin] Channel started: {} (token={}...)", channelEntity.getName(),
                botToken.substring(0, Math.min(12, botToken.length())));
    }

    @Override
    protected void doStop() {
        stopSignal.set(true);

        // 停止所有输入提示任务
        typingTasks.values().forEach(f -> f.cancel(false));
        typingTasks.clear();
        typingTickets.clear();
        if (typingScheduler != null) {
            typingScheduler.shutdownNow();
            typingScheduler = null;
        }

        if (pollThread != null) {
            pollThread.interrupt();
            try {
                pollThread.join(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pollThread = null;
        }
        client = null;
        uploadHttpClient = null;
        log.info("[weixin] Channel stopped: {}", channelEntity.getName());
    }

    // ==================== 长轮询循环 ====================

    private void pollLoop() {
        log.info("[weixin] Poll thread started");
        while (!stopSignal.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Map<String, Object> data = client.getUpdates(cursor);

                // 更新游标
                Object newCursor = data.get("get_updates_buf");
                if (newCursor != null) {
                    cursor = newCursor.toString();
                }

                // 处理消息
                Object msgsObj = data.get("msgs");
                if (msgsObj instanceof List<?> msgs) {
                    for (Object msgObj : msgs) {
                        if (msgObj instanceof Map<?, ?> msg) {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> msgMap = (Map<String, Object>) msg;
                                handleInboundMessage(msgMap);
                            } catch (Exception e) {
                                log.error("[weixin] Failed to handle message: {}", e.getMessage(), e);
                            }
                        }
                    }
                }

                // ret=-1 是正常的长轮询超时（无新消息）
                Object retObj = data.get("ret");
                int ret = retObj instanceof Number n ? n.intValue() : -1;
                if (ret != 0 && (msgsObj == null || ((List<?>) msgsObj).isEmpty())) {
                    if (ret != -1) {
                        log.warn("[weixin] getUpdates non-zero ret={}, retry in 3s", ret);
                        Thread.sleep(3000);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (!stopSignal.get()) {
                    log.error("[weixin] Poll error, retry in 5s: {}", e.getMessage());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.info("[weixin] Poll thread stopped");
    }

    // ==================== 入站消息处理 ====================

    @SuppressWarnings("unchecked")
    private void handleInboundMessage(Map<String, Object> msg) {
        String fromUserId = getStr(msg, "from_user_id");
        String toUserId = getStr(msg, "to_user_id");
        String contextToken = getStr(msg, "context_token");
        String groupId = getStr(msg, "group_id");
        int msgType = msg.get("message_type") instanceof Number n ? n.intValue() : 0;

        // 只处理用户→机器人消息 (message_type == 1)
        if (msgType != 1) {
            return;
        }

        // 去重
        String dedupKey = !contextToken.isBlank() ? contextToken
                : fromUserId + "_" + getStr(msg, "msg_id");
        synchronized (processedIds) {
            if (processedIds.containsKey(dedupKey)) {
                log.debug("[weixin] Duplicate message skipped: {}", dedupKey.substring(0, Math.min(40, dedupKey.length())));
                return;
            }
            processedIds.put(dedupKey, Boolean.TRUE);
        }

        // 解析消息内容
        List<MessageContentPart> contentParts = new ArrayList<>();
        List<String> textParts = new ArrayList<>();

        List<Map<String, Object>> itemList = (List<Map<String, Object>>) msg.getOrDefault("item_list", List.of());
        boolean mediaDownloadEnabled = getConfigBoolean("media_download_enabled", true);
        String mediaDir = getConfigString("media_dir", "data/media");

        for (Map<String, Object> item : itemList) {
            int itemType = item.get("type") instanceof Number n ? n.intValue() : 0;

            switch (itemType) {
                case 1 -> {
                    // Text
                    Map<String, Object> textItem = (Map<String, Object>) item.getOrDefault("text_item", Map.of());
                    String text = getStr(textItem, "text").strip();
                    if (!text.isEmpty()) {
                        textParts.add(text);
                    }
                }
                case 2 -> {
                    // Image
                    if (mediaDownloadEnabled) {
                        String path = downloadMediaItem(item, "image_item", "image.jpg", mediaDir);
                        if (path != null) {
                            MessageContentPart part = new MessageContentPart();
                            part.setType("image");
                            part.setPath(path);
                            part.setContentType("image/*");
                            contentParts.add(part);
                        } else {
                            // 下载失败，尝试构建 CDN URL
                            String cdnUrl = buildCdnUrl(item, "image_item");
                            if (cdnUrl != null) {
                                contentParts.add(MessageContentPart.image(cdnUrl, cdnUrl));
                            } else {
                                textParts.add("[图片: 下载失败]");
                            }
                        }
                    } else {
                        // 未启用下载，但仍然传递 CDN URL（供多模态分析）
                        String cdnUrl = buildCdnUrl(item, "image_item");
                        if (cdnUrl != null) {
                            contentParts.add(MessageContentPart.image(cdnUrl, cdnUrl));
                        } else {
                            textParts.add("[图片]");
                        }
                    }
                }
                case 3 -> {
                    // Voice — 使用 ASR 语音识别文本
                    Map<String, Object> voiceItem = (Map<String, Object>) item.getOrDefault("voice_item", Map.of());
                    Map<String, Object> voiceTextItem = (Map<String, Object>) voiceItem.getOrDefault("text_item", Map.of());
                    String asrText = getStr(voiceTextItem, "text").strip();
                    if (!asrText.isEmpty()) {
                        textParts.add(asrText);
                    } else {
                        textParts.add("[语音: 无转写结果]");
                    }
                }
                case 4 -> {
                    // File
                    Map<String, Object> fileItemMap = (Map<String, Object>) item.getOrDefault("file_item", Map.of());
                    String fileName = getStr(fileItemMap, "file_name");
                    if (fileName.isBlank()) fileName = "file.bin";
                    if (mediaDownloadEnabled) {
                        String path = downloadMediaItem(item, "file_item", fileName, mediaDir);
                        if (path != null) {
                            MessageContentPart part = new MessageContentPart();
                            part.setType("file");
                            part.setPath(path);
                            part.setFileName(fileName);
                            contentParts.add(part);
                        } else {
                            textParts.add("[文件: " + fileName + " 下载失败]");
                        }
                    } else {
                        textParts.add("[文件: " + fileName + "]");
                    }
                }
                case 5 -> {
                    // Video
                    if (mediaDownloadEnabled) {
                        String path = downloadMediaItem(item, "video_item", "video.mp4", mediaDir);
                        if (path != null) {
                            MessageContentPart part = new MessageContentPart();
                            part.setType("video");
                            part.setPath(path);
                            part.setContentType("video/*");
                            contentParts.add(part);
                        } else {
                            // 尝试构建 CDN URL
                            String cdnUrl = buildCdnUrl(item, "video_item");
                            if (cdnUrl != null) {
                                MessageContentPart part = new MessageContentPart();
                                part.setType("video");
                                part.setFileUrl(cdnUrl);
                                part.setContentType("video/*");
                                contentParts.add(part);
                            } else {
                                textParts.add("[视频: 下载失败]");
                            }
                        }
                    } else {
                        textParts.add("[视频]");
                    }
                }
                default -> textParts.add("[不支持的消息类型: " + itemType + "]");
            }
        }

        // 组装文本
        String textContent = String.join("\n", textParts).strip();
        if (!textContent.isEmpty()) {
            contentParts.addFirst(MessageContentPart.text(textContent));
        }
        if (contentParts.isEmpty()) {
            return;
        }

        // 缓存 context_token（用于主动推送）
        if (!fromUserId.isBlank() && !contextToken.isBlank()) {
            userContextTokens.put(fromUserId, contextToken);
        }

        // 构建统一消息
        boolean isGroup = !groupId.isBlank();
        String chatId = isGroup ? groupId : null;
        // replyToken 存储 contextToken + fromUserId，格式: contextToken|fromUserId
        String replyToken = contextToken + "|" + fromUserId;

        ChannelMessage channelMessage = ChannelMessage.builder()
                .messageId(getStr(msg, "msg_id"))
                .channelType(CHANNEL_TYPE)
                .senderId(fromUserId)
                .senderName(fromUserId) // iLink API 不提供昵称
                .chatId(chatId)
                .content(textContent)
                .contentType(contentParts.size() == 1 && "text".equals(contentParts.getFirst().getType()) ? "text" : "mixed")
                .contentParts(contentParts)
                .timestamp(LocalDateTime.now())
                .replyToken(replyToken)
                .rawPayload(msg)
                .build();

        log.info("[weixin] Recv: from={} group={} text_len={}",
                fromUserId.length() > 20 ? fromUserId.substring(0, 20) : fromUserId,
                groupId.length() > 20 ? groupId.substring(0, 20) : groupId,
                textContent.length());

        // 启动"输入中..."提示
        startTyping(fromUserId, contextToken);

        onMessage(channelMessage);
    }

    // ==================== 媒体下载 ====================

    @SuppressWarnings("unchecked")
    private String downloadMediaItem(Map<String, Object> item, String itemKey, String filenameHint, String mediaDir) {
        try {
            Map<String, Object> mediaItem = (Map<String, Object>) item.getOrDefault(itemKey, Map.of());
            Map<String, Object> media = (Map<String, Object>) mediaItem.getOrDefault("media", Map.of());
            String encryptQueryParam = getStr(media, "encrypt_query_param");
            String aesKey;

            // image_item 有顶级 aeskey (hex)
            String aeskeyHex = getStr(mediaItem, "aeskey");
            if (!aeskeyHex.isBlank()) {
                aesKey = Base64.getEncoder().encodeToString(hexToBytes(aeskeyHex));
            } else {
                aesKey = getStr(media, "aes_key");
            }

            if (encryptQueryParam.isBlank()) {
                log.warn("[weixin] No encrypt_query_param for media download");
                return null;
            }

            byte[] data = client.downloadMedia("", aesKey, encryptQueryParam);

            // 保存到本地
            Path dir = Path.of(mediaDir);
            Files.createDirectories(dir);
            String safeFilename = filenameHint.replaceAll("[^a-zA-Z0-9._-]", "");
            if (safeFilename.isBlank()) safeFilename = "media";
            String urlHash = md5Short(encryptQueryParam);
            Path filePath = dir.resolve("weixin_" + urlHash + "_" + safeFilename);
            Files.write(filePath, data);
            return filePath.toString();
        } catch (Exception e) {
            log.error("[weixin] Media download failed: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 发送消息 ====================

    @Override
    public void sendMessage(String targetId, String content) {
        if (client == null || content == null || content.isBlank()) {
            return;
        }
        try {
            // targetId 格式: contextToken|userId
            String[] parts = targetId.split("\\|", 2);
            String contextToken = parts.length > 0 ? parts[0] : "";
            String toUserId = parts.length > 1 ? parts[1] : "";

            if (toUserId.isBlank() || contextToken.isBlank()) {
                log.warn("[weixin] Cannot send: missing userId or contextToken in targetId");
                return;
            }

            // 发送前停止输入提示，发送后重新启动（模拟连续输入）
            stopTyping(toUserId);
            client.sendText(toUserId, content, contextToken);
        } catch (Exception e) {
            log.error("[weixin] Failed to send message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendContentParts(String targetId, List<MessageContentPart> parts) {
        if (client == null || parts == null || parts.isEmpty()) {
            return;
        }

        String[] split = targetId.split("\\|", 2);
        String contextToken = split.length > 0 ? split[0] : "";
        String toUserId = split.length > 1 ? split[1] : "";

        if (toUserId.isBlank() || contextToken.isBlank()) {
            log.warn("[weixin] sendContentParts: missing userId or contextToken");
            return;
        }

        // 停止输入提示
        stopTyping(toUserId);

        for (MessageContentPart part : parts) {
            if (part == null) continue;
            try {
                switch (part.getType()) {
                    case "text" -> {
                        if (part.getText() != null && !part.getText().isBlank()) {
                            client.sendText(toUserId, part.getText(), contextToken);
                        }
                    }
                    case "image" -> sendImagePart(toUserId, contextToken, part);
                    case "file" -> sendFilePart(toUserId, contextToken, part);
                    case "video" -> sendVideoPart(toUserId, contextToken, part);
                    default -> {
                        if (part.getText() != null && !part.getText().isBlank()) {
                            client.sendText(toUserId, part.getText(), contextToken);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[weixin] Failed to send content part ({}): {}", part.getType(), e.getMessage());
                // 降级为文本
                sendFallbackText(targetId, part);
            }
        }
    }

    @Override
    public void renderAndSend(String targetId, String content) {
        // 停止输入提示
        String[] split = targetId.split("\\|", 2);
        String toUserId = split.length > 1 ? split[1] : "";
        if (!toUserId.isBlank()) {
            stopTyping(toUserId);
        }

        // 调用父类默认渲染逻辑
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

    // ==================== 媒体上传发送 ====================

    private void sendImagePart(String toUserId, String contextToken, MessageContentPart part) throws Exception {
        byte[] imageBytes = resolveFileBytes(part);
        if (imageBytes == null) {
            sendFallbackText(contextToken + "|" + toUserId, part);
            return;
        }
        client.sendImage(toUserId, imageBytes, contextToken);
        log.info("[weixin] Image sent to {}: {}bytes", toUserId.substring(0, Math.min(12, toUserId.length())), imageBytes.length);
    }

    private void sendFilePart(String toUserId, String contextToken, MessageContentPart part) throws Exception {
        byte[] fileBytes = resolveFileBytes(part);
        if (fileBytes == null) {
            sendFallbackText(contextToken + "|" + toUserId, part);
            return;
        }
        String fileName = part.getFileName() != null ? part.getFileName() : "file.bin";
        client.sendFile(toUserId, fileBytes, fileName, contextToken);
        log.info("[weixin] File sent to {}: {} ({}bytes)", toUserId.substring(0, Math.min(12, toUserId.length())), fileName, fileBytes.length);
    }

    private void sendVideoPart(String toUserId, String contextToken, MessageContentPart part) throws Exception {
        byte[] videoBytes = resolveFileBytes(part);
        if (videoBytes == null) {
            sendFallbackText(contextToken + "|" + toUserId, part);
            return;
        }
        client.sendVideo(toUserId, videoBytes, contextToken);
        log.info("[weixin] Video sent to {}: {}bytes", toUserId.substring(0, Math.min(12, toUserId.length())), videoBytes.length);
    }

    /**
     * 从 MessageContentPart 解析文件字节：优先本地路径，其次 URL 下载
     */
    private byte[] resolveFileBytes(MessageContentPart part) {
        if (part.getPath() != null && !part.getPath().isBlank()) {
            try {
                Path p = Path.of(part.getPath());
                if (Files.exists(p)) {
                    return Files.readAllBytes(p);
                }
            } catch (Exception e) {
                log.debug("[weixin] Failed to read local file {}: {}", part.getPath(), e.getMessage());
            }
        }
        String url = part.getFileUrl();
        if (url != null && !url.isBlank() && uploadHttpClient != null) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .GET().build();
                HttpResponse<byte[]> resp = uploadHttpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200) {
                    return resp.body();
                }
            } catch (Exception e) {
                log.debug("[weixin] Failed to download from {}: {}", url, e.getMessage());
            }
        }
        return null;
    }

    private void sendFallbackText(String targetId, MessageContentPart part) {
        switch (part.getType()) {
            case "image" -> sendMessage(targetId, "[图片]");
            case "file" -> sendMessage(targetId, "[文件: " + (part.getFileName() != null ? part.getFileName() : "file") + "]");
            case "video" -> sendMessage(targetId, "[视频]");
            default -> { if (part.getText() != null) sendMessage(targetId, part.getText()); }
        }
    }

    // ==================== 输入中提示 ====================

    /**
     * 启动输入中提示（每 5 秒刷新一次）
     */
    private void startTyping(String userId, String contextToken) {
        if (client == null || userId.isBlank()) return;

        // 先停止旧的
        stopTyping(userId);

        try {
            String ticket = getTypingTicket(userId, contextToken);
            if (ticket == null || ticket.isBlank()) {
                log.debug("[weixin] No typing ticket for user {}", userId.substring(0, Math.min(12, userId.length())));
                return;
            }

            // 立即发送一次
            client.sendTyping(userId, ticket, 1);

            // 定时刷新
            if (typingScheduler != null && !typingScheduler.isShutdown()) {
                ScheduledFuture<?> future = typingScheduler.scheduleAtFixedRate(() -> {
                    try {
                        if (client != null) {
                            client.sendTyping(userId, ticket, 1);
                        }
                    } catch (Exception e) {
                        log.debug("[weixin] Typing refresh failed: {}", e.getMessage());
                    }
                }, TYPING_REFRESH_INTERVAL_MS, TYPING_REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                typingTasks.put(userId, future);
            }

            log.debug("[weixin] Typing started for {}", userId.substring(0, Math.min(12, userId.length())));
        } catch (Exception e) {
            log.debug("[weixin] Failed to start typing: {}", e.getMessage());
        }
    }

    /**
     * 停止输入中提示
     */
    private void stopTyping(String userId) {
        ScheduledFuture<?> future = typingTasks.remove(userId);
        if (future != null) {
            future.cancel(false);
        }

        // 发送停止状态
        TypingTicketEntry entry = typingTickets.get(userId);
        if (entry != null && entry.isValid() && client != null) {
            try {
                client.sendTyping(userId, entry.ticket(), 2);
                log.debug("[weixin] Typing stopped for {}", userId.substring(0, Math.min(12, userId.length())));
            } catch (Exception e) {
                log.debug("[weixin] Failed to stop typing: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取或缓存 typing ticket（24 小时 TTL）
     */
    private String getTypingTicket(String userId, String contextToken) {
        TypingTicketEntry cached = typingTickets.get(userId);
        if (cached != null && cached.isValid()) {
            return cached.ticket();
        }

        try {
            Map<String, Object> configResp = client.getConfig(userId, contextToken);
            int ret = configResp.get("ret") instanceof Number n ? n.intValue() : -1;
            if (ret != 0) {
                log.debug("[weixin] getConfig ret={} for typing ticket", ret);
                return null;
            }
            String ticket = (String) configResp.getOrDefault("typing_ticket", "");
            if (!ticket.isBlank()) {
                typingTickets.put(userId, new TypingTicketEntry(ticket, System.currentTimeMillis() + TYPING_TICKET_TTL_MS));
            }
            return ticket;
        } catch (Exception e) {
            log.debug("[weixin] Failed to get typing ticket: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 主动推送 ====================

    @Override
    public boolean supportsProactiveSend() {
        return true;
    }

    @Override
    public void proactiveSend(String targetId, String content) {
        if (client == null || content == null || content.isBlank()) {
            return;
        }
        try {
            // targetId 可以是 userId 或 weixin:userId
            String userId = targetId;
            if (userId.startsWith("weixin:group:")) {
                userId = userId.substring("weixin:group:".length());
            } else if (userId.startsWith("weixin:")) {
                userId = userId.substring("weixin:".length());
            }

            String contextToken = userContextTokens.get(userId);
            if (contextToken == null || contextToken.isBlank()) {
                log.warn("[weixin] No cached context_token for user {}, cannot proactive send", userId);
                return;
            }

            client.sendText(userId, content, contextToken);
            log.info("[weixin] Proactive message sent to {}: {}chars", userId, content.length());
        } catch (Exception e) {
            log.error("[weixin] Proactive send failed: {}", e.getMessage(), e);
        }
    }

    // ==================== QR 码登录（供 Controller 调用） ====================

    /**
     * 获取 QR 码登录信息
     *
     * @return 包含 qrcode, qrcode_img_content 等字段
     */
    public Map<String, Object> getQrCode() throws Exception {
        String baseUrl = getConfigString("base_url", ILinkClient.DEFAULT_BASE_URL);
        ILinkClient tempClient = new ILinkClient("", baseUrl, objectMapper);
        return tempClient.getBotQrcode();
    }

    /**
     * 查询 QR 码扫码状态
     *
     * @param qrcode QR 码标识
     * @return 状态信息
     */
    public Map<String, Object> getQrCodeStatus(String qrcode) throws Exception {
        String baseUrl = getConfigString("base_url", ILinkClient.DEFAULT_BASE_URL);
        ILinkClient tempClient = new ILinkClient("", baseUrl, objectMapper);
        return tempClient.getQrcodeStatus(qrcode);
    }

    // ==================== 工具方法 ====================

    /**
     * 从消息 item 中构建 CDN 下载 URL（不下载，仅构建 URL 供多模态分析使用）
     */
    @SuppressWarnings("unchecked")
    private String buildCdnUrl(Map<String, Object> item, String itemKey) {
        try {
            Map<String, Object> mediaItem = (Map<String, Object>) item.getOrDefault(itemKey, Map.of());
            Map<String, Object> media = (Map<String, Object>) mediaItem.getOrDefault("media", Map.of());
            String encryptQueryParam = getStr(media, "encrypt_query_param");
            if (encryptQueryParam.isBlank()) return null;

            String cdnBase = "https://novac2c.cdn.weixin.qq.com/c2c";
            return cdnBase + "/download?encrypted_query_param="
                    + java.net.URLEncoder.encode(encryptQueryParam, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("[weixin] Failed to build CDN URL: {}", e.getMessage());
            return null;
        }
    }

    private static String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private static String md5Short(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
