package vip.mate.channel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 指数退避工具类
 * <p>
 * 用于断线重连、Token 刷新失败重试等场景。
 * 每次调用 {@link #nextDelayMs()} 返回递增的延迟时间（带上限），
 * 重连成功后调用 {@link #reset()} 重置计数器。
 *
 * @author MateClaw Team
 */
public class ExponentialBackoff {

    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double factor;
    private final int maxAttempts;
    private final AtomicInteger attempts = new AtomicInteger(0);

    /**
     * @param initialDelayMs 初始延迟（毫秒）
     * @param maxDelayMs     最大延迟上限（毫秒）
     * @param factor         退避倍数（通常为 2.0）
     * @param maxAttempts    最大重试次数（-1 表示无限重试）
     */
    public ExponentialBackoff(long initialDelayMs, long maxDelayMs, double factor, int maxAttempts) {
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.factor = factor;
        this.maxAttempts = maxAttempts;
    }

    /** 默认配置：2s 起步，30s 上限，2 倍递增，无限重试 */
    public ExponentialBackoff() {
        this(2000, 30000, 2.0, -1);
    }

    /**
     * 计算下一次延迟（毫秒），并递增尝试次数
     *
     * @return 延迟毫秒数
     */
    public long nextDelayMs() {
        int attempt = attempts.getAndIncrement();
        long delay = (long) (initialDelayMs * Math.pow(factor, attempt));
        return Math.min(delay, maxDelayMs);
    }

    /**
     * 是否已超过最大重试次数
     */
    public boolean isExhausted() {
        if (maxAttempts < 0) return false;
        return attempts.get() >= maxAttempts;
    }

    /**
     * 重置退避计数器（重连成功后调用）
     */
    public void reset() {
        attempts.set(0);
    }

    /**
     * 当前已尝试次数
     */
    public int getAttempts() {
        return attempts.get();
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }
}
