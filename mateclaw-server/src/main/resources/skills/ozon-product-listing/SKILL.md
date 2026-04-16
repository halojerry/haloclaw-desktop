---
name: ozon-product-listing
description: "Ozon产品上架技能。触发词：/上架、/ozon上架、发布商品。功能：将选品池中的产品上架到Ozon店铺，包含图片生成、富文本生成、定价计算、API批量上架全流程。调用n8n工作流或直接调用Ozon API。"
dependencies:
  commands:
    - python3
  tools:
    - skillFileTool
    - skillScriptTool
platforms:
  - macos
  - linux
  - windows
---

> **Important:** All `scripts/` paths are relative to this skill directory.
> Use `run_skill_script` tool to execute scripts, or run with: `cd {this_skill_dir} && python scripts/...`

# Ozon产品上架

## 概述

本技能用于将产品从选品池上架到Ozon店铺，包含完整的图片生成、富文本生成、定价计算和批量上架功能。

## 上架流程

```
选品池产品 → 图片生成 → 富文本生成 → 定价计算 → 批量上架 → 状态确认
```

## 数据准备

### 必填字段

| 字段 | 说明 | 示例 |
|------|------|------|
| offer_id | 商品唯一标识 | BO-001 |
| name | 产品名称（俄语） | Тёплые вязаные перчатки |
| description_category_id | 类目ID | 17028733 |
| type_id | 类型ID | 90704 |
| price | 价格(CNY) | 45.00 |
| weight | 重量(g) | 150 |
| depth/width/height | 尺寸(mm) | 200/100/50 |
| images | 图片URL数组 | ["url1", "url2"] |
| attributes | 属性列表 | 见下方 |

### 必填属性

```python
# 品牌属性 (id: 4180)
brand_attr = {
    "id": 4180,
    "values": [{"dictionary_value_id": 970896248, "value": "Нет бренда"}]
}

# 模型属性 (id: 9048) - 产品名称作为模型值
model_attr = {
    "id": 9048,
    "values": [{"value": "产品名称"}]
}
```

## 上架方式

### 1. n8n工作流调用（推荐）

**工作流名称**: `WF-Auto-Listing`

```bash
curl -X POST "http://localhost:5678/webhook/ozon-listing" \
  -H "Content-Type: application/json" \
  -d '{
    "product_ids": [1, 2, 3],
    "images": ["生成好的图片URL数组"],
    "description": "富文本描述"
  }'
```

### 2. 使用MCP工具调用

> 请使用以下MCP工具：
> - `ozon_category`: 获取产品类目信息
> - `ozon_pricing`: 计算产品定价
> - `ozon_listing`: 执行产品上架

```bash
# 调用示例
call_tool("ozon_listing", {
    "offer_id": "BO-001",
    "name": "产品名称",
    "category_id": 17028733,
    "type_id": 90704,
    "price": 45.00,
    "weight": 150,
    "dimensions": {"depth": 200, "width": 100, "height": 50},
    "images": ["url1", "url2"]
})
```

### 3. 图片上传

## 定价计算

### 费用构成

| 费用项 | 计算方式 |
|--------|----------|
| 采购价 | 1688采购成本 (CNY) |
| 物流费 | 头程+尾程 (CNY) |
| 平台佣金 | 销售价 × 佣金率 (通常15-20%) |
| 利润率 | (销售价 - 成本 - 佣金) / 销售价 |

### 定价公式

> ⚠️ **定价计算已迁移至MCP工具**
> 请使用 `ozon_pricing` 工具进行定价计算。

### 富文本生成

### n8n工作流调用

```bash
curl -X POST "http://localhost:5678/webhook/ozon-rich-text" \
  -H "Content-Type: application/json" \
  -d '{
    "product_name": "Тёплые вязаные перчатки",
    "features": ["Материал: шерсть", "Размер: M/L/XL", "Цвет: черный/бежевый"],
    "description": "产品描述"
  }'
```

## Supabase存储

### listing_records表

```sql
CREATE TABLE listing_records (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) UNIQUE,
    product_pool_id BIGINT REFERENCES product_pool(id),
    ozon_product_id VARCHAR(50),
    task_id BIGINT,
    name VARCHAR(500),
    name_ru VARCHAR(500),
    category_id BIGINT,
    price_cny DECIMAL(10,2),
    price_rub DECIMAL(10,2),
    weight_g INT,
    dimensions JSONB,
    images JSONB,
    rich_text TEXT,
    listing_status VARCHAR(20),  -- pending/importing/active/error
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

## 状态监控

### 查询任务状态

```python
def check_listing_status(task_id):
    """检查上架任务状态"""
    response = requests.post(
        "https://api-seller.ozon.ru/v1/product/import/info",
        headers={
            "Client-Id": OZON_CLIENT_ID,
            "Api-Key": OZON_API_KEY
        },
        json={"task_id": task_id}
    )
    return response.json()
```

### 状态判断

| 状态 | 说明 |
|------|------|
| success | 上架成功 |
| validation_error | 验证错误，需检查字段 |
| failed | 上架失败，查看error字段 |

## 常见错误

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| PRODUCT_HAS_NOT_BEEN_TAGGED_YET | 缺少尺寸重量 | 填写weight/depth/width/height |
| description_category_is_empty | 类目为空 | 检查description_category_id |
| currency_differs_from_contract | 货币错误 | 使用CNY而非RUB |
| INVALID_VALUE | 属性值无效 | 检查attributes格式 |

## 注意事项

1. **IP白名单**: 确保服务器IP在Ozon Seller后台添加到白名单
2. **请求间隔**: 每次请求间隔5秒，避免限流
3. **超时设置**: API超时设为60秒
4. **图片数量**: 最多10张图片，使用images[:10]截断
