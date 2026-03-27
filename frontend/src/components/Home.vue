<script setup>
import { ref, onMounted } from 'vue'

const userInfo = ref(null)

onMounted(() => {
  // 从本地存储获取用户信息
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    userInfo.value = JSON.parse(storedUserInfo)
  }
})

const handleLogout = () => {
  // 清除本地存储
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  // 跳转到登录页
  window.location.href = '/'
}
</script>

<template>
  <div class="home-container">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h2>AI Chat</h2>
      </div>
      
      <div class="sidebar-section">
        <h3>通用功能</h3>
        <div class="sidebar-item active">
          <span class="icon">💬</span>
          <span>AI对话</span>
        </div>
      </div>
      
      <div class="sidebar-section">
        <h3>历史对话</h3>
        <!-- 历史对话列表 -->
      </div>
      
      <div class="sidebar-footer">
        <div class="user-info">
          <div class="avatar">
            {{ userInfo?.username?.charAt(0) || 'U' }}
          </div>
          <div class="user-details">
            <div class="username">{{ userInfo?.username || '当前用户名' }}</div>
            <div class="role">{{ userInfo?.role || '用户' }}</div>
          </div>
        </div>
        <button class="logout-button" @click="handleLogout">
          <span class="icon">🚪</span>
          <span>退出登录</span>
        </button>
      </div>
    </aside>
    
    <!-- 主内容区 -->
    <main class="main-content">
      <header class="main-header">
        <div class="header-left">
          <!-- 其他头部功能 -->
        </div>
        <div class="header-right">
          <button class="new-chat-button">+ 新靡对话</button>
        </div>
      </header>
      
      <div class="chat-container">
        <div class="chat-content">
          <div class="welcome-message">
            <div class="robot-icon">
              <img src="/icons.svg" alt="AI Robot" />
            </div>
            <div class="message-text">
              <p>你好，{{ userInfo?.username || '<当前用户名>' }}</p>
              <p>今天需要我帮你做点什么吗？</p>
            </div>
          </div>
          
          <!-- 对话内容区域（暂不实现） -->
        </div>
        
        <div class="chat-input-area">
          <input type="text" placeholder="请输入内容..." />
          <div class="input-actions">
            <button class="action-button">
              <span class="icon">🖼️</span>
              <span>上传图片</span>
            </button>
            <button class="action-button">
              <span class="icon">📄</span>
              <span>上传文档</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.home-container {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
}

/* 侧边栏样式 */
.sidebar {
  width: 280px;
  background-color: white;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.sidebar-header {
  margin-bottom: 30px;
}

.sidebar-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.sidebar-section {
  margin-bottom: 30px;
}

.sidebar-section h3 {
  font-size: 14px;
  font-weight: 600;
  color: #999;
  margin-bottom: 10px;
  text-transform: uppercase;
}

.sidebar-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 8px;
}

.sidebar-item:hover {
  background-color: #f0f0f0;
}

.sidebar-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.sidebar-item .icon {
  margin-right: 12px;
  font-size: 16px;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-right: 12px;
}

.user-details {
  flex: 1;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 2px;
}

.role {
  font-size: 12px;
  color: #999;
}

.logout-button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  background-color: #f5f5f5;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #666;
}

.logout-button:hover {
  background-color: #e0e0e0;
  color: #333;
}

.logout-button .icon {
  margin-right: 8px;
}

/* 主内容区样式 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-header {
  padding: 20px;
  background-color: white;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.new-chat-button {
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.new-chat-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 40px;
  overflow: hidden;
}

.chat-content {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 30px;
}

.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #666;
}

.robot-icon {
  margin-bottom: 20px;
}

.robot-icon img {
  width: 120px;
  height: 120px;
  object-fit: contain;
}

.message-text p {
  margin: 8px 0;
  font-size: 16px;
}

.chat-input-area {
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chat-input-area input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  padding: 12px 0;
  margin-bottom: 12px;
}

.input-actions {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
}

.action-button {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background-color: #f5f5f5;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
  color: #666;
}

.action-button:hover {
  background-color: #e0e0e0;
  color: #333;
}

.action-button .icon {
  margin-right: 6px;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 240px;
  }
  
  .chat-container {
    padding: 20px;
  }
  
  .welcome-message {
    padding: 40px 20px;
  }
}
</style>