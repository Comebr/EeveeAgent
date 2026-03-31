<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import UserManagement from './user/UserManagement.vue'

const router = useRouter()
const userInfo = ref(null)
const activeMenu = ref('knowledge') // 默认显示知识库管理
const searchQuery = ref('')
const sidebarCollapsed = ref(false)

// 知识库数据
const knowledgeList = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 弹窗控制
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showDeleteConfirm = ref(false)
const editingItem = ref({})
const itemToDelete = ref(null)

// 消息提示
const showMessage = ref(false)
const messageText = ref('')
const messageType = ref('success')

// 创建表单
const createForm = reactive({
  kbName: '',
  embeddingModel: 'qwen-embedding',
  collection: ''
})

// 计算属性
const totalPages = computed(() => {
  return Math.ceil(pagination.total / pagination.size) || 1
})

// 获取知识库列表
const fetchKnowledgeList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/kb/pageQuery', {
      params: {
        current: pagination.current,
        size: pagination.size,
        kbName: searchQuery.value
      }
    })
    if (response.data.code === '0') {
      const pageData = response.data.data
      knowledgeList.value = pageData.records.map(item => ({
                    kbId: item.id, // 使用id作为唯一标识
                    name: item.kbName,
                    embeddingModel: item.embeddingModel,
                    collection: item.collection,
                    owner: item.createBy,
                    createdAt: formatDate(item.createTime),
                    updatedAt: formatDate(item.updateTime)
                  }))
      pagination.total = pageData.total
    }
  } catch (error) {
    console.error('获取知识库列表失败:', error)
    showToast('获取知识库列表失败', 'error')
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).replace(/\//g, '/')
}

// 刷新知识库列表
const refreshKnowledgeList = () => {
  fetchKnowledgeList()
}

// 分页切换
const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  pagination.current = page
  fetchKnowledgeList()
}

// 编辑知识库
const handleEdit = (item) => {
  editingItem.value = { ...item }
  showEditModal.value = true
}

// 打开删除确认弹窗
const handleDeleteClick = (item) => {
  itemToDelete.value = item
  showDeleteConfirm.value = true
}

// 确认删除
const handleDeleteConfirm = async () => {
  if (!itemToDelete.value) return
  
  try {
    const response = await axios.delete(`/kb/deleteKb/${itemToDelete.value.kbId}`)
    if (response.data.code === '0') {
      showToast('删除成功', 'success')
      showDeleteConfirm.value = false
      fetchKnowledgeList()
    } else {
      showToast(response.data.message || '删除失败', 'error')
    }
  } catch (error) {
    console.error('删除知识库失败:', error)
    showToast('删除失败', 'error')
  } finally {
    itemToDelete.value = null
  }
}

// 取消删除
const handleDeleteCancel = () => {
  showDeleteConfirm.value = false
  itemToDelete.value = null
}

// 创建知识库
const handleCreate = async () => {
  if (!createForm.kbName || !createForm.embeddingModel || !createForm.collection) {
    showToast('请填写所有必填项', 'warning')
    return
  }
  try {
    const response = await axios.post('/kb/createKb', createForm)
    if (response.data.code === '0') {
      showToast('创建成功', 'success')
      showCreateModal.value = false
      // 重置表单
      createForm.kbName = ''
      createForm.embeddingModel = 'qwen-embedding'
      createForm.collection = ''
      fetchKnowledgeList()
    } else {
      showToast(response.data.message || '创建失败', 'error')
    }
  } catch (error) {
    console.error('创建知识库失败:', error)
    showToast('创建失败', 'error')
  }
}

// 更新知识库名称
const handleUpdate = async () => {
  if (!editingItem.value.name) {
    showToast('请输入知识库名称', 'warning')
    return
  }
  try {
    const response = await axios.put(`/kb/renameKb/${editingItem.value.kbId}`, {
      kbName: editingItem.value.name
    })
    if (response.data.code === '0') {
      showToast('更新成功', 'success')
      showEditModal.value = false
      fetchKnowledgeList()
    } else {
      showToast(response.data.message || '更新失败', 'error')
    }
  } catch (error) {
    console.error('更新知识库失败:', error)
    showToast('更新失败', 'error')
  }
}

// 显示消息
const showToast = (text, type = 'success', duration = 3000) => {
  messageText.value = text
  messageType.value = type
  showMessage.value = true
  
  setTimeout(() => {
    showMessage.value = false
  }, duration)
}

