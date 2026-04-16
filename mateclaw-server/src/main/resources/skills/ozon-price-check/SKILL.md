---
name: ozon-price-check
description: "Ozon定价计算与价格查询技能。触发词：/定价、/价格计算、查价格。功能：根据采购价计算Ozon销售价，支持毛利率计算、竞品价格对比、价格调整建议。直接调用服务器API或本地计算脚本。"
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

# Ozon定价计算

## 概述

本技能用于计算Ozon产品定价，支持采购价到销售价的自动计算、竞品价格对比、以及价格优化建议。

## 定价参数

### 默认配置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 汇率 | 12.5 | CNY/RUB |
| 平台佣金率 | 18% | Ozon标准佣金 |
| 目标利润率 | 30% | 可调整 |
| 头程物流 | 15 CNY | 空运每500g |
| 尾程物流 | 150 RUB | 俄罗斯快递 |
| 包装费 | 5 CNY | 包装材料 |

## 定价计算

### 核心公式

```
销售价 = (采购价 × 汇率 + 头程 + 包装 + 尾程) / (1 - 佣金率 - 利润率)
```

### Python实现

> ⚠️ **定价计算已迁移至MCP工具**
> 请使用 `ozon_pricing` 工具进行定价计算，详见下方使用步骤。

### 使用示例

```python
# 使用MCP工具 ozon_pricing
result = call_tool("ozon_pricing", {
    "cost": 35,      # 采购成本 CNY
    "weight": 0.2    # 重量 kg
})
```

### 输出示例

```json
{
  "purchase_price_cny": 35,
  "weight_g": 200,
  "selling_price_rub": 1210,
  "cost_breakdown": {
    "purchase": 35,
    "air_freight_cny": 15,
    "packaging_cny": 5,
    "total_cost_cny": 55,
    "exchange_rate": 12.5,
    "last_mile_rub": 150
  },
  "fees": {
    "platform_commission": 218,
    "logistics_total": 338
  },
  "profit_rub": 242,
  "actual_margin": 30.0
}
```

## 竞品价格对比

### 获取竞品价格

```python
def get_competitor_prices(keyword: str, limit: int = 10) -> list:
    """获取同类产品竞品价格"""
    # 调用Ozon搜索API或n8n工作流
    response = requests.post(
        "http://localhost:5678/webhook/ozon-price-check",
        json={"keyword": keyword, "limit": limit}
    )
    return response.json().get("competitors", [])
```

### 价格建议

```python
def suggest_price(your_cost: float, competitor_prices: list) -> dict:
    """根据竞品给出定价建议"""
    avg_price = sum(competitor_prices) / len(competitor_prices)
    min_price = min(competitor_prices)
    max_price = max(competitor_prices)
    
    my_price = calculate_ozon_price(your_cost)["selling_price_rub"]
    
    return {
        "my_price": my_price,
        "avg_competitor": round(avg_price, 0),
        "min_competitor": min_price,
        "max_competitor": max_price,
        "position": "低价" if my_price < avg_price * 0.8 else 
                    "中等" if my_price < avg_price * 1.2 else "高价",
        "recommendation": "建议调低价格" if my_price > max_price else "价格有竞争力"
    }
```

## 价格调整

### 价格隔离期说明

> ⚠️ **重要**: Ozon有价格隔离期机制，当价格变化超过30%时会触发，需分步调整或等待24-48小时。

### 分步降价

```python
def adjust_price_gradually(current_price: float, target_price: float, max_change: float = 0.15):
    """分步调整价格，避免触发隔离期"""
    steps = []
    price = current_price
    
    while price > target_price:
        max_decrease = price * max_change
        new_price = max(price - max_decrease, target_price)
        steps.append(round(new_price))
        price = new_price
    
    return {
        "current_price": current_price,
        "target_price": target_price,
        "steps": steps,
        "estimated_days": len(steps)
    }
```

## 定价模板

### 常用类目佣金率

| 类目 | 佣金率 |
|------|--------|
| 服装 | 15% |
| 电子产品 | 18% |
| 家居用品 | 18% |
| 儿童用品 | 15% |
| 美妆 | 20% |
| 运动户外 | 17% |

### 利润率参考

| 市场定位 | 利润率 |
|----------|--------|
| 低价冲量 | 15-20% |
| 中等定价 | 25-30% |
| 高端品质 | 35%+ |

## 命令行使用

```bash
# 简单计算
python scripts/calculate_price.py --price 35 --weight 200

# 指定利润率
python scripts/calculate_price.py --price 35 --weight 200 --margin 0.35

# 竞品对比
python scripts/calculate_price.py --price 35 --weight 200 --keyword "перчатки"
```

## API调用

### 服务器端点

```
GET /api/ozon/price/calculate
POST /api/ozon/price/compare
GET /api/ozon/price/suggest
```

### 请求示例

```bash
curl "http://localhost:3000/api/ozon/price/calculate" \
  -d "purchase_price=35&weight=200&margin=0.30"
```

## 常见问题

### Q: 如何设置利润率？
A: 根据产品竞争度和目标市场调整。竞争激烈的类目可降低利润率冲量，高利润类目可提高。

### Q: 物流费用如何估算？
A: 可使用集运转运商报价，通常按重量和体积计算。建议预留10-15%余量。

### Q: 遇到价格隔离期怎么办？
A: 分2-3步调整，每次变化不超过15%，或等待24-48小时后再做大幅调整。
