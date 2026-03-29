<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import AvatarSelector from './AvatarSelector.vue'

// 日期格式化函数
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  return date.toISOString().split('T')[0]
}

const router = useRouter()

// 用户列表数据
const userList = ref([])
const loading = ref(false)
const total = ref(0)

// 分页参数
const pagination = reactive({
  current: 1,
  size: 10
})

// 搜索参数
const searchForm = reactive({
  username: ''
})

// 对话框控制
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)

// 表单数据
const formRef = ref(null)
const formData = reactive({
  id: '',
  username: '',
  password: '',
  role: 'user',
  avatar: '/avatars/avatar-1.svg',
  email: ''
})

// 表单验证规则
const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20个字符', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 头像选择器控制
const avatarSelectorVisible = ref(false)

// 消息提示
const message = ref({
  visible: false,
  type: 'success', // success, error, warning
  content: ''
})

// 显示消息提示
const showMessage = (content, type = 'success') => {
  message.value = {
    visible: true,
    type,
    content
  }
  setTimeout(() => {
    message.value.visible = false
  }, 3000)
}

// 预设头像列表
const presetAvatars = [
  '/avatars/avatar-1.svg',
  '/avatars/avatar-2.svg',
  '/avatars/avatar-3.svg',
  '/avatars/avatar-4.svg',
  '/avatars/avatar-5.svg',
  '/avatars/avatar-6.svg',
  '/avatars/avatar-7.svg',
  '/avatars/avatar-8.svg'
]



// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const response = await axios.post('/api/management/pageQuery', {
      current: pagination.current,
      size: pagination.size,
      username: searchForm.username
    })
    
    if (response.data.code === '0') {
      userList.value = response.data.data.records || []
      total.value = response.data.data.total || 0
    } else {
      console.error('获取用户列表失败:', response.data.message)
      showMessage(response.data.message || '获取用户列表失败', 'error')
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    if (error.response) {
      // 服务器返回错误
      const errorData = error.response.data
      showMessage(errorData.message || errorData.msg || '获取用户列表失败', 'error')
    } else if (error.request) {
      // 请求已发出但没有收到响应
      showMessage('网络错误，请检查后端服务是否正常', 'error')
    } else {
      // 其他错误
      showMessage(error.message || '获取用户列表失败，请稍后重试', 'error')
    }
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchUserList()
}

// 重置搜索
const handleReset = () => {
  searchForm.username = ''
  pagination.current = 1
  fetchUserList()
}

// 分页变化
const handlePageChange = (page) => {
  pagination.current = page
  fetchUserList()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  fetchUserList()
}

// 打开新增对话框
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.assign(formData, {
    id: '',
    username: '',
    password: '',
    role: 'user',
    avatar: '/avatars/avatar-1.svg',
    email: ''
  })
  dialogVisible.value = true
}

// 打开编辑对话框
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(formData, {
    id: row.id,
    username: row.username,
    password: '',
    role: row.role,
    avatar: row.avatar || '/avatars/avatar-1.svg',
    email: row.email || ''
  })
  dialogVisible.value = true
}

// 表单验证错误
const formErrors = reactive({
  username: '',
  password: '',
  role: '',
  email: ''
})

// 验证表单
const validateForm = () => {
  let isValid = true
  
  // 重置错误信息
  formErrors.username = ''
  formErrors.password = ''
  formErrors.role = ''
  formErrors.email = ''
  
  // 验证用户名
  if (!formData.username.trim()) {
    formErrors.username = '用户名不能为空'
    isValid = false
  }
  
  // 验证密码（新增用户时）
  if (!isEdit.value && !formData.password.trim()) {
    formErrors.password = '密码不能为空'
    isValid = false
  }
  
  // 验证角色
  if (!formData.role) {
    formErrors.role = '请选择角色'
    isValid = false
  }
  
  // 验证邮箱（如果填写）
  if (formData.email.trim()) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(formData.email.trim())) {
      formErrors.email = '邮箱格式不正确'
      isValid = false
    }
  }
  
  return isValid
}