onMounted(() => {
  // 从本地存储获取用户信息
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    userInfo.value = JSON.parse(storedUserInfo)
  }
  // 获取知识库列表
  fetchKnowledgeList()
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
        <div v-else-if="activeMenu === 'knowledge'" class="page-content knowledge-page">
          <!-- 页面标题和操作栏 -->
          <div class="page-header">
            <div class="header-left">
              <h2>知识库管理</h2>
              <p class="description">管理所有知识库及其文档</p>
            </div>
            <div class="header-right">
              <div class="search-box">
                <input 
                  type="text" 
                  placeholder="搜索知识库名称" 
                  v-model="searchQuery"
                  @keyup.enter="fetchKnowledgeList"
                />
                <button class="search-button" @click="fetchKnowledgeList">搜索</button>
              </div>
              <button class="refresh-button" @click="refreshKnowledgeList" :disabled="loading">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :class="{ 'spin': loading }">
                  <path d="M21 12c0-4.97-4.03-9-9-9s-9 4.03-9 9 4.03 9 9 9"/>
                  <path d="M15 12l-3-3-3 3"/>
                  <path d="M9 12l3 3 3-3"/>
                </svg>
                刷新
              </button>
              <button class="add-button" @click="showCreateModal = true">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="5" x2="12" y2="19"/>
                  <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                新增知识库
              </button>
            </div>
          </div>
          
          <!-- 数据卡片 -->
          <div class="data-cards">
            <div class="card">
              <div class="card-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="4" y="4" width="16" height="20" rx="2" ry="2"/>
                  <rect x="6" y="6" width="12" height="4" fill="#1E293B"/>
                  <circle cx="9" cy="8" r="1" fill="currentColor"/>
                  <rect x="6" y="12" width="12" height="4" fill="#1E293B"/>
                  <circle cx="9" cy="14" r="1" fill="currentColor"/>
                  <rect x="6" y="18" width="12" height="4" fill="#1E293B"/>
                  <circle cx="9" cy="20" r="1" fill="currentColor"/>
                </svg>
              </div>
              <div class="card-content">
                <div class="card-value">{{ pagination.total }}</div>
                <div class="card-label">知识库</div>
              </div>
              <div class="card-action">
                <span>全部</span>
              </div>
            </div>
            <div class="card">
              <div class="card-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
              </div>
              <div class="card-content">
                <div class="card-value">0</div>
                <div class="card-label">文档数</div>
              </div>
              <div class="card-action">
                <span>全部</span>
              </div>
            </div>
            <div class="card">
              <div class="card-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="8" x2="12" y2="12"/>
                  <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
              </div>
              <div class="card-content">
                <div class="card-value">{{ pagination.total }}</div>
                <div class="card-label">定义组标准</div>
              </div>
              <div class="card-action">
                <span>全部</span>
              </div>
            </div>
            <div class="card">
              <div class="card-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
              </div>
              <div class="card-content">
                <div class="card-value">1</div>
                <div class="card-label">创建用户数</div>
              </div>
              <div class="card-action">
                <span>全部</span>
              </div>
            </div>
          </div>
          
          <!-- 知识库表格 -->
          <div class="knowledge-table">
            <table>
              <thead>
                <tr>
                  <th>名称</th>
                  <th>Embedding模型</th>
                  <th>Collection</th>
                  <th>负责人</th>
                  <th>创建时间</th>
                  <th>修改时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in knowledgeList" :key="item.kbId">
                  <td class="name-cell">{{ item.name }}</td>
                  <td>{{ item.embeddingModel }}</td>
                  <td class="collection-cell">{{ item.collection }}</td>
                  <td>{{ item.owner }}</td>
                  <td>{{ item.createdAt }}</td>
                  <td>{{ item.updatedAt }}</td>
                  <td class="actions-cell">
                    <button class="action-button edit-button" @click="handleEdit(item)">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                      </svg>
                      编辑
                    </button>
                    <button class="action-button delete-button" @click="handleDeleteClick(item)">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"/>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                      </svg>
                      删除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            
            <!-- 分页 -->
            <div class="pagination">
              <span class="total">共{{ pagination.total }}条</span>
              <div class="page-controls">
                <button 
                  class="page-button" 
                  :disabled="pagination.current <= 1"
                  @click="changePage(pagination.current - 1)"
                >上一页</button>
                <span class="current-page">{{ pagination.current }}/{{ totalPages }}</span>
                <button 
                  class="page-button" 
                  :disabled="pagination.current >= totalPages"
                  @click="changePage(pagination.current + 1)"
                >下一页</button>
              </div>
            </div>
          </div>
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
    
    <!-- 创建知识库弹窗 -->
    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>新增知识库</h3>
          <button class="close-button" @click="showCreateModal = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>知识库名称 <span class="required">*</span></label>
            <input 
              v-model="createForm.kbName" 
              type="text" 
              placeholder="请输入知识库名称"
            />
          </div>
          <div class="form-group">
            <label>Embedding模型 <span class="required">*</span></label>
            <select v-model="createForm.embeddingModel">
              <option value="qwen-embedding">qwen-embedding</option>
              <option value="text-embedding-ada-002">text-embedding-ada-002</option>
              <option value="bge-large-zh">bge-large-zh</option>
            </select>
          </div>
          <div class="form-group">
            <label>Collection名称 <span class="required">*</span></label>
            <input 
              v-model="createForm.collection" 
              type="text" 
              placeholder="请输入Collection名称"
            />
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="showCreateModal = false">取消</button>
          <button class="confirm-button" @click="handleCreate">确定</button>
        </div>
      </div>
    </div>
    
    <!-- 编辑知识库弹窗 -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>编辑知识库</h3>
          <button class="close-button" @click="showEditModal = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>知识库名称 <span class="required">*</span></label>
            <input 
              v-model="editingItem.name" 
              type="text" 
              placeholder="请输入知识库名称"
            />
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="showEditModal = false">取消</button>
          <button class="confirm-button" @click="handleUpdate">确定</button>
        </div>
      </div>
    </div>
    
    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="handleDeleteCancel">
      <div class="modal-content delete-modal">
        <div class="modal-header">
          <h3>确认删除</h3>
          <button class="close-button" @click="handleDeleteCancel">
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
          <p class="delete-message">确定要删除知识库 <span class="delete-name">{{ itemToDelete?.name }}</span> 吗？</p>
          <p class="delete-hint">此操作不可撤销，删除后相关数据将被永久清除。</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="handleDeleteCancel">取消</button>
          <button class="delete-confirm-button" @click="handleDeleteConfirm">删除</button>
        </div>
      </div>
    </div>
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

/* 侧边栏样式 */
.sidebar {
  width: 270px;
  min-width: 270px;
  background-color: #162431;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: all 0.3s ease;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
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
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: rgba(255, 255, 255, 0.7);
  font-size: 15px;
  position: relative;
}

.menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
  transform: translateX(4px);
}

