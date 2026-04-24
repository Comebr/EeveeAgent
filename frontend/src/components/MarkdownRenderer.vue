<script setup>
import { computed, ref, watch, onMounted, nextTick } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import StructuredContentContainer from './StructuredContentContainer.vue'

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

// 自定义marked渲染器，为链接添加target="_blank"
const renderer = new marked.Renderer()
renderer.link = function(href, title, text) {
  const link = marked.Renderer.prototype.link.call(this, href, title, text)
  return link.replace('<a ', '<a target="_blank" rel="noopener noreferrer" ')
}

marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false,
  renderer: renderer
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

// 解析Markdown内容，分割成普通文本、表格和代码块
const parseMarkdownContent = (content) => {
  const parts = []
  let lastIndex = 0
  
  // 匹配代码块，提取语言信息
  const codeRegex = /```(\w*)[\s\S]*?```/g
  let match
  
  while ((match = codeRegex.exec(content)) !== null) {
    if (match.index > lastIndex) {
      parts.push({
        type: 'text',
        content: content.slice(lastIndex, match.index)
      })
    }
    parts.push({
      type: 'code',
      content: match[0],
      language: match[1] || 'plaintext'
    })
    lastIndex = match.index + match[0].length
  }
  
  // 匹配表格
  const tableRegex = /\|.*?\|\s*\n\|.*?---.*?\|\s*\n(?:\|.*?\|\s*\n)+/g
  const remainingContent = content.slice(lastIndex)
  lastIndex = 0
  
  while ((match = tableRegex.exec(remainingContent)) !== null) {
    if (match.index > lastIndex) {
      parts.push({
        type: 'text',
        content: remainingContent.slice(lastIndex, match.index)
      })
    }
    parts.push({
      type: 'table',
      content: match[0]
    })
    lastIndex = match.index + match[0].length
  }
  
  if (lastIndex < remainingContent.length) {
    parts.push({
      type: 'text',
      content: remainingContent.slice(lastIndex)
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

    // 解析Markdown内容，分割成不同部分
    const contentParts = parseMarkdownContent(part.content)
    
    return {
      ...part,
      contentParts: contentParts.map((contentPart, index) => {
        const rawHtml = marked.parse(contentPart.content)
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
          ...contentPart,
          renderedHtml: cleanHtml
        }
      })
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
      <div v-else class="markdown-content">
        <!-- 根据contentParts渲染不同类型的内容 -->
        <template v-if="part.contentParts">
          <template v-for="(contentPart, index) in part.contentParts" :key="index">
            <div v-if="contentPart.type === 'text'" v-html="contentPart.renderedHtml"></div>
            <StructuredContentContainer v-else-if="contentPart.type === 'table'" type="表格">
              <div v-html="contentPart.renderedHtml"></div>
            </StructuredContentContainer>
            <StructuredContentContainer 
              v-else-if="contentPart.type === 'code'" 
              type="代码"
              :language="contentPart.language || 'plaintext'"
            >
              <div v-html="contentPart.renderedHtml"></div>
            </StructuredContentContainer>
          </template>
        </template>
        <div v-else v-html="part.renderedHtml"></div>
      </div>
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
  margin: 0;
  padding: 0;
  border-radius: 0;
  overflow: hidden;
  background: #f6f8fa;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
}

.dark-mode .markdown-body :deep(pre) {
  background: #1f2328;
}

.markdown-body :deep(pre .line-numbers) {
  position: absolute;
  top: 0;
  left: 0;
  width: 44px;
  padding: 16px 0;
  background: #f1f3f4;
  border-right: 1px solid #e1e4e8;
  text-align: center;
  font-size: 13px;
  line-height: 1.6;
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
  font-size: 13px;
  line-height: 1.6;
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

/* 语法高亮配色 */
.markdown-body :deep(pre code .hljs-keyword) {
  color: #d73a49;
}

.markdown-body :deep(pre code .hljs-string) {
  color: #032f62;
}

.markdown-body :deep(pre code .hljs-comment) {
  color: #6a737d;
  font-style: italic;
}

.markdown-body :deep(pre code .hljs-function) {
  color: #6f42c1;
}

.markdown-body :deep(pre code .hljs-variable) {
  color: #e36209;
}

.dark-mode .markdown-body :deep(pre code .hljs-keyword) {
  color: #ff7b72;
}

.dark-mode .markdown-body :deep(pre code .hljs-string) {
  color: #a5d6ff;
}

.dark-mode .markdown-body :deep(pre code .hljs-comment) {
  color: #8b949e;
  font-style: italic;
}

.dark-mode .markdown-body :deep(pre code .hljs-function) {
  color: #d2a8ff;
}

.dark-mode .markdown-body :deep(pre code .hljs-variable) {
  color: #ffa657;
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
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.markdown-body :deep(th) {
  padding: 16px 20px;
  background: #f8f9fa;
  color: #495057;
  font-weight: 600;
  text-align: left;
  border-bottom: 1px solid #e9ecef;
  letter-spacing: 0.5px;
}

.markdown-body :deep(td) {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f3f5;
  background: white;
  line-height: 1.6;
}

.markdown-body :deep(td):last-child {
  border-right: none;
}

.dark-mode .markdown-body :deep(th) {
  background: #2d2d2d;
  color: #e5e5e5;
  border-bottom-color: #3d3d3d;
}

.dark-mode .markdown-body :deep(td) {
  background: #252525;
  border-bottom-color: #333;
  color: #e5e5e5;
}

.markdown-body :deep(tr:last-child td) {
  border-bottom: none;
}

.markdown-body :deep(tr:hover td) {
  background: #f8f9fa;
}

.dark-mode .markdown-body :deep(tr:hover td) {
  background: #303030;
}

/* 状态标签样式 */
.markdown-body :deep(.status-compliant) {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: #e8f5e8;
  color: #2e7d32;
}

.markdown-body :deep(.status-suspicious) {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: #fff3e0;
  color: #ef6c00;
}

.markdown-body :deep(.status-violation) {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: #ffebee;
  color: #c62828;
}

.dark-mode .markdown-body :deep(.status-compliant) {
  background: #1b5e20;
  color: #c8e6c9;
}

.dark-mode .markdown-body :deep(.status-suspicious) {
  background: #ef6c00;
  color: #fff3e0;
}

.dark-mode .markdown-body :deep(.status-violation) {
  background: #c62828;
  color: #ffebee;
}

/* 技术术语自动添加代码样式 */
.markdown-body :deep(td code) {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
  font-size: 0.9em;
  padding: 2px 6px;
  background-color: #f1f3f5;
  border-radius: 4px;
  color: #e74d3d;
}

.dark-mode .markdown-body :deep(td code) {
  background-color: #2d2d2d;
  color: #f8c555;
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
