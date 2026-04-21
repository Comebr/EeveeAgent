<script setup>
import { computed, ref, watch, onMounted, nextTick } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  isStreaming: {
    type: Boolean,
    default: false
  }
})

const contentRef = ref(null)
const thinkingCollapsed = ref({})
const copiedCodeId = ref(null)
const copyButtons = ref([])

const updateCopyButtonState = () => {
  copyButtons.value.forEach(btn => {
    const codeId = btn.dataset.codeId
    btn.innerHTML = copiedCodeId.value === codeId
      ? '<svg t="1776748090530" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="16" height="16"><path d="M429.5 773.3c-9.3 0-18.6-3.2-26.1-9.7L108.7 509.5c-16.7-14.4-18.6-39.7-4.2-56.4 14.4-16.7 39.7-18.6 56.4-4.2l266.6 229.9 454.8-451.2c15.7-15.6 41-15.5 56.6 0.2 15.6 15.7 15.5 41-0.2 56.6l-481 477.3c-7.8 7.7-18 11.6-28.2 11.6z" fill="currentColor"></path></svg>'
      : '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>'
  })
}

watch(copiedCodeId, () => {
  nextTick(() => {
    updateCopyButtonState()
  })
})

const isDarkMode = computed(() => {
  if (typeof window !== 'undefined') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  return false
})

marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false
})

const parseThinkingBlocks = (content) => {
  const thinkingRegex = /---thinking---([\s\S]*?)---thinking---/g
  const parts = []
  let lastIndex = 0
  let match
  let id = 0

  while ((match = thinkingRegex.exec(content)) !== null) {
    if (match.index > lastIndex) {
      parts.push({
        type: 'markdown',
        content: content.slice(lastIndex, match.index),
        id: `md-${id++}`
      })
    }
    parts.push({
      type: 'thinking',
      content: match[1].trim(),
      id: `thinking-${id++}`
    })
    lastIndex = match.index + match[0].length
  }

  if (lastIndex < content.length) {
    parts.push({
      type: 'markdown',
      content: content.slice(lastIndex),
      id: `md-${id++}`
    })
  }

  return parts
}

const processedContent = computed(() => {
  if (!props.content) return []

  const parts = parseThinkingBlocks(props.content)

  return parts.map(part => {
    if (part.type === 'thinking') {
      return {
        ...part,
        renderedHtml: `<div class="thinking-content">${part.content}</div>`
      }
    }

    const rawHtml = marked.parse(part.content)

    const cleanHtml = DOMPurify.sanitize(rawHtml, {
      ALLOWED_TAGS: [
        'p', 'br', 'strong', 'b', 'em', 'i', 'u', 'strike', 'del',
        'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
        'ul', 'ol', 'li',
        'blockquote', 'code', 'pre',
        'a', 'img',
        'table', 'thead', 'tbody', 'tr', 'th', 'td',
        'hr', 'div', 'span'
      ],
      ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'target', 'data-language', 'data-code', 'data-line']
    })

    return {
      ...part,
      renderedHtml: cleanHtml
    }
  })
})

const toggleThinking = (id) => {
  thinkingCollapsed.value[id] = !thinkingCollapsed.value[id]
}

const copyCode = async (code, codeId) => {
  try {
    // 确保复制的是纯代码，不含行号
    const cleanCode = code.replace(/^\s*\d+\s*/gm, '')
    await navigator.clipboard.writeText(cleanCode)
    copiedCodeId.value = codeId
    setTimeout(() => {
      copiedCodeId.value = null
    }, 1500)
  } catch (err) {
    console.error('复制失败:', err)
    // 降级方案：使用传统的复制方法
    const textarea = document.createElement('textarea')
    textarea.value = code.replace(/^\s*\d+\s*/gm, '')
    document.body.appendChild(textarea)
    textarea.select()
    try {
      document.execCommand('copy')
      copiedCodeId.value = codeId
      setTimeout(() => {
        copiedCodeId.value = null
      }, 1500)
    } catch (execErr) {
      console.error('降级复制失败:', execErr)
    } finally {
      document.body.removeChild(textarea)
    }
  }
}

