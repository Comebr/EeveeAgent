<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import MarkdownRenderer from './MarkdownRenderer.vue'
import BaseCard from './common/BaseCard.vue'
import BaseButton from './common/BaseButton.vue'

const router = useRouter()
const userInfo = ref(null)
const showUserMenu = ref(false)
const sidebarCollapsed = ref(false)
const sidebarAutoHidden = ref(false) // 默认展开
const mouseNearSidebar = ref(false)

// 消息提示
const showMessage = ref(false)
const messageText = ref('')
const messageType = ref('success') // success, error, info

// 显示消息
const showToast = (text, type = 'success', duration = 3000) => {
  messageText.value = text
  messageType.value = type
  showMessage.value = true
  
  setTimeout(() => {
    showMessage.value = false
  }, duration)
}

// 聊天相关状态
const messages = ref([])
const inputText = ref('')
const isLoading = ref(false)
const chatContentRef = ref(null)
const autoScroll = ref(true) // 自动滚动标志
const isStreaming = ref(false) // 是否正在流式输出
const eventSource = ref(null) // 存储EventSource实例，用于暂停流式输出
const showScrollButton = ref(false) // 是否显示回到底部的按钮

// 实时获取用户信息
const fetchUserInfo = async () => {
  try {
    const response = await axios.get('/api/management/current')
    if (response.data.code === '0') {
      userInfo.value = response.data.data
      // 更新本地存储
      localStorage.setItem('userInfo', JSON.stringify(response.data.data))
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // 如果获取失败，尝试从本地存储获取
    const storedUserInfo = localStorage.getItem('userInfo')
    if (storedUserInfo) {
      userInfo.value = JSON.parse(storedUserInfo)
    }
  }
}

// 滚动到聊天底部
const scrollToBottom = async () => {
  await nextTick()
  if (chatContentRef.value && autoScroll.value) {
    chatContentRef.value.scrollTop = chatContentRef.value.scrollHeight
  }
}

// 处理滚动事件，当用户手动滚动时，禁用自动滚动
const handleScroll = () => {
  if (chatContentRef.value) {
    const { scrollTop, scrollHeight, clientHeight } = chatContentRef.value
    // 当滚动位置离底部超过10px时，认为用户在查看历史内容，禁用自动滚动
    // 减小阈值，使autoScroll更快地被禁用，减少抖动
    autoScroll.value = scrollHeight - scrollTop - clientHeight < 10
    // 当不在底部时显示回到底部的按钮
    showScrollButton.value = scrollHeight - scrollTop - clientHeight >= 10
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputText.value.trim()) return
  
  // 添加用户消息
  const userMessage = {
    id: Date.now(),
    content: inputText.value.trim(),
    role: 'user',
    timestamp: new Date().toISOString()
  }
  messages.value.push(userMessage)
  inputText.value = ''
  
  // 滚动到底部
  await scrollToBottom()
  
  // 添加AI正在思考的消息
  const aiThinkingMessage = {
    id: Date.now() + 1,
    content: '🤔 正在思考中...',
    role: 'assistant',
    timestamp: new Date().toISOString(),
    isThinking: true
  }
  messages.value.push(aiThinkingMessage)
  
  // 滚动到底部
  await scrollToBottom()
  
  try {
    const token = localStorage.getItem('token')
    
    // 由于 SSE 只支持 GET 请求，我们需要将参数作为查询参数传递
    const url = `/agent/rag/streamingChat?prompt=${encodeURIComponent(userMessage.content)}`
    
    eventSource.value = new EventSource(url)
    isStreaming.value = true
    
    let buffer = ''
    let displayContent = ''
    let firstDataReceived = false
    let isComplete = false
    let outputTimer = null
    
    const startSmoothOutput = () => {
        const CHAR_PER_INTERVAL = 2
        const INTERVAL_MS = 30
        
        outputTimer = setInterval(() => {
          if (buffer.length > 0) {
            const charsToAdd = Math.min(buffer.length, CHAR_PER_INTERVAL)
            displayContent += buffer.substring(0, charsToAdd)
            buffer = buffer.substring(charsToAdd)
            
            // 流式输出过程中使用临时内容，避免频繁更新导致代码高亮闪烁
            const msgIndex = messages.value.findIndex(msg => msg.id === aiThinkingMessage.id)
            if (msgIndex !== -1) {
              messages.value[msgIndex] = {
                ...messages.value[msgIndex],
                content: displayContent,
                isStreaming: true
              }
            }
            nextTick(() => {
              scrollToBottom()
            })
          } else if (isComplete) {
            clearInterval(outputTimer)
            outputTimer = null
          }
        }, INTERVAL_MS)
        
        // 存储定时器以便清理
        if (!window.__chatTimers) {
          window.__chatTimers = []
        }
        window.__chatTimers.push(outputTimer)
      }
    
    eventSource.value.onmessage = (event) => {
        if (!firstDataReceived) {
          firstDataReceived = true
          const msgIndex = messages.value.findIndex(msg => msg.id === aiThinkingMessage.id)
          if (msgIndex !== -1) {
            messages.value[msgIndex] = {
              ...messages.value[msgIndex],
              isThinking: false,
              isStreaming: true,
              content: ''
            }
          }
          startSmoothOutput()
        }
        
        buffer += event.data
      }
      
      eventSource.value.onerror = (error) => {
        console.error('SSE连接错误:', error)
        eventSource.value.close()
        isComplete = true
        isStreaming.value = false
        // 等待缓冲区数据处理完成后再清理定时器和标记结束
        const checkBuffer = () => {
          if (buffer.length === 0) {
            if (outputTimer) {
              clearInterval(outputTimer)
              outputTimer = null
            }
            const msgIndex = messages.value.findIndex(msg => msg.id === aiThinkingMessage.id)
            if (msgIndex !== -1) {
              // 一次性更新最终内容，确保代码高亮和语言显示正确
              messages.value[msgIndex] = {
                ...messages.value[msgIndex],
                isStreaming: false,
                content: displayContent
              }
            }
          } else {
            // 继续等待缓冲区处理
            setTimeout(checkBuffer, 50)
          }
        }
        checkBuffer()
        if (!firstDataReceived) {
          messages.value = messages.value.filter(msg => !msg.isThinking)
          messages.value.push({
            id: Date.now() + 3,
            content: '抱歉，处理您的请求时出现错误，请稍后重试。',
            role: 'assistant',
            timestamp: new Date().toISOString()
          })
          nextTick(() => {
            scrollToBottom()
          })
        }
      }
      
      eventSource.value.addEventListener('close', () => {
        eventSource.value.close()
        isComplete = true
        isStreaming.value = false
        // 等待缓冲区数据处理完成后再标记流式传输结束
        const checkBuffer = () => {
          if (buffer.length === 0) {
            if (outputTimer) {
              clearInterval(outputTimer)
              outputTimer = null
            }
            const msgIndex = messages.value.findIndex(msg => msg.id === aiThinkingMessage.id)
            if (msgIndex !== -1) {
              // 一次性更新最终内容，确保代码高亮和语言显示正确
              messages.value[msgIndex] = {
                ...messages.value[msgIndex],
                isStreaming: false,
                content: displayContent
              }
            }
          } else {
            // 继续等待缓冲区处理
            setTimeout(checkBuffer, 50)
          }
        }
        checkBuffer()
      })
    
  } catch (error) {
    console.error('聊天请求失败:', error)
    messages.value = messages.value.filter(msg => !msg.isThinking)
    messages.value.push({
      id: Date.now() + 3,
      content: '抱歉，处理您的请求时出现错误，请稍后重试。',
      role: 'assistant',
      timestamp: new Date().toISOString()
    })
    await scrollToBottom()
  }
}

// 重新生成回答
const regenerateResponse = async (message) => {
  // 前端实现：重新发送上一条用户消息
  const userMessages = messages.value.filter(msg => msg.role === 'user')
  if (userMessages.length > 0) {
    const lastUserMessage = userMessages[userMessages.length - 1]
    
    // 标记当前消息为思考中
    const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
    if (msgIndex !== -1) {
      messages.value[msgIndex] = {
        ...messages.value[msgIndex],
        isThinking: true,
        content: '',
        isStreaming: false
      }
      await scrollToBottom()
    }
    
    // 直接发送请求，不添加新消息
    try {
      const token = localStorage.getItem('token')
      
      // 由于 SSE 只支持 GET 请求，我们需要将参数作为查询参数传递
      const url = `/agent/rag/streamingChat?prompt=${encodeURIComponent(lastUserMessage.content)}`
      
      const eventSource = new EventSource(url)
      
      let buffer = ''
      let displayContent = ''
      let firstDataReceived = false
      let isComplete = false
      let outputTimer = null
      
      const startSmoothOutput = () => {
        const CHAR_PER_INTERVAL = 2
        const INTERVAL_MS = 30
        
        outputTimer = setInterval(() => {
          if (buffer.length > 0) {
            const charsToAdd = Math.min(buffer.length, CHAR_PER_INTERVAL)
            displayContent += buffer.substring(0, charsToAdd)
            buffer = buffer.substring(charsToAdd)
            
            // 流式输出过程中使用临时内容，避免频繁更新导致代码高亮闪烁
            const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
            if (msgIndex !== -1) {
              messages.value[msgIndex] = {
                ...messages.value[msgIndex],
                content: displayContent,
                isStreaming: true
              }
            }
            nextTick(() => {
              scrollToBottom()
            })
          } else if (isComplete) {
            clearInterval(outputTimer)
            outputTimer = null
          }
        }, INTERVAL_MS)
        
        // 存储定时器以便清理
        if (!window.__chatTimers) {
          window.__chatTimers = []
        }
        window.__chatTimers.push(outputTimer)
      }
      
      eventSource.onmessage = (event) => {
        if (!firstDataReceived) {
          firstDataReceived = true
          const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
          if (msgIndex !== -1) {
            messages.value[msgIndex] = {
              ...messages.value[msgIndex],
              isThinking: false,
              isStreaming: true,
              content: ''
            }
          }
          startSmoothOutput()
        }
        
        buffer += event.data
      }
      
      eventSource.onerror = (error) => {
        console.error('SSE连接错误:', error)
        eventSource.close()
        isComplete = true
        // 等待缓冲区数据处理完成后再清理定时器和标记结束
        const checkBuffer = () => {
          if (buffer.length === 0) {
            if (outputTimer) {
              clearInterval(outputTimer)
              outputTimer = null
            }
            const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
            if (msgIndex !== -1) {
              // 一次性更新最终内容，确保代码高亮和语言显示正确
              messages.value[msgIndex] = {
                ...messages.value[msgIndex],
                isStreaming: false,
                content: displayContent
              }
            }
          } else {
            // 继续等待缓冲区处理
            setTimeout(checkBuffer, 50)
          }
        }
        checkBuffer()
        if (!firstDataReceived) {
          const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
          if (msgIndex !== -1) {
            messages.value[msgIndex] = {
              ...messages.value[msgIndex],
              isThinking: false,
              isStreaming: false,
              content: '抱歉，处理您的请求时出现错误，请稍后重试。'
            }
          }
          nextTick(() => {
            scrollToBottom()
          })
        }
      }
      
      eventSource.addEventListener('close', () => {
        eventSource.close()
        isComplete = true
        // 等待缓冲区数据处理完成后再标记流式传输结束
        const checkBuffer = () => {
          if (buffer.length === 0) {
            if (outputTimer) {
              clearInterval(outputTimer)
              outputTimer = null
            }
            const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
            if (msgIndex !== -1) {
              // 一次性更新最终内容，确保代码高亮和语言显示正确
              messages.value[msgIndex] = {
                ...messages.value[msgIndex],
                isStreaming: false,
                content: displayContent
              }
            }
          } else {
            // 继续等待缓冲区处理
            setTimeout(checkBuffer, 50)
          }
        }
        checkBuffer()
      })
      
    } catch (error) {
      console.error('聊天请求失败:', error)
      const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
      if (msgIndex !== -1) {
        messages.value[msgIndex] = {
          ...messages.value[msgIndex],
          isThinking: false,
          isStreaming: false,
          content: '抱歉，处理您的请求时出现错误，请稍后重试。'
        }
      }
      await scrollToBottom()
    }
  }
}

// 复制消息
const copyMessage = async (message) => {
  try {
    await navigator.clipboard.writeText(message.content)
    // 标记为已复制
    const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
    if (msgIndex !== -1) {
      messages.value[msgIndex] = {
        ...messages.value[msgIndex],
        copied: true
      }
      // 3秒后恢复原样
      setTimeout(() => {
        const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
        if (msgIndex !== -1) {
          messages.value[msgIndex] = {
            ...messages.value[msgIndex],
            copied: false
          }
        }
      }, 3000)
    }
  } catch (error) {
    console.error('复制失败:', error)
  }
}

// 点赞
const toggleLike = (message) => {
  const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
  if (msgIndex !== -1) {
    messages.value[msgIndex] = {
      ...messages.value[msgIndex],
      liked: !messages.value[msgIndex].liked,
      disliked: false // 点赞时取消点踩
    }
  }
}

// 点踩
const toggleDislike = (message) => {
  const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
  if (msgIndex !== -1) {
    messages.value[msgIndex] = {
      ...messages.value[msgIndex],
      disliked: !messages.value[msgIndex].disliked,
      liked: false // 点踩时取消点赞
    }
  }
}

// 处理输入框回车事件
const handleKeyPress = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

// 暂停流式输出
const pauseStreaming = () => {
  if (eventSource.value) {
    eventSource.value.close()
    isStreaming.value = false
  }
}

// 手动滚动到底部
const scrollToBottomManually = async () => {
  autoScroll.value = true
  showScrollButton.value = false
  await scrollToBottom()
}

onMounted(() => {
  // 实时获取用户信息
  fetchUserInfo()
  
  // 点击外部关闭菜单
  document.addEventListener('click', handleClickOutside)
  
  // 登录成功后显示消息
const showLoginSuccess = localStorage.getItem('showLoginSuccess') === 'true'
if (showLoginSuccess) {
  showToast('已成功登录', 'success')
  localStorage.removeItem('showLoginSuccess')
}
  
  // 智能隐藏侧边栏相关事件
  document.addEventListener('click', handleClick)
  document.addEventListener('wheel', handleWheel)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  
  // 移除智能隐藏侧边栏相关事件
  document.removeEventListener('click', handleClick)
  document.removeEventListener('wheel', handleWheel)
  
  // 清理所有可能的定时器
  const timers = window.__chatTimers || []
  timers.forEach(timer => clearInterval(timer))
  window.__chatTimers = []
})

const handleClickOutside = (event) => {
  const menu = document.querySelector('.user-menu-container')
  if (menu && !menu.contains(event.target)) {
    showUserMenu.value = false
  }
}

const handleLogout = () => {
  // 清除本地存储
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.setItem('showLogoutSuccess', 'true')
  // 跳转到登录页
  window.location.href = '/'
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  // 切换时重置自动隐藏状态
  sidebarAutoHidden.value = false
}

// 处理点击事件
const handleClick = (event) => {
  // 当点击侧边栏区域或侧边栏切换按钮时，显示侧边栏
  const sidebar = document.querySelector('.sidebar')
  const sidebarToggle = document.querySelector('.sidebar-toggle')
  
  if (sidebar && (sidebar.contains(event.target) || (sidebarToggle && sidebarToggle.contains(event.target)))) {
    sidebarAutoHidden.value = false
  }
}

// 处理滚轮事件
const handleWheel = (event) => {
  // 当鼠标在侧边栏区域内滚动时，显示侧边栏
  const sidebar = document.querySelector('.sidebar')
  if (sidebar) {
    const rect = sidebar.getBoundingClientRect()
    // 检测鼠标是否在侧边栏区域内（包括可能隐藏的区域）
    if (event.clientX < rect.right + 50) {
      sidebarAutoHidden.value = false
    }
  }
}

const goToAdmin = () => {
  router.push('/admin')
}
</script>

<template>
  <div class="home-container">
    <!-- 消息提示 -->
    <div v-if="showMessage" :class="['toast-message', messageType]">
      <div class="toast-content">
        <svg v-if="messageType === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        <svg v-else-if="messageType === 'error'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="15" y1="9" x2="9" y2="15"/>
          <line x1="9" y1="9" x2="15" y2="15"/>
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="16" x2="12" y2="12"/>
          <line x1="12" y1="8" x2="12.01" y2="8"/>
        </svg>
        <span>{{ messageText }}</span>
      </div>
    </div>
    
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed, 'auto-hidden': sidebarAutoHidden }">
      <!-- Logo区域 -->
      <div class="sidebar-header">
        <div class="logo">
          <div class="logo-icon">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="8" fill="#667eea"/>
              <path d="M8 12h16M8 16h12M8 20h8" stroke="white" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <span class="logo-text" v-show="!sidebarCollapsed">EeveeAgent</span>
        </div>
      </div>
      
      <!-- 通用功能 -->
      <div class="sidebar-section">
        <h3 class="section-title" v-show="!sidebarCollapsed">通用功能</h3>
        <div class="menu-list">
          <div class="menu-item active" :class="{ collapsed: sidebarCollapsed }">
            <span class="menu-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
                <line x1="9" y1="9" x2="9.01" y2="9"/>
                <line x1="15" y1="9" x2="15.01" y2="9"/>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">AI 对话</span>
          </div>
        </div>
      </div>
      
      <!-- 历史会话 -->
      <div class="sidebar-section">
        <h3 class="section-title" v-show="!sidebarCollapsed">历史会话</h3>
        <!-- 历史会话列表（暂不实现具体内容） -->
      </div>
      
      <!-- 底部用户信息 -->
      <div class="sidebar-footer">
        <div class="user-menu-container" v-show="!sidebarCollapsed">
          <div class="user-info" @click="toggleUserMenu">
              <div class="avatar">
                <img v-if="userInfo?.avatar" :src="userInfo.avatar" alt="avatar" />
                <span v-else>{{ userInfo?.username?.charAt(0).toUpperCase() || 'U' }}</span>
              </div>
              <div class="user-details">
                <div class="username">{{ userInfo?.username || 'MOMO' }}</div>
                <div class="role">{{ userInfo?.role || 'User' }}</div>
              </div>
              <button class="more-button">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="5" r="1"/>
                  <circle cx="12" cy="12" r="1"/>
                  <circle cx="12" cy="19" r="1"/>
                </svg>
              </button>
            </div>
          
          <!-- 下拉菜单 -->
          <div v-if="showUserMenu" class="user-dropdown">
            <div class="dropdown-item" @click="handleLogout">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
              <span>退出登录</span>
            </div>
          </div>
        </div>
        <!-- 折叠状态下的用户头像 -->
        <div v-show="sidebarCollapsed" class="collapsed-avatar">
          <div class="avatar">
            <img v-if="userInfo?.avatar" :src="userInfo.avatar" alt="avatar" />
            <span v-else>{{ userInfo?.username?.charAt(0).toUpperCase() || 'U' }}</span>
          </div>
        </div>
      </div>
    </aside>
    
    <!-- 主内容区 -->
    <main class="main-content" :class="{ expanded: sidebarCollapsed || sidebarAutoHidden }">
      <!-- 顶部header -->
      <header class="main-header">
        <div class="header-left">
          <!-- 侧边栏展开/收起按钮 -->
          <button class="sidebar-toggle" @click="toggleSidebar">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="12" x2="21" y2="12"/>
              <line x1="3" y1="6" x2="21" y2="6"/>
              <line x1="3" y1="18" x2="21" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="header-right">
          <!-- 管理后台按钮 -->
          <button class="admin-button" @click="goToAdmin">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
            <span>管理后台</span>
          </button>
          <!-- 新建对话按钮 -->
          <button class="new-chat-button">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            <span>新建对话</span>
          </button>
        </div>
      </header>
      
      <!-- 聊天区域 -->
      <div class="chat-container">
        <div class="chat-content" ref="chatContentRef" @scroll="handleScroll">
          <!-- 回到底部按钮 -->
          <button 
            v-if="showScrollButton" 
            class="scroll-to-bottom-button" 
            @click="scrollToBottomManually"
            title="回到底部"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="18 15 12 21 6 15"/>
            </svg>
          </button>
          <!-- 消息列表 -->
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="robot-icon">
              <svg width="80" height="80" viewBox="0 0 80 80" fill="none">
                <circle cx="40" cy="40" r="36" fill="#EEF2FF"/>
                <circle cx="40" cy="40" r="28" fill="#667eea"/>
                <circle cx="32" cy="36" r="4" fill="white"/>
                <circle cx="48" cy="36" r="4" fill="white"/>
                <path d="M32 48c4 4 12 4 16 0" stroke="white" stroke-width="2" stroke-linecap="round"/>
                <rect x="28" y="20" width="8" height="4" rx="2" fill="#667eea"/>
                <rect x="44" y="20" width="8" height="4" rx="2" fill="#667eea"/>
              </svg>
            </div>
            <div class="welcome-text">
              <p class="greeting">你好，{{ userInfo?.username || 'MOMO' }}</p>
              <p class="question">今天需要我帮你做点什么吗？</p>
            </div>
          </div>
          
          <div v-else class="message-list">
            <!-- 用户消息 -->
            <div v-for="message in messages" :key="message.id" :class="['message-item', message.role]">
              <div v-if="message.role === 'user'" class="user-message">
                <div class="message-content">{{ message.content }}</div>
                <div class="message-avatar">
                  <div class="avatar">
                    <img v-if="userInfo?.avatar" :src="userInfo.avatar" alt="avatar" />
                    <span v-else>{{ userInfo?.username?.charAt(0).toUpperCase() || 'U' }}</span>
                  </div>
                </div>
              </div>
              
              <div v-else class="ai-message">
                <div class="message-avatar">
                  <div class="ai-avatar" style="background-color: #667eea; border-radius: 50%; display: flex; align-items: center; justify-content: center; width: 36px; height: 36px;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                      <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                      <line x1="8" y1="21" x2="16" y2="21"/>
                      <line x1="12" y1="17" x2="12" y2="21"/>
                    </svg>
                  </div>
                </div>
                <div class="message-bubble">
                  <div v-if="message.isThinking" class="thinking-indicator">
                    <div class="thinking-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <span class="thinking-text">正在思考...</span>
                  </div>
                  <div v-else class="message-content">
                    <BaseCard>
                      <MarkdownRenderer 
                        :content="message.content" 
                        :is-streaming="message.isStreaming"
                      />
                    </BaseCard>
                  </div>
                  
                  <!-- AI消息操作按钮 -->
                  <div v-if="!message.isThinking" class="message-actions">
                    <button class="action-btn" title="重新生成" @click="regenerateResponse(message)">
                      <svg width="16" height="16" viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M935.005091 459.752727a34.909091 34.909091 0 1 1 49.361454 49.361455l-78.382545 78.382545a34.816 34.816 0 0 1-49.338182 0l-78.405818-78.382545a34.909091 34.909091 0 1 1 49.361455-49.361455l14.801454 14.824728C818.525091 311.738182 678.330182 186.181818 508.928 186.181818c-130.466909 0-250.484364 76.706909-305.710545 195.397818a34.932364 34.932364 0 0 1-63.301819-29.463272C206.522182 208.896 351.418182 116.363636 508.904727 116.363636c210.152727 0 383.534545 159.953455 404.992 364.474182l21.085091-21.085091z m-73.960727 189.021091a34.932364 34.932364 0 0 1 16.965818 46.382546C811.310545 838.353455 666.461091 930.909091 508.951273 930.909091c-210.106182 0-383.534545-159.953455-404.968728-364.497455l-21.108363 21.108364a34.909091 34.909091 0 1 1-49.384727-49.361455l78.42909-78.42909a34.909091 34.909091 0 0 1 49.338182 0l78.382546 78.42909a34.909091 34.909091 0 1 1-49.338182 49.338182l-14.824727-14.801454C199.354182 735.534545 339.549091 861.090909 508.951273 861.090909c130.490182 0 250.507636-76.706909 305.710545-195.397818a34.909091 34.909091 0 0 1 46.382546-16.919273z" fill="#797979" p-id="5070"></path>
                      </svg>
                    </button>
                    <button class="action-btn" title="复制" @click="copyMessage(message)" :class="{ 'copied': message.copied }">
                      <svg v-if="!message.copied" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
                        <rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
                      </svg>
                      <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="20 6 9 17 4 12"/>
                      </svg>
                    </button>
                    <button class="action-btn" title="点赞" @click="toggleLike(message)" :class="{ 'active': message.liked }">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>
                      </svg>
                    </button>
                    <button class="action-btn" title="点踩" @click="toggleDislike(message)" :class="{ 'active': message.disliked }">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.28a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3z"/>
                      </svg>
                    </button>
                    <button class="action-btn" title="更多">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="1"/>
                        <circle cx="19" cy="12" r="1"/>
                        <circle cx="5" cy="12" r="1"/>
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 输入区域 -->
        <div class="chat-input-area">
          <div class="input-wrapper">
            <input 
              type="text" 
              v-model="inputText" 
              placeholder="请输入内容..." 
              @keypress="handleKeyPress"
              :disabled="isLoading"
            />
            <div class="input-actions">
              <button class="action-button">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
                <span>上传图片</span>
              </button>
              <button class="action-button">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
                <span>上传文档</span>
              </button>
              <div class="char-count">{{ inputText.length }}/200</div>
              <button 
                v-if="!isStreaming" 
                class="send-button" 
                @click="sendMessage"
                :disabled="isLoading || !inputText.trim()"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="22" y1="2" x2="11" y2="13"/>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
              </button>
              <button 
                v-else 
                class="pause-button" 
                @click="pauseStreaming"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="6" y="4" width="4" height="16"/>
                  <rect x="14" y="4" width="4" height="16"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.home-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  background-color: #f8f9fa;
  overflow: hidden;
}

