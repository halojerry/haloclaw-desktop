package vip.mate.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1688图片搜索服务
 * <p>
 * 功能：
 * 1. 上传图片到1688图搜页面进行搜索
 * 2. 提取商品列表信息（图片、标题、价格、链接）
 * 3. 返回结构化搜索结果
 * </p>
 *
 * @author mate
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSearch1688Service {

    private final BrowserManager browserManager;
    private final PlaywrightProperties properties;

    // 1688图搜URL
    private static final String IMAGE_SEARCH_URL = "https://www.1688.com/";
    // 图片上传输入框选择器
    private static final String IMAGE_UPLOAD_SELECTOR = "input[type='file']";
    // 商品列表项选择器
    private static final String PRODUCT_ITEM_SELECTOR = ".offer-list .offer-item, .sm-offer-item, [class*='offer-item']";
    // 加载完成标识
    private static final String LOADING_SELECTOR = ".sm-loading, .offer-loading";

    /**
     * 搜索结果商品信息
     */
    public record SearchResult(
            String title,
            String price,
            String imageUrl,
            String productUrl,
            String supplier,
            String sales
    ) {}

    /**
     * 图片搜索响应
     */
    public record ImageSearchResponse(
            boolean success,
            String message,
            List<SearchResult> results,
            String screenshotPath
    ) {}

    /**
     * 使用图片URL进行1688图搜
     *
     * @param imageUrl 图片URL
     * @param contextId 浏览器上下文ID（可选，为null时创建临时上下文）
     * @return 搜索结果
     */
    public ImageSearchResponse searchByImageUrl(String imageUrl, String contextId) {
        log.info("Starting 1688 image search with URL: {}", imageUrl);
        
        BrowserManager.TemporaryContext tempContext = null;
        Page page = null;
        String screenshotPath = null;
        
        try {
            // 创建或获取上下文
            if (contextId != null && !contextId.isEmpty()) {
                page = browserManager.newPage(contextId);
            } else {
                tempContext = browserManager.createTemporaryContext();
                page = tempContext.page();
            }

            // 访问1688首页
            page.navigate(IMAGE_SEARCH_URL, new Page.NavigateOptions()
                    .setTimeout(properties.getTimeout())
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            
            log.info("1688 homepage loaded");

            // 点击"图片搜索"入口
            clickImageSearchEntry(page);

            // 上传图片
            uploadImage(page, imageUrl);

            // 等待搜索结果
            waitForResults(page);

            // 提取商品列表
            List<SearchResult> results = extractProductList(page);

            // 保存截图
            screenshotPath = saveScreenshot(page, "1688-search-result");

            log.info("1688 image search completed. Found {} results", results.size());
            
            return new ImageSearchResponse(true, "Search completed successfully", results, screenshotPath);

        } catch (Exception e) {
            log.error("1688 image search failed", e);
            screenshotPath = saveScreenshot(page, "1688-search-error");
            return new ImageSearchResponse(false, "Search failed: " + e.getMessage(), 
                    new ArrayList<>(), screenshotPath);
        } finally {
            // 关闭临时上下文
            if (tempContext != null) {
                tempContext.close();
            }
        }
    }

    /**
     * 使用本地图片文件进行1688图搜
     *
     * @param imagePath 本地图片路径
     * @return 搜索结果
     */
    public ImageSearchResponse searchByImageFile(String imagePath) {
        log.info("Starting 1688 image search with local file: {}", imagePath);
        
        BrowserManager.TemporaryContext tempContext = null;
        Page page = null;
        String screenshotPath = null;
        
        try {
            tempContext = browserManager.createTemporaryContext();
            page = tempContext.page();

            // 访问1688首页
            page.navigate(IMAGE_SEARCH_URL, new Page.NavigateOptions()
                    .setTimeout(properties.getTimeout())
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // 点击"图片搜索"入口
            clickImageSearchEntry(page);

            // 上传本地图片
            uploadLocalImage(page, imagePath);

            // 等待搜索结果
            waitForResults(page);

            // 提取商品列表
            List<SearchResult> results = extractProductList(page);

            // 保存截图
            screenshotPath = saveScreenshot(page, "1688-search-result");

            return new ImageSearchResponse(true, "Search completed successfully", results, screenshotPath);

        } catch (Exception e) {
            log.error("1688 image search failed", e);
            if (page != null) {
                screenshotPath = saveScreenshot(page, "1688-search-error");
            }
            return new ImageSearchResponse(false, "Search failed: " + e.getMessage(), 
                    new ArrayList<>(), screenshotPath);
        } finally {
            if (tempContext != null) {
                tempContext.close();
            }
        }
    }

    /**
     * 点击图片搜索入口
     */
    private void clickImageSearchEntry(Page page) {
        try {
            // 尝试多种方式找到图搜入口
            String[] selectors = {
                "//*[contains(text(),'图片搜索')]",
                "//*[contains(@class,'image-search')]",
                "//*[contains(@class,'tuwen')]//a",
                ".sm-search-tab img[src*='tu']",
                "[class*='search-icon']",
                "//button[contains(@class,'search')]"
            };
            
            for (String selector : selectors) {
                try {
                    if (selector.startsWith("//")) {
                        Locator locator = page.locator(selector);
                        if (locator.count() > 0) {
                            locator.first().click();
                            log.info("Clicked image search entry with selector: {}", selector);
                            page.waitForTimeout(1000);
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // 如果找不到入口，尝试直接访问图搜页面
            log.info("Image search entry not found, trying direct URL");
            page.navigate("https://s.1688.com/youyuan/index.htm?tab=imageSearch&imageType=url", 
                    new Page.NavigateOptions().setTimeout(properties.getTimeout()));
            
        } catch (Exception e) {
            log.warn("Failed to click image search entry: {}", e.getMessage());
            throw new RuntimeException("Cannot find image search entry on 1688", e);
        }
    }

    /**
     * 上传图片（通过URL下载后上传）
     */
    private void uploadImage(Page page, String imageUrl) {
        try {
            // 找到文件上传输入框
            Locator fileInput = page.locator(IMAGE_UPLOAD_SELECTOR).first();
            
            // 如果有URL输入框，先输入URL
            Locator urlInput = page.locator("input[type='text'][placeholder*='图片']").first();
            if (urlInput.isVisible()) {
                urlInput.fill(imageUrl);
                log.info("Filled image URL");
                
                // 点击搜索按钮
                Locator searchBtn = page.locator("button:has-text('搜索'), button:has-text('找相似')").first();
                if (searchBtn.isVisible()) {
                    searchBtn.click();
                    log.info("Clicked search button");
                }
            } else {
                // 直接设置input的值（需要先将URL下载为文件）
                String tempFile = downloadImageToTemp(imageUrl);
                if (tempFile != null) {
                    fileInput.setInputFiles(Paths.get(tempFile));
                    log.info("Uploaded image file");
                }
            }
            
            page.waitForTimeout(2000);
            
        } catch (Exception e) {
            log.error("Failed to upload image", e);
            throw new RuntimeException("Image upload failed", e);
        }
    }

    /**
     * 上传本地图片
     */
    private void uploadLocalImage(Page page, String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            if (!Files.exists(path)) {
                throw new RuntimeException("Image file not found: " + imagePath);
            }

            Locator fileInput = page.locator(IMAGE_UPLOAD_SELECTOR).first();
            fileInput.setInputFiles(path);
            log.info("Uploaded local image: {}", imagePath);
            
            page.waitForTimeout(2000);
            
        } catch (Exception e) {
            log.error("Failed to upload local image", e);
            throw new RuntimeException("Local image upload failed", e);
        }
    }

    /**
     * 等待搜索结果加载
     */
    private void waitForResults(Page page) {
        try {
            // 等待加载动画消失或商品列表出现
            page.waitForSelector(PRODUCT_ITEM_SELECTOR, new Page.WaitForSelectorOptions()
                    .setTimeout(properties.getTimeout())
                    .setState(WaitForSelectorState.VISIBLE));
            
            // 额外等待确保内容完全加载
            page.waitForTimeout(2000);
            
            log.info("Search results loaded");
            
        } catch (Exception e) {
            log.warn("Timeout waiting for search results: {}", e.getMessage());
            // 不抛出异常，继续尝试提取内容
        }
    }

    /**
     * 提取商品列表
     */
    private List<SearchResult> extractProductList(Page page) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            // 尝试多种商品列表选择器
            String[] selectors = {
                ".offer-list .offer-item",
                ".sm-offer-item",
                "[class*='offer-item']",
                ".result-list .item",
                "[data*='offer']"
            };
            
            Locator productLocator = null;
            for (String selector : selectors) {
                Locator temp = page.locator(selector);
                if (temp.count() > 0) {
                    productLocator = temp;
                    log.info("Found product list with selector: {}, count: {}", selector, temp.count());
                    break;
                }
            }
            
            if (productLocator == null) {
                log.warn("No product list found on page");
                return results;
            }
            
            int count = Math.min(productLocator.count(), 20); // 最多取20个
            for (int i = 0; i < count; i++) {
                try {
                    Locator item = productLocator.nth(i);
                    
                    String title = extractText(item, "[class*='title'], [class*='name'], .sm-offer-title");
                    String price = extractText(item, "[class*='price'], .sm-offer-price");
                    String imageUrl = extractImage(item, "img[class*='image'], img[class*='photo']");
                    String productUrl = extractLink(item, "a[href*='detail'], a[href*='product']");
                    String supplier = extractText(item, "[class*='company'], [class*='supply']");
                    String sales = extractText(item, "[class*='sale'], [class*='deal']");
                    
                    if (title != null && !title.isEmpty()) {
                        results.add(new SearchResult(
                                cleanText(title),
                                cleanPrice(price),
                                imageUrl,
                                productUrl,
                                cleanText(supplier),
                                cleanSales(sales)
                        ));
                    }
                } catch (Exception e) {
                    log.debug("Failed to extract product {}: {}", i, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to extract product list", e);
        }
        
        return results;
    }

    /**
     * 提取文本
     */
    private String extractText(Locator parent, String selector) {
        try {
            Locator loc = parent.locator(selector).first();
            if (loc.isVisible()) {
                return loc.textContent();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 提取图片URL
     */
    private String extractImage(Locator parent, String selector) {
        try {
            Locator loc = parent.locator(selector).first();
            if (loc.isVisible()) {
                return loc.getAttribute("src");
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 提取链接
     */
    private String extractLink(Locator parent, String selector) {
        try {
            Locator loc = parent.locator(selector).first();
            if (loc.isVisible()) {
                return loc.getAttribute("href");
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 清理文本
     */
    private String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * 清理价格
     */
    private String cleanPrice(String price) {
        if (price == null) return "";
        // 提取数字和货币符号
        Pattern pattern = Pattern.compile("[¥€£]?\\d+\\.?\\d*");
        Matcher matcher = pattern.matcher(price);
        if (matcher.find()) {
            return matcher.group();
        }
        return cleanText(price);
    }

    /**
     * 清理销量
     */
    private String cleanSales(String sales) {
        if (sales == null) return "";
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(sales);
        if (matcher.find()) {
            return matcher.group() + "件";
        }
        return cleanText(sales);
    }

    /**
     * 下载图片到临时文件
     */
    private String downloadImageToTemp(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            Path tempFile = Files.createTempFile("1688-search-", ".jpg");
            
            try (java.io.InputStream in = url.openStream();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile.toFile())) {
                in.transferTo(out);
            }
            
            return tempFile.toAbsolutePath().toString();
        } catch (Exception e) {
            log.error("Failed to download image: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 保存截图
     */
    private String saveScreenshot(Page page, String prefix) {
        if (page == null) return null;
        
        try {
            Path screenshotDir = Paths.get(properties.getScreenshotDir());
            if (!screenshotDir.isAbsolute()) {
                screenshotDir = Paths.get(System.getProperty("user.dir"), properties.getScreenshotDir());
            }
            if (!screenshotDir.toFile().exists()) {
                screenshotDir.toFile().mkdirs();
            }
            
            String filename = prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            Path screenshotPath = screenshotDir.resolve(filename);
            
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setFullPage(false));
            
            log.info("Screenshot saved: {}", screenshotPath);
            return screenshotPath.toString();
            
        } catch (Exception e) {
            log.warn("Failed to save screenshot: {}", e.getMessage());
            return null;
        }
    }
}
