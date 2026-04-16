package vip.mate.service.cloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.client.*;

import java.util.List;
import java.util.Map;

/**
 * 云端店铺服务
 * 封装店铺相关的业务逻辑
 *
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudStoreService {

    private final CloudStoreClient storeClient;
    private final CloudAuthService authService;

    /**
     * 获取店铺列表
     */
    public List<CloudStoreClient.StoreInfo> getStores() {
        String token = authService.getValidToken();
        ApiResult<List<CloudStoreClient.StoreInfo>> result = storeClient.getStores(token);

        if (!result.isSuccess()) {
            log.error("获取店铺列表失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "GET_STORES_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 获取店铺详情
     */
    public CloudStoreClient.StoreInfo getStore(Long storeId) {
        String token = authService.getValidToken();
        ApiResult<CloudStoreClient.StoreInfo> result = storeClient.getStore(token, storeId);

        if (!result.isSuccess()) {
            log.error("获取店铺详情失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "GET_STORE_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 创建店铺
     */
    public CloudStoreClient.StoreInfo createStore(String name, String type,
            Map<String, Object> credentials, Map<String, Object> settings) {
        String token = authService.getValidToken();

        CloudStoreClient.StoreCreateRequest request = new CloudStoreClient.StoreCreateRequest();
        request.setName(name);
        request.setType(type);
        request.setCredentials(credentials);
        request.setSettings(settings);

        ApiResult<CloudStoreClient.StoreInfo> result = storeClient.createStore(token, request);

        if (!result.isSuccess()) {
            log.error("创建店铺失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "CREATE_STORE_FAILED", result.getMessage());
        }

        log.info("店铺创建成功: {}", name);
        return result.getData();
    }

    /**
     * 更新店铺
     */
    public CloudStoreClient.StoreInfo updateStore(Long storeId, String name,
            Map<String, Object> credentials, Map<String, Object> settings) {
        String token = authService.getValidToken();

        CloudStoreClient.StoreUpdateRequest request = new CloudStoreClient.StoreUpdateRequest();
        request.setName(name);
        request.setCredentials(credentials);
        request.setSettings(settings);

        ApiResult<CloudStoreClient.StoreInfo> result = storeClient.updateStore(token, storeId, request);

        if (!result.isSuccess()) {
            log.error("更新店铺失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "UPDATE_STORE_FAILED", result.getMessage());
        }

        log.info("店铺更新成功: {}", storeId);
        return result.getData();
    }

    /**
     * 删除店铺
     */
    public void deleteStore(Long storeId) {
        String token = authService.getValidToken();
        ApiResult<Void> result = storeClient.deleteStore(token, storeId);

        if (!result.isSuccess()) {
            log.error("删除店铺失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "DELETE_STORE_FAILED", result.getMessage());
        }

        log.info("店铺删除成功: {}", storeId);
    }

    /**
     * 绑定Ozon店铺
     */
    public CloudStoreClient.StoreInfo bindOzonStore(Long storeId, String clientId, String apiKey, String name) {
        String token = authService.getValidToken();

        CloudStoreClient.OzonBindRequest request = new CloudStoreClient.OzonBindRequest();
        request.setStoreId(storeId);
        request.setClientId(clientId);
        request.setApiKey(apiKey);
        request.setName(name);

        ApiResult<CloudStoreClient.StoreInfo> result = storeClient.bindOzonStore(token, request);

        if (!result.isSuccess()) {
            log.error("绑定Ozon店铺失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "BIND_OZON_FAILED", result.getMessage());
        }

        log.info("Ozon店铺绑定成功: {}", name);
        return result.getData();
    }

    /**
     * 获取Ozon凭证
     */
    public CloudStoreClient.OzonCredentials getOzonCredentials(Long storeId) {
        String token = authService.getValidToken();
        ApiResult<CloudStoreClient.OzonCredentials> result = storeClient.getOzonCredentials(token, storeId);

        if (!result.isSuccess()) {
            log.error("获取Ozon凭证失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "GET_OZON_CREDS_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 刷新Ozon凭证
     */
    public CloudStoreClient.OzonCredentials refreshOzonCredentials(Long storeId) {
        String token = authService.getValidToken();
        ApiResult<CloudStoreClient.OzonCredentials> result = storeClient.refreshOzonCredentials(token, storeId);

        if (!result.isSuccess()) {
            log.error("刷新Ozon凭证失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "REFRESH_OZON_CREDS_FAILED", result.getMessage());
        }

        log.info("Ozon凭证刷新成功");
        return result.getData();
    }

    /**
     * 根据类型获取店铺
     */
    public List<CloudStoreClient.StoreInfo> getStoresByType(String type) {
        List<CloudStoreClient.StoreInfo> allStores = getStores();
        return allStores.stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .toList();
    }

    /**
     * 获取活跃店铺
     */
    public List<CloudStoreClient.StoreInfo> getActiveStores() {
        List<CloudStoreClient.StoreInfo> allStores = getStores();
        return allStores.stream()
                .filter(s -> "active".equalsIgnoreCase(s.getStatus()))
                .toList();
    }
}
