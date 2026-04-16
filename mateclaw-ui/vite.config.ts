import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { resolve } from 'path'

// Ozon-Claw 桌面客户端写死配置
const OZON_CLAW_CONFIG = {
  // API 服务地址（写死）
  VITE_API_BASE_URL: 'https://api.ozon-claw.com',
  VITE_NEW_API_BASE_URL: 'https://new-api.ozon-claw.com',
  // 租户ID（写死）
  VITE_TENANT_ID: 'ozon-claw-v1',
  // Ozon 平台配置（写死）
  VITE_OZON_SELLER_API: 'https://api-seller.ozon.ru',
  VITE_OZON_API_VERSION: 'v3',
}

export default defineConfig(({ mode }) => {
  // 加载环境变量文件
  const env = loadEnv(mode, process.cwd(), '')
  
  return {
    plugins: [
      vue(),
      tailwindcss(),
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    // 编译时环境变量注入（写死配置）
    define: {
      // 基础配置：优先使用环境变量，若无则使用写死值
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(env.VITE_API_BASE_URL || OZON_CLAW_CONFIG.VITE_API_BASE_URL),
      'import.meta.env.VITE_NEW_API_BASE_URL': JSON.stringify(env.VITE_NEW_API_BASE_URL || OZON_CLAW_CONFIG.VITE_NEW_API_BASE_URL),
      'import.meta.env.VITE_TENANT_ID': JSON.stringify(env.VITE_TENANT_ID || OZON_CLAW_CONFIG.VITE_TENANT_ID),
      'import.meta.env.VITE_OZON_SELLER_API': JSON.stringify(env.VITE_OZON_SELLER_API || OZON_CLAW_CONFIG.VITE_OZON_SELLER_API),
      'import.meta.env.VITE_OZON_API_VERSION': JSON.stringify(env.VITE_OZON_API_VERSION || OZON_CLAW_CONFIG.VITE_OZON_API_VERSION),
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:18088',
          changeOrigin: true,
        },
      },
    },
    build: {
      outDir: '../mateclaw-server/src/main/resources/static',
      emptyOutDir: true,
      // 生产构建优化
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true,
        },
      },
    },
    // 资源文件处理
    assetsInclude: [
      // 排除 package.json 以避免路径编码问题
    ],
  }
})
