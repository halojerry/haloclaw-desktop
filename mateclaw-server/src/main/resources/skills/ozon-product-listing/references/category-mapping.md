# Ozon API 使用指南

## 认证配置

```bash
export OZON_CLIENT_ID="your_client_id"
export OZON_API_KEY="your_api_key"
```

## 可用API端点

### 产品相关

| 方法 | 端点 | 用途 |
|------|------|------|
| POST | /v2/product/list | 获取产品列表 |
| POST | /v3/product/import | 批量导入产品 |
| POST | /v3/product/info/list | 获取产品信息 |
| POST | /v1/product/import/info | 获取导入任务状态 |

### 图片相关

| 方法 | 端点 | 用途 |
|------|------|------|
| POST | /v1/product/pictures/import | 上传产品图片 |
| POST | /v1/product/attributes/update | 更新产品属性(含图片) |

### 库存相关

| 方法 | 端点 | 用途 |
|------|------|------|
| POST | /v2/products/stocks | 更新库存 |

### 类目相关

| 方法 | 端点 | 状态 |
|------|------|------|
| POST | /v1/description-category/tree | 获取类目树 ✅ |
| POST | /v2/category/tree | 获取类目树 ❌ (需IP白名单) |
| POST | /v3/category/attribute | 获取类目属性 ❌ (需IP白名单) |

## IP白名单

**错误表现**: 所有类目API返回404

**解决**: 在Ozon Seller后台设置 → API设置 → 添加服务器IP到白名单

## 常见错误码

| 错误码 | 含义 | 解决方案 |
|--------|------|----------|
| PRODUCT_HAS_NOT_BEEN_TAGGED_YET | 缺少尺寸重量 | 填写weight/depth/width/height |
| description_category_is_empty | 类目为空 | 检查description_category_id |
| currency_differs_from_contract | 货币错误 | 使用CNY |
| INVALID_VALUE | 属性值无效 | 检查attributes格式 |
| LEVELS_CATEGORY_NOT_FOUND | 类目层级错误 | 检查type_id |

## 请求示例

```bash
curl -X POST "https://api-seller.ozon.ru/v3/product/import" \
  -H "Client-Id: ${OZON_CLIENT_ID}" \
  -H "Api-Key: ${OZON_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{
      "name": "产品名称",
      "offer_id": "BO-001",
      "description_category_id": 17028733,
      "type_id": 90704,
      "price": "45",
      "currency_code": "CNY",
      "weight": "200",
      "weight_unit": "g"
    }]
  }'
```
