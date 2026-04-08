<template>
  <div class="page-viewer" v-if="store.currentPage">
    <div class="page-viewer-header">
      <div>
        <h2 class="page-viewer-title">{{ store.currentPage.title }}</h2>
        <div class="page-viewer-meta">
          <span>v{{ store.currentPage.version }}</span>
          <span>&middot;</span>
          <span>{{ store.currentPage.lastUpdatedBy === 'ai' ? 'AI generated' : 'Manually edited' }}</span>
          <span>&middot;</span>
          <span>{{ store.currentPage.slug }}</span>
        </div>
      </div>
      <div class="page-viewer-actions">
        <button class="btn-secondary btn-sm" @click="editing = !editing">
          {{ editing ? t('common.cancel') : t('common.edit') }}
        </button>
        <button v-if="editing" class="btn-primary btn-sm" @click="saveEdit">
          {{ t('common.save') }}
        </button>
      </div>
    </div>

    <!-- Summary -->
    <div v-if="store.currentPage.summary" class="page-summary">
      {{ store.currentPage.summary }}
    </div>

    <!-- Content -->
    <div v-if="!editing" class="page-content" v-html="renderedContent"></div>
    <textarea v-else v-model="editContent" class="page-editor" rows="30"></textarea>

    <!-- Backlinks -->
    <div v-if="backlinks.length > 0" class="backlinks-section">
      <h4 class="backlinks-title">{{ t('wiki.backlinks') }} ({{ backlinks.length }})</h4>
      <div class="backlinks-list">
        <span
          v-for="bl in backlinks" :key="bl.slug"
          class="backlink-tag"
          @click="openPage(bl.slug)"
        >
          {{ bl.title }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useWikiStore, type WikiPage } from '@/stores/useWikiStore'
import { wikiApi } from '@/api/index'

const { t } = useI18n()
const store = useWikiStore()

const editing = ref(false)
const editContent = ref('')
const backlinks = ref<WikiPage[]>([])

// Simple markdown to HTML renderer with [[link]] support
const renderedContent = computed(() => {
  if (!store.currentPage?.content) return ''
  let html = store.currentPage.content
    // Escape HTML
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    // Headers
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^# (.+)$/gm, '<h1>$1</h1>')
    // Bold and italic
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    // Wiki links
    .replace(/\[\[([^\]]+)\]\]/g, (_match, title) => {
      const slug = title.trim().toLowerCase().replace(/[^a-z0-9\u4e00-\u9fff\s-]/g, '').replace(/\s+/g, '-')
      return `<a class="wiki-link" data-slug="${slug}" onclick="return false">${title}</a>`
    })
    // Regular links
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
    // Lists
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    // Code
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // Paragraphs
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
  return `<p>${html}</p>`
})

watch(() => store.currentPage, async (page) => {
  if (page && store.currentKB) {
    editing.value = false
    editContent.value = page.content || ''
    // Fetch backlinks
    try {
      const res: any = await wikiApi.getBacklinks(store.currentKB.id, page.slug)
      backlinks.value = res.data || []
    } catch {
      backlinks.value = []
    }
  }
}, { immediate: true })

async function saveEdit() {
  if (!store.currentKB || !store.currentPage) return
  await wikiApi.updatePage(store.currentKB.id, store.currentPage.slug, editContent.value)
  await store.loadPage(store.currentKB.id, store.currentPage.slug)
  editing.value = false
}

async function openPage(slug: string) {
  if (!store.currentKB) return
  await store.loadPage(store.currentKB.id, slug)
}

// Handle wiki link clicks via event delegation
onMounted(() => {
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement
    if (target.classList.contains('wiki-link')) {
      const slug = target.dataset.slug
      if (slug) openPage(slug)
    }
  })
})
</script>

<style scoped>
/* Buttons */
.btn-primary { display: flex; align-items: center; gap: 6px; padding: 8px 16px; background: var(--mc-primary); color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-primary:hover { background: var(--mc-primary-hover); }
.btn-primary:disabled { background: var(--mc-border); cursor: not-allowed; }
.btn-primary.btn-sm { padding: 6px 14px; font-size: 13px; }
.btn-secondary { padding: 8px 16px; background: var(--mc-bg-elevated); color: var(--mc-text-primary); border: 1px solid var(--mc-border); border-radius: 8px; font-size: 14px; cursor: pointer; }
.btn-secondary:hover { background: var(--mc-bg-sunken); }
.btn-secondary.btn-sm { padding: 6px 14px; font-size: 13px; }

/* Header */
.page-viewer-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.page-viewer-title { font-size: 24px; font-weight: 600; color: var(--mc-text-primary); }
.page-viewer-meta { font-size: 12px; color: var(--mc-text-secondary); display: flex; gap: 8px; margin-top: 4px; }
.page-viewer-actions { display: flex; gap: 8px; }

/* Summary */
.page-summary { padding: 12px 16px; background: var(--mc-bg-sunken); border-radius: 8px; font-size: 14px; color: var(--mc-text-secondary); margin-bottom: 16px; border-left: 3px solid var(--mc-primary); }

/* Content */
.page-content { font-size: 15px; line-height: 1.75; color: var(--mc-text-primary); }
.page-content :deep(h1) { font-size: 24px; font-weight: 600; margin: 24px 0 12px; color: var(--mc-text-primary); }
.page-content :deep(h2) { font-size: 20px; font-weight: 600; margin: 20px 0 8px; color: var(--mc-text-primary); }
.page-content :deep(h3) { font-size: 18px; font-weight: 600; margin: 16px 0 8px; color: var(--mc-text-primary); }
.page-content :deep(li) { margin-left: 24px; list-style: disc; }
.page-content :deep(code) { background: var(--mc-bg-sunken); padding: 2px 6px; border-radius: 4px; font-size: 0.85em; }
.page-content :deep(.wiki-link) { color: var(--mc-primary); text-decoration: none; cursor: pointer; border-bottom: 1px dashed var(--mc-primary); }
.page-content :deep(.wiki-link:hover) { text-decoration: underline; }

/* Editor */
.page-editor { width: 100%; padding: 16px; border: 1px solid var(--mc-border); border-radius: 8px; font-family: 'JetBrains Mono', monospace; font-size: 14px; line-height: 1.6; resize: vertical; background: var(--mc-bg-elevated); color: var(--mc-text-primary); outline: none; }
.page-editor:focus { border-color: var(--mc-primary); box-shadow: 0 0 0 2px rgba(217,119,87,0.1); }

/* Backlinks */
.backlinks-section { margin-top: 32px; padding-top: 16px; border-top: 1px solid var(--mc-border); }
.backlinks-title { font-size: 12px; font-weight: 600; text-transform: uppercase; color: var(--mc-text-secondary); margin-bottom: 8px; letter-spacing: 0.05em; }
.backlinks-list { display: flex; flex-wrap: wrap; gap: 6px; }
.backlink-tag { padding: 4px 10px; background: var(--mc-bg-sunken); border-radius: 9999px; font-size: 12px; cursor: pointer; color: var(--mc-primary); transition: background 0.15s; }
.backlink-tag:hover { background: var(--mc-primary-bg); }
</style>
