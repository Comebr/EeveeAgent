<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-light.css'

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

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false,
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  }
})

// 渲染后的 HTML
const renderedHtml = computed(() => {
  if (!props.content) return ''
  
  // 使用 marked 解析 Markdown
  const rawHtml = marked.parse(props.content)
  
  // 使用 DOMPurify 清理 HTML，防止 XSS
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
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'target', 'data-language', 'data-code']
  })
  
  return cleanHtml
})

// 代码高亮
const contentRef = ref(null)

const highlightCode = () => {
  if (contentRef.value) {
    // 高亮代码块
    contentRef.value.querySelectorAll('pre code').forEach((block) => {
      hljs.highlightElement(block)
    })
    
    // 添加复制按钮
    addCopyButtons()
  }
}

// 添加复制按钮
const addCopyButtons = () => {
  if (contentRef.value) {
    contentRef.value.querySelectorAll('pre').forEach((pre) => {
      // 检查是否已经有复制按钮
      if (!pre.querySelector('.copy-button')) {
        const copyButton = document.createElement('button')
        copyButton.className = 'copy-button'
        copyButton.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg>'
        copyButton.title = '复制代码'
        
        copyButton.addEventListener('click', () => {
          const code = pre.querySelector('code')?.textContent || ''
          navigator.clipboard.writeText(code).then(() => {
            copyButton.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
            copyButton.title = '已复制!'
            setTimeout(() => {
              copyButton.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg>'
              copyButton.title = '复制代码'
            }, 2000)
          })
        })
        
        pre.appendChild(copyButton)
      }
    })
  }
}

watch(() => props.content, () => {
  highlightCode()
}, { immediate: true })

onMounted(() => {
  highlightCode()
})
</script>

<template>
  <div 
    ref="contentRef"
    class="markdown-content"
    :class="{ 'streaming': isStreaming }"
    v-html="renderedHtml"
  />
</template>

<style scoped>
.markdown-content {
  font-size: 15px;
  line-height: 1.7;
  color: #374151;
}

.markdown-content :deep(h1) {
  font-size: 1.5em;
  font-weight: 600;
  margin: 1.5em 0 0.5em;
  color: #111827;
}

.markdown-content :deep(h2) {
  font-size: 1.3em;
  font-weight: 600;
  margin: 1.3em 0 0.5em;
  color: #111827;
}

.markdown-content :deep(h3) {
  font-size: 1.1em;
  font-weight: 600;
  margin: 1.2em 0 0.5em;
  color: #111827;
}

.markdown-content :deep(p) {
  margin: 0.8em 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.8em 0;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin: 0.3em 0;
}

.markdown-content :deep(ul) {
  list-style-type: disc;
}

.markdown-content :deep(ol) {
  list-style-type: decimal;
}

.markdown-content :deep(strong) {
  font-weight: 600;
  color: #111827;
}

.markdown-content :deep(em) {
  font-style: italic;
}

.markdown-content :deep(code) {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
  font-size: 0.9em;
  background-color: #f3f4f6;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  color: #ef4444;
}

/* 代码块样式优化 */
.markdown-content :deep(pre) {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 0;
  margin: 1.5em 0;
  overflow-x: auto;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  border: 1px solid #e9ecef;
  position: relative;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 1.2em 1.5em;
  border-radius: 0;
  color: inherit;
  font-size: 0.85em;
  line-height: 1.6;
  display: block;
  tab-size: 2;
}

/* 代码语言标识 */
.markdown-content :deep(pre)::before {
  content: attr(data-language) || 'code';
  display: block;
  padding: 0.5em 1.5em;
  background-color: #e9ecef;
  color: #495057;
  font-size: 0.75em;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
  border-bottom: 1px solid #dee2e6;
  border-radius: 8px 8px 0 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 复制按钮 */
.markdown-content :deep(pre .copy-button) {
  position: absolute;
  top: 8px;
  right: 12px;
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 1;
}

.markdown-content :deep(pre .copy-button:hover) {
  background: #f8f9fa;
  border-color: #ced4da;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #e5e7eb;
  padding-left: 1em;
  margin: 1em 0;
  color: #6b7280;
}

.markdown-content :deep(a) {
  color: #667eea;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 1.5em 0;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 0.5em;
  text-align: left;
}

.markdown-content :deep(th) {
  background-color: #f9fafb;
  font-weight: 600;
}

/* 滚动条样式 */
.markdown-content :deep(pre::-webkit-scrollbar) {
  height: 8px;
}

.markdown-content :deep(pre::-webkit-scrollbar-track) {
  background: #f8f9fa;
  border-radius: 0 0 8px 8px;
}

.markdown-content :deep(pre::-webkit-scrollbar-thumb) {
  background: #dee2e6;
  border-radius: 4px;
}

.markdown-content :deep(pre::-webkit-scrollbar-thumb:hover) {
  background: #ced4da;
}

/* 打字机光标效果 */
.markdown-content.streaming::after {
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
</style>
