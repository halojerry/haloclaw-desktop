package vip.mate.playwright;

import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Playwright浏览器管理器 - 单例模式
 * <p>
 * 功能：
 * 1. Playwright实例生命周期管理
 * 2. Browser实例池管理
 * 3. BrowserContext创建与复用
 * 4. 资源自动清理
 * </p>
 *
 * @author mate
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrowserManager {

    private final PlaywrightProperties properties;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext persistentContext;  // 持久化上下文（用于用户数据目录模式）
    private final Map<String, BrowserContext> contextPool = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean initializing = new AtomicBoolean(false);

    /**
     * 初始化Playwright和浏览器
     */
    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("Playwright is disabled in configuration");
            return;
        }

        if (initialized.get()) {
            return;
        }

        // 防止重复初始化
        if (!initializing.compareAndSet(false, true)) {
            log.info("Playwright initialization in progress...");
            waitForInitialization();
            return;
        }

        try {
            log.info("Initializing Playwright...");
            
            // 创建Playwright实例
            playwright = Playwright.create();
            
            // 配置浏览器路径（如果有）
            BrowserType browserType = getBrowserType();
            
            // 浏览器启动参数
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(properties.isHeadless())
                    .setArgs(getLaunchArgs());
            
            // 如果指定了用户数据目录，使用持久化模式
            if (properties.getUserDataDir() != null && !properties.getUserDataDir().isEmpty()) {
                log.info("Using persistent browser context with user data dir: {}", properties.getUserDataDir());
                Path userDataPath = Paths.get(properties.getUserDataDir());
                // launchPersistentContext 返回 BrowserContext
                BrowserType.LaunchPersistentContextOptions contextOptions = new BrowserType.LaunchPersistentContextOptions()
                        .setHeadless(properties.isHeadless())
                        .setArgs(getLaunchArgs());
                persistentContext = browserType.launchPersistentContext(userDataPath, contextOptions);
                browser = null;  // 持久化模式下不使用普通浏览器
            } else {
                browser = browserType.launch(launchOptions);
                persistentContext = null;
            }

            // 确保截图目录存在
            Path screenshotPath = Paths.get(properties.getScreenshotDir());
            if (!screenshotPath.isAbsolute()) {
                screenshotPath = Paths.get(System.getProperty("user.dir"), properties.getScreenshotDir());
            }
            if (!screenshotPath.toFile().exists()) {
                screenshotPath.toFile().mkdirs();
            }

            initialized.set(true);
            log.info("Playwright initialized successfully. Browser: {}, Headless: {}", 
                    properties.getBrowserType(), properties.isHeadless());

        } catch (Exception e) {
            log.error("Failed to initialize Playwright", e);
            throw new RuntimeException("Playwright initialization failed", e);
        } finally {
            initializing.set(false);
        }
    }

    /**
     * 等待初始化完成
     */
    private void waitForInitialization() {
        int waitCount = 0;
        while (!initialized.get() && waitCount < 30) {
            try {
                Thread.sleep(1000);
                waitCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 获取浏览器类型
     */
    private BrowserType getBrowserType() {
        return switch (properties.getBrowserType().toLowerCase()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
    }

    /**
     * 获取浏览器启动参数
     */
    private java.util.List<String> getLaunchArgs() {
        java.util.List<String> args = new java.util.ArrayList<>();
        args.add("--disable-blink-features=AutomationControlled");
        args.add("--no-sandbox");
        args.add("--disable-setuid-sandbox");
        
        // Electron环境下特殊处理
        args.add("--disable-gpu");
        args.add("--disable-dev-shm-usage");
        
        // 禁用图片加载提升速度（如需要可开启）
        // args.add("--blink-settings=imagesEnabled=false");
        
        return args;
    }

    /**
     * 获取或创建浏览器上下文
     *
     * @param contextId 上下文ID
     * @return BrowserContext
     */
    public BrowserContext getContext(String contextId) {
        if (!initialized.get()) {
            throw new IllegalStateException("Playwright not initialized. Call init() first.");
        }

        return contextPool.computeIfAbsent(contextId, k -> {
            log.info("Creating new browser context: {}", k);
            try {
                Browser.NewContextOptions options = new Browser.NewContextOptions()
                        .setViewportSize(1920, 1080)
                        .setUserAgent(getRandomUserAgent())
                        .setIgnoreHTTPSErrors(true);
                
                // 添加存储状态（cookies等）
                if (properties.isDisableCSP()) {
                    options.setExtraHTTPHeaders(Map.of("Content-Security-Policy", ""));
                }
                
                return browser.newContext(options);
            } catch (Exception e) {
                log.error("Failed to create browser context: {}", k, e);
                throw new RuntimeException("Failed to create browser context", e);
            }
        });
    }

    /**
     * 释放指定上下文
     *
     * @param contextId 上下文ID
     */
    public void releaseContext(String contextId) {
        BrowserContext context = contextPool.remove(contextId);
        if (context != null) {
            try {
                context.close();
                log.info("Released browser context: {}", contextId);
            } catch (Exception e) {
                log.warn("Error closing context {}: {}", contextId, e.getMessage());
            }
        }
    }

    /**
     * 获取新页面
     *
     * @param contextId 上下文ID
     * @return Page
     */
    public Page newPage(String contextId) {
        BrowserContext context = getContext(contextId);
        return context.newPage();
    }

    /**
     * 创建临时上下文（自动清理）
     *
     * @return TemporaryContext 包含context和page
     */
    public TemporaryContext createTemporaryContext() {
        if (!initialized.get()) {
            throw new IllegalStateException("Playwright not initialized");
        }

        String tempId = "temp-" + System.currentTimeMillis();
        BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(1920, 1080)
                        .setUserAgent(getRandomUserAgent())
        );
        Page page = context.newPage();
        
        return new TemporaryContext(tempId, context, page);
    }

    /**
     * 随机User-Agent
     */
    private String getRandomUserAgent() {
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"
        };
        return userAgents[(int) (Math.random() * userAgents.length)];
    }

    /**
     * 检查浏览器是否可用
     */
    public boolean isAvailable() {
        return initialized.get() && browser != null && browser.isConnected();
    }

    /**
     * 获取浏览器状态信息
     */
    public BrowserStatus getStatus() {
        BrowserStatus status = new BrowserStatus();
        status.setInitialized(initialized.get());
        status.setBrowserType(properties.getBrowserType());
        status.setHeadless(properties.isHeadless());
        status.setEnabled(properties.isEnabled());
        
        if (browser != null) {
            status.setConnected(browser.isConnected());
            status.setContextCount(contextPool.size());
            status.setBrowserVersion(browser.version());
        }
        
        status.setScreenshotDir(properties.getScreenshotDir());
        status.setTimeout(properties.getTimeout());
        
        return status;
    }

    /**
     * 释放所有资源
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Playwright...");
        
        // 关闭所有上下文
        contextPool.values().forEach(context -> {
            try {
                context.close();
            } catch (Exception e) {
                log.warn("Error closing context: {}", e.getMessage());
            }
        });
        contextPool.clear();
        
        // 关闭浏览器
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception e) {
                log.warn("Error closing browser: {}", e.getMessage());
            }
        }
        
        // 关闭Playwright
        if (playwright != null) {
            try {
                playwright.close();
            } catch (Exception e) {
                log.warn("Error closing playwright: {}", e.getMessage());
            }
        }
        
        initialized.set(false);
        log.info("Playwright shutdown complete");
    }

    /**
     * 临时上下文包装类
     */
    public record TemporaryContext(String id, BrowserContext context, Page page) {
        public void close() {
            try {
                page.close();
            } catch (Exception ignored) {}
            try {
                context.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * 浏览器状态信息
     */
    @Data
    public static class BrowserStatus {
        private boolean initialized;
        private String browserType;
        private boolean headless;
        private boolean enabled;
        private boolean connected;
        private String browserVersion;
        private int contextCount;
        private String screenshotDir;
        private long timeout;
    }
}
