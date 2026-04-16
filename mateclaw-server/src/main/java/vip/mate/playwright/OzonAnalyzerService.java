package vip.mate.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ozon竞品分析服务
 * <p>
 * 功能：
 * 1. 打开Ozon商品详情页
 * 2. 提取商品基本信息（标题、价格、评分、评论数、销量）
 * 3. 提取竞品推荐列表
 * 4. 截图保存
 * </p>
 *
 * @author mate
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OzonAnalyzerService {

    private final BrowserManager browserManager;
    private final PlaywrightProperties properties;

    // Ozon商品页基础URL
    private static final String OZON_PRODUCT_URL = "https://www.ozon.ru/product/";

    /**
     * 商品基本信息
     */
    public record ProductInfo(
            String productId,
            String title,
            String price,
            String originalPrice,
            String discount,
            String rating,
            String reviewCount,
            String salesCount,
            String brand,
            String seller,
            String category,
            List<String> images,
            String description
    ) {}

    /**
     * 竞品推荐项
     */
    public record RecommendedProduct(
            String productId,
            String title,
            String price,
            String imageUrl,
            String productUrl
    ) {}

    /**
     * 分析响应
     */
    public record AnalyzeResponse(
            boolean success,
            String message,
            ProductInfo productInfo,
            List<RecommendedProduct> recommendations,
            String screenshotPath,
            String pageUrl
    ) {}

    /**
     * 分析Ozon商品
     *
     * @param productId 商品ID
     * @param contextId 浏览器上下文ID（可选）
     * @return 分析结果
     */
    public AnalyzeResponse analyzeProduct(String productId, String contextId) {
        log.info("Starting Ozon product analysis for ID: {}", productId);
        
        String url = productId.startsWith("http") ? productId : OZON_PRODUCT_URL + productId;
        
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

            // 访问商品页面
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(properties.getTimeout())
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));
            
            log.info("Ozon product page loaded: {}", url);

            // 等待页面主要元素加载
            waitForPageReady(page);

            // 提取商品信息
            ProductInfo productInfo = extractProductInfo(page, productId);

            // 提取竞品推荐
            List<RecommendedProduct> recommendations = extractRecommendations(page);

            // 保存截图
            screenshotPath = saveScreenshot(page, "ozon-product-" + productId);

            log.info("Ozon product analysis completed. Product: {}, Recommendations: {}", 
                    productInfo.title(), recommendations.size());
            
            return new AnalyzeResponse(
                    true, 
                    "Analysis completed successfully", 
                    productInfo, 
                    recommendations, 
                    screenshotPath,
                    url
            );

        } catch (Exception e) {
            log.error("Ozon product analysis failed", e);
            if (page != null) {
                screenshotPath = saveScreenshot(page, "ozon-analysis-error-" + productId);
            }
            return new AnalyzeResponse(
                    false, 
                    "Analysis failed: " + e.getMessage(), 
                    null, 
                    new ArrayList<>(), 
                    screenshotPath,
                    url
            );
        } finally {
            // 关闭临时上下文
            if (tempContext != null) {
                tempContext.close();
            }
        }
    }

    /**
     * 批量分析多个商品
     *
     * @param productIds 商品ID列表
     * @return 分析结果列表
     */
    public List<AnalyzeResponse> batchAnalyze(List<String> productIds) {
        List<AnalyzeResponse> results = new ArrayList<>();
        
        for (String productId : productIds) {
            try {
                AnalyzeResponse result = analyzeProduct(productId, null);
                results.add(result);
                
                // 请求间隔，避免被限流
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("Batch analysis failed for product {}: {}", productId, e.getMessage());
                results.add(new AnalyzeResponse(
                        false, 
                        "Analysis failed: " + e.getMessage(),
                        null,
                        new ArrayList<>(),
                        null,
                        OZON_PRODUCT_URL + productId
                ));
            }
        }
        
        return results;
    }

    /**
     * 等待页面加载完成
     */
    private void waitForPageReady(Page page) {
        try {
            // 等待主要商品信息容器
            String[] selectors = {
                    "[data-widget='webProductHeading']",
                    "[data-widget='webPriceWrapper']",
                    "[data-widget='webProductGallery']",
                    ".widget-search-result-container"
            };
            
            for (String selector : selectors) {
                try {
                    Locator locator = page.locator(selector);
                    if (locator.count() > 0) {
                        locator.first().waitFor(new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                                .setTimeout(5000));
                        log.info("Page ready, found selector: {}", selector);
                        break;
                    }
                } catch (Exception ignored) {}
            }
            
            // 额外等待确保JS执行完成
            page.waitForTimeout(1500);
            
        } catch (Exception e) {
            log.warn("Wait for page ready warning: {}", e.getMessage());
        }
    }

    /**
     * 提取商品信息
     */
    private ProductInfo extractProductInfo(Page page, String productId) {
        String title = extractTitle(page);
        String price = extractPrice(page);
        String originalPrice = extractOriginalPrice(page);
        String discount = calculateDiscount(price, originalPrice);
        String rating = extractRating(page);
        String reviewCount = extractReviewCount(page);
        String salesCount = extractSalesCount(page);
        String brand = extractBrand(page);
        String seller = extractSeller(page);
        String category = extractCategory(page);
        List<String> images = extractImages(page);
        String description = extractDescription(page);
        
        return new ProductInfo(
                productId,
                title,
                price,
                originalPrice,
                discount,
                rating,
                reviewCount,
                salesCount,
                brand,
                seller,
                category,
                images,
                description
        );
    }

    /**
     * 提取商品标题
     */
    private String extractTitle(Page page) {
        try {
            // 尝试多种标题选择器
            String[] selectors = {
                    "[data-widget='webProductHeading'] h1",
                    "h1.tsHeadline550Medium",
                    "h1[class*='heading']",
                    ".product-title",
                    "h1"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    String text = locator.first().textContent();
                    if (text != null && !text.trim().isEmpty()) {
                        return text.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract title: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取价格
     */
    private String extractPrice(Page page) {
        try {
            String[] selectors = {
                    "[data-widget='webPriceWrapper'] [class*='price'] span[class*='c3015-a1']",
                    "[data-widget='webPrice'] span[class*='tsHeadline500']",
                    ".price span",
                    "[class*='price'] span[class*='tsHeadline']"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    String text = locator.first().textContent();
                    if (text != null) {
                        return extractNumber(text) + " ₽";
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract price: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取原价
     */
    private String extractOriginalPrice(Page page) {
        try {
            String[] selectors = {
                    "[data-widget='webPriceWrapper'] span[class*='strike']",
                    "[class*='original-price']",
                    "[class*='old-price']",
                    "[class*='c3015-o1']"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    String text = locator.first().textContent();
                    if (text != null) {
                        return extractNumber(text) + " ₽";
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract original price: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取评分
     */
    private String extractRating(Page page) {
        try {
            String[] selectors = {
                    "[data-widget='webReviewRating'] span[class*='tsHeadline']",
                    "[class*='rating'] [class*='tsHeadline']",
                    "[class*='stars'] + span"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    String text = locator.first().textContent();
                    if (text != null) {
                        return text.trim().replace(",", ".");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract rating: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取评论数
     */
    private String extractReviewCount(Page page) {
        try {
            String[] selectors = {
                    "[data-widget='webReviewRating'] a[class*='link'] span",
                    "[class*='review'] [class*='count']",
                    "a[href*='reviews'] span"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    String text = locator.first().textContent();
                    if (text != null) {
                        return extractNumber(text);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract review count: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取销量
     */
    private String extractSalesCount(Page page) {
        try {
            String[] selectors = {
                    "[class*='sales'] span",
                    "[class*='sold'] span",
                    "[data-widget='webOrderQuantity']"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    String text = locator.first().textContent();
                    if (text != null) {
                        return extractNumber(text);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract sales count: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取品牌
     */
    private String extractBrand(Page page) {
        try {
            String[] selectors = {
                    "[data-widget='webBrand'] a",
                    "[class*='brand'] a",
                    "[class*='manufacturer']"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    String text = locator.first().textContent();
                    if (text != null && !text.trim().isEmpty()) {
                        return text.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract brand: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取卖家
     */
    private String extractSeller(Page page) {
        try {
            String[] selectors = {
                    "[data-widget='webSeller'] a span",
                    "[class*='seller'] a",
                    "[class*='store'] a"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    String text = locator.first().textContent();
                    if (text != null && !text.trim().isEmpty()) {
                        return text.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract seller: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取分类
     */
    private String extractCategory(Page page) {
        try {
            Locator breadcrumbs = page.locator("[class*='breadcrumb'] a, [class*='Breadcrumb'] a");
            List<String> categories = new ArrayList<>();
            for (int i = 0; i < Math.min(breadcrumbs.count(), 5); i++) {
                String text = breadcrumbs.nth(i).textContent();
                if (text != null && !text.trim().isEmpty()) {
                    categories.add(text.trim());
                }
            }
            return String.join(" > ", categories);
        } catch (Exception e) {
            log.debug("Failed to extract category: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取商品图片列表
     */
    private List<String> extractImages(Page page) {
        List<String> images = new ArrayList<>();
        try {
            String[] selectors = {
                    "[data-widget='webProductGallery'] img[src*='cdn1']",
                    "[class*='gallery'] img",
                    "[class*='swiper'] img"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                for (int i = 0; i < Math.min(locator.count(), 10); i++) {
                    String src = locator.nth(i).getAttribute("src");
                    if (src != null && !src.contains("data:image")) {
                        // 替换缩略图尺寸获取高清图
                        String hiRes = src.replaceAll("/wc\\d+/", "/wc500/");
                        if (!images.contains(hiRes)) {
                            images.add(hiRes);
                        }
                    }
                }
                if (!images.isEmpty()) break;
            }
        } catch (Exception e) {
            log.debug("Failed to extract images: {}", e.getMessage());
        }
        return images;
    }

    /**
     * 提取商品描述
     */
    private String extractDescription(Page page) {
        try {
            String[] selectors = {
                    "[data-widget='webDescription']",
                    "[class*='description']",
                    "#section-description"
            };
            
            for (String selector : selectors) {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    String text = locator.first().textContent();
                    if (text != null && text.length() > 50) {
                        return text.trim().substring(0, Math.min(text.length(), 2000));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract description: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取竞品推荐
     */
    private List<RecommendedProduct> extractRecommendations(Page page) {
        List<RecommendedProduct> recommendations = new ArrayList<>();
        
        try {
            // 尝试多种推荐区域选择器
            String[] selectors = {
                    "[data-widget='webSearchWithFilters'] [data-widget='webProductCard']",
                    "[data-widget='webRecentlyViewedProducts'] [class*='product']",
                    "[class*='recommendation'] [class*='product']",
                    ".also-viewed [class*='item']"
            };
            
            for (String selector : selectors) {
                Locator items = page.locator(selector);
                if (items.count() > 0) {
                    int count = Math.min(items.count(), 10);
                    for (int i = 0; i < count; i++) {
                        try {
                            Locator item = items.nth(i);
                            
                            String title = extractRecTitle(item);
                            String price = extractRecPrice(item);
                            String imageUrl = extractRecImage(item);
                            String recProductUrl = extractRecLink(item);
                            String recProductId = extractProductIdFromUrl(recProductUrl);
                            
                            if (title != null && !title.isEmpty()) {
                                recommendations.add(new RecommendedProduct(
                                        recProductId,
                                        title,
                                        price,
                                        imageUrl,
                                        recProductUrl
                                ));
                            }
                        } catch (Exception ignored) {}
                    }
                    if (!recommendations.isEmpty()) break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract recommendations: {}", e.getMessage());
        }
        
        return recommendations;
    }

    private String extractRecTitle(Locator item) {
        try {
            String[] selectors = {"span[class*='title'], a[class*='name'], [class*='product-title']"};
            for (String s : selectors) {
                Locator loc = item.locator(s);
                if (loc.count() > 0) return loc.first().textContent().trim();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String extractRecPrice(Locator item) {
        try {
            String[] selectors = {"[class*='price'] span", "[class*='cost']"};
            for (String s : selectors) {
                Locator loc = item.locator(s);
                if (loc.count() > 0) {
                    String text = loc.first().textContent();
                    if (text != null) return extractNumber(text);
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String extractRecImage(Locator item) {
        try {
            Locator img = item.locator("img").first();
            return img.getAttribute("src");
        } catch (Exception ignored) {}
        return "";
    }

    private String extractRecLink(Locator item) {
        try {
            Locator link = item.locator("a[href*='/product/']").first();
            return "https://www.ozon.ru" + link.getAttribute("href");
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * 从URL提取商品ID
     */
    private String extractProductIdFromUrl(String url) {
        if (url == null) return "";
        Pattern pattern = Pattern.compile("/product/([^/?]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 计算折扣
     */
    private String calculateDiscount(String price, String originalPrice) {
        if (price.isEmpty() || originalPrice.isEmpty()) return "";
        try {
            double current = Double.parseDouble(extractNumber(price));
            double original = Double.parseDouble(extractNumber(originalPrice));
            if (original > 0) {
                int discount = (int) ((1 - current / original) * 100);
                return discount > 0 ? "-" + discount + "%" : "";
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * 提取数字
     */
    private String extractNumber(String text) {
        if (text == null) return "";
        Pattern pattern = Pattern.compile("\\d+(?:[.,]\\d+)?");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group().replace(",", ".");
        }
        return "";
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
