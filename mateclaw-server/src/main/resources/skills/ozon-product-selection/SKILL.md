---
name: ozon-product-selection
description: "Ozon蓝海产品选品技能。触发词：/选品、/ozon选品、找产品。功能：根据关键词或类目搜索Ozon热销产品，分析竞争度、毛利率、上架时间，筛选蓝海产品存入选品池。调用n8n工作流或服务器API获取数据。"
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

# Ozon蓝海产品选品

## 概述

本技能用于在Ozon平台上进行蓝海产品选品，通过分析产品竞争度、毛利率、上架时间等指标，筛选出具有潜力的蓝海产品。

## 数据源与调用方式

### 1. n8n工作流调用（推荐）

**工作流名称**: `WF-Daily-Selection`

**触发方式**: 调用n8n Webhook
```bash
curl -X POST "http://localhost:5678/webhook/ozon-selection" \
  -H "Content-Type: application/json" \
  -d '{"keywords": ["关键词1", "关键词2"], "min_margin": 25, "category": "类目ID"}'
```

**返回数据**:
```json
{
  "products": [
    {
      "ozon_product_id": "123456789",
      "name": "产品名称",
      "category": "类目",
      "price_rub": 1500,
      "competitors_count": 15,
      "avg_rating": 4.5,
      "first_review_date": "2024-01-15",
      "estimated_margin": 35,
      "recommendation": "蓝海/红海/一般"
    }
  ],
  "summary": {
    "total_analyzed": 100,
    "blue_ocean_count": 12,
    "generated_at": "2024-04-18T10:00:00Z"
  }
}
```

### 2. 服务器API调用

**端点**: `GET /api/ozon/selection`

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keywords | string[] | 是 | 搜索关键词数组 |
| min_margin | number | 否 | 最低毛利率，默认25% |
| category_id | string | 否 | 指定类目ID |

**认证**: 通过环境变量 `OZON_CLIENT_ID` 和 `OZON_API_KEY`

### 3. Ozon API直接调用

```bash
# 获取类目热销产品
curl -X POST "https://api-seller.ozon.ru/v2/product/list" \
  -H "Client-Id: ${OZON_CLIENT_ID}" \
  -H "Api-Key: ${OZON_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "category_id": 17028733,
    "limit": 50
  }'
```

## 选品策略

### 蓝海产品判断标准

| 指标 | 蓝海阈值 | 说明 |
|------|----------|------|
| 竞争对手数量 | < 20 | 同一关键词下卖家数量 |
| 平均评分 | < 4.3 | 低评分意味着改进空间 |
| 首评时间 | > 6个月 | 老品稳定但有新机会 |
| 毛利率 | > 25% | 确保利润空间 |
| 月销量 | 50-500 | 不要太高（竞争大）或太低（无市场） |

### 选品流程

1. **关键词输入** → 用户提供搜索词
2. **数据采集** → 调用API/工作流获取产品列表
3. **数据分析** → 计算竞争度、利润率、市场容量
4. **蓝海筛选** → 按阈值过滤出蓝海产品
5. **结果输出** → 保存到Supabase选品池

## 输出格式

### 选品报告

```markdown
# Ozon蓝海选品报告

## 搜索条件
- 关键词: [关键词列表]
- 最低毛利率: 25%
- 生成时间: 2024-04-18 10:00

## 蓝海产品推荐

| 产品名 | 价格(RUB) | 竞争对手 | 评分 | 毛利率 | 推荐理由 |
|--------|-----------|----------|------|--------|----------|
| 产品1 | 1500 | 12 | 4.2 | 32% | 竞争小，评分低有改进空间 |

## 统计数据
- 分析产品总数: 100
- 蓝海产品数: 12
- 蓝海率: 12%
```

## Supabase存储

### product_pool表结构

```sql
CREATE TABLE product_pool (
    id BIGSERIAL PRIMARY KEY,
    ozon_product_id VARCHAR(50),
    name VARCHAR(500),
    name_en VARCHAR(500),
    category_id BIGINT,
    category_name VARCHAR(200),
    price_rub DECIMAL(10,2),
    competitors_count INT,
    avg_rating DECIMAL(3,2),
    first_review_date DATE,
    estimated_margin DECIMAL(5,2),
    monthly_sales INT,
    is_blue_ocean BOOLEAN DEFAULT false,
    status VARCHAR(20) DEFAULT 'pending',  -- pending/approved/rejected
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 插入选品结果

```python
def save_to_pool(products):
    for p in products:
        supabase.table('product_pool').insert({
            'ozon_product_id': p['ozon_product_id'],
            'name': p['name'],
            'price_rub': p['price_rub'],
            'competitors_count': p['competitors_count'],
            'estimated_margin': p['estimated_margin'],
            'is_blue_ocean': p['estimated_margin'] >= 25
        }).execute()
```

## 常见问题

### Q: API返回空结果怎么办？
A: 检查关键词是否热门，或尝试扩大搜索范围。Ozon API有频率限制，建议添加重试逻辑。

### Q: 如何处理竞争度计算？
A: 竞争度 = 同一类目下卖家数量，可通过产品列表接口估算。

### Q: 毛利率如何计算？
A: `(销售价 - 采购价 - 平台佣金 - 物流费) / 销售价 * 100`

## 相关文档

- `references/n8n-workflow-setup.md` - n8n工作流配置
- `references/ozon-api-guide.md` - Ozon API使用指南
