import { createI18n } from 'vue-i18n'
import { ref } from 'vue'
import { settingsApi } from '@/api'
import enUS from './locales/en-US'
import zhCN from './locales/zh-CN'

export type AppLocale = 'zh-CN' | 'en-US'

const STORAGE_KEY = 'mateclaw_locale'
const DEFAULT_LOCALE: AppLocale = 'zh-CN'

const messages = {
  'zh-CN': zhCN,
  'en-US': enUS,
}

export const currentLocale = ref<AppLocale>(DEFAULT_LOCALE)

export const i18n = createI18n({
  legacy: false,
  locale: currentLocale.value,
  fallbackLocale: DEFAULT_LOCALE,
  messages,
})

function normalizeLocale(locale?: string | null): AppLocale {
  if (locale === 'en' || locale === 'en-US') {
    return 'en-US'
  }
  return 'zh-CN'
}

export function applyLocale(locale?: string | null) {
  const normalized = normalizeLocale(locale)
  currentLocale.value = normalized
  i18n.global.locale.value = normalized
  localStorage.setItem(STORAGE_KEY, normalized)
  return normalized
}

export async function initializeLocale() {
  try {
    const res: any = await settingsApi.getLanguage()
    return applyLocale(res.data)
  } catch {
    return applyLocale(localStorage.getItem(STORAGE_KEY))
  }
}