.menu-item.active {
  background-color: rgba(102, 126, 234, 0.2);
  color: #667eea;
  position: relative;
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background-color: #667eea;
  border-radius: 0 3px 3px 0;
}

.sidebar.collapsed .menu-item {
  justify-content: center;
  padding: 10px;
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
  width: 24px;
  height: 24px;
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
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
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
  transition: all 0.2s ease;
}

.search-box:hover {
  background-color: #e8eaf0;
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
  gap: 8px;
  padding: 10px 20px;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.back-button:hover {
  background-color: #fafbff;
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.back-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.1), transparent);
  transition: left 0.5s ease;
}

.back-button:hover::before {
  left: 100%;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  background-color: #f5f7fa;
  transition: all 0.2s ease;
}

.user-info:hover {
  background-color: #e8eaf0;
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
  color: #667eea;
  font-weight: 500;
}

.breadcrumb-link {
  cursor: pointer;
  color: #999;
  transition: color 0.2s ease;
}

.breadcrumb-link:hover {
  color: #667eea;
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
  border-color: rgba(255, 255, 255, 0.2);
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
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
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

/* 知识库管理页面样式 */
.knowledge-page {
  padding: 0;
  background: transparent;
  box-shadow: none;
  min-height: 600px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px;
  background-color: white;
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.page-header .header-left h2 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.page-header .description {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.page-header .header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.page-header .search-box {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  height: 36px;
  background-color: white;
}

.page-header .search-box input {
  border: none;
  outline: none;
  padding: 0 12px;
  font-size: 14px;
  color: #333;
  width: 200px;
  height: 100%;
}

.page-header .search-box input::placeholder {
  color: #999;
}

.search-button {
  padding: 0 20px;
  background-color: #f5f7fa;
  border: none;
  border-left: 1px solid #e8e8e8;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  height: 100%;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.search-button:hover {
  background-color: #667eea;
  color: white;
  border-left-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

.refresh-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.refresh-button:hover:not(:disabled) {
  background-color: #fafbff;
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.refresh-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.add-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
}

.add-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #5a6fd8 0%, #6a409a 100%);
}

.add-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.add-button:hover::before {
  left: 100%;
}

/* 数据卡片 */
.data-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  padding: 0 24px 24px;
  margin-bottom: 20px;
}

.card {
  background: linear-gradient(135deg, white 0%, #fafbff 100%);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 20px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid #e8e8e8;
  position: relative;
  overflow: hidden;
}

.card:hover {
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.15);
  transform: translateY(-4px);
  border-color: #667eea;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px 0 0 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.card:hover::before {
  opacity: 1;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e6f7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
  flex-shrink: 0;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.card:hover .card-icon {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.2);
}

.card-content {
  flex: 1;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin-bottom: 6px;
  transition: color 0.3s ease;
}

.card:hover .card-value {
  color: #667eea;
}

.card-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.card-action {
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.card-action:hover {
  color: #667eea;
  transform: translateX(4px);
}

.card-action::after {
  content: '→';
  font-size: 12px;
  transition: transform 0.3s ease;
}

.card-action:hover::after {
  transform: translateX(2px);
}

/* 知识库表格 */
.knowledge-table {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin: 0 24px 24px;
  border: 1px solid #e8e8e8;
  transition: all 0.3s ease;
}

.knowledge-table:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.knowledge-table table {
  width: 100%;
  border-collapse: collapse;
}

.knowledge-table th,
.knowledge-table td {
  padding: 16px 20px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.knowledge-table th {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf0 100%);
  font-size: 14px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.knowledge-table td {
  font-size: 14px;
  color: #666;
  vertical-align: middle;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.knowledge-table tr {
  transition: all 0.3s ease;
}

.knowledge-table tr:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.1);
}

.knowledge-table tr:hover td {
  background-color: #fafbff;
  color: #333;
}

.knowledge-table .name-cell {
  font-weight: 500;
  color: #333;
  font-size: 15px;
  transition: color 0.3s ease;
}

.knowledge-table tr:hover .name-cell {
  color: #667eea;
}

.knowledge-table .collection-cell {
  display: inline-block;
  padding: 6px 16px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e6f7ff 100%);
  border-radius: 16px;
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.1);
}

.knowledge-table tr:hover .collection-cell {
  transform: scale(1.05);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.2);
}

