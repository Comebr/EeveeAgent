<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import UserManagement from './user/UserManagement.vue'

const router = useRouter()
const userInfo = ref(null)
const activeMenu = ref('overview')
const searchQuery = ref('')
const sidebarCollapsed = ref(false)

onMounted(() => {
  // 从本地存储获取用户信息
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    userInfo.value = JSON.parse(storedUserInfo)
  }
})

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/')
}

const goToHome = () => {
  router.push('/home')
}

const setActiveMenu = (menu) => {
  activeMenu.value = menu
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <div class="admin-container">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <!-- Logo区域 -->
      <div class="sidebar-header">
        <div class="logo">
          <div class="logo-icon">E</div>
          <span class="logo-text" v-show="!sidebarCollapsed">Eevee 管理后台</span>
        </div>
      </div>
      
      <!-- 主菜单 -->
      <div class="sidebar-section">
        <h3 class="section-title" v-show="!sidebarCollapsed">主菜单</h3>
        <div class="menu-list">
          <div 
            class="menu-item" 
            :class="{ active: activeMenu === 'overview' }"
            @click="setActiveMenu('overview')"
          >
            <span class="menu-icon">
              <svg width="20" height="20" viewBox="0 0 24 24">
                <rect x="4" y="4" width="16" height="12" fill="currentColor"/>
                <rect x="6" y="8" width="2" height="8" fill="#1E293B"/>
                <rect x="10" y="6" width="2" height="10" fill="#1E293B"/>
                <rect x="14" y="10" width="2" height="6" fill="#1E293B"/>
                <rect x="6" y="18" width="12" height="1" fill="currentColor"/>
                <rect x="6" y="21" width="12" height="1" fill="currentColor"/>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">数据概览</span>
          </div>
          <div 
            class="menu-item"
            :class="{ active: activeMenu === 'knowledge' }"
            @click="setActiveMenu('knowledge')"
          >
            <span class="menu-icon">
              <svg width="20" height="20" viewBox="0 0 24 24">
                <rect x="4" y="4" width="16" height="20" rx="2" ry="2" fill="currentColor"/>
                <rect x="6" y="6" width="12" height="4" fill="#1E293B"/>
                <circle cx="9" cy="8" r="1" fill="currentColor"/>
                <rect x="6" y="12" width="12" height="4" fill="#1E293B"/>
                <circle cx="9" cy="14" r="1" fill="currentColor"/>
                <rect x="6" y="18" width="12" height="4" fill="#1E293B"/>
                <circle cx="9" cy="20" r="1" fill="currentColor"/>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">知识库管理</span>
          </div>
        </div>
      </div>
      
      <!-- 系统 -->
      <div class="sidebar-section">
        <h3 class="section-title" v-show="!sidebarCollapsed">系统</h3>
        <div class="menu-list">
          <div 
            class="menu-item"
            :class="{ active: activeMenu === 'users' }"
            @click="setActiveMenu('users')"
          >
            <span class="menu-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">用户管理</span>
          </div>
          <div 
            class="menu-item"
            :class="{ active: activeMenu === 'settings' }"
            @click="setActiveMenu('settings')"
          >
            <span class="menu-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3"/>
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">系统设置</span>
          </div>
        </div>
      </div>
      
      <!-- 底部收起按钮 -->
      <div class="sidebar-footer">
        <button class="collapse-button" @click="toggleSidebar">
          <span v-if="!sidebarCollapsed">« 收起侧边栏</span>
          <span v-else>»</span>
        </button>
      </div>
    </aside>
    
    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部header -->
      <header class="main-header">
        <div class="header-left">
          <!-- 搜索框 -->
          <div class="search-box">
            <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="M21 21l-4.35-4.35"/>
            </svg>
            <input 
              type="text" 
              placeholder="搜索..." 
              v-model="searchQuery"
            />
          </div>
        </div>
        
        <div class="header-right">
          <!-- 返回首页按钮 -->
          <button class="back-button" @click="goToHome">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
              <polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
            <span>返回首页</span>
          </button>
          
          <!-- 用户信息 -->
          <div class="user-info">
            <div class="avatar">
              <img v-if="userInfo?.avatar" :src="userInfo.avatar" alt="avatar" />
              <span v-else>{{ userInfo?.username?.charAt(0).toUpperCase() || 'A' }}</span>
            </div>
            <span class="username">{{ userInfo?.username || 'admin' }}</span>
          </div>
        </div>
      </header>
      
      <!-- 面包屑 -->
      <div class="breadcrumb">
        <span class="breadcrumb-link" @click="setActiveMenu('overview')">首页</span>
        <span class="separator">/</span>
        <span class="current">{{ activeMenu === 'overview' ? '数据概览' : activeMenu === 'knowledge' ? '知识库管理' : activeMenu === 'users' ? '用户管理' : '系统设置' }}</span>
      </div>
      
      <!-- 内容区域 -->
      <div class="content-area">
        <!-- 这里根据activeMenu显示不同的内容 -->
        <div v-if="activeMenu === 'overview'" class="page-content">
          <h2>数据概览</h2>
          <p>欢迎使用 Eevee 管理后台</p>
        </div>
        <div v-else-if="activeMenu === 'knowledge'" class="page-content">
          <h2>知识库管理</h2>
          <p>知识库管理功能开发中...</p>
        </div>
        <div v-else-if="activeMenu === 'users'" class="page-content user-page">
          <UserManagement />
        </div>
        <div v-else-if="activeMenu === 'settings'" class="page-content">
          <h2>系统设置</h2>
          <p>系统设置功能开发中...</p>
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

.admin-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  background-color: #f5f7fa;
  overflow: hidden;
}