/* 消息提示样式 */
.toast-message {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  animation: slideDown 0.3s ease-out;
}

.toast-message.success {
  background-color: #f0fdf4;
  border: 1px solid #dcfce7;
  color: #166534;
}

.toast-message.error {
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  color: #b91c1c;
}

.toast-message.info {
  background-color: #eff6ff;
  border: 1px solid #dbeafe;
  color: #1d4ed8;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translate(-50%, -20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

/* 侧边栏样式 */
.sidebar {
  width: 260px;
  min-width: 260px;
  background-color: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: all 0.3s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 60px;
  min-width: 60px;
}

.sidebar.auto-hidden {
  width: 0;
  min-width: 0;
  overflow: hidden;
}

.sidebar.auto-hidden .sidebar-header,
.sidebar.auto-hidden .sidebar-section,
.sidebar.auto-hidden .sidebar-footer {
  opacity: 0;
  transform: translateX(-100%);
  transition: all 0.3s ease;
}

.sidebar:not(.auto-hidden) .sidebar-header,
.sidebar:not(.auto-hidden) .sidebar-section,
.sidebar:not(.auto-hidden) .sidebar-footer {
  opacity: 1;
  transform: translateX(0);
  transition: all 0.3s ease;
}

/* Logo区域 */
.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
}

