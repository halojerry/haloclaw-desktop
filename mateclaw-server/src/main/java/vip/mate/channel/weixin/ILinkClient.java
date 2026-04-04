package vip.mate.channel.weixin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * 微信 iLink Bot HTTP 客户端
 * <p>
 * 微信 iLink Bot HTTP 客户端实现：
 * <ul>
 *   <li>iLink API 基础地址：https://ilinkai.weixin.qq.com</li>
 *   <li>HTTP/JSON 协议，无需第三方 SDK</li>
 *   <li>Bearer Token 认证（通过 QR 码登录获取）</li>
 *   <li>长轮询 getupdates（服务端最长持有 35 秒）</li>
 * </ul>
 * <p>
 * 认证流程：
 * <ol>
 *   <li>GET /ilink/bot/get_bot_qrcode?bot_type=3 → 获取二维码</li>
 *   <li>轮询 GET /ilink/bot/get_qrcode_status?qrcode=xxx → 等待扫码确认</li>
 *   <li>确认后获得 bot_token + baseurl</li>
 *   <li>后续请求均带 Bearer token</li>
 * </ol>
 *
 * @author MateClaw Team
 */
@Slf4j
public class ILinkClient {

    public static final String DEFAULT_BASE_URL = "https://ilinkai.weixin.qq.com";
    private static final String CHANNEL_VERSION = "2.0.1";

    /** 长轮询超时（服务端最长 35s，客户端设 45s） */
    private static final Duration GETUPDATES_TIMEOUT = Duration.ofSeconds(45);
    /** 普通请求超时 */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    /** 媒体下载超时 */
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(60);

