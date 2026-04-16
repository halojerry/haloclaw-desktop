<template>
  <div class="ozon-login-page">
    <div class="login-container">
      <!-- Logo 区域 -->
      <div class="login-header">
        <div class="logo-wrapper">
          <img src="/logo/ozon-claw-logo.png" alt="Ozon-Claw" class="logo-image" @error="handleLogoError" />
          <div class="logo-text">
            <span class="logo-ozon">Ozon</span>
            <span class="logo-claw">Claw</span>
          </div>
        </div>
        <p class="login-subtitle">{{ t('login.subtitle') }}</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            :placeholder="t('login.placeholders.username')"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            :type="showPassword ? 'text' : 'password'"
            :placeholder="t('login.placeholders.password')"
            size="large"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <!-- 记住我 & 忘记密码 -->
        <div class="form-options">
          <el-checkbox v-model="formData.rememberMe">
            {{ t('login.rememberMe') }}
          </el-checkbox>
          <el-link type="primary" :underline="false" @click="handleForgotPassword">
            {{ t('login.forgotPassword') }}
          </el-link>
        </div>

        <!-- 错误提示 -->
        <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          show-icon
          :closable="false"
          class="login-error"
        />

        <!-- 登录按钮 -->
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="login-btn"
          native-type="submit"
        >
          {{ loading ? t('login.signingIn') : t('login.signIn') }}
        </el-button>

        <!-- 注册链接 -->
        <div class="form-footer">
          <span class="footer-text">{{ t('login.noAccount') }}</span>
          <el-link type="primary" :underline="false" @click="goToRegister">
            {{ t('login.registerNow') }}
          </el-link>
        </div>
      </el-form>

      <!-- 设备信息 -->
      <div class="device-info">
        <el-divider>
          <span class="device-label">{{ t('login.deviceInfo') }}</span>
        </el-divider>
        <div class="device-details">
          <div class="device-item">
            <el-icon><Monitor /></el-icon>
            <span class="device-id">{{ deviceId || t('login.noDeviceId') }}</span>
          </div>
          <el-button size="small" text type="primary" @click="goToDeviceManagement">
            {{ t('login.manageDevices') }}
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 背景装饰 -->
    <div class="login-decoration">
      <div class="decoration-circle decoration-circle-1"></div>
      <div class="decoration-circle decoration-circle-2"></div>
      <div class="decoration-circle decoration-circle-3"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User, Lock, Monitor, ArrowRight } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const { t } = useI18n()

const formRef = ref<FormInstance>()
const loading = ref(false)
const showPassword = ref(false)
const errorMsg = ref('')
const deviceId = ref('')

const formData = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const formRules: FormRules = {
  username: [
    { required: true, message: t('login.errors.usernameRequired'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('login.errors.passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('login.errors.passwordMinLength'), trigger: 'blur' }
  ]
}

onMounted(() => {
  // 从 localStorage 读取记住的用户名
  const savedUsername = localStorage.getItem('rememberedUsername')
  if (savedUsername) {
    formData.username = savedUsername
    formData.rememberMe = true
  }

  // 获取设备ID
  deviceId.value = localStorage.getItem('deviceId') || generateDeviceId()
})

function generateDeviceId(): string {
  const id = 'OZON-' + Math.random().toString(36).substring(2, 10).toUpperCase()
  localStorage.setItem('deviceId', id)
  return id
}

function handleLogoError(e: Event) {
  // Logo 加载失败时隐藏图片
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}

async function handleLogin() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true
    errorMsg.value = ''

    // 模拟登录请求
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 保存记住的用户名
    if (formData.rememberMe) {
      localStorage.setItem('rememberedUsername', formData.username)
    } else {
      localStorage.removeItem('rememberedUsername')
    }

    // 保存登录状态
    localStorage.setItem('token', 'mock-token-' + Date.now())
    localStorage.setItem('username', formData.username)
    localStorage.setItem('role', 'user')

    ElMessage.success(t('login.loginSuccess'))
    router.push('/')
  } catch (error) {
    errorMsg.value = t('login.errors.loginFailed')
  } finally {
    loading.value = false
  }
}

function handleForgotPassword() {
  ElMessage.info(t('login.forgotPasswordHint'))
}

function goToRegister() {
  router.push('/register')
}

function goToDeviceManagement() {
  router.push('/settings/devices')
}
</script>

<style scoped>
.ozon-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F5F7FA 0%, #E8ECF0 100%);
  padding: 24px;
  position: relative;
  overflow: hidden;
}

/* 深色模式适配 */
:deep(.dark) .ozon-login-page,
:root.dark .ozon-login-page,
html.dark .ozon-login-page {
  background: linear-gradient(135deg, #1A1D21 0%, #0F1215 100%);
}

.login-container {
  width: 100%;
  max-width: 420px;
  background: var(--ozon-bg-card);
  border-radius: var(--ozon-radius-xl);
  box-shadow: var(--ozon-shadow-lg);
  padding: 40px;
  position: relative;
  z-index: 10;
  animation: fadeUp 0.5s ease-out;
}

/* Logo 区域 */
.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.logo-image {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.logo-text {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.logo-ozon {
  color: var(--ozon-primary);
}

.logo-claw {
  color: var(--ozon-text-primary);
}

.login-subtitle {
  color: var(--ozon-text-secondary);
  font-size: 14px;
  margin: 0;
}

/* 表单样式 */
.login-form {
  margin-bottom: 24px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-error {
  margin-bottom: 16px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--ozon-radius-md);
  background: var(--ozon-primary);
  border-color: var(--ozon-primary);
  box-shadow: var(--ozon-shadow-primary);
  transition: var(--ozon-transition);
}

.login-btn:hover {
  background: var(--ozon-primary-light);
  border-color: var(--ozon-primary-light);
  box-shadow: 0 6px 20px rgba(0, 91, 255, 0.4);
  transform: translateY(-1px);
}

.form-footer {
  text-align: center;
  margin-top: 20px;
}

.footer-text {
  color: var(--ozon-text-secondary);
  font-size: 14px;
  margin-right: 6px;
}

/* 设备信息 */
.device-info {
  margin-top: 24px;
}

.device-label {
  font-size: 12px;
  color: var(--ozon-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.device-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.device-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ozon-text-secondary);
  font-size: 13px;
}

.device-id {
  font-family: monospace;
  background: var(--ozon-bg);
  padding: 4px 8px;
  border-radius: var(--ozon-radius-sm);
}

/* 背景装饰 */
.login-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}

.decoration-circle-1 {
  width: 400px;
  height: 400px;
  background: var(--ozon-primary);
  top: -200px;
  right: -100px;
}

.decoration-circle-2 {
  width: 300px;
  height: 300px;
  background: var(--ozon-primary);
  bottom: -150px;
  left: -100px;
}

.decoration-circle-3 {
  width: 200px;
  height: 200px;
  background: var(--ozon-success);
  top: 50%;
  left: 10%;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
