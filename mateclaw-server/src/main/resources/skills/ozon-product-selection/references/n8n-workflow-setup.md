# n8n工作流配置指南

## 工作流列表

| 工作流 | Webhook URL | 用途 |
|--------|-------------|------|
| WF-Daily-Selection | `/webhook/ozon-selection` | 每日选品 |
| WF-Source-Matching | `/webhook/1688-image-search` | 1688图搜 |
| WF-Auto-Listing | `/webhook/ozon-listing` | 自动上架 |
| WF-Status-Monitor | `/webhook/ozon-status` | 状态监控 |

## 调用示例

### 选品工作流

```javascript
// n8n Webhook Trigger
// Method: POST
// Content-Type: application/json

const keywords = $json.keywords || [];
const minMargin = $json.min_margin || 25;

// 调用1688搜索或网络搜索获取竞品数据
// 筛选蓝海产品
// 返回结构化数据

return [{
  products: [
    {
      ozon_product_id: "123456789",
      name: "产品名称",
      price_rub: 1500,
      competitors_count: 15,
      estimated_margin: 35
    }
  ],
  summary: {
    total_analyzed: 100,
    blue_ocean_count: 12
  }
}];
```

### 上架工作流

```javascript
// 1. 从Supabase获取待上架产品
// 2. 调用图片生成API (MiniMax)
// 3. 生成俄语富文本描述
// 4. 计算定价
// 5. 调用Ozon API批量上架
// 6. 更新Supabase状态
```

## 环境变量

```bash
# n8n .env
N8N_BASIC_AUTH_ACTIVE=true
N8N_ENDPOINT_RESOLVE_DATA=true

# Ozon API
OZON_CLIENT_ID=your_client_id
OZON_API_KEY=your_api_key

# Supabase
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_SERVICE_KEY=your_service_key

# MiniMax
MINIMAX_API_KEY=your_api_key
```