.knowledge-table .actions-cell {
  display: flex;
  gap: 8px;
  white-space: nowrap;
  align-items: center;
}

.knowledge-table .action-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  background-color: white;
  position: relative;
  overflow: hidden;
  min-width: 80px;
  justify-content: center;
}

.knowledge-table .edit-button {
  color: #667eea;
  border-color: #667eea;
}

.knowledge-table .edit-button:hover {
  background-color: #667eea;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.25);
}

.knowledge-table .delete-button {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.knowledge-table .delete-button:hover {
  background-color: #ff4d4f;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.25);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-top: 1px solid #e8e8e8;
  background-color: #fafafa;
}

.pagination .total {
  font-size: 14px;
  color: #666;
}

.pagination .page-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination .page-button {
  padding: 8px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background-color: white;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  min-width: 80px;
  text-align: center;
}

.pagination .page-button:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
}

.pagination .page-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.pagination .current-page {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  min-width: 60px;
  text-align: center;
}

/* 用户管理页面样式 */
.page-content.user-page {
  padding: 0;
  background: transparent;
  box-shadow: none;
}

/* 加载动画 */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.spin {
  animation: spin 1s linear infinite;
}

/* 弹窗样式 */
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
  padding: 24px 28px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.modal-header h3 {
  font-size: 20px;
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
  flex-shrink: 0;
}

.close-button:hover {
  background-color: #f0f0f0;
  color: #333;
  transform: rotate(90deg);
}

.modal-body {
  padding: 28px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-body .form-group {
  margin-bottom: 24px;
}

.modal-body .form-group:last-child {
  margin-bottom: 0;
}

.modal-body label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}

.modal-body .required {
  color: #ff4d4f;
  margin-left: 4px;
}

.modal-body input,
.modal-body select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: all 0.2s ease;
  background-color: #fafafa;
}

.modal-body input:hover,
.modal-body select:hover {
  border-color: #667eea;
  background-color: white;
}

.modal-body input:focus,
.modal-body select:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background-color: white;
}

