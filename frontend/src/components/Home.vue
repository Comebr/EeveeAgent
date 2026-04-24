<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import MarkdownRenderer from './MarkdownRenderer.vue'
import BaseButton from './common/BaseButton.vue'
import logo from '../assets/logo.png'

const router = useRouter()
const userInfo = ref(null)
const showUserMenu = ref(false)
const sidebarCollapsed = ref(false)
const sidebarAutoHidden = ref(false) // 默认展开
const mouseNearSidebar = ref(false)

// 消息提示
const showMessage = ref(false)
const messageText = ref('')
const messageType = ref('') // success, error, info

// 显示消息
const showToast = (text, type = 'success', duration = 3000) => {
  messageText.value = text
  messageType.value = type
  showMessage.value = true
  
  setTimeout(() => {
    showMessage.value = false
  }, duration)
}

// 会话相关状态
const conversations = ref([])
const currentConversationId = ref(null)
const showConversationMenu = ref(false)
const selectedConversation = ref(null)
const renameDialogVisible = ref(false)
const newTitle = ref('')
const isRefreshing = ref(false)
const refreshTimer = ref(null)
const isPageActive = ref(true)
const showDeleteConfirm = ref(false)
const conversationToDelete = ref(null)

// 消息操作相关状态
const showMessageMenu = ref(false)
const selectedMessage = ref(null)
const messageMenuPosition = ref({ x: 0, y: 0 })

// 聊天相关状态
const inputText = ref('')
const isLoading = ref(false)
const isInputFocused = ref(false) // 输入框焦点状态
const chatContentRef = ref(null)
const autoScroll = ref(true) // 自动滚动标志
const showScrollButton = ref(false) // 是否显示回到底部的按钮

// 会话状态管理Map，为每个会话维护独立的状态
const sessionStates = ref(new Map())

// 获取当前会话的状态
const getCurrentSessionState = () => {
  const sessionId = currentConversationId.value || 'temp'
  if (!sessionStates.value.has(sessionId)) {
    sessionStates.value.set(sessionId, {
      messages: [],
      isStreaming: false,
      eventSource: null,
      currentRoundId: null,
      outputTimer: null,
      isCancelling: false
    })
  }
  return sessionStates.value.get(sessionId)
}

// 获取当前会话的消息列表
const messages = computed(() => {
  const sessionState = getCurrentSessionState()
  return sessionState.messages
})

// 获取当前会话的流式状态
const isStreaming = computed(() => {
  const sessionState = getCurrentSessionState()
  return sessionState.isStreaming
})

// 获取当前会话的EventSource
const eventSource = computed({
  get: () => {
    const sessionState = getCurrentSessionState()
    return sessionState.eventSource
  },
  set: (value) => {
    const sessionState = getCurrentSessionState()
    sessionState.eventSource = value
  }
})

// 获取当前会话的请求ID
const currentRoundId = computed({
  get: () => {
    const sessionState = getCurrentSessionState()
    return sessionState.currentRoundId
  },
  set: (value) => {
    const sessionState = getCurrentSessionState()
    sessionState.currentRoundId = value
  }
})

// 实时获取用户信息
const fetchUserInfo = async () => {
  try {
    const response = await axios.get('/api/management/current')
    
    if (response.data.code === '0') {
      userInfo.value = response.data.data
    } else {
      // 登录失效，跳转到登录页并传递消息参数
      router.push('/?message=登录已失效，请重新登录&messageType=info')
    }
  } catch (error) {
    // 登录失效，跳转到登录页并传递消息参数
    router.push('/?message=登录已失效，请重新登录&messageType=info')
  }
}

// 滚动到聊天底部
const scrollToBottom = async (force = false) => {
  await nextTick()
  if (chatContentRef.value && (autoScroll.value || force)) {
    chatContentRef.value.scrollTop = chatContentRef.value.scrollHeight
  }
}

