<template>
  <div class="login-page">
    <div class="login-frame">
      <div class="login-hero">
        <div class="login-hero__kicker">{{ t('login.kicker') }}</div>
        <div class="login-logo">
          <img src="/logo/mateclaw_logo_s.png" alt="MateClaw" class="logo-image" />
          <h1 class="logo-title">Mate<span class="logo-title-highlight">Claw</span></h1>
          <p class="logo-subtitle">{{ t('login.subtitle') }}</p>
        </div>
        <h2 class="login-hero__title">{{ t('login.heroTitle') }}</h2>
        <p class="login-hero__desc">{{ t('login.heroDesc') }}</p>
        <div class="login-hero__points">
          <div class="hero-point">{{ t('login.pointContext') }}</div>
          <div class="hero-point">{{ t('login.pointKnowledge') }}</div>
          <div class="hero-point">{{ t('login.pointExecution') }}</div>
        </div>
      </div>

      <div class="login-card">
        <div class="login-card__intro">
          <div class="login-card__kicker">{{ t('login.signIn') }}</div>
          <h3 class="login-card__title">{{ t('login.cardTitle') }}</h3>
          <p class="login-card__desc">{{ t('login.cardDesc') }}</p>
        </div>

        <!-- 登录表单 -->
        <form class="login-form" @submit.prevent="handleLogin">
          <div class="form-group">
            <label class="form-label">{{ t('login.fields.username') }}</label>
            <div class="input-wrap">
              <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <input
                v-model="form.username"
                type="text"
                class="form-input"
                :placeholder="t('login.placeholders.username')"
                autocomplete="username"
                required
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">{{ t('login.fields.password') }}</label>
            <div class="input-wrap">
              <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                class="form-input"
                :placeholder="t('login.placeholders.password')"
                autocomplete="current-password"
                required
              />
              <button type="button" class="eye-btn" @click="showPassword = !showPassword">
                <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </button>
            </div>
          </div>

          <div v-if="errorMsg" class="error-msg">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {{ errorMsg }}
          </div>

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="!loading">{{ t('login.signIn') }}</span>
            <span v-else class="loading-dots">
              <span></span><span></span><span></span>
            </span>
          </button>
        </form>

        <p class="login-hint" v-html="t('login.hint')"></p>
      </div>
    </div>

    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { authApi } from '@/api/index'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const showPassword = ref(false)
const errorMsg = ref('')
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  if (!form.username || !form.password) return
  loading.value = true
  errorMsg.value = ''
  try {
    const res: any = await authApi.login(form)
    const data = res.data || res
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username || form.username)
    localStorage.setItem('role', data.role || 'user')
    router.push('/')
  } catch (e: any) {
    errorMsg.value = typeof e === 'string' ? e : t('login.failed')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--mc-primary-bg) 0%, #FAF5F0 50%, #F5EDE5 100%);
  position: relative;
  overflow: hidden;
  padding: 28px;
}

:root.dark .login-page,
html.dark .login-page {
  background: linear-gradient(135deg, var(--mc-bg) 0%, #1E1814 50%, #1A1210 100%);
}

.login-frame {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 24px;
  position: relative;
  z-index: 1;
}

.login-hero,
.login-card {
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border);
  border-radius: 28px;
  box-shadow: 0 20px 60px rgba(217, 119, 87, 0.12);
  backdrop-filter: blur(18px);
}

.login-hero {
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 560px;
}