const highlightCode = () => {
  if (!contentRef.value) return

  copyButtons.value = []

  contentRef.value.querySelectorAll('pre code').forEach((block) => {
    // 提取语言类型
    let language = 'plaintext'
    const className = block.className
    if (className) {
      const match = className.match(/language-(\w+)/)
      if (match && match[1]) {
        language = match[1]
      }
    }
    
    // 生成行号
    const lines = block.textContent.split('\n')
    const lineCount = lines.length
    const lineNumbers = lines.map((_, i) => `<span class="line-number">${i + 1}</span>`).join('')

    // 设置属性
    const preEl = block.parentElement
    preEl.setAttribute('data-language', language)
    preEl.setAttribute('data-line', lineCount)
    preEl.setAttribute('data-code-id', preEl.dataset.codeId || Date.now())

    // 添加行号
    let lineNumbersDiv = preEl.querySelector('.line-numbers')
    if (!lineNumbersDiv) {
      lineNumbersDiv = document.createElement('div')
      lineNumbersDiv.className = 'line-numbers'
      preEl.insertBefore(lineNumbersDiv, block)
    }
    lineNumbersDiv.innerHTML = lineNumbers

    // 添加复制按钮
    let copyBtn = preEl.querySelector('.copy-code-btn')
    if (!copyBtn) {
      copyBtn = document.createElement('button')
      copyBtn.className = 'copy-code-btn'
      copyBtn.title = '复制代码'
      preEl.appendChild(copyBtn)
    }
    
    // 确保所有按钮都添加到数组中
    if (!copyButtons.value.includes(copyBtn)) {
      copyButtons.value.push(copyBtn)
    }
    
    // 设置属性和事件
    copyBtn.dataset.codeId = preEl.dataset.codeId
    copyBtn.onclick = () => {
      console.log('复制按钮点击')
      copyCode(block.textContent, preEl.dataset.codeId)
    }
    
    // 初始化按钮图标
    copyBtn.innerHTML = copiedCodeId.value === preEl.dataset.codeId
      ? '<svg t="1776748090530" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="16" height="16"><path d="M429.5 773.3c-9.3 0-18.6-3.2-26.1-9.7L108.7 509.5c-16.7-14.4-18.6-39.7-4.2-56.4 14.4-16.7 39.7-18.6 56.4-4.2l266.6 229.9 454.8-451.2c15.7-15.6 41-15.5 56.6 0.2 15.6 15.7 15.5 41-0.2 56.6l-481 477.3c-7.8 7.7-18 11.6-28.2 11.6z" fill="currentColor"></path></svg>'
      : '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>'

    // 应用语法高亮
    hljs.highlightElement(block)
  })
}

watch(() => props.content, () => {
  nextTick(() => {
    highlightCode()
  })
}, { immediate: true })

onMounted(() => {
  highlightCode()

  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    nextTick(() => {
      highlightCode()
    })
  })
})
</script>

<template>
  <div
    ref="contentRef"
    class="markdown-body"
    :class="{ 'streaming': isStreaming, 'dark-mode': isDarkMode }"
  >
    <template v-for="part in processedContent" :key="part.id">
      <div v-if="part.type === 'thinking'" class="thinking-block">
        <div class="thinking-header" @click="toggleThinking(part.id)">
          <svg class="thinking-icon" :class="{ 'collapsed': thinkingCollapsed[part.id] }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
          <span class="thinking-label">思考过程</span>
        </div>
        <div v-show="!thinkingCollapsed[part.id]" class="thinking-body" v-html="part.renderedHtml"></div>
      </div>
      <div v-else class="markdown-content" v-html="part.renderedHtml"></div>
    </template>
  </div>
</template>