/* 侧边栏样式 #1E293B */
.sidebar {
  width: 270px;
  min-width: 270px;
  background-color: #162431;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.sidebar.collapsed {
  width: 60px;
  min-width: 60px;
}

/* Logo区域 */
.sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.logo-text {
  font-size: 16px;
  font-weight: 500;
  color: white;
}

/* 侧边栏内容区域 */
.sidebar-section {
  padding: 20px 12px;
}

.section-title {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 12px;
  padding: 0 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px; /* 调整图标与文字之间的间距 */
  padding: 8px 12px;/* 调整上下内边距改变高度 */
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: rgba(255, 255, 255, 0.7);
  font-size: 15px; /* 字体大小 */
  position: relative;
}

.menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
}

.menu-item.active {
  background-color: rgba(59, 54, 121, 0.71);
  color: #fafdff;
  position: relative;
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
  border-left: 6px solid #347CAF;
}

.sidebar.collapsed .menu-item {
  justify-content: center;
  padding: 8px;
}

.sidebar.collapsed .menu-item.active {
  border-radius: 6px;
}

.sidebar.collapsed .menu-item.active::before {
  display: none;
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
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

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
  width: 280px;
}

.search-icon {
  color: #999;
}

.search-box input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: #333;
  width: 100%;
}

.search-box input::placeholder {
  color: #999;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-button:hover {
  background-color: #f5f5f5;
  border-color: #d0d0d0;
  color: #333;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.username {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* 面包屑 */
.breadcrumb {
  padding: 16px 24px;
  background-color: white;
  border-bottom: 1px solid #e8e8e8;
  font-size: 14px;
  color: #666;
  flex-shrink: 0;
}

.breadcrumb .separator {
  margin: 0 8px;
  color: #999;
}

.breadcrumb .current {
  color: #333;
  font-weight: 500;
}

.breadcrumb-link {
  cursor: pointer;
  color: #999;
  transition: color 0.2s ease;
}

.breadcrumb-link:hover {
  color: #000;
  text-decoration: none;
}

/* 侧边栏底部 */
.sidebar-footer {
  margin-top: auto;
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.collapse-button {
  width: 100%;
  padding: 8px;
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.collapse-button:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
}

.sidebar.collapsed .collapse-button {
  padding: 8px 0;
}

/* 内容区域 */
.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.page-content {
  background-color: white;
  border-radius: 8px;
  padding: 24px;
  min-height: 400px;
}

.page-content h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.page-content p {
  font-size: 14px;
  color: #666;
}

/* 用户管理页面样式 */
.page-content.user-page {
  padding: 0;
  background: transparent;
  box-shadow: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -220px;
    height: 100vh;
    z-index: 1000;
    transition: left 0.3s ease;
  }
  
  .sidebar.open {
    left: 0;
  }
  
  .search-box {
    width: 200px;
  }
  
  .content-area {
    padding: 16px;
  }
}
</style>
