# Ozon Skills 设计方案

## 完成内容

### 1. 目录结构

```
skills/
├── ozon-product-selection/              # 选品Skill
│   ├── SKILL.md                        # 技能定义（触发词/API调用/选品策略）
│   ├── scripts/
│   │   └── ozon_selection.py           # 选品执行脚本
│   └── references/
│       ├── n8n-workflow-setup.md       # n8n工作流配置
│       └── ozon-api-guide.md           # Ozon API指南+类目映射
│
├── ozon-product-listing/               # 上架Skill
│   ├── SKILL.md                        # 技能定义（批量上架/图片上传/状态监控）
│   ├── scripts/
│   │   └── ozon_listing.py             # 上架执行脚本
│   └── references/
│       └── category-mapping.md          # 类目映射参考
│
├── ozon-price-check/                   # 定价Skill
│   ├── SKILL.md                        # 技能定义（定价计算/竞品对比/价格调整）
│   └── scripts/
│       └── calculate_price.py          # 定价计算脚本
│
└── ozon-1688-search/                   # 1688图搜Skill
    ├── SKILL.md                        # 技能定义（图片搜索/货源分析/供应商筛选）
    └── scripts/
        └── search_1688.py              # 图搜执行脚本
```

### 2. SKILL.md 模板规范

参考现有Skills（docx/himalaya）的格式：
- **Frontmatter**: name/description/dependencies/platforms
- **正文**: 概述 → 前置条件 → 核心功能 → 代码示例 → 常见问题 → 参考文档

### 3. 触发词设计

| Skill | 触发词 | 优先级 |
|-------|--------|--------|
| ozon-product-selection | /选品、/ozon选品、找产品 | 高 |
| ozon-product-listing | /上架、/ozon上架、发布商品 | 高 |
| ozon-price-check | /定价、/价格计算、查价格 | 中 |
| ozon-1688-search | /1688图搜、找货源、搜同款 | 中 |

### 4. 调用方式

#### n8n工作流（推荐）
- 触发Webhook完成复杂任务（选品、上架、图搜）
- 适合需要多步骤处理、数据库操作的场景

#### 服务器API
- 定价计算、状态查询等轻量操作
- 使用环境变量 `OZON_CLIENT_ID`、`OZON_API_KEY`

#### Ozon API直接调用
- 批量上架 `/v3/product/import`
- 图片上传 `/v1/product/pictures/import`
- 任务状态 `/v1/product/import/info`

### 5. 关键经验整合

已在SKILL.md中整合的实战经验：
- ✅ IP白名单限制说明
- ✅ 批量上架必填字段（品牌4180、模型9048）
- ✅ 货币必须用CNY
- ✅ 尺寸重量单位转换
- ✅ 请求间隔和超时设置
- ✅ ozon-seller SDK正确导入方式
