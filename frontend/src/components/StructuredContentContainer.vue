<script setup>
import { ref } from 'vue'

const props = defineProps({
  type: {
    type: String,
    required: true
  },
  title: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: ''
  },
  copyable: {
    type: Boolean,
    default: true
  },
  collapsible: {
    type: Boolean,
    default: true
  },
  initialExpanded: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['copy', 'expand', 'collapse'])

const contentRef = ref(null)
const isExpanded = ref(props.initialExpanded)
const isCopied = ref(false)

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
  if (isExpanded.value) {
    emit('expand')
  } else {
    emit('collapse')
  }
}

const copyContent = async () => {
  try {
    if (contentRef.value) {
      const text = contentRef.value.textContent
      await navigator.clipboard.writeText(text)
      isCopied.value = true
      setTimeout(() => {
        isCopied.value = false
      }, 1500)
      emit('copy', text)
    }
  } catch (error) {
    console.error('复制失败:', error)
  }
}
</script>

<template>
  <div class="structured-content-container">
    <!-- 顶部Header栏 -->
    <div class="structured-content-header">
      <div class="content-type-tag">
        {{ type === '代码' && language ? language : type }}
      </div>
      <div class="action-buttons">
        <button 
          v-if="copyable" 
          class="action-button copy-button"
          @click="copyContent"
          :class="{ 'copied': isCopied }"
          title="复制内容"
        >
          <svg v-if="!isCopied" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
          </svg>
          <svg v-else t="1776748090530" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="16" height="16">
            <path d="M429.5 773.3c-9.3 0-18.6-3.2-26.1-9.7L108.7 509.5c-16.7-14.4-18.6-39.7-4.2-56.4 14.4-16.7 39.7-18.6 56.4-4.2l266.6 229.9 454.8-451.2c15.7-15.6 41-15.5 56.6 0.2 15.6 15.7 15.5 41-0.2 56.6l-481 477.3c-7.8 7.7-18 11.6-28.2 11.6z" fill="currentColor"></path>
          </svg>
        </button>
        <button 
          v-if="collapsible" 
          class="action-button expand-button"
          @click="toggleExpand"
          title="展开/收起"
        >
          <svg :class="{ 'rotated': !isExpanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>
      </div>
    </div>
    
    <!-- 主体内容区 -->
    <div 
      ref="contentRef"
      class="structured-content-body"
      :class="{ 'collapsed': !isExpanded }"
    >
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.structured-content-container {
  margin: 12px 0;
  border-radius: 12px;
  background: #f8f9fa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s ease;
}

.structured-content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #f1f3f4;
  height: 40px;
  border-bottom: 1px solid #e9ecef;
}

.content-type-tag {
  font-size: 12px;
  font-weight: 500;
  color: #667eea;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s ease;
  padding: 0;
}

.action-button:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.action-button.copied {
  color: #22c55e;
  background: rgba(34, 197, 94, 0.1);
}

.expand-button svg {
  transition: transform 0.3s ease;
}

.expand-button svg.rotated {
  transform: rotate(-90deg);
}

.structured-content-body {
  padding: 16px;
  max-height: 600px;
  overflow-y: auto;
  transition: all 0.3s ease;
}

.structured-content-body.collapsed {
  max-height: 0;
  padding: 0;
  overflow: hidden;
}

/* 滚动条样式 */
.structured-content-body::-webkit-scrollbar {
  width: 6px;
}

.structured-content-body::-webkit-scrollbar-track {
  background: #f1f3f4;
  border-radius: 3px;
}

.structured-content-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.structured-content-body::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 暗色模式 */
:deep(.dark-mode) .structured-content-container {
  background: #252525;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

:deep(.dark-mode) .structured-content-header {
  background: #2d2d2d;
  border-bottom-color: #3d3d3d;
}

:deep(.dark-mode) .content-type-tag {
  color: #818cf8;
}

:deep(.dark-mode) .action-button {
  color: #9ca3af;
}

:deep(.dark-mode) .action-button:hover {
  background: rgba(129, 140, 248, 0.1);
  color: #818cf8;
}

:deep(.dark-mode) .action-button.copied {
  color: #4ade80;
  background: rgba(74, 222, 128, 0.1);
}

:deep(.dark-mode) .structured-content-body {
  background: #252525;
}

:deep(.dark-mode) .structured-content-body::-webkit-scrollbar-track {
  background: #2d2d2d;
}

:deep(.dark-mode) .structured-content-body::-webkit-scrollbar-thumb {
  background: #484f58;
}

:deep(.dark-mode) .structured-content-body::-webkit-scrollbar-thumb:hover {
  background: #57606a;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .structured-content-container {
    margin: 8px 0;
  }
  
  .structured-content-header {
    padding: 6px 12px;
    height: 36px;
  }
  
  .structured-content-body {
    padding: 12px;
    max-height: 400px;
  }
  
  .structured-content-body.collapsed {
    max-height: 80px;
  }
}
</style>