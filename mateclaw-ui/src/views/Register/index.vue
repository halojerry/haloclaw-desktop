<template>
  <div class="ozon-register-page">
    <div class="register-container">
      <!-- Logo 区域 -->
      <div class="register-header">
        <div class="logo-wrapper" @click="goToLogin">
          <img src="/logo/ozon-claw-logo.png" alt="Ozon-Claw" class="logo-image" @error="handleLogoError" />
          <div class="logo-text">
            <span class="logo-ozon">Ozon</span>
            <span class="logo-claw">Claw</span>
          </div>
        </div>
        <p class="register-subtitle">{{ t('register.subtitle') }}</p>
      </div>

      <!-- 注册表单 -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            :placeholder="t('register.placeholders.username')"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="formData.email"
            type="email"
            :placeholder="t('register.placeholders.email')"
            size="large"
            prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            :type="showPassword ? 'text' : 'password'"
            :placeholder="t('register.placeholders.password')"
            size="large"
            prefix-icon="Lock"
            show-password
          />
          <!-- 密码强度指示 -->
          <div class="password-strength" v-if="formData.password">
            <div class="strength-bars">
              <span
                v-for="i in 4"
                :key="i"
                class="strength-bar"
                :class="{ active: passwordStrength >= i }"
              ></span>
            </div>
            <span class="strength-text" :class="'strength-' + passwordStrengthLevel">
              {{ t('register.passwordStrength.' + passwordStrengthLevel) }}
            </span>
          </div>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            :type="showPassword ? 'text' : 'password'"
            :placeholder="t('register.placeholders.confirmPassword')"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <!-- 邀请码（可选） -->
        <el-form-item>
          <el-input
            v-model="formData.inviteCode"
            :placeholder="t('register.placeholders.inviteCode')"
            size="large"
            prefix-icon="Present"
            clearable
          >
            <template #prefix>
              <el-icon><Present /></el-icon>
            </template>
            <template #append>
              <el-tooltip :content="t('register.inviteCodeTip')" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
          </el-input>
        </el-form-item>

        <!-- 服务条款 -->
        <el-form-item prop="agreeTerms">
          <el-checkbox v-model="formData.agreeTerms">
            <span class="terms-text">
              {{ t('register.agreeTermsPrefix') }}
              <el-link type="primary" :underline="false" @click.stop="showTermsDialog = true">
                {{ t('register.termsOfService') }}
              </el-link>
              {{ t('register.agreeTermsAnd') }}
              <el-link type="primary" :underline="false" @click.stop="showPrivacyDialog = true">
                {{ t('register.privacyPolicy') }}
              </el-link>
            </span>
          </el-checkbox>
        </el-form-item>

        <!-- 错误提示 -->
        <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          show-icon
          :closable="false"
          class="register-error"
        />

        <!-- 注册按钮 -->
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="register-btn"
          native-type="submit"
        >
          {{ loading ? t('register.registering') : t('register.signUp') }}
        </el-button>

        <!-- 登录链接 -->
        <div class="form-footer">
          <span class="footer-text">{{ t('register.hasAccount') }}</span>
          <el-link type="primary" :underline="false" @click="goToLogin">
            {{ t('register.loginNow') }}
          </el-link>
        </div>
      </el-form>
    </div>

    <!-- 背景装饰 -->
    <div class="register-decoration">
      <div class="decoration-circle decoration-circle-1"></div>
      <div class="decoration-circle decoration-circle-2"></div>
    </div>

    <!-- 服务条款弹窗 -->
    <el-dialog
      v-model="showTermsDialog"
      :title="t('register.termsOfService')"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="terms-content">
        <p>{{ t('register.termsContent') }}</p>
      </div>
      <template #footer>
        <el-button @click="showTermsDialog = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>

    <!-- 隐私政策弹窗 -->
    <el-dialog
      v-model="showPrivacyDialog"
      :title="t('register.privacyPolicy')"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="terms-content">
        <p>{{ t('register.privacyContent') }}</p>
      </div>
      <template #footer>
        <el-button @click="showPrivacyDialog = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Present, QuestionFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const { t } = useI18n()

