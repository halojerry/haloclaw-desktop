<script setup lang="ts">
import { ref, computed } from 'vue'
import { ArrowDown, InfoFilled } from '@element-plus/icons-vue'
import { useMarkdownRenderer } from '@/composables/useMarkdownRenderer'
import TypingCursor from './TypingCursor.vue'
import type { MessageSegment } from '@/types'

const props = withDefaults(defineProps<{
  segment: MessageSegment
  showCursor?: boolean
  /** 是否是最后一个 content segment（最终回答） */
  isLast?: boolean
}>(), {
  showCursor: false,
  isLast: true,
})

const { renderMarkdown } = useMarkdownRenderer()
const expanded = ref(props.isLast) // 最终回答默认展开，中间摘要默认折叠

const renderedContent = computed(() => renderMarkdown(props.segment.text || ''))
const isRunning = computed(() => props.segment.status === 'running')
const isIntermediate = computed(() => !props.isLast)

const textLength = computed(() => {
  const len = props.segment.text?.length || 0
  return len < 1000 ? `${len} 字` : `${(len / 1000).toFixed(1)}k 字`
})
</script>

<template>
  <div class="seg-content" :class="{ 'is-intermediate': isIntermediate }">
    <!-- 中间推理摘要：折叠式显示 -->
    <div v-if="isIntermediate" class="seg-content__header" @click="expanded = !expanded">
      <el-icon class="seg-content__info-icon" :size="13"><InfoFilled /></el-icon>
      <span class="seg-content__label">中间推理总结</span>
      <span class="seg-content__hint">{{ textLength }}</span>
      <el-icon class="seg-content__arrow" :class="{ 'is-open': expanded }" :size="11"><ArrowDown /></el-icon>
    </div>

    <!-- 内容体 -->
    <Transition name="seg-slide">
      <div v-if="expanded || isLast" class="seg-content__body">
        <div class="markdown-body" v-html="renderedContent"></div>
        <TypingCursor v-if="isRunning && showCursor" />
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.seg-content {
  padding: 2px 0;
}
.seg-content.is-intermediate {
  margin: 3px 0;
  border-radius: 8px;
  border: 1px solid var(--mc-border-light);
  background: var(--mc-bg-muted);
  overflow: hidden;
}
.seg-content__header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  color: var(--mc-text-tertiary);
  user-select: none;
  transition: color 0.15s;
}
.seg-content__header:hover {
  color: var(--mc-text-secondary);
}
.seg-content__info-icon {
  color: var(--mc-primary-light);
}
.seg-content__label {
  font-weight: 500;
  flex: 1;
}
.seg-content__hint {
  font-size: 11px;
}
.seg-content__arrow {
  transition: transform 0.2s;
}
.seg-content__arrow.is-open {
  transform: rotate(180deg);
}
.is-intermediate .seg-content__body {
  padding: 0 12px 8px;
}

.seg-slide-enter-active, .seg-slide-leave-active {
  transition: all 0.2s ease;
}
.seg-slide-enter-from, .seg-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