<style scoped>
.markdown-body {
  font-size: 15px;
  line-height: 1.8;
  color: #1f2329;
  word-break: break-word;
  overflow-wrap: break-word;
}

.markdown-body.dark-mode {
  color: #e5e5e5;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: #1f2329;
}

.dark-mode .markdown-body :deep(h1),
.dark-mode .markdown-body :deep(h2),
.dark-mode .markdown-body :deep(h3),
.dark-mode .markdown-body :deep(h4),
.dark-mode .markdown-body :deep(h5),
.dark-mode .markdown-body :deep(h6) {
  color: #ffffff;
}

.markdown-body :deep(h1) {
  font-size: 1.75em;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.markdown-body :deep(h2) {
  font-size: 1.5em;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
}

.markdown-body :deep(h3) {
  font-size: 1.25em;
}

.markdown-body :deep(h4) {
  font-size: 1.1em;
}

.markdown-body :deep(p) {
  margin: 16px 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 16px 0;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin: 8px 0;
  line-height: 1.8;
}

.markdown-body :deep(ul > li) {
  list-style-type: disc;
}

.markdown-body :deep(ul > li::marker) {
  color: #667eea;
}

.markdown-body :deep(ol > li) {
  list-style-type: decimal;
}

.markdown-body :deep(ol > li::marker) {
  color: #667eea;
}

.markdown-body :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 20px;
  border-left: 4px solid #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  border-radius: 0 8px 8px 0;
  color: #4b5563;
}

.dark-mode .markdown-body :deep(blockquote) {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.15) 100%);
  color: #9ca3af;
}

.markdown-body :deep(blockquote p) {
  margin: 8px 0;
}

.markdown-body :deep(strong) {
  font-weight: 600;
  color: #1f2329;
}

.dark-mode .markdown-body :deep(strong) {
  color: #ffffff;
}

.markdown-body :deep(em) {
  font-style: italic;
}

.markdown-body :deep(code) {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
  font-size: 0.9em;
  padding: 2px 6px;
  background-color: #f1f3f5;
  border-radius: 4px;
  color: #e74d3d;
}

.dark-mode .markdown-body :deep(code) {
  background-color: #2d2d2d;
  color: #f8c555;
}

.markdown-body :deep(pre) {
  position: relative;
  margin: 16px 0;
  padding: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f6f8fa;
  border: 1px solid #e1e4e8;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.dark-mode .markdown-body :deep(pre) {
  background: #1f2328;
  border-color: #30363d;
}

.markdown-body :deep(pre)::before {
  content: attr(data-language);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #24292f;
  color: #f0f6fc;
  font-size: 12px;
  font-weight: 500;
  text-transform: lowercase;
  letter-spacing: 0.5px;
}

.dark-mode .markdown-body :deep(pre)::before {
  background: #1a1f24;
}

.markdown-body :deep(pre .line-numbers) {
  position: absolute;
  top: 36px;
  left: 0;
  width: 44px;
  padding: 16px 0;
  background: #f1f3f4;
  border-right: 1px solid #e1e4e8;
  text-align: center;
  font-size: 12px;
  line-height: 1.5;
  color: #6e7781;
  user-select: none;
  z-index: 1;
}

.dark-mode .markdown-body :deep(pre .line-numbers) {
  background: #24292f;
  border-color: #30363d;
  color: #6e7781;
}

.markdown-body :deep(pre .line-number) {
  display: block;
}

.markdown-body :deep(pre code) {
  display: block;
  padding: 16px 16px 16px 58px;
  background: transparent;
  border-radius: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #24292e;
  overflow-x: auto;
  tab-size: 2;
  transition: all 0.2s;
}

.markdown-body :deep(pre code::selection) {
  background: rgba(36, 41, 46, 0.2);
}

.dark-mode .markdown-body :deep(pre code::selection) {
  background: rgba(230, 237, 243, 0.2);
}