const formRef = ref<FormInstance>()
const loading = ref(false)
const showPassword = ref(false)
const errorMsg = ref('')
const showTermsDialog = ref(false)
const showPrivacyDialog = ref(false)

const formData = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  inviteCode: '',
  agreeTerms: false
})

// 密码强度计算
const passwordStrength = computed(() => {
  let strength = 0
  const pwd = formData.password

  if (pwd.length >= 8) strength++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) strength++
  if (/\d/.test(pwd)) strength++
  if (/[!@#$%^&*(),.?":{}|<>]/.test(pwd)) strength++

  return strength
})

const passwordStrengthLevel = computed(() => {
  const level = ['weak', 'weak', 'fair', 'good', 'strong'][passwordStrength.value]
  return level || 'weak'
})

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value !== formData.password) {
    callback(new Error(t('register.errors.passwordMismatch')))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  username: [
    { required: true, message: t('register.errors.usernameRequired'), trigger: 'blur' },
    { min: 3, max: 20, message: t('register.errors.usernameLength'), trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: t('register.errors.usernamePattern'), trigger: 'blur' }
  ],
  email: [
    { required: true, message: t('register.errors.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('register.errors.emailInvalid'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('register.errors.passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('register.errors.passwordMinLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('register.errors.confirmPasswordRequired'), trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  agreeTerms: [
    { validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error(t('register.errors.agreeTermsRequired')))
      } else {
        callback()
      }
    }, trigger: 'change' }
  ]
}

function handleLogoError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}

async function handleRegister() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true
    errorMsg.value = ''

    // 模拟注册请求
    await new Promise(resolve => setTimeout(resolve, 1500))

    ElMessage.success(t('register.registerSuccess'))
    
    // 自动跳转到登录页
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (error) {
    // 表单验证失败
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.ozon-register-page {
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
:deep(.dark) .ozon-register-page,
:root.dark .ozon-register-page,
html.dark .ozon-register-page {
  background: linear-gradient(135deg, #1A1D21 0%, #0F1215 100%);
}

.register-container {
  width: 100%;
  max-width: 460px;
  background: var(--ozon-bg-card);
  border-radius: var(--ozon-radius-xl);
  box-shadow: var(--ozon-shadow-lg);
  padding: 40px;
  position: relative;
  z-index: 10;
  animation: fadeUp 0.5s ease-out;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
  cursor: pointer;
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

.register-subtitle {
  color: var(--ozon-text-secondary);
  font-size: 14px;
  margin: 0;
}

.register-form {
  margin-bottom: 24px;
}

/* 密码强度 */
.password-strength {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.strength-bars {
  display: flex;
  gap: 4px;
}

.strength-bar {
  width: 32px;
  height: 4px;
  background: var(--ozon-border);
  border-radius: 2px;
  transition: var(--ozon-transition);
}

.strength-bar.active {
  background: var(--ozon-primary);
}

.strength-text {
  font-size: 12px;
  color: var(--ozon-text-tertiary);
}

.strength-weak .strength-bar.active {
  background: var(--ozon-danger);
}
.strength-weak .strength-text {
  color: var(--ozon-danger);
}

.strength-fair .strength-bar.active {
  background: var(--ozon-warning);
}
.strength-fair .strength-text {
  color: var(--ozon-warning);
}

.strength-good .strength-bar.active {
  background: var(--ozon-primary-light);
}
.strength-good .strength-text {
  color: var(--ozon-primary);
}

.strength-strong .strength-bar.active {
  background: var(--ozon-success);
}
.strength-strong .strength-text {
  color: var(--ozon-success);
}

.terms-text {
  font-size: 13px;
  color: var(--ozon-text-secondary);
}

.register-error {
  margin-bottom: 16px;
}

.register-btn {
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

.register-btn:hover {
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

/* 背景装饰 */
.register-decoration {
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
  top: -150px;
  left: -150px;
}

.decoration-circle-2 {
  width: 300px;
  height: 300px;
  background: var(--ozon-success);
  bottom: -100px;
  right: -100px;
}

.terms-content {
  max-height: 400px;
  overflow-y: auto;
  color: var(--ozon-text-secondary);
  line-height: 1.6;
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
