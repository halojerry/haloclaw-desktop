package vip.mate.service.cloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.client.*;

import java.util.List;
import java.util.Map;

/**
 * 云端商品服务
 * 封装商品相关的业务逻辑
 *
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudProductService {

    private final CloudProductClient productClient;
    private final CloudAuthService authService;

    /**
     * 获取商品列表
     */
    public CloudProductClient.PagedResult<CloudProductClient.ProductInfo> getProducts(
            Long storeId, String status, String keyword, int page, int pageSize) {

        String token = authService.getValidToken();

        CloudProductClient.ProductQuery query = new CloudProductClient.ProductQuery();
        query.setStoreId(storeId);
        query.setStatus(status);
        query.setKeyword(keyword);
        query.setPage(page);
        query.setPageSize(pageSize);

        ApiResult<CloudProductClient.PagedResult<CloudProductClient.ProductInfo>> result =
                productClient.getProducts(token, query);

        if (!result.isSuccess()) {
            log.error("获取商品列表失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "GET_PRODUCTS_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 获取商品详情
     */
    public CloudProductClient.ProductInfo getProduct(Long productId) {
        String token = authService.getValidToken();
        ApiResult<CloudProductClient.ProductInfo> result = productClient.getProduct(token, productId);

        if (!result.isSuccess()) {
            log.error("获取商品详情失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "GET_PRODUCT_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 同步商品到云端
     */
    public CloudProductClient.ProductSyncResult syncProduct(Long storeId, String sku, String name,
            String description, Double price, String currency, List<String> images,
            Map<String, Object> attributes) {

        String token = authService.getValidToken();

        CloudProductClient.ProductSyncRequest request = new CloudProductClient.ProductSyncRequest();
        request.setStoreId(storeId);
        request.setSku(sku);
        request.setName(name);
        request.setDescription(description);
        request.setPrice(price);
        request.setCurrency(currency);
        request.setImages(images);
        request.setAttributes(attributes);

        ApiResult<CloudProductClient.ProductSyncResult> result = productClient.syncProduct(token, request);

        if (!result.isSuccess()) {
            log.error("同步商品失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "SYNC_PRODUCT_FAILED", result.getMessage());
        }

        CloudProductClient.ProductSyncResult syncResult = result.getData();
        if (syncResult.isSuccess()) {
            log.info("商品同步成功: SKU={}", sku);
        } else {
            log.warn("商品同步失败: SKU={}, 原因={}", sku, syncResult.getMessage());
        }

        return syncResult;
    }

    /**
     * 批量同步商品
     */
    public List<CloudProductClient.ProductSyncResult> batchSyncProducts(
            List<CloudProductClient.ProductSyncRequest> requests) {

        String token = authService.getValidToken();
        ApiResult<List<CloudProductClient.ProductSyncResult>> result =
                productClient.batchSyncProducts(token, requests);

        if (!result.isSuccess()) {
            log.error("批量同步商品失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "BATCH_SYNC_FAILED", result.getMessage());
        }

        log.info("批量同步完成，成功: {}, 失败: {}",
                result.getData().stream().filter(CloudProductClient.ProductSyncResult::isSuccess).count(),
                result.getData().stream().filter(r -> !r.isSuccess()).count());

        return result.getData();
    }

    /**
     * 发布商品到Ozon
     */
    public CloudProductClient.PublishResult publishToOzon(Long productId, Long storeId) {
        String token = authService.getValidToken();
        ApiResult<CloudProductClient.PublishResult> result =
                productClient.publishToOzon(token, productId, storeId);

        if (!result.isSuccess()) {
            log.error("发布商品到Ozon失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "PUBLISH_FAILED", result.getMessage());
        }

        CloudProductClient.PublishResult publishResult = result.getData();
        log.info("商品发布任务已创建: taskId={}", publishResult.getTaskId());

        return publishResult;
    }

    /**
     * 批量发布商品到Ozon
     */
    public List<CloudProductClient.PublishResult> batchPublishToOzon(List<Long> productIds, Long storeId) {
        String token = authService.getValidToken();
        ApiResult<List<CloudProductClient.PublishResult>> result =
                productClient.batchPublishToOzon(token, productIds, storeId);

        if (!result.isSuccess()) {
            log.error("批量发布商品失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "BATCH_PUBLISH_FAILED", result.getMessage());
        }

        log.info("批量发布任务已创建，数量: {}", productIds.size());
        return result.getData();
    }

    /**
     * 获取发布状态
     */
    public CloudProductClient.PublishResult getPublishStatus(String taskId) {
        String token = authService.getValidToken();
        ApiResult<CloudProductClient.PublishResult> result = productClient.getPublishStatus(token, taskId);

        if (!result.isSuccess()) {
            log.error("获取发布状态失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "GET_STATUS_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 删除商品
     */
    public void deleteProduct(Long productId) {
        String token = authService.getValidToken();
        ApiResult<Void> result = productClient.deleteProduct(token, productId);

        if (!result.isSuccess()) {
            log.error("删除商品失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "DELETE_PRODUCT_FAILED", result.getMessage());
        }

        log.info("商品删除成功: {}", productId);
    }

    /**
     * 根据店铺获取商品
     */
    public CloudProductClient.PagedResult<CloudProductClient.ProductInfo> getProductsByStore(
            Long storeId, int page, int pageSize) {
        return getProducts(storeId, null, null, page, pageSize);
    }

    /**
     * 根据状态获取商品
     */
    public CloudProductClient.PagedResult<CloudProductClient.ProductInfo> getProductsByStatus(
            String status, int page, int pageSize) {
        return getProducts(null, status, null, page, pageSize);
    }
}