.modal-body input::placeholder {
  color: #999;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 20px 28px;
  border-top: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.cancel-button {
  padding: 10px 24px;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 80px;
  text-align: center;
}

.cancel-button:hover {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.confirm-button {
  padding: 10px 24px;
  background-color: #667eea;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
  min-width: 80px;
  text-align: center;
}

.confirm-button:hover {
  background-color: #5a6fd8;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.3);
}

/* 删除确认弹窗 */
.delete-modal {
  width: 480px;
}

.delete-modal .modal-body {
  text-align: center;
  padding: 40px 28px;
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
  margin-bottom: 0;
  line-height: 1.4;
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

/* 消息提示样式 */
.toast-message {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 16px 24px;
  border-radius: 12px;
  z-index: 3000;
  display: flex;
  align-items: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  animation: toastSlideIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  min-width: 300px;
  max-width: 480px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

@keyframes toastSlideIn {
  from {
    top: -80px;
    opacity: 0;
    transform: translateX(-50%) scale(0.9);
  }
  to {
    top: 24px;
    opacity: 1;
    transform: translateX(-50%) scale(1);
  }
}

.toast-message.success {
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%);
  border: 1px solid #b7eb8f;
  color: #52c41a;
  box-shadow: 0 8px 24px rgba(82, 196, 26, 0.15);
}

.toast-message.error {
  background: linear-gradient(135deg, #fff2f0 0%, #fff0f0 100%);
  border: 1px solid #ffccc7;
  color: #ff4d4f;
  box-shadow: 0 8px 24px rgba(255, 77, 79, 0.15);
}

.toast-message.warning {
  background: linear-gradient(135deg, #fffbe6 0%, #fffef0 100%);
  border: 1px solid #ffe58f;
  color: #faad14;
  box-shadow: 0 8px 24px rgba(250, 173, 20, 0.15);
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.toast-content svg {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
}

.toast-content span {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  flex: 1;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .data-cards {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }
  
  .page-header .header-right {
    gap: 8px;
  }
  
  .add-button,
  .refresh-button {
    padding: 8px 16px;
    font-size: 13px;
  }
}

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
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .page-header .header-right {
    width: 100%;
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .page-header .search-box {
    flex: 1;
    min-width: 200px;
  }
  
  .data-cards {
    grid-template-columns: 1fr;
    padding: 0 16px 16px;
  }
  
  .card {
    padding: 20px;
    gap: 16px;
  }
  
  .card-icon {
    width: 48px;
    height: 48px;
  }
  
  .card-value {
    font-size: 24px;
  }
  
  .knowledge-table {
    margin: 0 16px 16px;
    border-radius: 8px;
  }
  
  .knowledge-table th,
  .knowledge-table td {
    padding: 12px 16px;
    font-size: 13px;
  }
  
  .knowledge-table .actions-cell {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .knowledge-table .action-button {
    padding: 6px 12px;
    font-size: 12px;
    min-width: 70px;
  }
  
  .modal-content {
    width: 95%;
    border-radius: 8px;
  }
  
  .delete-modal {
    width: 90%;
  }
  
  .toast-message {
    min-width: 280px;
    max-width: 90%;
    padding: 14px 20px;
  }
}

@media (max-width: 480px) {
  .main-header {
    padding: 0 16px;
  }
  
  .header-right {
    gap: 8px;
  }
  
  .back-button span {
    display: none;
  }
  
  .back-button {
    padding: 8px 12px;
  }
  
  .user-info .username {
    display: none;
  }
  
  .page-header .header-right {
    flex-direction: column;
    align-items: stretch;
  }
  
  .page-header .search-box {
    width: 100%;
    margin-bottom: 8px;
  }
  
  .page-header .search-box input {
    width: 100%;
  }
  
  .add-button,
  .refresh-button {
    width: 100%;
    justify-content: center;
  }
  
  .card {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }
  
  .card-icon {
    width: 40px;
    height: 40px;
  }
  
  .card-value {
    font-size: 20px;
  }
  
  .knowledge-table th,
  .knowledge-table td {
    padding: 10px 12px;
    font-size: 12px;
  }
  
  .knowledge-table .collection-cell {
    padding: 4px 12px;
    font-size: 11px;
  }
  
  .pagination {
    flex-direction: column;
    gap: 12px;
    align-items: center;
  }
  
  .pagination .page-controls {
    width: 100%;
    justify-content: center;
  }
  
  .modal-header {
    padding: 20px 24px;
  }
  
  .modal-body {
    padding: 24px;
  }
  
  .modal-footer {
    padding: 16px 24px;
  }
  
  .toast-message {
    min-width: 260px;
    padding: 12px 16px;
  }
  
  .toast-content span {
    font-size: 13px;
  }
}
</style>