// 提交表单
const handleSubmit = async () => {
  // 验证表单
  if (!validateForm()) {
    return
  }

  try {
    let response
    if (isEdit.value) {
      response = await axios.put(`/api/management/users/${formData.id}`, formData)
    } else {
      response = await axios.post('/api/management/create', formData)
    }
    
    if (response.data.code === '0') {
      dialogVisible.value = false
      fetchUserList()
      showMessage('保存成功')
    } else {
      showMessage(response.data.message || '保存失败', 'error')
    }
  } catch (error) {
    console.error('保存用户失败:', error)
    if (error.response) {
      // 服务器返回错误
      const errorData = error.response.data
      showMessage(errorData.message || errorData.msg || '保存失败', 'error')
    } else if (error.request) {
      // 请求已发出但没有收到响应
      showMessage('网络错误，请检查后端服务是否正常', 'error')
    } else {
      // 其他错误
      showMessage(error.message || '保存失败，请稍后重试', 'error')
    }
  }
}

// 删除用户
const handleDelete = async (row) => {
  if (!confirm(`确定要删除用户 "${row.username}" 吗？`)) return
  
  try {
    const response = await axios.delete(`/api/management/users/${row.id}`)
    
    if (response.data.code === '0') {
      fetchUserList()
      showMessage('删除成功')
    } else {
      showMessage(response.data.message || '删除失败', 'error')
    }
  } catch (error) {
    console.error('删除用户失败:', error)
    if (error.response) {
      // 服务器返回错误
      const errorData = error.response.data
      showMessage(errorData.message || errorData.msg || '删除失败', 'error')
    } else if (error.request) {
      // 请求已发出但没有收到响应
      showMessage('网络错误，请检查后端服务是否正常', 'error')
    } else {
      // 其他错误
      showMessage(error.message || '删除失败，请稍后重试', 'error')
    }
  }
}

// 打开头像选择器
const openAvatarSelector = () => {
  avatarSelectorVisible.value = true
}

// 选择头像
const handleAvatarSelect = (avatar) => {
  formData.avatar = avatar
  avatarSelectorVisible.value = false
}



// 获取角色标签样式
const getRoleClass = (role) => {
  return role === 'admin' ? 'role-admin' : 'role-user'
}

// 获取角色显示文本
const getRoleText = (role) => {
  return role === 'admin' ? '管理员' : '普通用户'
}

onMounted(() => {
  fetchUserList()
})
</script>

