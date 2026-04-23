<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'
import BaseCard from './common/BaseCard.vue'
import BaseTable from './common/BaseTable.vue'
import BaseButton from './common/BaseButton.vue'
import BaseForm from './common/BaseForm.vue'
import FormField from './common/FormField.vue'

const emit = defineEmits(['back'])

const loading = ref(false)
const searchQuery = ref('')

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const sampleList = ref([])

const showCreateModal = ref(false)
const showEditModal = ref(false)
const showDeleteConfirm = ref(false)

const editingItem = ref({})
const itemToDelete = ref(null)

const createForm = reactive({
  title: '',
  description: '',
  question: ''
})

const editForm = reactive({
  title: '',
  description: '',
  question: ''
})

const showMessage = ref(false)
const messageText = ref('')
const messageType = ref('success')

const totalPages = computed(() => {
  return Math.ceil(pagination.total / pagination.size) || 1
})

const columns = [
  { key: 'title', title: '标题' },
  { key: 'description', title: '描述' },
  { key: 'question', title: '示例问题' },
  { key: 'createTime', title: '创建时间' },
  { key: 'updateTime', title: '更新时间' },
  { key: 'action', title: '操作', width: '160px' }
]

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

const loadSampleList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/example/sample-questions', {
      params: {
        current: pagination.current,
        size: pagination.size,
        keyword: searchQuery.value.trim()
      }
    })
    
    if (response.data.code === '0') {
      const pageData = response.data.data
      sampleList.value = pageData.records.map(item => ({
        id: item.id,
        title: item.title || '-',
        description: item.description || '-',
        question: item.question,
        createTime: formatDate(item.createTime),
        updateTime: formatDate(item.updateTime)
      }))
      pagination.total = pageData.total || 0
    } else {
      showToast('获取示例问题列表失败', 'error')
    }
  } catch (error) {
    console.error('获取示例问题列表失败:', error)
    showToast('获取示例问题列表失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadSampleList()
}

const handleRefresh = () => {
  searchQuery.value = ''
  pagination.current = 1
  loadSampleList()
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  pagination.current = page
  loadSampleList()
}

const handleCreate = async () => {
  if (!createForm.question.trim()) {
    showToast('示例问题内容不能为空', 'warning')
    return
  }
  
  try {
    const response = await axios.post('/example/sample-questions', {
      title: createForm.title.trim(),
      description: createForm.description.trim(),
      question: createForm.question.trim()
    })
    
    if (response.data.code === '0') {
      showToast('创建成功', 'success')
      showCreateModal.value = false
      resetCreateForm()
      loadSampleList()
    } else {
      showToast(response.data.message || '创建失败', 'error')
    }
  } catch (error) {
    console.error('创建示例问题失败:', error)
    const errorMessage = error.response?.data?.message || error.message || '创建失败'
    showToast(errorMessage, 'error')
  }
}

const handleEdit = (item) => {
  editingItem.value = { ...item }
  editForm.title = item.title === '-' ? '' : item.title
  editForm.description = item.description === '-' ? '' : item.description
  editForm.question = item.question
  showEditModal.value = true
}

const handleUpdate = async () => {
  if (!editForm.question.trim()) {
    showToast('示例问题内容不能为空', 'warning')
    return
  }
  
  try {
    const response = await axios.put(`/example/sample-questions/${editingItem.value.id}`, {
      title: editForm.title.trim(),
      description: editForm.description.trim(),
      question: editForm.question.trim()
    })
    
    if (response.data.code === '0') {
      showToast('更新成功', 'success')
      showEditModal.value = false
      resetEditForm()
      loadSampleList()
    } else {
      showToast(response.data.message || '更新失败', 'error')
    }
  } catch (error) {
    console.error('更新示例问题失败:', error)
    const errorMessage = error.response?.data?.message || error.message || '更新失败'
    showToast(errorMessage, 'error')
  }
}

const handleDeleteClick = (item) => {
  itemToDelete.value = item
  showDeleteConfirm.value = true
}

const handleDeleteConfirm = async () => {
  if (!itemToDelete.value) return
  
  try {
    const response = await axios.delete(`/example/sample-questions/${itemToDelete.value.id}`)
    
    if (response.data.code === '0') {
      showToast('删除成功', 'success')
      showDeleteConfirm.value = false
      itemToDelete.value = null
      loadSampleList()
    } else {
      showToast(response.data.message || '删除失败', 'error')
    }
  } catch (error) {
    console.error('删除示例问题失败:', error)
    const errorMessage = error.response?.data?.message || error.message || '删除失败'
    showToast(errorMessage, 'error')
  }
}

const handleDeleteCancel = () => {
  showDeleteConfirm.value = false
  itemToDelete.value = null
}

const resetCreateForm = () => {
  createForm.title = ''
  createForm.description = ''
  createForm.question = ''
}

const resetEditForm = () => {
  editForm.title = ''
  editForm.description = ''
  editForm.question = ''
  editingItem.value = {}
}

const showToast = (text, type = 'success', duration = 3000) => {
  messageText.value = text
  messageType.value = type
  showMessage.value = true
  
  setTimeout(() => {
    showMessage.value = false
  }, duration)
}

onMounted(() => {
  loadSampleList()
})
</script>

<template>
  <div class="sample-management">
    <div class="page-header">
      <h2 class="page-title">示例管理</h2>
      <div class="header-actions">
        <BaseButton type="secondary" @click="emit('back')">
          返回
        </BaseButton>
      </div>
    </div>

    <div class="sample-content">
      <div class="toolbar">
        <div class="search-box">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索标题、描述或问题内容"
            @keyup.enter="handleSearch"
            class="search-input"
          />
          <BaseButton type="primary" @click="handleSearch" size="small">
            搜索
          </BaseButton>
        </div>
        <div class="action-buttons">
          <BaseButton type="secondary" @click="handleRefresh">
            刷新
          </BaseButton>
          <BaseButton type="primary" @click="showCreateModal = true">
            新增示例
          </BaseButton>
        </div>
      </div>

      <BaseTable
        :columns="columns"
        :data="sampleList"
        :loading="loading"
      >
        <template #action="{ row }">
          <div class="action-cell">
            <button class="btn-edit" @click="handleEdit(row)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
              编辑
            </button>
            <button class="btn-delete" @click="handleDeleteClick(row)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
              删除
            </button>
          </div>
        </template>
      </BaseTable>

      <div v-if="sampleList.length === 0 && !loading" class="empty-state">
        <svg class="empty-icon" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
        <p>暂无示例问题</p>
      </div>

      <div v-if="pagination.total > 0" class="pagination">
        <button
          class="page-btn"
          :disabled="pagination.current === 1"
          @click="changePage(pagination.current - 1)"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ pagination.current }} / {{ totalPages }} 页
        </span>
        <button
          class="page-btn"
          :disabled="pagination.current === totalPages"
          @click="changePage(pagination.current + 1)"
        >
          下一页
        </button>
      </div>
    </div>

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

    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">新建示例问题</h3>
          <BaseButton type="secondary" class="close-button" @click="showCreateModal = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </BaseButton>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">标题 <span class="optional">(可选)</span></label>
            <input
              v-model="createForm.title"
              type="text"
              placeholder="请输入标题"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label class="form-label">描述 <span class="optional">(可选)</span></label>
            <textarea
              v-model="createForm.description"
              placeholder="请输入描述"
              class="form-textarea"
              rows="3"
            ></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">示例问题 <span class="required">*</span></label>
            <textarea
              v-model="createForm.question"
              placeholder="请输入示例问题内容"
              class="form-textarea"
              rows="4"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <BaseButton type="secondary" @click="showCreateModal = false">
            取消
          </BaseButton>
          <BaseButton type="primary" @click="handleCreate">
            确定
          </BaseButton>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">编辑示例问题</h3>
          <BaseButton type="secondary" class="close-button" @click="showEditModal = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </BaseButton>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">标题 <span class="optional">(可选)</span></label>
            <input
              v-model="editForm.title"
              type="text"
              placeholder="请输入标题"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label class="form-label">描述 <span class="optional">(可选)</span></label>
            <textarea
              v-model="editForm.description"
              placeholder="请输入描述"
              class="form-textarea"
              rows="3"
            ></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">示例问题 <span class="required">*</span></label>
            <textarea
              v-model="editForm.question"
              placeholder="请输入示例问题内容"
              class="form-textarea"
              rows="4"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <BaseButton type="secondary" @click="showEditModal = false">
            取消
          </BaseButton>
          <BaseButton type="primary" @click="handleUpdate">
            确定
          </BaseButton>
        </div>
      </div>
    </div>

    <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="handleDeleteCancel">
      <div class="modal modal-small">
        <div class="modal-header">
          <h3 class="modal-title">确认删除</h3>
        </div>
        <div class="modal-body">
          <p class="confirm-text">
            确定要删除示例问题「{{ itemToDelete?.question }}」吗？此操作不可恢复。
          </p>
        </div>
        <div class="modal-footer">
          <BaseButton type="secondary" @click="handleDeleteCancel">
            取消
          </BaseButton>
          <BaseButton type="danger" @click="handleDeleteConfirm">
            删除
          </BaseButton>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sample-management {
  padding: 24px;
  min-height: calc(100vh - 80px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.sample-content {
  margin-top: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.search-box {
  display: flex;
  gap: 8px;
  flex: 1;
  max-width: 500px;
}

.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.btn-edit {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #ffffff;
  color: #666;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-edit:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.btn-delete {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #ffffff;
  color: #ff4d4f;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-delete:hover {
  border-color: #ff4d4f;
  background: #fff1f0;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.empty-icon {
  margin-bottom: 16px;
  color: #d9d9d9;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.page-btn {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #ffffff;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  border-color: #1890ff;
  color: #1890ff;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #666;
}

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
    transform: translate(-50%, -20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

.modal-overlay {
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

.modal {
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-small {
  max-width: 400px;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.close-button {
  padding: 4px;
  min-width: auto;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.optional {
  color: #999;
  font-weight: 400;
  font-size: 12px;
}

.required {
  color: #ff4d4f;
  font-weight: 400;
  font-size: 12px;
}

.form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.form-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
  box-sizing: border-box;
}

.form-textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.confirm-text {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

@media (max-width: 768px) {
  .sample-management {
    padding: 16px;
  }

  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .search-box {
    max-width: 100%;
  }

  .action-buttons {
    justify-content: flex-end;
  }

  .page-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .modal {
    width: 95%;
    max-height: 90vh;
  }
}
</style>
