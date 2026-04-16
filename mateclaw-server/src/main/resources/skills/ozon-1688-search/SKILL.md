---
name: ozon-1688-search
description: "1688图片搜索与货源匹配技能。触发词：/1688图搜、找货源、搜同款。功能：通过产品图片在1688/AliExpress搜索相似货源，分析供应商价格、销量、评价，筛选优质货源。调用本地Playwright或n8n工作流执行自动化搜索。"
dependencies:
  commands:
    - python3
    - playwright
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

# 1688图片搜索与货源匹配

## 概述

本技能用于在1688平台通过图片搜索相似产品，找到优质货源供应商，支持价格分析、销量筛选、供应商评价等功能。

## 搜索方式

### 1. Playwright自动化（推荐本地）

**优势**: 支持GUI操作、可绕过部分反爬

```python
from playwright.sync_api import sync_playwright
import os

def search_1688_by_image(image_path: str, proxy: str = None):
    """使用Playwright在1688图搜"""
    with sync_playwright() as p:
        # 启动浏览器
        browser = p.chromium.launch(headless=False)
        context = browser.new_context(
            proxy={"server": proxy} if proxy else None,
            user_agent="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
        )
        page = context.new_page()
        
        # 登录1688（使用已保存的Cookie）
        page.goto("https://www.1688.com")
        page.context.add_cookies(load_cookies())
        
        # 打开图搜页面
        page.goto("https://s.1688.com/youyuan/index.htm?tab=imageSearch&imageType=url&imageAddress=" + image_path)
        
        # 等待结果加载
        page.wait_for_selector(".sw-dpl-offer-list", timeout=30000)
        
        # 提取结果
        products = []
        for item in page.query_selector_all(".sw-dpl-offer-list .offer-item"):
            product = {
                "title": item.query_selector(".title-text").inner_text(),
                "price": item.query_selector(".price-text").inner_text(),
                "sales": item.query_selector(".sale-info").inner_text() if item.query_selector(".sale-info") else "0",
                "supplier": item.query_selector(".company-name").inner_text(),
                "url": item.query_selector("a").get_attribute("href")
            }
            products.append(product)
        
        browser.close()
        return products
```

### 2. AliPrice插件（更稳定）

**优势**: 专为图搜设计，更稳定

```python
def search_aliprice(image_url: str):
    """通过AliPrice API搜索"""
    # AliPrice提供图片搜索API
    response = requests.get(
        "https://api.aliprice.com/search",
        params={
            "image": image_url,
            "api_key": os.environ.get("ALIPRICE_API_KEY")
        }
    )
    return response.json()
```

### 3. n8n工作流调用

```bash
curl -X POST "http://localhost:5678/webhook/1688-image-search" \
  -H "Content-Type: application/json" \
  -d '{
    "image_url": "https://example.com/product.jpg",
    "min_price": 10,
    "max_price": 100,
    "min_sales": 100
  }'
```

## 货源分析

### 供应商筛选标准

| 指标 | 优质标准 | 说明 |
|------|----------|------|
| 成交量 | > 1000/月 | 高销量代表稳定 |
| 好评率 | > 95% | 质量可靠 |
| 诚信年限 | > 3年 | 供应商稳定性 |
| 响应速度 | < 1小时 | 服务质量 |
| 起订量 | ≤ 1件 | 降低囤货风险 |
| 支持七天无理由 | 是 | 降低退货风险 |

### 货源数据提取

```python
def analyze_supplier(page) -> dict:
    """分析供应商详情"""
    return {
        "supplier_name": page.query_selector(".company-name").inner_text(),
        "contact": page.query_selector(".contact-info").inner_text(),
        "main_products": page.query_selector(".main-products").inner_text(),
        "total_orders": page.query_selector(".order-count").inner_text(),
        "good_rate": page.query_selector(".good-rate").inner_text(),
        "response_rate": page.query_selector(".response-rate").inner_text(),
        "established_years": page.query_selector(".established-year").inner_text(),
        "location": page.query_selector(".location").inner_text()
    }
```

### 价格对比分析

> ⚠️ **定价参数已迁移至MCP工具**
> 请使用 `ozon_pricing` 工具获取实时汇率和佣金率。