<template>
  <div class="user-management">
    <!-- 搜索区域 -->
    <div class="search-area">
      <div class="search-form">
        <div class="form-item">
          <input 
            v-model="searchForm.username" 
            type="text" 
            placeholder="请输入用户名"
            @keyup.enter="handleSearch"
          />
        </div>
        <button class="btn btn-primary" @click="handleSearch">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35"/>
          </svg>
          搜索
        </button>
        <button class="btn btn-default" @click="handleReset">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
            <path d="M3 3v5h5"/>
          </svg>
          重置
        </button>
      </div>
      <button class="btn btn-primary" @click="handleAdd">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        新增用户
      </button>
    </div>

    <!-- 表格区域 -->
    <div class="table-area">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width: 60px;">头像</th>
            <th>用户名</th>
            <th>角色</th>
            <th>创建时间</th>
            <th>更新时间</th>
            <th style="width: 150px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6" class="loading-cell">
              <div class="loading-spinner"></div>
              加载中...
            </td>
          </tr>
          <tr v-else-if="userList.length === 0">
            <td colspan="6" class="empty-cell">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <line x1="9" y1="9" x2="15" y2="9"/>
                <line x1="9" y1="15" x2="15" y2="15"/>
              </svg>
              <p>暂无数据</p>
            </td>
          </tr>
          <tr v-for="row in userList" :key="row.id">
            <td>
              <div class="avatar-cell">
                <img :src="row.avatar || '/avatars/avatar-1.svg'" alt="avatar" />
              </div>
            </td>
            <td>{{ row.username }}</td>
            <td>
              <span class="role-tag" :class="getRoleClass(row.role)">
                {{ getRoleText(row.role) }}
              </span>
            </td>
            <td>{{ formatDate(row.createTime) }}</td>
            <td>{{ formatDate(row.updateTime) }}</td>
            <td>
              <div class="action-btns">
                <button class="btn-icon btn-edit" @click="handleEdit(row)" title="编辑" :disabled="row.username === 'admin'">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button class="btn-icon btn-delete" @click="handleDelete(row)" title="删除" :disabled="row.username === 'admin'">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页区域 -->
    <div class="pagination-area" v-if="total > 0">
      <div class="pagination-info">
        共 {{ total }} 条记录
      </div>
      <div class="pagination">
        <button 
          class="page-btn" 
          :disabled="pagination.current === 1"
          @click="handlePageChange(pagination.current - 1)"
        >
          上一页
        </button>
        <span class="page-info">{{ pagination.current }} / {{ Math.ceil(total / pagination.size) }}</span>
        <button 
          class="page-btn" 
          :disabled="pagination.current >= Math.ceil(total / pagination.size)"
          @click="handlePageChange(pagination.current + 1)"
        >
          下一页
        </button>
      </div>
      <div class="page-size">
        <select v-model="pagination.size" @change="handleSizeChange(pagination.size)">
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <div v-if="dialogVisible" class="dialog-overlay" @click.self="dialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>{{ dialogTitle }}</h3>
          <button class="btn-close" @click="dialogVisible = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="dialog-body">
          <form ref="formRef" class="form">
            <!-- 头像选择 -->
            <div class="form-item">
              <label>头像</label>
              <div class="avatar-selector-trigger" @click="openAvatarSelector">
                <img :src="formData.avatar" alt="avatar" />
                <div class="avatar-overlay">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class="form-item">
              <label>用户名 <span class="required">*</span></label>
              <input 
                v-model="formData.username" 
                type="text" 
                placeholder="请输入用户名"
                :disabled="isEdit"
                :class="{ 'error': formErrors.username }"
              />
              <div v-if="formErrors.username" class="error-text">{{ formErrors.username }}</div>
            </div>

            <div class="form-item" v-if="!isEdit">
              <label>密码 <span class="required">*</span></label>
              <input 
                v-model="formData.password" 
                type="password" 
                placeholder="请输入密码"
                :class="{ 'error': formErrors.password }"
              />
              <div v-if="formErrors.password" class="error-text">{{ formErrors.password }}</div>
            </div>

            <div class="form-item">
              <label>角色 <span class="required">*</span></label>
              <select v-model="formData.role" :class="{ 'error': formErrors.role }">
                <option value="user">普通用户</option>
                <option value="admin">管理员</option>
              </select>
              <div v-if="formErrors.role" class="error-text">{{ formErrors.role }}</div>
            </div>

            <div class="form-item">
              <label>邮箱</label>
              <input 
                v-model="formData.email" 
                type="email" 
                placeholder="请输入邮箱（选填）"
                :class="{ 'error': formErrors.email }"
              />
              <div v-if="formErrors.email" class="error-text">{{ formErrors.email }}</div>
            </div>
          </form>
        </div>
        <div class="dialog-footer">
          <button class="btn btn-default" @click="dialogVisible = false">取消</button>
          <button class="btn btn-primary" @click="handleSubmit">确定</button>
        </div>
      </div>
    </div>

    <!-- 头像选择器 -->
    <AvatarSelector
      v-model:visible="avatarSelectorVisible"
      :current-avatar="formData.avatar"
      :preset-avatars="presetAvatars"
      @select="handleAvatarSelect"
    />

    <!-- 消息提示 -->
    <div v-if="message.visible" class="message" :class="`message-${message.type}`">
      <div class="message-content">
        <svg v-if="message.type === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        <svg v-else-if="message.type === 'error'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="15" y1="9" x2="9" y2="15"/>
          <line x1="9" y1="9" x2="15" y2="15"/>
        </svg>
        <svg v-else-if="message.type === 'warning'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
          <line x1="12" y1="9" x2="12" y2="13"/>
          <line x1="12" y1="17" x2="12.01" y2="17"/>
        </svg>
        <span>{{ message.content }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-management {
  padding: 20px;
}

/* 搜索区域 */
.search-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.search-form {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-form .form-item input {
  width: 240px;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.search-form .form-item input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

/* 按钮样式 */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.btn-default {
  background: white;
  border: 1px solid #d9d9d9;
  color: #666;
}

.btn-default:hover {
  border-color: #667eea;
  color: #667eea;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-icon:disabled:hover {
  background: transparent;
  color: inherit;
}

.btn-edit {
  background: #e6f7ff;
  color: #1890ff;
}

.btn-edit:hover {
  background: #1890ff;
  color: white;
}

.btn-delete {
  background: #fff1f0;
  color: #ff4d4f;
}

.btn-delete:hover {
  background: #ff4d4f;
  color: white;
}

/* 表格区域 */
.table-area {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.data-table th {
  background: #fafafa;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.data-table td {
  font-size: 14px;
  color: #666;
}

.data-table tbody tr:hover {
  background: #f5f7fa;
}

.avatar-cell {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-cell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.role-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.role-admin {
  background: #fff2e8;
  color: #fa8c16;
}

.role-user {
  background: #f6ffed;
  color: #52c41a;
}

.role-visitor {
  background: #f0f5ff;
  color: #1890ff;
}

.role-editor {
  background: #f9f0ff;
  color: #722ed1;
}

.role-moderator {
  background: #fff7e6;
  color: #fa541c;
}

.action-btns {
  display: flex;
  gap: 8px;
}

/* 加载和空状态 */
.loading-cell,
.empty-cell {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-cell svg {
  color: #ddd;
  margin-bottom: 16px;
}

.loading-spinner {
  display: inline-block;
  width: 24px;
  height: 24px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 8px;
  vertical-align: middle;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 分页区域 */
.pagination-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 16px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.pagination-info {
  color: #666;
  font-size: 14px;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-btn {
  padding: 6px 16px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #666;
  font-size: 14px;
}

.page-size select {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

/* 对话框 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: white;
  border-radius: 12px;
  width: 480px;
  max-width: 90vw;
  max-height: 90vh;
  overflow: hidden;
  animation: dialogSlideIn 0.3s ease;
}

@keyframes dialogSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.dialog-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #f5f5f5;
  color: #666;
}

.dialog-body {
  padding: 24px;
}

.form .form-item {
  margin-bottom: 20px;
}

.form .form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form .form-item .required {
  color: #ff4d4f;
}

.form .form-item input,
.form .form-item select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.form .form-item input:focus,
.form .form-item select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.form .form-item input.error,
.form .form-item select.error {
  border-color: #ff4d4f;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.2);
}

.error-text {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 6px;
  line-height: 1.2;
}

/* 头像选择触发器 */
.avatar-selector-trigger {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 2px solid #e8e8e8;
  transition: all 0.2s;
}

.avatar-selector-trigger:hover {
  border-color: #667eea;
}

.avatar-selector-trigger img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-selector-trigger:hover .avatar-overlay {
  opacity: 1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}

/* 消息提示 */
.message {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1200;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: messageSlideIn 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.message-success {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.message-error {
  background: #fff1f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.message-warning {
  background: #fffbe6;
  color: #fa8c16;
  border: 1px solid #ffe58f;
}

.message-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

.message-content svg {
  flex-shrink: 0;
}
</style>