/* 侧边栏内容区域 */
.sidebar-section {
  padding: 16px 12px;
  flex-shrink: 0;
}

.section-title {
  font-size: 12px;
  font-weight: 500;
  color: #999;
  margin-bottom: 8px;
  padding: 0 8px;
  white-space: nowrap;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #666;
  font-size: 14px;
  white-space: nowrap;
}

.menu-item.collapsed {
  justify-content: center;
  padding: 10px;
}

.menu-item:hover {
  background-color: #f5f5f5;
  color: #333;
}

.menu-item.active {
  background-color: #f0f5ff;
  color: #667eea;
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 侧边栏底部 */
.sidebar-footer {
  margin-top: auto;
  padding: 12px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.user-menu-container {
  position: relative;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-details {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.more-button {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.more-button:hover {
  background-color: #e8e8e8;
  color: #666;
}

/* 折叠状态下的头像 */
.collapsed-avatar {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

/* 用户下拉菜单 */
.user-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  margin-bottom: 8px;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 4px;
  z-index: 100;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #ef4444;
  transition: all 0.2s ease;
}

.dropdown-item:hover {
  background-color: #fef2f2;
  color: #dc2626;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* 顶部header */
.main-header {
  height: 60px;
  padding: 0 24px;
  background-color: white;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.sidebar-toggle {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  transition: all 0.2s ease;
}

.sidebar-toggle:hover {
  background-color: #f5f5f5;
  color: #000;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background-color: white;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  color: #667eea;
  cursor: pointer;
  transition: all 0.2s ease;
}

.admin-button:hover {
  background-color: #f0f5ff;
  border-color: #667eea;
  color: #5a6fe6;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.new-chat-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 6px;
  font-size: 14px;
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.new-chat-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 聊天区域 */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-content {
  flex: 1;
  overflow-y: auto;
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  /* 隐藏滚动条 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

/* 隐藏滚动条 - Chrome, Safari and Opera */
.chat-content::-webkit-scrollbar {
  display: none;
}

.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.robot-icon {
  margin-bottom: 24px;
}

.welcome-text {
  color: #1a1a1a;
}

.greeting {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 8px;
}

.question {
  font-size: 16px;
  color: #666;
}

/* 消息列表 */
.message-list {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 800px;
}

.message-item {
  display: block;
  width: 100%;
}

/* 用户消息 */
.user-message {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  justify-content: flex-end;
  width: 100%;
  margin-bottom: 20px;
}

.user-message .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 12px 16px;
  border-radius: 18px 18px 4px 18px;
  font-size: 14px;
  line-height: 1.4;
  word-wrap: break-word;
  max-width: 80%;
}

.user-message .message-avatar {
  display: flex;
  align-items: flex-end;
}

/* AI消息 */
.ai-message {
  display: flex;
  gap: 12px;
  justify-content: flex-start;
  width: 100%;
  margin-bottom: 20px;
}

.ai-message .message-avatar {
  display: flex;
  align-items: flex-start;
  flex-shrink: 0;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-bubble {
  flex: 1;
  max-width: calc(100% - 60px);
}

.message-content :deep(.base-card) {
  margin: 0;
}

/* 思考中动画 */
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background-color: #f8f9fa;
  border-radius: 12px;
  width: fit-content;
}

.thinking-dots {
  display: flex;
  gap: 4px;
}

.thinking-dots span {
  width: 8px;
  height: 8px;
  background-color: #667eea;
  border-radius: 50%;
  animation: thinking-bounce 1.4s infinite ease-in-out both;
}

.thinking-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.thinking-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes thinking-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.thinking-text {
  font-size: 14px;
  color: #666;
}

/* 消息操作按钮 */
.message-actions {
  display: flex;
  gap: 6px;
  margin-top: 12px;
  padding-left: 4px;
}

.action-btn {
  width: 28px;
  height: 28px;
  border: none;
  background-color: #f5f5f5;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.action-btn:hover {
  background-color: #e8e8e8;
  color: #333;
}

/* 重新生成按钮颜色调整 */
.action-btn:first-child svg {
  stroke: #333;
}

/* 复制成功状态 */
.action-btn.copied {
  background-color: #d4edda;
  color: #155724;
}

/* 点赞/踩激活状态 */
.action-btn.active {
  color: #667eea;
  background-color: #f0f5ff;
}

/* 输入区域 */
.chat-input-area {
  padding: 20px 40px 40px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.input-wrapper {
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  width: 100%;
  max-width: 800px;
  margin: 0;
}

.input-wrapper input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  padding: 8px 0;
  margin-bottom: 12px;
  color: #1a1a1a;
}

.input-wrapper input::placeholder {
  color: #999;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background-color: #f5f5f5;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-button:hover {
  background-color: #e8e8e8;
  color: #333;
}

.char-count {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}

.send-button {
  width: 36px;
  height: 36px;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.2s ease;
}

.send-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.pause-button {
  width: 36px;
  height: 36px;
  border: none;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.2s ease;
}

.pause-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

/* 回到底部按钮 */
.scroll-to-bottom-button {
  position: fixed;
  bottom: 100px;
  right: 40px;
  width: 48px;
  height: 48px;
  border: 2px solid #000;
  border-radius: 50%;
  background-color: white;
  color: black;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 99;
}

.scroll-to-bottom-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  background-color: #f5f5f5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    transform: translateX(-100%);
  }
  
  .sidebar.collapsed {
    transform: translateX(-100%);
    width: 260px;
    min-width: 260px;
  }
  
  .sidebar:not(.collapsed) {
    transform: translateX(0);
  }
  
  .main-content {
    width: 100%;
  }
  
  .chat-content {
    padding: 20px;
  }
  
  .chat-input-area {
    padding: 16px 20px 20px;
  }
}
</style>
