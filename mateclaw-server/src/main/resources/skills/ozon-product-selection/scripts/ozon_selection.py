#!/usr/bin/env python3
"""
Ozon选品脚本 - 调用n8n工作流获取蓝海产品
用法: python scripts/ozon_selection.py --keywords "перчатки" "шапка" --min-margin 25
"""

import argparse
import requests
import json
import os
import sys

# 配置
N8N_URL = os.environ.get("N8N_URL", "http://localhost:5678")
WEBHOOK_URL = f"{N8N_URL}/webhook/ozon-selection"

OZON_CLIENT_ID = os.environ.get("OZON_CLIENT_ID")
OZON_API_KEY = os.environ.get("OZON_API_KEY")


def search_selection(keywords: list, min_margin: float = 25, category_id: str = None) -> dict:
    """调用选品工作流"""
    payload = {
        "keywords": keywords,
        "min_margin": min_margin,
        "category_id": category_id
    }
    
    try:
        response = requests.post(WEBHOOK_URL, json=payload, timeout=30)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"❌ 调用选品工作流失败: {e}")
        return {"error": str(e)}


def main():
    parser = argparse.ArgumentParser(description="Ozon蓝海选品")
    parser.add_argument("--keywords", nargs="+", required=True, help="搜索关键词")
    parser.add_argument("--min-margin", type=float, default=25, help="最低毛利率")
    parser.add_argument("--category", help="类目ID")
    args = parser.parse_args()
    
    print(f"🔍 开始Ozon蓝海选品...")
    print(f"关键词: {args.keywords}")
    print(f"最低毛利率: {args.min_margin}%")
    
    results = search_selection(args.keywords, args.min_margin, args.category)
    
    if "error" in results:
        print(f"❌ 错误: {results['error']}")
        sys.exit(1)
    
    print(f"\n📊 选品结果:")
    print(f"分析产品数: {results.get('summary', {}).get('total_analyzed', 0)}")
    print(f"蓝海产品数: {results.get('summary', {}).get('blue_ocean_count', 0)}")
    
    for i, p in enumerate(results.get("products", [])[:10], 1):
        print(f"\n{i}. {p.get('name', 'N/A')}")
        print(f"   价格: {p.get('price_rub', 'N/A')} RUB")
        print(f"   竞争对手: {p.get('competitors_count', 'N/A')}")
        print(f"   毛利率: {p.get('estimated_margin', 'N/A')}%")
        print(f"   推荐: {p.get('recommendation', 'N/A')}")


if __name__ == "__main__":
    main()
