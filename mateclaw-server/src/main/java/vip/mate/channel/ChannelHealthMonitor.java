package vip.mate.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 渠道健康监控
 * <p>
 * 每 5 分钟检查所有活跃渠道适配器的健康状态：
 * - 连接状态为 ERROR 超过 5 分钟 → 触发重启
 * - 连接状态为 CONNECTED 但超过 1 小时无事件 → 标记 stale 并重启
 * - 每渠道每小时最多 10 次重启，cooldown 2 分钟
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelHealthMonitor {

    private final ChannelManager channelManager;

    /** 错误状态超过此时间触发重启（毫秒） */
    private static final long ERROR_THRESHOLD_MS = 5 * 60 * 1000;

    /** 连接正常但无事件超过此时间视为 stale（毫秒） */
    private static final long STALE_THRESHOLD_MS = 60 * 60 * 1000;

    /** 每渠道每小时最大重启次数 */
    private static final int MAX_RESTARTS_PER_HOUR = 10;

    /** 同一渠道两次重启最小间隔（毫秒） */
    private static final long COOLDOWN_MS = 2 * 60 * 1000;

    /** 重启历史记录（channelId → 重启时间列表） */
    private final ConcurrentHashMap<Long, List<Instant>> restartHistory = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 300_000) // 每 5 分钟
    public void checkHealth() {
        Collection<ChannelAdapter> adapters = channelManager.getActiveAdapters();
        if (adapters.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        int checked = 0;
        int restarted = 0;

        for (ChannelAdapter adapter : adapters) {
            if (!(adapter instanceof AbstractChannelAdapter aca)) {
                continue;
            }
            checked++;

            Long channelId = aca.channelEntity.getId();
            AbstractChannelAdapter.ConnectionState state = aca.getConnectionState().get();
            long lastEvent = aca.getLastEventTimeMs().get();
            long sinceLastEvent = now - lastEvent;

            String reason = null;

            // 检查 1：ERROR 状态超过阈值
            if (state == AbstractChannelAdapter.ConnectionState.ERROR && sinceLastEvent > ERROR_THRESHOLD_MS) {
                reason = String.format("ERROR state for %ds", sinceLastEvent / 1000);
            }

            // 检查 2：CONNECTED 但长时间无事件（stale）
            if (reason == null && state == AbstractChannelAdapter.ConnectionState.CONNECTED
                    && sinceLastEvent > STALE_THRESHOLD_MS) {
                reason = String.format("stale connection, no events for %dm", sinceLastEvent / 60000);
            }

            if (reason != null) {
                if (canRestart(channelId, now)) {
                    log.warn("[ChannelHealth] Restarting channel {} ({}): {}",
                            channelId, aca.getDisplayName(), reason);
                    try {
                        channelManager.restartChannel(channelId);
                        recordRestart(channelId, now);
                        restarted++;
                    } catch (Exception e) {
                        log.error("[ChannelHealth] Failed to restart channel {}: {}",
                                channelId, e.getMessage());
                    }
                } else {
                    log.warn("[ChannelHealth] Channel {} ({}) unhealthy ({}), but restart rate-limited",
                            channelId, aca.getDisplayName(), reason);
                }
            }
        }

        if (restarted > 0) {
            log.info("[ChannelHealth] Check completed: {}/{} channels checked, {} restarted",
                    checked, adapters.size(), restarted);
        }
    }

    /**
     * 检查是否允许重启（限流 + cooldown）
     */
    private boolean canRestart(Long channelId, long nowMs) {
        List<Instant> history = restartHistory.computeIfAbsent(channelId, k -> new ArrayList<>());

        // 清理 1 小时前的记录
        Instant oneHourAgo = Instant.ofEpochMilli(nowMs - 3600_000);
        history.removeIf(t -> t.isBefore(oneHourAgo));

        // 限流检查
        if (history.size() >= MAX_RESTARTS_PER_HOUR) {
            return false;
        }

        // cooldown 检查
        if (!history.isEmpty()) {
            Instant lastRestart = history.get(history.size() - 1);
            if (nowMs - lastRestart.toEpochMilli() < COOLDOWN_MS) {
                return false;
            }
        }

        return true;
    }

    private void recordRestart(Long channelId, long nowMs) {
        restartHistory.computeIfAbsent(channelId, k -> new ArrayList<>())
                .add(Instant.ofEpochMilli(nowMs));
    }
}
