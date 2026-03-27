<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const userInfo = ref(null)
const showUserMenu = ref(false)
const sidebarCollapsed = ref(false)

onMounted(() => {
  // 从本地存储获取用户信息
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    userInfo.value = JSON.parse(storedUserInfo)
  }
  
  // 点击外部关闭菜单
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
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
  // 跳转到登录页
  window.location.href = '/'
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const goToAdmin = () => {
  router.push('/admin')
}
</script>

<template>
  <div class="home-container">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
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
          <div class="menu-item" :class="{ collapsed: sidebarCollapsed }">
            <span class="menu-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
                <line x1="12" y1="17" x2="12" y2="21"/>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">AI 生成视频</span>
          </div>
          <div class="menu-item" :class="{ collapsed: sidebarCollapsed }">
            <span class="menu-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <polyline points="21 15 16 10 5 21"/>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">AI 生成图片</span>
          </div>
          <div class="menu-item" :class="{ collapsed: sidebarCollapsed }">
            <span class="menu-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="16 18 22 12 16 6"/>
                <polyline points="8 6 2 12 8 18"/>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">AI 生成代码</span>
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
              <div class="email">{{ userInfo?.email || 'momo@MODAO.com' }}</div>
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
    <main class="main-content" :class="{ expanded: sidebarCollapsed }">
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
        <div class="chat-content">
          <div class="welcome-message">
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
        </div>
        
        <!-- 输入区域 -->
        <div class="chat-input-area">
          <div class="input-wrapper">
            <input type="text" placeholder="请输入内容..." />
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
              <div class="char-count">0/200</div>
              <button class="send-button">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="22" y1="2" x2="11" y2="13"/>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"/>
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

.email {
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

/* 输入区域 */
.chat-input-area {
  padding: 20px 40px 40px;
  flex-shrink: 0;
}

.input-wrapper {
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
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