// 处理滚动事件，当用户手动滚动时，禁用自动滚动
const handleScroll = () => {
  if (chatContentRef.value) {
    const { scrollTop, scrollHeight, clientHeight } = chatContentRef.value
    const distanceToBottom = scrollHeight - scrollTop - clientHeight
    // 当滚动位置离底部超过5px时，认为用户在查看历史内容，禁用自动滚动
    // 减小阈值，使autoScroll更快地被禁用，减少抖动
    autoScroll.value = distanceToBottom < 5
    // 当不在底部时显示回到底部的按钮
    showScrollButton.value = distanceToBottom >= 5
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputText.value.trim()) return
  
  // 检查是否是新对话（没有conversationId）
  let tempConversationId = currentConversationId.value
  if (!tempConversationId) {
    // 生成临时conversationId
    tempConversationId = 'temp_' + Date.now()
    
    // 更新当前会话ID，确保切换到新会话
    currentConversationId.value = tempConversationId
    
    // 在会话列表顶部插入新会话
    const newConversation = {
      id: tempConversationId,
      conversationId: tempConversationId,
      userId: userInfo.value?.userId,
      title: '新会话',
      lastTalkTime: new Date(),
      delFlag: 0,
      isTemp: true // 标记为临时会话
    }
    conversations.value.unshift(newConversation)
  }
  
  // 保存当前会话ID，用于后续操作
  const currentSessionId = tempConversationId
  
  // 获取当前会话的状态
  if (!sessionStates.value.has(currentSessionId)) {
    sessionStates.value.set(currentSessionId, {
      messages: [],
      isStreaming: false,
      eventSource: null,
      currentRequestId: null
    })
  }
  const sessionState = sessionStates.value.get(currentSessionId)
  
  // 添加用户消息
  const userMessage = {
    id: Date.now(),
    content: inputText.value.trim(),
    role: 'user',
    timestamp: new Date().toISOString()
  }
  sessionState.messages.push(userMessage)
  inputText.value = ''
  
  // 滚动到底部（强制）
  await scrollToBottom(true)
  
  // 添加AI正在思考的消息
  const aiThinkingMessage = {
    id: Date.now() + 1,
    content: '🤔 正在思考中...',
    role: 'assistant',
    timestamp: new Date().toISOString(),
    isThinking: true
  }
  sessionState.messages.push(aiThinkingMessage)
  
  // 滚动到底部（强制）
  await scrollToBottom(true)
  
  try {
    // 由于 SSE 只支持 GET 请求，我们需要将参数作为查询参数传递
    const openThinking = deepThinking.value
    // 生成roundId用于取消功能
    const roundId = 'round_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    sessionState.currentRoundId = roundId
    let url = `/agent/rag/streamingChat?prompt=${encodeURIComponent(userMessage.content)}&roundId=${roundId}&openThinking=${openThinking}`
    // 只有当currentConversationId不是临时ID时才传递，新会话不传conversationId
    if (currentConversationId.value && !currentConversationId.value.startsWith('temp_')) {
      url += `&conversationId=${currentConversationId.value}`
    }
    
    const eventSource = new EventSource(url)
    sessionState.eventSource = eventSource
    sessionState.isStreaming = true
    sessionState.outputTimer = null
    
    let buffer = ''
    let displayContent = ''
    let firstDataReceived = false
    let isComplete = false
    let receivedConversationId = currentConversationId.value
    
    const startSmoothOutput = () => {
        // 流式chunk分段输出配置
        const MIN_CHUNK_SIZE = 5
        const MAX_CHUNK_SIZE = 20
        const CHAR_INTERVAL_MS = 12 // 每个字符的输出间隔
        
        let currentIndex = 0
        
        const processChunk = () => {
          if (sessionState.isCancelling) return
          
          if (currentIndex >= buffer.length) {
            if (isComplete) {
              // 所有内容处理完成
              const msgIndex = sessionState.messages.findIndex(msg => msg.id === aiThinkingMessage.id)
              if (msgIndex !== -1) {
                sessionState.messages[msgIndex] = {
                  ...sessionState.messages[msgIndex],
                  isStreaming: false,
                  content: buffer // 确保最终内容完整
                }
              }
              nextTick(() => {
                if (currentConversationId.value === currentSessionId || (!currentConversationId.value && currentSessionId.includes('temp'))) {
                  scrollToBottom()
                }
              })
            } else {
              // 缓冲区已处理完但流式传输未结束，继续等待新数据
              sessionState.outputTimer = setTimeout(processChunk, 50)
            }
            return
          }
          
          // 生成随机chunk大小
          const chunkSize = Math.min(
            MAX_CHUNK_SIZE,
            Math.max(MIN_CHUNK_SIZE, Math.floor(Math.random() * (MAX_CHUNK_SIZE - MIN_CHUNK_SIZE + 1)) + MIN_CHUNK_SIZE),
            buffer.length - currentIndex
          )
          
          const chunk = buffer.substring(currentIndex, currentIndex + chunkSize)
          let chunkIndex = 0
          
          const outputChar = () => {
            if (sessionState.isCancelling) return
            
            if (chunkIndex < chunk.length) {
              displayContent += chunk[chunkIndex]
              chunkIndex++
              
              // 更新消息内容
              const msgIndex = sessionState.messages.findIndex(msg => msg.id === aiThinkingMessage.id)
              if (msgIndex !== -1) {
                sessionState.messages[msgIndex] = {
                  ...sessionState.messages[msgIndex],
                  content: displayContent,
                  isStreaming: true
                }
              }
              
              // 滚动到底部
              nextTick(() => {
                if (currentConversationId.value === currentSessionId || (!currentConversationId.value && currentSessionId.includes('temp'))) {
                  scrollToBottom()
                }
              })
              
              // 继续输出下一个字符
              sessionState.outputTimer = setTimeout(outputChar, CHAR_INTERVAL_MS)
            } else {
              // 当前chunk输出完成，处理下一个chunk
              currentIndex += chunkSize
              // 片段之间几乎无延迟，直接处理下一个chunk
              processChunk()
            }
          }
          
          // 开始输出当前chunk
          outputChar()
        }
        
        // 开始处理
        processChunk()
      }
    
    // 处理 init 事件，获取会话ID
    eventSource.addEventListener('init', (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.conversationId) {
          receivedConversationId = data.conversationId
          
          // 更新当前会话ID
          currentConversationId.value = data.conversationId
          
          // 更新会话状态Map中的键
          if (currentSessionId.includes('temp')) {
            sessionStates.value.set(data.conversationId, sessionState)
            sessionStates.value.delete(currentSessionId)
          }
          
          // 查找并更新临时会话
          const tempConversationIndex = conversations.value.findIndex(c => c.isTemp === true)
          if (tempConversationIndex !== -1) {
            // 更新临时会话为真实会话
            conversations.value[tempConversationIndex] = {
              ...conversations.value[tempConversationIndex],
              id: data.conversationId,
              conversationId: data.conversationId,
              isTemp: false
            }
          } else {
            // 如果没有临时会话，创建新会话
            const newConversation = {
              id: data.conversationId,
              conversationId: data.conversationId,
              userId: userInfo.value?.userId || localStorage.getItem('userId'),
              title: '新对话',
              lastTalkTime: new Date(),
              delFlag: 0
            }
            conversations.value.unshift(newConversation)
          }
        }
      } catch (error) {
        // 静默处理错误
      }
    })
    
    eventSource.onmessage = (event) => {
        if (!firstDataReceived) {
          firstDataReceived = true
          const msgIndex = sessionState.messages.findIndex(msg => msg.id === aiThinkingMessage.id)
          if (msgIndex !== -1) {
            sessionState.messages[msgIndex] = {
              ...sessionState.messages[msgIndex],
              isThinking: false,
              isStreaming: true,
              content: ''
            }
          }
        }
        
        // 非JSON数据，添加到buffer
        buffer += event.data
        displayContent = buffer // 确保displayContent与buffer同步
        
        // 如果是首次收到数据，启动输出
        if (firstDataReceived && !sessionState.outputTimer) {
          startSmoothOutput()
        }
      }
      
      eventSource.onerror = (error) => {
        eventSource.close()
        isComplete = true
        sessionState.isStreaming = false
        // 等待缓冲区数据处理完成后再清理定时器和标记结束
        const checkBuffer = () => {
          if (buffer.length === 0) {
            if (outputTimer) {
              clearTimeout(outputTimer)
              outputTimer = null
            }
            const msgIndex = sessionState.messages.findIndex(msg => msg.id === aiThinkingMessage.id)
            if (msgIndex !== -1) {
              // 一次性更新最终内容，确保代码高亮和语言显示正确
              sessionState.messages[msgIndex] = {
                ...sessionState.messages[msgIndex],
                isStreaming: false,
                content: buffer
              }
            }
          } else {
            // 继续等待缓冲区处理
            setTimeout(checkBuffer, 50)
          }
        }
        checkBuffer()
        if (!firstDataReceived) {
          sessionState.messages = sessionState.messages.filter(msg => !msg.isThinking)
          sessionState.messages.push({
            id: Date.now() + 3,
            content: '抱歉，处理您的请求时出现错误，请稍后重试。',
            role: 'assistant',
            timestamp: new Date().toISOString()
          })
          nextTick(() => {
            // 只有当当前会话是激活会话时才滚动
            if (currentConversationId.value === currentSessionId || (!currentConversationId.value && currentSessionId.includes('temp'))) {
              scrollToBottom()
            }
          })
        }
      }
      
      eventSource.addEventListener('close', () => {
        eventSource.close()
        isComplete = true
        sessionState.isStreaming = false
        // 等待缓冲区数据处理完成后再标记流式传输结束
        const checkBuffer = () => {
          if (buffer.length === 0) {
            if (outputTimer) {
              clearTimeout(outputTimer)
              outputTimer = null
            }
            const msgIndex = sessionState.messages.findIndex(msg => msg.id === aiThinkingMessage.id)
            if (msgIndex !== -1) {
              // 一次性更新最终内容，确保代码高亮和语言显示正确
              sessionState.messages[msgIndex] = {
                ...sessionState.messages[msgIndex],
                isStreaming: false,
                content: buffer
              }
            }
            
            // 检查是否是新会话且已收到conversationId
            if (receivedConversationId && !currentConversationId.value) {
              currentConversationId.value = receivedConversationId
              
              // 更新会话状态Map中的键
              if (currentSessionId.includes('temp')) {
                sessionStates.value.set(receivedConversationId, sessionState)
                sessionStates.value.delete(currentSessionId)
              }
              
              // 查找并更新临时会话
              const tempConversationIndex = conversations.value.findIndex(c => c.isTemp === true)
              if (tempConversationIndex !== -1) {
                // 更新临时会话为真实会话
                conversations.value[tempConversationIndex] = {
                  ...conversations.value[tempConversationIndex],
                  id: receivedConversationId,
                  conversationId: receivedConversationId,
                  isTemp: false
                }
              } else {
                // 如果没有临时会话，创建新会话
                const newConversation = {
                  id: receivedConversationId,
                  conversationId: receivedConversationId,
                  userId: userInfo.value?.userId || localStorage.getItem('userId'),
                  title: '新对话',
                  lastTalkTime: new Date(),
                  delFlag: 0
                }
                conversations.value.unshift(newConversation)
              }
            }
            
            // 主动获取后端生成的标题
            if (receivedConversationId) {
              // 等待后端生成标题（给后端一些时间处理）
              setTimeout(() => {
                fetchConversationTitle(receivedConversationId)
              }, 1000)
            }
          } else {
            // 继续等待缓冲区处理
            setTimeout(checkBuffer, 50)
          }
        }
        checkBuffer()
      })
    
  } catch (error) {
    sessionState.messages = sessionState.messages.filter(msg => !msg.isThinking)
    sessionState.messages.push({
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
  // 获取当前会话ID
  const currentSessionId = currentConversationId.value || 'temp'
  
  // 获取当前会话的状态
  if (!sessionStates.value.has(currentSessionId)) {
    sessionStates.value.set(currentSessionId, {
      messages: [],
      isStreaming: false,
      eventSource: null,
      currentRequestId: null
    })
  }
  const sessionState = sessionStates.value.get(currentSessionId)
  
  // 前端实现：重新发送上一条用户消息
  const userMessages = sessionState.messages.filter(msg => msg.role === 'user')
  if (userMessages.length > 0) {
    const lastUserMessage = userMessages[userMessages.length - 1]
    
    // 标记当前消息为思考中
    const msgIndex = sessionState.messages.findIndex(msg => msg.id === message.id)
    if (msgIndex !== -1) {
      sessionState.messages[msgIndex] = {
        ...sessionState.messages[msgIndex],
        isThinking: true,
        content: '',
        isStreaming: false
      }
      await scrollToBottom()
    }
    
    // 直接发送请求，不添加新消息
    try {
      // 由于 SSE 只支持 GET 请求，我们需要将参数作为查询参数传递
      const openThinking = deepThinking.value
      // 生成roundId用于取消功能
      const roundId = 'round_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
      sessionState.currentRoundId = roundId
      let url = `/agent/rag/streamingChat?prompt=${encodeURIComponent(lastUserMessage.content)}&roundId=${roundId}&openThinking=${openThinking}`
      if (currentConversationId.value) {
        url += `&conversationId=${currentConversationId.value}`
      }
      
      const eventSource = new EventSource(url)
      sessionState.eventSource = eventSource
      sessionState.isStreaming = true
      
      let buffer = ''
      let displayContent = ''
      let firstDataReceived = false
      let isComplete = false
      let outputTimer = null
      
      const startSmoothOutput = () => {
        // 流式chunk分段输出配置
        const MIN_CHUNK_SIZE = 5
        const MAX_CHUNK_SIZE = 20
        const CHAR_INTERVAL_MS = 12 // 每个字符的输出间隔
        
        let currentIndex = 0
        
        const processChunk = () => {
          if (currentIndex >= buffer.length) {
            if (isComplete) {
              // 所有内容处理完成
              const msgIndex = sessionState.messages.findIndex(msg => msg.id === message.id)
              if (msgIndex !== -1) {
                sessionState.messages[msgIndex] = {
                  ...sessionState.messages[msgIndex],
                  isStreaming: false,
                  content: buffer // 确保最终内容完整
                }
              }
              nextTick(() => {
                if (currentConversationId.value === currentSessionId || (!currentConversationId.value && currentSessionId.includes('temp'))) {
                  scrollToBottom()
                }
              })
            }
            return
          }
          
          // 生成随机chunk大小
          const chunkSize = Math.min(
            MAX_CHUNK_SIZE,
            Math.max(MIN_CHUNK_SIZE, Math.floor(Math.random() * (MAX_CHUNK_SIZE - MIN_CHUNK_SIZE + 1)) + MIN_CHUNK_SIZE),
            buffer.length - currentIndex
          )
          
          const chunk = buffer.substring(currentIndex, currentIndex + chunkSize)
          let chunkIndex = 0
          
          const outputChar = () => {
            if (chunkIndex < chunk.length) {
              displayContent += chunk[chunkIndex]
              chunkIndex++
              
              // 更新消息内容
              const msgIndex = sessionState.messages.findIndex(msg => msg.id === message.id)
              if (msgIndex !== -1) {
                sessionState.messages[msgIndex] = {
                  ...sessionState.messages[msgIndex],
                  content: displayContent,
                  isStreaming: true
                }
              }
              
              // 滚动到底部
              nextTick(() => {
                if (currentConversationId.value === currentSessionId || (!currentConversationId.value && currentSessionId.includes('temp'))) {
                  scrollToBottom()
                }
              })
              
              // 继续输出下一个字符
              outputTimer = setTimeout(outputChar, CHAR_INTERVAL_MS)
            } else {
              // 当前chunk输出完成，处理下一个chunk
              currentIndex += chunkSize
              // 片段之间几乎无延迟，直接处理下一个chunk
              processChunk()
            }
          }
          
          // 开始输出当前chunk
          outputChar()
        }
        
        // 开始处理
        processChunk()
        
        // 存储定时器以便清理
        if (!window.__chatTimers) {
          window.__chatTimers = []
        }
        window.__chatTimers.push(outputTimer)
      }
      
      eventSource.onmessage = (event) => {
        if (!firstDataReceived) {
          firstDataReceived = true
          const msgIndex = sessionState.messages.findIndex(msg => msg.id === message.id)
          if (msgIndex !== -1) {
            sessionState.messages[msgIndex] = {
              ...sessionState.messages[msgIndex],
              isThinking: false,
              isStreaming: true,
              content: ''
            }
          }
        }
        
        // 检查是否包含conversationId
        if (event.data.startsWith('{') && event.data.endsWith('}')) {
          try {
            const data = JSON.parse(event.data)
            if (data.conversationId) {
              receivedConversationId = data.conversationId
            }
          } catch (error) {
            // 不是JSON格式，继续处理
          }
        } else {
          // 非JSON数据，添加到buffer
          buffer += event.data
          
          // 如果是首次收到数据，启动输出
          if (firstDataReceived && !sessionState.outputTimer) {
            startSmoothOutput()
          }
        }
      }
      
      eventSource.onerror = (error) => {
        eventSource.close()
        isComplete = true
        sessionState.isStreaming = false
        // 等待缓冲区数据处理完成后再清理定时器和标记结束
        const checkBuffer = () => {
          if (buffer.length === 0) {
            if (outputTimer) {
              clearInterval(outputTimer)
              outputTimer = null
            }
            const msgIndex = sessionState.messages.findIndex(msg => msg.id === message.id)
            if (msgIndex !== -1) {
              // 一次性更新最终内容，确保代码高亮和语言显示正确
              sessionState.messages[msgIndex] = {
                ...sessionState.messages[msgIndex],
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
          const msgIndex = sessionState.messages.findIndex(msg => msg.id === message.id)
          if (msgIndex !== -1) {
            sessionState.messages[msgIndex] = {
              ...sessionState.messages[msgIndex],
              isThinking: false,
              isStreaming: false,
              content: '抱歉，处理您的请求时出现错误，请稍后重试。'
            }
          }
          nextTick(() => {
            // 只有当当前会话是激活会话时才滚动
            if (currentConversationId.value === currentSessionId || (!currentConversationId.value && currentSessionId.includes('temp'))) {
              scrollToBottom()
            }
          })
        }
      }
      
      eventSource.addEventListener('close', () => {
        eventSource.close()
        isComplete = true
        sessionState.isStreaming = false
        // 等待缓冲区数据处理完成后再标记流式传输结束
        const checkBuffer = () => {
          if (buffer.length === 0) {
            if (outputTimer) {
              clearInterval(outputTimer)
              outputTimer = null
            }
            const msgIndex = sessionState.messages.findIndex(msg => msg.id === message.id)
            if (msgIndex !== -1) {
              // 一次性更新最终内容，确保代码高亮和语言显示正确
              sessionState.messages[msgIndex] = {
                ...sessionState.messages[msgIndex],
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
      const msgIndex = sessionState.messages.findIndex(msg => msg.id === message.id)
      if (msgIndex !== -1) {
        sessionState.messages[msgIndex] = {
          ...sessionState.messages[msgIndex],
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
    // 确保复制的内容保留层级关系
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

// 处理粘贴事件，保留格式并剥离富文本样式
const handlePaste = (event) => {
  event.preventDefault()
  
  // 获取粘贴的内容
  const clipboardData = event.clipboardData || window.clipboardData
  let pastedText = clipboardData.getData('text')
  
  // 检查粘贴文本长度，超过2000则截断
  let isTruncated = false
  if (pastedText.length > 2000) {
    pastedText = pastedText.substring(0, 2000)
    isTruncated = true
  }
  
  // 保留换行和空格，剥离富文本样式，清洗多余空行
  const cleanedText = cleanExcessEmptyLines(pastedText)
  
  // 计算最终文本长度，确保不超过2000
  const input = event.target
  const startPos = input.selectionStart
  const endPos = input.selectionEnd
  const textBefore = inputText.value.substring(0, startPos)
  const textAfter = inputText.value.substring(endPos)
  
  // 计算剩余可输入长度
  const remainingLength = 2000 - (textBefore.length + textAfter.length)
  const finalText = textBefore + (cleanedText.length > remainingLength ? cleanedText.substring(0, remainingLength) : cleanedText) + textAfter
  
  inputText.value = finalText
  
  // 显示截断提示
  if (isTruncated || cleanedText.length > remainingLength) {
    showToast('内容过长，已自动截断', 'warning')
  }
  
  // 光标定位到粘贴内容之后
  setTimeout(() => {
    const newCursorPos = startPos + Math.min(cleanedText.length, remainingLength)
    input.selectionStart = input.selectionEnd = newCursorPos
    // 调整textarea高度
    adjustTextareaHeight(input)
  }, 0)
}

// 处理输入事件，限制输入长度
const handleInput = (e) => {
  const textarea = e.target
  
  // 限制输入长度为2000
  if (inputText.value.length > 2000) {
    inputText.value = inputText.value.substring(0, 2000)
    showToast('内容已达最大长度限制', 'warning')
  }
  
  // 调整textarea高度
  adjustTextareaHeight(textarea)
}

// 自动调整textarea高度
const adjustTextareaHeight = (textarea) => {
  textarea.style.height = 'auto'
  textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
}

// 计算有效字数（过滤空行和首尾空白）
const getEffectiveCharCount = (text) => {
  if (!text) return 0
  // 过滤空行和首尾空白，只算有效文本
  const lines = text.split('\n')
  const validLines = lines.filter(line => line.trim() !== '')
  const validText = validLines.join('\n')
  return validText.length
}

// 清洗多余空行（保留排版，去除连续空行）
const cleanExcessEmptyLines = (text) => {
  if (!text) return ''
  // 替换连续的空行为单个空行
  return text.replace(/\n{3,}/g, '\n\n').trim()
}

// 暂停流式输出
const pauseStreaming = () => {
  const sessionState = getCurrentSessionState()
  if (sessionState.eventSource) {
    sessionState.eventSource.close()
    sessionState.isStreaming = false
  }
}

// 取消流式输出
const cancelStreaming = async () => {
  const sessionState = getCurrentSessionState()
  
  // 幂等：正在取消中或已经不在流式状态，则直接返回
  if (sessionState.isCancelling || !sessionState.isStreaming) {
    return
  }
  
  // 保存roundId，用于调用后端取消接口
  const roundIdToCancel = sessionState.currentRoundId
  
  // 1. 设置取消状态，阻止新的渲染更新
  sessionState.isCancelling = true
  
  // 2. 调用 SSE 原生关闭，不再接收后端推的 token
  if (sessionState.eventSource) {
    sessionState.eventSource.close()
    sessionState.eventSource = null
  }
  
  // 3. 渲染层立刻停掉：清理所有定时器
  if (sessionState.outputTimer) {
    clearTimeout(sessionState.outputTimer)
    clearInterval(sessionState.outputTimer)
    sessionState.outputTimer = null
  }
  
  // 清理全局定时器
  const timers = window.__chatTimers || []
  timers.forEach(timer => {
    clearTimeout(timer)
    clearInterval(timer)
  })
  window.__chatTimers = []
  
  // 4. 终止自动滚动等流式联动效果
  autoScroll.value = false
  
  // 5. 保留已经生成完的文字，更新UI状态
  const streamingMessageIndex = sessionState.messages.findIndex(msg => msg.isStreaming)
  if (streamingMessageIndex !== -1) {
    // 保留已输出的内容，不清空
    sessionState.messages[streamingMessageIndex] = {
      ...sessionState.messages[streamingMessageIndex],
      isStreaming: false,
      isThinking: false
      // content保持原样，不追加任何文字
    }
  }
  
  // 6. UI 状态重置
  sessionState.isStreaming = false
  sessionState.currentRoundId = null
  
  // 7. 调用后端取消接口（异步，不阻塞UI更新）
  if (roundIdToCancel) {
    try {
      await axios.post('/agent/rag/cancel', null, {
        params: { roundId: roundIdToCancel }
      })
    } catch (error) {
      console.error('取消流式输出失败:', error)
    }
  }
  
  // 8. 重置取消状态
  sessionState.isCancelling = false
}

// 手动滚动到底部
const scrollToBottomManually = async () => {
  autoScroll.value = true
  showScrollButton.value = false
  if (chatContentRef.value) {
    chatContentRef.value.scrollTo({
      top: chatContentRef.value.scrollHeight,
      behavior: 'smooth'
    })
  }
  await scrollToBottom()
}



// 深度思考模式
const deepThinking = ref(false)

const toggleDeepThinking = () => {
  deepThinking.value = !deepThinking.value
}

// 示例问题相关状态
const sampleQuestions = ref([])
const isLoadingSampleQuestions = ref(false)

// 获取示例问题
const fetchSampleQuestions = async () => {
  isLoadingSampleQuestions.value = true
  try {
    const response = await axios.get('/example/rag/sample-questions')
    if (response.data.code === '0') {
      sampleQuestions.value = response.data.data
    }
  } catch (error) {
    console.error('获取示例问题失败:', error)
  } finally {
    isLoadingSampleQuestions.value = false
  }
}

// 选择示例问题
const selectSampleQuestion = (question) => {
  inputText.value = question.question
  // 调整textarea高度
  const textarea = document.querySelector('textarea')
  if (textarea) {
    adjustTextareaHeight(textarea)
  }
  // 滚动到输入框
  textarea?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

// 会话管理相关函数
const fetchConversations = async (isSilent = false) => {
  if (isRefreshing.value) return
  
  isRefreshing.value = true
  
  try {
    // 确保用户信息已获取
    if (!userInfo.value) {
      await fetchUserInfo()
    }
    
    const userId = userInfo.value?.userId
    
    if (!userId) {
      if (!isSilent) {
        showToast('用户未登录，无法获取会话列表', 'error')
      }
      isRefreshing.value = false
      return
    }
    
    const response = await axios.get(`/conversation/list`, {
      params: { userId }
    })
    
    if (response.data.code === '0') {
      conversations.value = response.data.data
      if (!isSilent) {
        showToast('会话列表已更新', 'success')
      }
    } else {
      if (!isSilent) {
        showToast('获取会话列表失败', 'error')
      }
    }
  } catch (error) {
    if (!isSilent) {
      showToast('获取会话列表失败，将自动重试', 'error')
    }
    
    // 网络异常后自动重试
    if (refreshTimer.value) {
      clearTimeout(refreshTimer.value)
    }
    refreshTimer.value = setTimeout(() => {
      fetchConversations(true)
    }, 3000)
  } finally {
    isRefreshing.value = false
  }
}

const openConversation = async (conversation) => {
  try {
    const conversationId = conversation.conversationId
    currentConversationId.value = conversationId
    
    // 检查会话状态Map中是否已有该会话的状态
    if (!sessionStates.value.has(conversationId)) {
      // 从后端获取会话消息
      const response = await axios.get(`/conversation/${conversationId}`)
      if (response.data.code === '0') {
        const messages = response.data.data.map(msg => ({
          id: msg.id,
          content: msg.content,
          role: msg.role,
          timestamp: msg.createTime
        }))
        // 存储到会话状态Map
        sessionStates.value.set(conversationId, {
          messages: messages,
          isStreaming: false,
          eventSource: null,
          currentRequestId: null
        })
      }
    }
    
    // 滚动到底部
    await scrollToBottom()
  } catch (error) {
    console.error('打开会话失败:', error)
  }
}

const deleteConversation = async (conversationId) => {
  showDeleteConfirm.value = true
  conversationToDelete.value = conversationId
}

const confirmDeleteConversation = async () => {
  const conversationId = conversationToDelete.value
  if (!conversationId) return
  
  try {
    const response = await axios.put('/conversation/remove', null, {
      params: { conversationId }
    })
    
    if (response.data.code === '0') {
      showToast('会话删除成功', 'success')
      conversations.value = conversations.value.filter(c => c.conversationId !== conversationId)
      
      if (currentConversationId.value === conversationId) {
        messages.value = []
        currentConversationId.value = null
      }
    }
  } catch (error) {
    console.error('删除会话失败:', error)
    showToast('删除会话失败', 'error')
  } finally {
    showDeleteConfirm.value = false
    conversationToDelete.value = null
  }
}

const cancelDeleteConversation = () => {
  showDeleteConfirm.value = false
  conversationToDelete.value = null
}

// 显示消息操作菜单
const showMessageOptions = (message, event) => {
  event.stopPropagation()
  selectedMessage.value = message
  
  // 先显示菜单，然后在DOM更新后计算位置
  showMessageMenu.value = true
  
  // 等待DOM更新后设置菜单位置
  nextTick(() => {
    const rect = event.currentTarget.getBoundingClientRect()
    const menu = document.querySelector('.message-menu')
    if (menu) {
      // 显示在更多按钮的右侧
      menu.style.top = `${rect.top}px`
      menu.style.left = `${rect.right + 8}px`
    }
  })
  
  // 点击外部关闭菜单
  setTimeout(() => {
    document.addEventListener('click', hideMessageMenu)
  }, 0)
}

// 隐藏消息操作菜单
const hideMessageMenu = () => {
  showMessageMenu.value = false
  selectedMessage.value = null
  document.removeEventListener('click', hideMessageMenu)
}



// 删除消息
const deleteMessage = async (message) => {
  try {
    const response = await axios.put('/messages/remove', {
      messageId: message.id,
      role: message.role
    })
    
    if (response.data.code === '0') {
      showToast('消息删除成功', 'success')
      // 获取当前会话状态并更新messages数组
      const sessionState = getCurrentSessionState()
      sessionState.messages = sessionState.messages.filter(msg => msg.id !== message.id)
    }
  } catch (error) {
    console.error('删除消息失败:', error)
    showToast('删除消息失败', 'error')
  }
  finally {
    hideMessageMenu()
  }
}

// 获取单个会话的最新信息（用于更新标题）
const fetchConversationTitle = async (conversationId) => {
  try {
    const response = await axios.get(`/conversation/list`, {
      params: { userId: userInfo.value?.userId }
    })
    
    if (response.data.code === '0') {
      const updatedConversations = response.data.data
      const conversation = updatedConversations.find(c => c.conversationId === conversationId)
      
      if (conversation) {
        // 更新会话列表中的标题
        const conversationIndex = conversations.value.findIndex(c => c.conversationId === conversationId)
        if (conversationIndex !== -1) {
          conversations.value[conversationIndex].title = conversation.title
        }
      }
    }
  } catch (error) {
    console.error('获取会话标题失败:', error)
  }
}

const renameConversation = async () => {
  if (!newTitle.value.trim()) {
    showToast('标题不能为空', 'error')
    return
  }
  
  try {
    const response = await axios.post('/conversation/mkdir', {
      conversationId: selectedConversation.value.conversationId,
      title: newTitle.value.trim()
    })
    
    if (response.data.code === '0') {
      showToast('会话重命名成功', 'success')
      const index = conversations.value.findIndex(c => c.conversationId === selectedConversation.value.conversationId)
      if (index !== -1) {
        conversations.value[index].title = newTitle.value.trim()
      }
      renameDialogVisible.value = false
      showConversationMenu.value = false
    }
  } catch (error) {
    console.error('重命名会话失败:', error)
    showToast('重命名会话失败', 'error')
  }
}

const showConversationOptions = (conversation, event, button) => {
  event.stopPropagation()
  
  // 如果点击的是当前已选中的会话且菜单已显示，则隐藏菜单
  if (selectedConversation.value === conversation && showConversationMenu.value) {
    showConversationMenu.value = false
    document.removeEventListener('click', closeConversationMenu)
    return
  }
  
  selectedConversation.value = conversation
  
  // 先显示菜单，然后在DOM更新后计算位置
  showConversationMenu.value = true
  
  // 等待DOM更新后设置菜单位置
  nextTick(() => {
    // 计算菜单位置
    const target = button || event.currentTarget
    const rect = target.getBoundingClientRect()
    const menu = document.querySelector('.conversation-menu')
    if (menu) {
      // 显示在更多按钮的右侧
      menu.style.top = `${rect.top}px`
      menu.style.left = `${rect.right + 8}px`
    }
  })
  
  // 点击外部关闭菜单
  setTimeout(() => {
    document.addEventListener('click', closeConversationMenu)
  }, 0)
}

const closeConversationMenu = () => {
  showConversationMenu.value = false
  document.removeEventListener('click', closeConversationMenu)
}

const openRenameDialog = () => {
  newTitle.value = selectedConversation.value.title
  renameDialogVisible.value = true
  showConversationMenu.value = false
}

// 新建对话
const newChat = () => {
  inputText.value = ''
  currentConversationId.value = null
  // 不需要清空messages，因为messages是通过computed从sessionStates中获取的
  // 当currentConversationId为null时，会使用'temp'作为键，自动创建新的会话状态
}

// 处理页面可见性变化
const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible' && isPageActive.value === false) {
    // 页面从后台切回前台，静默刷新会话列表
    isPageActive.value = true
    fetchConversations(true)
  } else if (document.visibilityState === 'hidden') {
    isPageActive.value = false
  }
}

onMounted(async () => {
  // 实时获取用户信息
  await fetchUserInfo()
  
  // 点击外部关闭菜单
  document.addEventListener('click', handleClickOutside)
  
  // 登录成功后显示消息
// 不再使用localStorage存储登录状态
  
  // 智能隐藏侧边栏相关事件
  document.addEventListener('click', handleClick)
  document.addEventListener('wheel', handleWheel)
  
  // 页面可见性变化监听
  document.addEventListener('visibilitychange', handleVisibilityChange)
  
  // 获取会话列表（首次进入全量拉取）
  fetchConversations()
  
  // 获取示例问题
  fetchSampleQuestions()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  
  // 移除智能隐藏侧边栏相关事件
  document.removeEventListener('click', handleClick)
  document.removeEventListener('wheel', handleWheel)
  
  // 移除页面可见性变化监听
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  
  // 清理所有可能的定时器
  const timers = window.__chatTimers || []
  timers.forEach(timer => clearInterval(timer))
  window.__chatTimers = []
  
  // 清理自动重试定时器
  if (refreshTimer.value) {
    clearTimeout(refreshTimer.value)
  }
})

const handleClickOutside = (event) => {
  const menu = document.querySelector('.user-menu-container')
  if (menu && !menu.contains(event.target)) {
    showUserMenu.value = false
  }
}

const handleLogout = () => {
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
  window.open('/admin', '_blank')
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
    
    <!-- 重命名会话对话框 -->
    <div v-if="renameDialogVisible" class="dialog-overlay">
      <div class="dialog">
        <div class="dialog-header">
          <h3>重命名会话</h3>
          <button class="dialog-close" @click="renameDialogVisible = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="dialog-body">
          <input 
            type="text" 
            v-model="newTitle" 
            placeholder="请输入新的会话标题" 
            class="dialog-input"
            @keypress="(e) => e.key === 'Enter' && renameConversation()"
          />
        </div>
        <div class="dialog-footer">
          <button class="dialog-button cancel" @click="renameDialogVisible = false">取消</button>
          <button class="dialog-button confirm" @click="renameConversation">确定</button>
        </div>
      </div>
    </div>
    
    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="cancelDeleteConversation">
      <div class="modal-content delete-modal">
        <div class="modal-header">
          <h3>确认删除</h3>
          <button class="close-button" @click="cancelDeleteConversation">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="delete-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ff4d4f" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="15" y1="9" x2="9" y2="15"/>
              <line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
          </div>
          <p class="delete-message">确定要删除会话 <span class="delete-name">{{ selectedConversation?.title }}</span> 吗？</p>
          <p class="delete-hint">此操作不可撤销，删除后相关数据将被永久清除。</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="cancelDeleteConversation">取消</button>
          <button class="delete-confirm-button" @click="confirmDeleteConversation">删除</button>
        </div>
      </div>
    </div>
    
    <!-- 会话操作菜单 -->
    <div v-if="showConversationMenu && selectedConversation" class="conversation-menu">
      <div class="menu-item" @click="openRenameDialog">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
        </svg>
        <span>重命名</span>
      </div>
      <div class="menu-item danger" @click="deleteConversation(selectedConversation.conversationId)">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
        </svg>
        <span>删除</span>
      </div>
    </div>
    
    <!-- 消息操作菜单 -->
    <div v-if="showMessageMenu && selectedMessage" class="message-menu">
      <div class="menu-item danger" @click="deleteMessage(selectedMessage)">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
        </svg>
        <span>删除</span>
      </div>
    </div>
    
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed, 'auto-hidden': sidebarAutoHidden }">
      <!-- Logo区域 -->
      <div class="sidebar-header">
        <div class="logo">
          <div class="logo-icon">
            <img :src="logo" width="64" height="64" alt="EeveeAgent Logo" style="object-fit: contain; border-radius: 8px;"/>
          </div>
          <span class="logo-text" v-show="!sidebarCollapsed">EeveeAgent</span>
        </div>
      </div>
      
      <!-- 通用功能 -->
      <div class="sidebar-section">
        <h3 class="section-title" v-show="!sidebarCollapsed">通用功能</h3>
        <div class="menu-list">
          <div class="menu-item new-chat" :class="{ collapsed: sidebarCollapsed }" @click="newChat">
            <span class="menu-icon">
              <svg t="1776686284557" class="icon" viewBox="0 0 1025 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="4850" width="16" height="16">
                <path d="M900.938476 441.20303c-17.27205 0-30.034648 14.010497-30.034649 31.225824v412.487184c0 41.067206-31.849774 76.093448-72.973702 76.093448h-648.90723c-41.123928 0-88.941131-34.997881-88.941131-76.093448V235.243052c0-41.209012 47.817202-85.055629 88.941131-85.055629h349.439947c17.300411 0 31.225824-12.762598 31.225824-30.06301 0-17.27205-13.925413-30.006287-31.225824-30.006287H149.022895c-82.134412 0-148.982066 62.763623-148.982067 145.096564v649.644625c0 82.276218 66.876016 136.134384 148.982067 136.134384h648.935591c82.162773 0 133.014638-53.829804 133.014638-136.134384V472.428854c0-17.215327-12.79096-31.225824-30.034648-31.225824z m93.025162-309.19522l-105.844483-105.844483c-34.770991-34.856075-95.974741-34.856075-130.745732 0l-86.445334 101.618645-460.673083 441.047043-30.403345 264.270873 14.634446 12.79096 257.492515-35.054604 440.536539-461.297033 101.476838-82.928528c36.04725-36.075612 36.04725-98.52726-0.028361-134.602873z m-740.712855 640.228664l7.572475-123.740483 114.211076 114.154353-121.783551 9.58613z m176.747808-41.804601l-139.736273-139.651189 411.891596-409.764495 137.12703 137.155392-409.282353 412.260292z m520.373683-511.099528l-61.373918 61.373919-149.464209-149.464209 61.345556-61.430641c5.814073-5.785711 13.556716-8.96218 21.809863-8.962181 8.281508 0 16.024151 3.176469 21.838224 8.962181l105.787761 105.759399c12.081927 12.110288 12.081927 31.679606 0.056723 43.761532z" p-id="4851"></path>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">新对话</span>
          </div>
        </div>
      </div>
      
      <!-- 历史会话 -->
      <div class="sidebar-section conversation-section">
        <h3 class="section-title" v-show="!sidebarCollapsed">历史会话</h3>
        <div class="menu-list conversation-list">
          <div 
            v-for="conversation in conversations" 
            :key="conversation.conversationId"
            :class="['menu-item', { active: currentConversationId === conversation.conversationId }]"
            @click="openConversation(conversation)"
          >
            <span class="menu-icon">
              <svg t="1777010404897" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="10462" width="16" height="16"><path d="M821.333333 800H547.584l-86.464 96.074667a32 32 0 1 1-47.573333-42.816l96-106.666667A32 32 0 0 1 533.333333 736h288a53.333333 53.333333 0 0 0 53.333334-53.333333V234.666667a53.333333 53.333333 0 0 0-53.333334-53.333334H202.666667a53.333333 53.333333 0 0 0-53.333334 53.333334v448a53.333333 53.333333 0 0 0 53.333334 53.333333h138.666666a32 32 0 0 1 0 64H202.666667c-64.8 0-117.333333-52.533333-117.333334-117.333333V234.666667c0-64.8 52.533333-117.333333 117.333334-117.333334h618.666666c64.8 0 117.333333 52.533333 117.333334 117.333334v448c0 64.8-52.533333 117.333333-117.333334 117.333333zM704 341.333333a32 32 0 0 1 0 64H320a32 32 0 0 1 0-64h384zM512 512a32 32 0 0 1 0 64H320a32 32 0 0 1 0-64h192z" fill="currentColor" p-id="10463"></path></svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">{{ conversation.title }}</span>
            <button 
              v-show="!sidebarCollapsed" 
              class="conversation-more-button"
              @click.stop="showConversationOptions(conversation, $event, $event.target)"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="1"/>
                <circle cx="19" cy="12" r="1"/>
                <circle cx="5" cy="12" r="1"/>
              </svg>
            </button>
          </div>
          <div v-if="conversations.length === 0" class="empty-conversations" v-show="!sidebarCollapsed">
            <p>暂无历史会话</p>
          </div>
        </div>
        

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
            <svg class="icon" viewBox="0 0 1821 1024" width="32" height="32">
              <path d="M330.666667 105.333333h354.666666v354.666667H330.666667zM330.666667 553.333333h354.666666v354.666667H330.666667zM778.666667 553.333333h354.666666v354.666667H778.666667z" fill="#077CE7"></path>
              <path d="M955.996 31.873333l250.784 250.785334-250.784 250.784-250.785333-250.784z" fill="#83BDF3"></path>
            </svg>
            <span>管理后台</span>
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
            <div class="welcome-content">
              <h1 class="welcome-title">有什么我能帮你的吗？</h1>
              
              <!-- 热门问题标签 -->
              <div class="hot-questions-container">
                <div 
                  v-for="(question, index) in sampleQuestions" 
                  :key="index"
                  class="hot-question-tag"
                  @click="selectSampleQuestion(question)"
                >
                  {{ question.question }}
                </div>
                <div v-if="isLoadingSampleQuestions" class="loading-hot-questions">
                  <div class="loading-spinner"></div>
                  <span>加载中...</span>
                </div>
                <div v-if="!isLoadingSampleQuestions && sampleQuestions.length === 0" class="no-hot-questions">
                  <span>暂无热门问题</span>
                </div>
              </div>
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
                  <div class="ai-avatar" style="display: flex; align-items: center; justify-content: center; width: 44px; height: 44px;">
                    <svg t="1776749202611" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="12774" width="28" height="28"><path d="M576 192v64H352A160 160 0 0 0 192 416v192A160 160 0 0 0 352 768h320A160 160 0 0 0 832 608v-192h64v192a224 224 0 0 1-224 224h-320A224 224 0 0 1 128 608v-192A224 224 0 0 1 352 192H576z m73.344 217.344l45.312 45.312-3.52 3.456-39.168 39.168-12.608 12.608L637.248 512l57.408 57.344-45.312 45.312-80-80a32 32 0 0 1 0-45.312l17.28-17.28 20.096-20.032 21.12-21.184 3.84-3.712 14.208-14.272 3.456-3.52zM432 416v192H352v-192h80z m351.488-292.032c8.128 31.168 21.824 56.192 41.088 75.52 19.2 19.2 44.288 32.896 75.52 41.024 15.872 4.16 15.872 26.816 0 30.976-31.232 8.128-56.32 21.824-75.52 41.088-19.2 19.2-32.96 44.288-41.088 75.52-4.16 15.872-26.816 15.872-30.976 0-8.128-31.232-21.824-56.32-41.088-75.52-19.2-19.2-44.288-32.96-75.52-41.088-15.872-4.16-15.872-26.816 0-30.976 31.232-8.128 56.32-21.824 75.52-41.088 19.2-19.2 32.96-44.288 41.088-75.52 4.16-15.872 26.816-15.872 30.976 0z" fill="#666666" p-id="12775"></path></svg>
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
                    <MarkdownRenderer 
                      :content="message.content" 
                      :is-streaming="message.isStreaming"
                    />
                  </div>
                  
                  <!-- AI消息操作按钮 -->
                  <div v-if="!message.isThinking && !message.isStreaming" class="message-actions">
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
                    <button class="action-btn" title="更多" @click="showMessageOptions(message, $event)">
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
          <div class="input-wrapper" :class="{ 'input-focused': isInputFocused }">
            <textarea 
              v-model="inputText" 
              placeholder="请输入内容..." 
              @keypress="handleKeyPress"
              @paste="handlePaste"
              @input="handleInput"
              @focus="isInputFocused = true"
              @blur="isInputFocused = false"
              :disabled="isLoading"
              rows="1"
              class="chat-textarea"
            ></textarea>
            <div class="input-actions">
              <!-- 深度思考按钮 -->
              <button 
                class="deep-thinking-button" 
                :class="{ active: deepThinking }"
                @click="toggleDeepThinking"
              >
                <svg t="1776686431066" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="8999" width="16" height="16">
                  <path d="M81.066667 469.333333l81.066666-78.933333C128 358.4 113.066667 320 113.066667 275.2c0-44.8 14.933333-83.2 46.933333-115.2C192 128 230.4 113.066667 275.2 113.066667c44.8 0 83.2 14.933333 115.2 46.933333l236.8 236.8 78.933333-78.933333L469.333333 81.066667C416 27.733333 352 0 275.2 0 198.4 0 134.4 27.733333 81.066667 81.066667S0 198.4 0 275.2c0 76.8 27.733333 140.8 81.066667 194.133333zM554.666667 81.066667l78.933333 81.066666C665.6 128 704 113.066667 748.8 113.066667c44.8 0 83.2 14.933333 115.2 46.933333 32 32 46.933333 70.4 46.933333 115.2s-14.933333 83.2-46.933333 115.2L627.2 627.2l78.933333 78.933333L942.933333 469.333333c53.333333-53.333333 81.066667-119.466667 81.066667-194.133333 0-76.8-27.733333-140.8-81.066667-194.133333S823.466667 0 748.8 0c-76.8 0-140.8 27.733333-194.133333 81.066667z m-85.333334 861.866666l-78.933333-81.066666c-32 32-70.4 46.933333-115.2 46.933333-44.8 0-83.2-14.933333-115.2-46.933333C128 829.866667 113.066667 791.466667 113.066667 746.666667s14.933333-83.2 46.933333-115.2l236.8-236.8-78.933333-78.933334L81.066667 552.533333C27.733333 608 0 672 0 748.8c0 76.8 27.733333 140.8 81.066667 194.133333S200.533333 1024 275.2 1024s140.8-27.733333 194.133333-81.066667z m100.266667-373.333333c21.333333-21.333333 27.733333-51.2 21.333333-78.933333-6.4-27.733333-29.866667-51.2-57.6-57.6-27.733333-6.4-57.6 0-78.933333 21.333333-32 32-32 83.2 0 115.2 32 32 83.2 32 115.2 0z m373.333333-14.933333l-81.066666 78.933333c32 32 46.933333 70.4 46.933333 115.2 0 44.8-14.933333 83.2-46.933333 115.2-32 32-70.4 46.933333-115.2 46.933333-44.8 0-83.2-14.933333-115.2-46.933333l-234.666667-236.8-78.933333 78.933333L554.666667 942.933333c53.333333 53.333333 119.466667 81.066667 194.133333 81.066667 76.8 0 140.8-27.733333 194.133333-81.066667s81.066667-119.466667 81.066667-194.133333c0-76.8-27.733333-140.8-81.066667-194.133333z" p-id="9000"></path>
                </svg>
                <span>深度思考</span>
              </button>

              <div class="send-button-container">
                <div class="char-count" :class="{ 'limit-exceeded': inputText.length > 2000 }">
                  {{ inputText.length }}/2000
                </div>
                <button 
                  v-if="isStreaming" 
                  class="cancel-button" 
                  @click="cancelStreaming"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
                <button 
                  v-else 
                  class="send-button" 
                  :class="{ 'send-button-active': inputText.trim() && !isLoading }"
                  @click="sendMessage"
                  :disabled="isLoading || !inputText.trim()"
                >
                  <svg t="1777011460737" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="19653" width="16" height="16"><path d="M469.333333 371.84L319.445333 512 256 452.693333 512 213.333333l256 239.36L704.554667 512 554.666667 371.84V853.333333h-85.333334z" fill="currentColor" p-id="19654"></path></svg>
                </button>
              </div>
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

.toast-message.warning {
  background-color: #fffbeb;
  border: 1px solid #fef3c7;
  color: #92400e;
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
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
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
  gap: 2px;
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

/* 历史会话区域 - 可滚动 */
.conversation-section {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding-bottom: 0;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
  margin-right: -4px;
}

/* 自定义滚动条样式 */
.conversation-list::-webkit-scrollbar {
  width: 4px;
}

.conversation-list::-webkit-scrollbar-track {
  background: transparent;
}

.conversation-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.conversation-list::-webkit-scrollbar-thumb:hover {
  background: #b3b3b3;
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
  position: relative;
  z-index: 10;
}

.menu-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.conversation-more-button {
  flex-shrink: 0;
  margin-left: auto;
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
  background-color: transparent;
  color: #1a1a1a;
  font-weight: 600;
  border-left: none;
  box-shadow: none;
  transform: none;
  transition: all 0.3s ease;
}

.menu-item.new-chat {
  background-color: #e6f0ff;
  color: #667eea;
  font-weight: 600;
  border-left: 3px solid #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
  transform: translateX(2px);
  transition: all 0.3s ease;
}

.menu-item.new-chat:hover {
  background-color: #e6f0ff;
  color: #667eea;
}

.menu-item.new-chat .menu-icon svg {
  fill: #667eea;
  stroke: #667eea;
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
  border: 1px solid #b8860b;
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

/* 深度思考按钮样式 */
.deep-thinking-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  background-color: white;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.deep-thinking-button:hover {
  border-color: #667eea;
  color: #667eea;
}

.deep-thinking-button.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: white;
}

.deep-thinking-button.active svg {
  fill: white;
  stroke: white;
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
  gap: 1px;
  padding: 8px 16px;
  background-color: white;
  border: 2px solid #e0e0e0;
  border-radius: 20px;
  font-size: 16px;
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

/* 会话操作按钮 */
.conversation-more-button {
  width: 24px;
  height: 24px;
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
  margin-left: auto;
  opacity: 0;
}

.menu-item:hover .conversation-more-button {
  opacity: 1;
}

.conversation-more-button:hover {
  background-color: #e8e8e8;
  color: #666;
}

/* 会话菜单 */
.conversation-menu {
  position: fixed;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 4px;
  z-index: 1000;
  min-width: 120px;
}

.conversation-menu .menu-item {
  padding: 8px 12px;
  font-size: 14px;
  color: #333;
  justify-content: flex-start;
  gap: 8px;
}

.conversation-menu .menu-item:hover {
  background-color: #f5f5f5;
}

.conversation-menu .menu-item.danger {
  color: #ef4444;
}

.conversation-menu .menu-item.danger:hover {
  background-color: #fef2f2;
  color: #dc2626;
}

/* 消息操作菜单 */
.message-menu {
  position: fixed;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 4px;
  z-index: 1000;
  min-width: 120px;
}

.message-menu .menu-item {
  padding: 8px 12px;
  font-size: 14px;
  color: #333;
  justify-content: flex-start;
  gap: 8px;
}

.message-menu .menu-item:hover {
  background-color: #f5f5f5;
}

.message-menu .menu-item.danger {
  color: #ef4444;
}

.message-menu .menu-item.danger:hover {
  background-color: #fef2f2;
  color: #dc2626;
}

/* 空会话提示 */
.empty-conversations {
  padding: 20px 12px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

/* 重命名对话框 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  width: 400px;
  max-width: 90%;
  animation: slideDown 0.3s ease-out;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.dialog-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.dialog-close {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  transition: all 0.2s ease;
}

.dialog-close:hover {
  background-color: #f5f5f5;
  color: #666;
}

.dialog-body {
  padding: 20px;
}

.dialog-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.dialog-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
}

.dialog-button {
  padding: 8px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dialog-button.cancel {
  background-color: white;
  color: #666;
}

.dialog-button.cancel:hover {
  background-color: #f5f5f5;
  border-color: #d1d5db;
}

.dialog-button.confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: white;
}

.dialog-button.confirm:hover {
  opacity: 0.9;
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
  position: relative;
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
  padding: 40px 20px;
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
  margin-bottom: 40px;
}

/* 欢迎页面样式 */
.welcome-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  max-width: 800px;
  width: 100%;
  padding: 0 20px;
}

.welcome-title {
  font-size: 32px;
  font-weight: 600;
  color: #333;
  margin-bottom: 40px;
  text-align: center;
}

/* 热门问题标签样式 */
.hot-questions-container {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  width: 100%;
  margin-top: 20px;
}

.hot-question-tag {
  background-color: #f5f5f5;
  color: #333;
  border-radius: 18px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e0e0e0;
}

.hot-question-tag:hover {
  background-color: #f8f9fa;
  border-color: #e0e0e0;
  color: #333;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.loading-hot-questions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #666;
  width: 100%;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.no-hot-questions {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
  width: 100%;
}

/* 消息列表 */
.message-list {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 900px;
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
  margin-bottom: 35px;
}

.user-message .message-content {
  background: linear-gradient(135deg, #e8e8ef 0%, #f0f3ed 100%);
  color: black;
  padding: 12px 16px;
  border-radius: 18px 18px 4px 18px;
  font-size: 14px;
  line-height: 1.4;
  word-wrap: break-word;
  max-width: 80%;
  width: fit-content;
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

.message-content {
  width: 100%;
}

.message-content :deep(.base-card) {
  margin: 0;
  padding: 0;
  box-shadow: none;
  border: none;
  background: transparent;
}

.message-content :deep(.base-card:hover) {
  transform: none;
  box-shadow: none;
}

.message-content :deep(.markdown-body) {
  padding: 0;
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
  /* 核心布局 */
  width: 100%;
  max-width: 960px; /* 输入区域标准宽度，适配答题场景 */
  margin: 0 auto;
  padding: 20px 24px; /* 舒适内边距，不挤压文字 */
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 12px; /* 按钮/输入框自动间距，替代margin */

  /* 视觉优化 */
  box-sizing: border-box; /* 必备：防止padding撑宽容器 */
  background: transparent; /* 保持原有背景，不破坏样式 */
}

.input-wrapper {
  background: linear-gradient(to bottom, rgba(248, 249, 250, 0.8), rgba(255, 255, 255, 0.2));
  backdrop-filter: blur(10px);
  border: 1px solid rgba(224, 224, 224, 0.5);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  width: 100%;
  max-width: 820px;
  margin: 0;
  transition: all 0.3s ease;
  position: relative;
}

.input-wrapper::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, rgba(248, 249, 250, 0.9), rgba(248, 249, 250, 0.1));
  border-radius: 12px;
  z-index: -1;
  opacity: 0.8;
}

.input-wrapper.input-focused {
  border-color: rgba(100, 149, 237, 0.6);
  box-shadow: 0 0 0 2px rgba(100, 149, 237, 0.2), 0 4px 12px rgba(100, 149, 237, 0.15);
  background: linear-gradient(to bottom, rgba(248, 249, 250, 0.95), rgba(255, 255, 255, 0.4));
}

.input-wrapper input,
.input-wrapper textarea {
  width: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  padding: 8px 0;
  margin-bottom: 12px;
  color: #1a1a1a;
  font-family: inherit;
  resize: none;
  overflow: auto;
  min-height: 24px;
  max-height: 200px;
  background-color: transparent;
}

.input-wrapper input::placeholder,
.input-wrapper textarea::placeholder {
  color: #999;
}

.chat-textarea {
  line-height: 1.4;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: nowrap;
  width: 100%;
}

.send-button-container {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.char-count {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  margin: 0;
}

.char-count.limit-exceeded {
  color: #ff4d4f;
  font-weight: 500;
}

.send-button {
  width: 36px;
  height: 36px;
  border: none;
  background: #e8e8e8;
  border-radius: 50%;
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  transition: all 0.2s ease;
}

.send-button.send-button-active {
  background: #667eea;
  color: white;
  cursor: pointer;
}

.send-button.send-button-active:hover {
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

.cancel-button {
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

.cancel-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}



/* 回到底部按钮 */
.scroll-to-bottom-button {
  position: fixed;
  bottom: 100px;
  right: 40px;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background-color: rgba(224, 224, 224, 0.8);
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 99;
}

.scroll-to-bottom-button:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background-color: rgba(192, 192, 192, 0.9);
}

.scroll-to-bottom-button svg {
  width: 16px;
  height: 16px;
  stroke: #333;
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .scroll-to-bottom-button {
    background-color: rgba(64, 64, 64, 0.8);
    color: #e0e0e0;
  }
  
  .scroll-to-bottom-button:hover {
    background-color: rgba(80, 80, 80, 0.9);
  }
  
  .scroll-to-bottom-button svg {
    stroke: #e0e0e0;
  }
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

/* 删除确认弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: modalFadeIn 0.3s ease;
  backdrop-filter: blur(4px);
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  width: 520px;
  max-width: 90%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  animation: modalSlideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.delete-modal {
  width: 480px;
}

.delete-modal .modal-body {
  text-align: center;
  padding: 40px 28px;
}

@keyframes modalSlideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-button {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.close-button:hover {
  background-color: #f0f0f0;
  color: #666;
}

.modal-body {
  padding: 24px 28px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 28px;
  border-top: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.delete-icon {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
}

.delete-icon svg {
  width: 64px;
  height: 64px;
  opacity: 0.9;
}

.delete-message {
  font-size: 18px;
  color: #333;
  margin-bottom: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.delete-name {
  color: #ff4d4f;
  font-weight: 600;
  display: inline-block;
  padding: 0 4px;
}

.delete-hint {
  font-size: 14px;
  color: #999;
}

.cancel-button {
  padding: 10px 24px;
  background-color: white;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 80px;
  text-align: center;
}

.cancel-button:hover {
  background-color: #f5f5f5;
  border-color: #bfbfbf;
}

.delete-confirm-button {
  padding: 10px 24px;
  background-color: #ff4d4f;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
  min-width: 80px;
  text-align: center;
}

.delete-confirm-button:hover {
  background-color: #ff7875;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 77, 79, 0.3);
}
</style>
