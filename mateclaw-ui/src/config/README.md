# Ozon-Claw 桌面客户端配置说明

## 概述

本配置模块为 Ozon-Claw 桌面客户端提供写死的业务配置，在编译时确定，不依赖运行时环境变量。

## 目录结构

```
mateclaw-ui/src/config/
├── business.ts       # 核心业务配置
└── index.ts          # 导出索引

mateclaw-server/src/main/resources/
├── application-ozonclaw.yml    # 后端 Ozon-Claw 配置
└── resources/

mateclaw-server/src/main/java/vip/mate/config/ozonclaw/
├── OzonClawProperties.java     # 后端配置属性类
└── OzonApiConfig.java          # Ozon API 配置类

mateclaw-server/src/main/java/vip/mate/controller/ozonclaw/
└── DeviceController.java       # 设备管理控制器
```

## 前端配置使用

### 引入配置

```typescript
import { 
  API_BASE_URL,
  TENANT_ID,
  DeviceIdManager,
  AVAILABLE_MODELS,
  BUSINESS_CONFIG 
} from '@/config'

// 使用 API 地址
const apiUrl = API_BASE_URL

// 获取设备ID
const deviceId = DeviceIdManager.getDeviceId()

// 使用模型列表
const modelNames = AVAILABLE_MODELS.map(m => m.name)
```

### 设备ID管理

```typescript
import { DeviceIdManager } from '@/config'

// 获取当前设备ID（首次自动生成）
const deviceId = DeviceIdManager.getDeviceId()

// 重置设备ID（用于设备绑定变更）
const newDeviceId = DeviceIdManager.resetDeviceId()
```

## 后端配置使用

### 启用 Ozon-Claw Profile

在启动参数中添加：
```bash
java -jar mateclaw-server.jar --spring.profiles.active=ozonclaw
```

或在 application.yml 中设置：
```yaml
spring:
  profiles:
    active: ozonclaw
```

### 注入配置

```java
@Autowired
private OzonClawProperties ozonClawProperties;

@GetMapping("/info")
public Map<String, Object> getInfo() {
    Map<String, Object> info = new HashMap<>();
    info.put("version", ozonClawProperties.getClient().getVersion());
    info.put("tenantId", ozonClawProperties.getApi().getTenantId());
    return info;
}
```

## 写死配置项

| 配置项 | 值 | 说明 |
|--------|-----|------|
| API_BASE_URL | https://api.ozon-claw.com | 主 API 服务地址 |
| NEW_API_BASE_URL | https://new-api.ozon-claw.com | New API 服务地址 |
| TENANT_ID | ozon-claw-v1 | 租户标识 |
| CLIENT_VERSION | 1.0.0 | 客户端版本 |
| OZON_SELLER_API | https://api-seller.ozon.ru | Ozon Seller API |
| OZON_API_VERSION | v3 | Ozon API 版本 |

## 编译构建

### 前端构建

```bash
cd mateclaw-ui
pnpm install
pnpm build  # 构建产物输出到 mateclaw-server/src/main/resources/static
```

### 后端构建

```bash
cd mateclaw-server
mvn clean package -PozonClaw
```

## 注意事项

1. **不要修改 business.ts** - 此文件为写死配置，修改需重新编译
2. **设备ID存储在 localStorage** - 清除浏览器数据会导致设备ID丢失
3. **CORS 配置** - 后端 WebMvcConfig 已配置允许 ozon-claw.com 域名
4. **数据库初始化** - 使用 H2 内嵌数据库，文件存储在 ./data/ozon-claw