.login-hero__kicker,
.login-card__kicker {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  background: var(--mc-bg-muted);
  border: 1px solid var(--mc-border-light);
  color: var(--mc-accent);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.login-hero__title {
  font-size: clamp(34px, 4vw, 56px);
  line-height: 0.98;
  font-weight: 800;
  letter-spacing: -0.05em;
  color: var(--mc-text-primary);
  margin: 18px 0 12px;
}

.login-hero__desc {
  font-size: 16px;
  line-height: 1.75;
  color: var(--mc-text-secondary);
  max-width: 640px;
  margin: 0;
}

.login-hero__points {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.hero-point {
  padding: 10px 14px;
  border-radius: 16px;
  border: 1px solid var(--mc-border-light);
  background: linear-gradient(180deg, var(--mc-bg-muted), var(--mc-bg-elevated));
  color: var(--mc-text-primary);
  font-size: 14px;
  font-weight: 600;
}

.login-card {
  padding: 36px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-card__intro {
  margin-bottom: 24px;
}

.login-card__title {
  margin: 14px 0 8px;
  font-size: 28px;
  line-height: 1.05;
  letter-spacing: -0.04em;
  color: var(--mc-text-primary);
}

.login-card__desc {
  margin: 0;
  color: var(--mc-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

/* Logo */
.login-logo {
  margin-top: 22px;
}

.logo-image {
  display: block;
  margin: 0 auto 12px;
  width: 80px;
  height: 80px;
  object-fit: contain;
  filter: drop-shadow(0 8px 24px rgba(217, 119, 87, 0.35));
}

.logo-title {
  font-size: 42px;
  font-weight: 800;
  color: var(--mc-text-primary);
  margin: 0 0 8px;
  letter-spacing: -0.05em;
}

.logo-title-highlight {
  color: var(--mc-primary);
}

.logo-subtitle {
  font-size: 15px;
  color: var(--mc-text-tertiary);
  margin: 0;
}

/* 表单 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--mc-text-primary);
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  color: var(--mc-text-tertiary);
  pointer-events: none;
}

.form-input {
  width: 100%;
  padding: 10px 40px 10px 38px;
  border: 1px solid var(--mc-border);
  border-radius: 10px;
  font-size: 14px;
  color: var(--mc-text-primary);
  outline: none;
  transition: all 0.15s;
  background: var(--mc-bg-sunken);
}

.form-input:focus {
  border-color: var(--mc-primary);
  background: var(--mc-bg-elevated);
  box-shadow: 0 0 0 3px rgba(217, 119, 87, 0.1);
}

.eye-btn {
  position: absolute;
  right: 10px;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--mc-text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.eye-btn:hover {
  color: var(--mc-primary);
}

/* 错误提示 */
.error-msg {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: var(--mc-danger-bg);
  border: 1px solid var(--mc-danger);
  border-radius: 8px;
  font-size: 13px;
  color: var(--mc-danger);
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, var(--mc-primary), var(--mc-primary-hover));
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  margin-top: 4px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(217, 119, 87, 0.3);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 5px;
  align-items: center;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  background: white;
  border-radius: 50%;
  animation: bounce 1.2s infinite;
}

.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-5px); }
}

/* 提示 */
.login-hint {
  text-align: left;
  font-size: 12px;
  color: var(--mc-text-tertiary);
  margin: 20px 0 0;
}

.login-hint code {
  background: var(--mc-inline-code-bg);
  padding: 1px 6px;
  border-radius: 4px;
  color: var(--mc-inline-code-color);
  font-size: 12px;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.4;
}

.bg-circle-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #E0C4B0, transparent);
  top: -100px;
  right: -100px;
}

.bg-circle-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #F0D0B8, transparent);
  bottom: -80px;
  left: -80px;
}

.bg-circle-3 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, #F5E4D8, transparent);
  bottom: 100px;
  right: 100px;
}

:root.dark .bg-circle-1,
html.dark .bg-circle-1 {
  background: radial-gradient(circle, rgba(217, 119, 87, 0.2), transparent);
}
:root.dark .bg-circle-2,
html.dark .bg-circle-2 {
  background: radial-gradient(circle, rgba(193, 87, 43, 0.15), transparent);
}
:root.dark .bg-circle-3,
html.dark .bg-circle-3 {
  background: radial-gradient(circle, rgba(123, 63, 30, 0.12), transparent);
}

@media (max-width: 960px) {
  .login-frame {
    grid-template-columns: 1fr;
  }

  .login-hero {
    min-height: auto;
    padding: 28px;
  }

  .login-card {
    padding: 28px;
  }

  .logo-title {
    font-size: 34px;
  }

  .login-hero__title {
    font-size: 34px;
  }
}
</style>