```python
def compare_prices(suppliers: list, ozon_price_rub: float) -> dict:
    """对比货源价格与Ozon定价"""
    # 使用MCP工具获取定价参数
    pricing_config = call_tool("ozon_pricing", {"action": "get_config"})
    exchange_rate = pricing_config["exchange_rate"]
    commission_rate = pricing_config["commission_rate"]
    
    results = []
    
    for s in suppliers:
        price_cny = float(s["price"].replace("¥", ""))
        cost_rub = price_cny * exchange_rate
        margin = (ozon_price_rub - cost_rub - ozon_price_rub * commission_rate) / ozon_price_rub * 100
        
        results.append({
            "supplier": s["supplier"],
            "price_cny": price_cny,
            "cost_rub": round(cost_rub, 0),
            "estimated_margin": round(margin, 1),
            "recommendation": "推荐" if margin >= 25 else "利润偏低"
        })
    
    # 按利润率排序
    results.sort(key=lambda x: x["estimated_margin"], reverse=True)
    
    return {
        "top_suppliers": results[:5],
        "best_choice": results[0] if results else None
    }
```

## 执行脚本

### 主脚本: 1688图搜

```python
#!/usr/bin/env python3
"""
1688图片搜索与货源匹配
用法: python search_1688.py --image <图片URL或路径> [--proxy <代理>] [--min-margin <最低利润率>]
"""

import argparse
import json
import sys
from playwright.sync_api import sync_playwright

def main():
    parser = argparse.ArgumentParser(description="1688图片搜索")
    parser.add_argument("--image", required=True, help="图片URL或本地路径")
    parser.add_argument("--proxy", help="代理服务器")
    parser.add_argument("--min-margin", type=float, default=25, help="最低利润率")
    args = parser.parse_args()
    
    print(f"🔍 开始搜索1688货源...")
    print(f"图片: {args.image}")
    
    # TODO: 实现搜索逻辑
    results = search_1688_by_image(args.image, args.proxy)
    
    # 筛选利润率达标的供应商
    filtered = [r for r in results if r["margin"] >= args.min_margin]
    
    print(f"\n📊 搜索结果:")
    print(f"总供应商数: {len(results)}")
    print(f"利润率≥{args.min_margin}%: {len(filtered)}")
    
    for i, r in enumerate(filtered[:5], 1):
        print(f"\n{i}. {r['supplier']}")
        print(f"   价格: ¥{r['price_cny']} (≈{r['cost_rub']}RUB)")
        print(f"   利润率: {r['margin']}%")

if __name__ == "__main__":
    main()
```

## Supabase存储

### source_matching表

```sql
CREATE TABLE source_matching (
    id BIGSERIAL PRIMARY KEY,
    product_pool_id BIGINT REFERENCES product_pool(id),
    ozon_product_id VARCHAR(50),
    ozon_product_name VARCHAR(500),
    1688_supplier_id VARCHAR(100),
    supplier_name VARCHAR(200),
    supplier_url VARCHAR(500),
    product_url VARCHAR(500),
    price_cny DECIMAL(10,2),
    price_rub DECIMAL(10,2),
    sales_count INT,
    good_rate DECIMAL(5,2),
    estimated_margin DECIMAL(5,2),
    is_selected BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

## 注意事项

### 反爬限制

1. **1688反爬严格**: 建议使用AliPrice插件或浏览器复用
2. **Cookie管理**: 保存登录状态，定期刷新Cookie
3. **请求频率**: 每次操作间隔5-10秒
4. **代理IP**: 使用住宅代理，降低被封风险

### 推荐工具组合

| 工具 | 用途 |
|------|------|
| AliPrice浏览器插件 | 图搜1688/速卖通 |
| Playwright | 自动化操作 |
| n8n工作流 | 定时批量搜索 |
| 手动选品 | 高价值产品 |

## 常见问题

### Q: 1688图搜被拦截怎么办？
A: 使用AliPrice插件或Playwright GUI模式，避免频繁请求。

### Q: 如何找到更便宜的货源？
A: 
1. 使用1688的"找相似"功能
2. 多家对比，注意成交量和起订量
3. 考虑二手货源或工厂直销店

### Q: 供应商如何验证可靠性？
A: 检查诚信通年限、实地验厂、七天无理由退换、响应速度等。
