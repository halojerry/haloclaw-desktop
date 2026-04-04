<template>
  <div class="settings-section about-section">
    <div class="section-header">
      <h2 class="section-title">{{ t('settings.aboutTitle') }}</h2>
      <p class="section-desc">{{ t('settings.aboutDesc') }}</p>
    </div>

    <div class="settings-card">
      <div class="about-info">
        <img src="/logo/mateclaw_logo_s.png" alt="MateClaw" class="about-logo" />
        <h3 class="about-name">Mate<span class="about-name-highlight">Claw</span></h3>
        <p class="about-version">Version 1.0.0</p>
        <p class="about-desc">Java + Vue AI agent platform powered by Spring AI Alibaba.</p>
      </div>
      <div class="tech-stack">
        <div class="tech-item" v-for="tech in techStack" :key="tech.name">
          <span class="tech-icon">{{ tech.icon }}</span>
          <div>
            <div class="tech-name">{{ tech.name }}</div>
            <div class="tech-version">{{ tech.version }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const techStack = [
  { icon: '☕', name: 'Spring Boot', version: '3.3.x' },
  { icon: '🤖', name: 'Spring AI', version: '1.1.x' },
  { icon: '🌿', name: 'Spring AI Alibaba', version: '1.1.x' },
  { icon: '💚', name: 'Vue 3', version: '3.5.x' },
  { icon: '⚡', name: 'Vite', version: '6.x' },
  { icon: '🗄️', name: 'MyBatis Plus', version: '3.5.x' },
]
</script>

<style scoped>
.settings-section { width: 100%; }
.settings-section.about-section { max-width: none; }
.section-header { display: flex; flex-direction: column; gap: 6px; margin-bottom: 20px; }
.section-title { margin: 0; font-size: 22px; font-weight: 700; color: var(--mc-text-primary); }
.section-desc { margin: 0; font-size: 14px; color: var(--mc-text-secondary); }

.settings-card {
  background: var(--mc-bg-elevated); border: 1px solid var(--mc-border); border-radius: 16px; padding: 18px;
  box-shadow: 0 8px 24px rgba(124, 63, 30, 0.04); width: 100%;
  display: flex; flex-direction: column; align-items: center; gap: 20px;
}
.about-info { text-align: center; margin-bottom: 0; padding: 12px 8px; display: flex; flex-direction: column; align-items: center; }
.about-logo { width: 80px; height: 80px; object-fit: contain; filter: drop-shadow(0 6px 16px rgba(217, 119, 87, 0.3)); }
.about-name { margin: 12px 0 4px; font-size: 24px; color: var(--mc-text-primary); }
.about-name-highlight { color: var(--mc-primary); }
.about-version, .about-desc { margin: 0; color: var(--mc-text-secondary); }
.tech-stack { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; width: 100%; }
.tech-item { display: flex; gap: 10px; align-items: center; padding: 14px; background: var(--mc-bg-sunken); border-radius: 12px; }
.tech-icon { font-size: 20px; }
.tech-name { font-weight: 600; color: var(--mc-text-primary); }
.tech-version { font-size: 13px; color: var(--mc-text-secondary); }

@media (max-width: 900px) {
  .settings-card { grid-template-columns: 1fr; }
}
</style>