.dark-mode .markdown-body :deep(pre code) {
  color: #e6edf3;
}

.markdown-body :deep(pre .copy-code-btn) {
  position: absolute;
  top: 8px;
  right: 12px;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  color: #f0f6fc;
  font-size: 12px;
}

.markdown-body :deep(pre .copy-code-btn:hover) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.dark-mode .markdown-body :deep(pre .copy-code-btn) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  color: #f0f6fc;
}

.dark-mode .markdown-body :deep(pre .copy-code-btn:hover) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.markdown-body :deep(a) {
  color: #667eea;
  text-decoration: none;
  transition: color 0.2s;
}

.markdown-body :deep(a:hover) {
  color: #764ba2;
  text-decoration: underline;
}

.markdown-body :deep(hr) {
  margin: 24px 0;
  border: none;
  border-top: 1px solid #e9ecef;
}

.markdown-body :deep(table) {
  width: 100%;
  margin: 16px 0;
  border-collapse: collapse;
  font-size: 14px;
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.markdown-body :deep(th) {
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  text-align: left;
  border-bottom: 2px solid #e9ecef;
}

.markdown-body :deep(td) {
  padding: 12px 16px;
  border-bottom: 1px solid #e9ecef;
  background: white;
}

.dark-mode .markdown-body :deep(td) {
  background: #2d2d2d;
  border-color: #3d3d3d;
  color: #e5e5e5;
}

.markdown-body :deep(tr:last-child td) {
  border-bottom: none;
}

.markdown-body :deep(tr:hover td) {
  background: #f8f9fa;
}

.dark-mode .markdown-body :deep(tr:hover td) {
  background: #3d3d3d;
}

.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 16px 0;
}

.markdown-body :deep(pre)::-webkit-scrollbar {
  height: 8px;
  width: 8px;
}

.markdown-body :deep(pre)::-webkit-scrollbar-track {
  background: #f1f3f4;
  border-radius: 4px;
}

.markdown-body :deep(pre)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
  border: 2px solid #f1f3f4;
}

.markdown-body :deep(pre)::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.dark-mode .markdown-body :deep(pre)::-webkit-scrollbar-track {
  background: #24292f;
}

.dark-mode .markdown-body :deep(pre)::-webkit-scrollbar-thumb {
  background: #484f58;
  border: 2px solid #24292f;
}

.dark-mode .markdown-body :deep(pre)::-webkit-scrollbar-thumb:hover {
  background: #57606a;
}

.thinking-block {
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #f8f9fa 0%, #f1f3f5 100%);
  border: 1px solid #e9ecef;
}

.dark-mode .thinking-block {
  background: linear-gradient(135deg, #2d2d2d 0%, #262626 100%);
  border-color: #3d3d3d;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
  background: rgba(102, 126, 234, 0.08);
  transition: background 0.2s;
}

.thinking-header:hover {
  background: rgba(102, 126, 234, 0.15);
}

.thinking-icon {
  transition: transform 0.3s;
  color: #667eea;
}

.thinking-icon.collapsed {
  transform: rotate(-90deg);
}

.thinking-label {
  font-size: 14px;
  font-weight: 500;
  color: #667eea;
}

.thinking-body {
  padding: 16px;
  font-size: 14px;
  line-height: 1.7;
  color: #6b7280;
  border-top: 1px solid #e9ecef;
}

.dark-mode .thinking-body {
  color: #9ca3af;
  border-color: #3d3d3d;
}

.streaming::after {
  content: '▋';
  display: inline-block;
  color: #667eea;
  animation: blink 1s infinite;
  margin-left: 2px;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

@media (max-width: 768px) {
  .markdown-body {
    font-size: 14px;
  }

  .markdown-body :deep(pre code) {
    padding-left: 16px;
    font-size: 12px;
  }

  .markdown-body :deep(pre .line-numbers) {
    display: none;
  }

  .thinking-body {
    padding: 12px;
  }
}
</style>