    @Setter
    private String botToken;
    @Setter
    private String baseUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ILinkClient(String botToken, String baseUrl, ObjectMapper objectMapper) {
        this.botToken = botToken;
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : DEFAULT_BASE_URL;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ==================== 请求头构建 ====================

    /**
     * 构建 iLink API 请求头
     * <p>
     * X-WECHAT-UIN: base64(str(random_uint32)) — 每请求一个随机值，防重放
     * Authorization: Bearer {token}
     * AuthorizationType: ilink_bot_token
     */
    private Map<String, String> makeHeaders() {
        long uinVal = new Random().nextLong(0, 0xFFFFFFFFL + 1);
        String uinB64 = Base64.getEncoder().encodeToString(
                String.valueOf(uinVal).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("AuthorizationType", "ilink_bot_token");
        headers.put("X-WECHAT-UIN", uinB64);
        if (botToken != null && !botToken.isBlank()) {
            headers.put("Authorization", "Bearer " + botToken);
        }
        return headers;
    }

    private HttpRequest.Builder applyHeaders(HttpRequest.Builder builder) {
        makeHeaders().forEach(builder::header);
        return builder;
    }

    // ==================== 认证 API ====================

    /**
     * 获取登录二维码
     *
     * @return 包含 qrcode, qrcode_img_content(Base64 PNG), url 等字段
     */
    public Map<String, Object> getBotQrcode() throws Exception {
        HttpRequest request = applyHeaders(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ilink/bot/get_bot_qrcode?bot_type=3"))
                .GET())
                .timeout(DEFAULT_TIMEOUT)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("getBotQrcode failed: HTTP " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    /**
     * 轮询二维码扫码状态
     *
     * @param qrcode 二维码标识（来自 getBotQrcode）
     * @return 包含 status(waiting/scanned/confirmed/expired), bot_token, baseurl 等
     */
    public Map<String, Object> getQrcodeStatus(String qrcode) throws Exception {
        String encoded = URLEncoder.encode(qrcode, StandardCharsets.UTF_8);
        HttpRequest request = applyHeaders(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ilink/bot/get_qrcode_status?qrcode=" + encoded))
                .GET())
                .timeout(DEFAULT_TIMEOUT)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("getQrcodeStatus failed: HTTP " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    /**
     * 等待 QR 码扫码确认（阻塞，最长 maxWaitSeconds 秒）
     *
     * @param qrcode          二维码标识
     * @param pollIntervalMs  轮询间隔（毫秒）
     * @param maxWaitSeconds  最长等待时间（秒）
     * @return QrLoginResult 包含 token 和 baseUrl
     */
    public QrLoginResult waitForLogin(String qrcode, long pollIntervalMs, int maxWaitSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + maxWaitSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> data = getQrcodeStatus(qrcode);
            String status = (String) data.getOrDefault("status", "");
            if ("confirmed".equals(status)) {
                String token = (String) data.getOrDefault("bot_token", "");
                String newBaseUrl = (String) data.getOrDefault("baseurl", baseUrl);
                return new QrLoginResult(token, newBaseUrl);
            }
            if ("expired".equals(status)) {
                throw new RuntimeException("WeChat QR code expired, please retry login");
            }
            Thread.sleep(pollIntervalMs);
        }
        throw new RuntimeException("WeChat QR code not scanned within " + maxWaitSeconds + "s");
    }

    // ==================== 消息 API ====================

    /**
     * 长轮询获取新消息（服务端最长持有 35 秒）
     *
     * @param cursor 上一次返回的 get_updates_buf，首次传空字符串
     * @return 包含 ret, msgs, get_updates_buf 等字段
     */
    public Map<String, Object> getUpdates(String cursor) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("get_updates_buf", cursor != null ? cursor : "");
        body.put("base_info", Map.of("channel_version", CHANNEL_VERSION));

        HttpRequest request = applyHeaders(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ilink/bot/getupdates"))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))))
                .timeout(GETUPDATES_TIMEOUT)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("getUpdates failed: HTTP " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    /**
     * 发送消息
     *
     * @param msg 消息体（遵循 iLink sendmessage 协议）
     * @return API 响应
     */
    public Map<String, Object> sendMessage(Map<String, Object> msg) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msg", msg);
        body.put("base_info", Map.of("channel_version", CHANNEL_VERSION));

        HttpRequest request = applyHeaders(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ilink/bot/sendmessage"))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))))
                .timeout(DEFAULT_TIMEOUT)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("sendMessage failed: HTTP " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    /**
     * 发送纯文本消息（便捷方法）
     *
     * @param toUserId     收件人 ID
     * @param text         消息文本
     * @param contextToken 上下文 token（来自入站消息，必需）
     */
    public void sendText(String toUserId, String text, String contextToken) throws Exception {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("from_user_id", "");
        msg.put("to_user_id", toUserId);
        msg.put("client_id", UUID.randomUUID().toString());
        msg.put("message_type", 2);  // BOT
        msg.put("message_state", 2); // FINISH
        msg.put("context_token", contextToken);
        msg.put("item_list", List.of(Map.of(
                "type", 1,
                "text_item", Map.of("text", text)
        )));
        sendMessage(msg);
    }

    // ==================== 媒体下载 ====================

    /**
     * 下载 CDN 媒体文件并可选解密
     * <p>
     * iLink 媒体文件存储在 https://novac2c.cdn.weixin.qq.com/c2c。
     * 下载 URL 通过 encrypt_query_param 构建。
     *
     * @param url                直接 HTTP URL（如果有）
     * @param aesKeyParam        AES key（hex / base64，为空则不解密）
     * @param encryptQueryParam  CDN 查询参数
     * @return 解密后的文件字节
     */
    public byte[] downloadMedia(String url, String aesKeyParam, String encryptQueryParam) throws Exception {
        String downloadUrl;
        if (encryptQueryParam != null && !encryptQueryParam.isBlank()) {
            String cdnBase = "https://novac2c.cdn.weixin.qq.com/c2c";
            String enc = URLEncoder.encode(encryptQueryParam, StandardCharsets.UTF_8);
            downloadUrl = cdnBase + "/download?encrypted_query_param=" + enc;
        } else if (url != null && url.startsWith("http")) {
            downloadUrl = url;
        } else {
            throw new IllegalArgumentException("Cannot download media: no valid URL. url=" + url);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .timeout(DOWNLOAD_TIMEOUT)
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new RuntimeException("downloadMedia failed: HTTP " + response.statusCode());
        }

        byte[] data = response.body();
        if (aesKeyParam != null && !aesKeyParam.isBlank()) {
            data = WeixinAesUtil.aesEcbDecrypt(data, aesKeyParam);
        }
        return data;
    }

    // ==================== 内部模型 ====================

    /**
     * QR 码登录结果
     */
    public record QrLoginResult(String token, String baseUrl) {}
}
