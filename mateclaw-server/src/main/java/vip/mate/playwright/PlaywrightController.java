package vip.mate.playwright;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Playwright API控制器
 * <p>
 * 提供浏览器自动化操作的REST API接口：
 * 1. 1688图片搜索
 * 2. Ozon商品分析
 * 3. 浏览器状态查询
 * </p>
 *
 * @author mate
 */
@Slf4j
@RestController
@RequestMapping("/api/playwright")
@RequiredArgsConstructor
@Tag(name = "Playwright自动化", description = "浏览器自动化操作接口")
public class PlaywrightController {

    private final BrowserManager browserManager;
    private final ImageSearch1688Service imageSearch1688Service;
    private final OzonAnalyzerService ozonAnalyzerService;
    private final PlaywrightProperties properties;

    /**
     * 获取浏览器状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取浏览器状态", description = "查询Playwright浏览器实例状态")
    public ResponseEntity<BrowserManager.BrowserStatus> getStatus() {
        log.info("Getting browser status");
        return ResponseEntity.ok(browserManager.getStatus());
    }

    /**
     * 1688图片搜索 - 通过图片URL
     */
    @PostMapping("/image-search")
    @Operation(summary = "1688图片搜索", description = "使用图片URL在1688平台搜索同款商品")
    public ResponseEntity<ImageSearch1688Service.ImageSearchResponse> imageSearch(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam(value = "contextId", required = false) String contextId) {
        
        log.info("1688 image search request: imageUrl={}, contextId={}", imageUrl, contextId);
        
        if (!browserManager.isAvailable()) {
            return ResponseEntity.status(503)
                    .body(new ImageSearch1688Service.ImageSearchResponse(
                            false, "Browser not available. Please check Playwright initialization.", 
                            List.of(), null));
        }
        
        ImageSearch1688Service.ImageSearchResponse response = 
                imageSearch1688Service.searchByImageUrl(imageUrl, contextId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 1688图片搜索 - 通过上传图片文件
     */
    @PostMapping(value = "/image-search/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "1688图片搜索(上传)", description = "上传本地图片文件进行1688图搜")
    public ResponseEntity<ImageSearch1688Service.ImageSearchResponse> imageSearchUpload(
            @RequestParam("file") MultipartFile file) {
        
        log.info("1688 image search upload request: filename={}", file.getOriginalFilename());
        
        if (!browserManager.isAvailable()) {
            return ResponseEntity.status(503)
                    .body(new ImageSearch1688Service.ImageSearchResponse(
                            false, "Browser not available", List.of(), null));
        }
        
        // 保存上传的文件
        Path uploadDir = Paths.get(properties.getScreenshotDir(), "uploads");
        try {
            if (!uploadDir.toFile().exists()) {
                uploadDir.toFile().mkdirs();
            }
            
            String ext = getFileExtension(file.getOriginalFilename());
            String tempFile = uploadDir.resolve(UUID.randomUUID().toString() + ext).toString();
            file.transferTo(Paths.get(tempFile));
            
            // 执行搜索
            ImageSearch1688Service.ImageSearchResponse response = 
                    imageSearch1688Service.searchByImageFile(tempFile);
            
            // 清理临时文件
            Files.deleteIfExists(Paths.get(tempFile));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Image upload search failed", e);
            return ResponseEntity.internalServerError()
                    .body(new ImageSearch1688Service.ImageSearchResponse(
                            false, "Upload failed: " + e.getMessage(), List.of(), null));
        }
    }

    /**
     * Ozon商品分析
     */
    @PostMapping("/ozon-analyze")
    @Operation(summary = "Ozon商品分析", description = "分析Ozon商品页面，提取商品信息和竞品推荐")
    public ResponseEntity<OzonAnalyzerService.AnalyzeResponse> analyzeOzon(
            @RequestParam("productId") String productId,
            @RequestParam(value = "contextId", required = false) String contextId) {
        
        log.info("Ozon analyze request: productId={}, contextId={}", productId, contextId);
        
        if (!browserManager.isAvailable()) {
            return ResponseEntity.status(503)
                    .body(new OzonAnalyzerService.AnalyzeResponse(
                            false, "Browser not available", null, List.of(), null, null));
        }
        
        OzonAnalyzerService.AnalyzeResponse response = 
                ozonAnalyzerService.analyzeProduct(productId, contextId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 批量Ozon商品分析
     */
    @PostMapping("/ozon-analyze/batch")
    @Operation(summary = "批量Ozon商品分析", description = "批量分析多个Ozon商品")
    public ResponseEntity<Map<String, Object>> batchAnalyzeOzon(
            @RequestBody List<String> productIds) {
        
        log.info("Batch Ozon analyze request: count={}", productIds.size());
        
        if (!browserManager.isAvailable()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Browser not available");
            return ResponseEntity.status(503).body(error);
        }
        
        List<OzonAnalyzerService.AnalyzeResponse> responses = 
                ozonAnalyzerService.batchAnalyze(productIds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("total", productIds.size());
        result.put("results", responses);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 释放浏览器上下文
     */
    @DeleteMapping("/context/{contextId}")
    @Operation(summary = "释放浏览器上下文", description = "释放指定的浏览器上下文")
    public ResponseEntity<Map<String, Object>> releaseContext(
            @PathVariable String contextId) {
        
        log.info("Release context request: contextId={}", contextId);
        
        browserManager.releaseContext(contextId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("contextId", contextId);
        result.put("message", "Context released");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 创建新浏览器上下文
     */
    @PostMapping("/context")
    @Operation(summary = "创建浏览器上下文", description = "创建新的持久化浏览器上下文")
    public ResponseEntity<Map<String, Object>> createContext() {
        log.info("Create context request");
        
        String contextId = "ctx-" + UUID.randomUUID().toString().substring(0, 8);
        
        try {
            browserManager.getContext(contextId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("contextId", contextId);
            result.put("message", "Context created");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to create context", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Failed to create context: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) return ".jpg";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".jpg";
    }
}
