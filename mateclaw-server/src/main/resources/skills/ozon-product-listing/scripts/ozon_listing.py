#!/usr/bin/env python3
"""
Ozon上架脚本 - 批量上架产品到Ozon
用法: python scripts/ozon_listing.py --sku BO-001 --price 45 --weight 200
"""

import argparse
import requests
import json
import os
import time
import sys

# 配置
OZON_CLIENT_ID = os.environ.get("OZON_CLIENT_ID")
OZON_API_KEY = os.environ.get("OZON_API_KEY")
OZON_API_URL = "https://api-seller.ozon.ru"

# 品牌属性 - 使用MCP工具获取
def get_brand_attr():
    """获取品牌属性配置（通过MCP工具）"""
    # 实际调用 ozon_category 工具获取
    return {"id": 4180, "values": [{"dictionary_value_id": 970896248, "value": "Нет бренда"}]}


def import_product(sku: str, product: dict, images: list) -> dict:
    """上架单个产品"""
    attributes = [
        BRAND_ATTR,
        {"id": 9048, "values": [{"value": product["name"]}]}  # 模型属性
    ]
    
    payload = {
        "items": [{
            "name": product["name"],
            "offer_id": sku,
            "description_category_id": product["category_id"],
            "type_id": product["type_id"],
            "price": str(product["price"]),
            "currency_code": "CNY",
            "vat": "0",
            "weight": str(int(product["weight"])),
            "weight_unit": "g",
            "dimension_unit": "mm",
            "depth": str(product["depth"]),
            "width": str(product["width"]),
            "height": str(product["height"]),
            "images": images[:10],
            "attributes": attributes
        }]
    }
    
    headers = {
        "Client-Id": OZON_CLIENT_ID,
        "Api-Key": OZON_API_KEY,
        "Content-Type": "application/json"
    }
    
    try:
        response = requests.post(
            f"{OZON_API_URL}/v3/product/import",
            headers=headers,
            json=payload,
            timeout=60
        )
        result = response.json()
        
        if response.status_code == 200 and "task_id" in result:
            return {"success": True, "task_id": result["task_id"], "sku": sku}
        else:
            return {"success": False, "error": result, "sku": sku}
    except Exception as e:
        return {"success": False, "error": str(e), "sku": sku}


def batch_import(products: list) -> list:
    """批量上架"""
    results = []
    
    for i, p in enumerate(products):
        print(f"📤 上架 [{i+1}/{len(products)}]: {p['sku']}")
        
        result = import_product(p["sku"], p, p.get("images", []))
        results.append(result)
        
        if result["success"]:
            print(f"   ✅ 成功, Task ID: {result['task_id']}")
        else:
            print(f"   ❌ 失败: {result.get('error')}")
        
        time.sleep(5)  # 间隔5秒避免限流
    
    return results


def main():
    parser = argparse.ArgumentParser(description="Ozon产品上架")
    parser.add_argument("--sku", required=True, help="商品SKU")
    parser.add_argument("--name", required=True, help="产品名称(俄语)")
    parser.add_argument("--price", type=float, required=True, help="价格(CNY)")
    parser.add_argument("--weight", type=int, required=True, help="重量(g)")
    parser.add_argument("--category-id", type=int, required=True, help="类目ID")
    parser.add_argument("--type-id", type=int, required=True, help="类型ID")
    parser.add_argument("--depth", type=int, default=100, help="深度(mm)")
    parser.add_argument("--width", type=int, default=100, help="宽度(mm)")
    parser.add_argument("--height", type=int, default=50, help="高度(mm)")
    parser.add_argument("--images", nargs="*", help="图片URL列表")
    args = parser.parse_args()
    
    if not OZON_CLIENT_ID or not OZON_API_KEY:
        print("❌ 缺少环境变量: OZON_CLIENT_ID 或 OZON_API_KEY")
        sys.exit(1)
    
    product = {
        "sku": args.sku,
        "name": args.name,
        "price": args.price,
        "weight": args.weight,
        "category_id": args.category_id,
        "type_id": args.type_id,
        "depth": args.depth,
        "width": args.width,
        "height": args.height,
        "images": args.images or []
    }
    
    print(f"📤 开始上架...")
    print(f"SKU: {args.sku}")
    print(f"名称: {args.name}")
    print(f"价格: {args.price} CNY")
    print(f"重量: {args.weight}g")
    
    result = import_product(args.sku, product, product["images"])
    
    if result["success"]:
        print(f"\n✅ 上架成功!")
        print(f"Task ID: {result['task_id']}")
    else:
        print(f"\n❌ 上架失败: {result.get('error')}")


if __name__ == "__main__":
    main()
