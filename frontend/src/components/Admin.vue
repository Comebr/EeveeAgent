<script setup>
import { ref, onMounted, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import UserManagement from './user/UserManagement.vue'
import IntentManagement from './intent/IntentManagement.vue'
import BaseCard from './common/BaseCard.vue'
import BaseTable from './common/BaseTable.vue'
import BaseButton from './common/BaseButton.vue'
import BaseForm from './common/BaseForm.vue'
import FormField from './common/FormField.vue'

const router = useRouter()
const userInfo = ref(null)
const activeMenu = ref('knowledge') // 默认显示知识库管理
const searchQuery = ref('')
const sidebarCollapsed = ref(false)
const showUserDropdown = ref(false)

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

// 文档管理
const activeTab = ref('knowledge') // knowledge 或 documents 或 chunks
const selectedKnowledgeBase = ref(null)
const documents = ref([])
const showUploadModal = ref(false)
const showDocumentDeleteConfirm = ref(false)
const documentToDelete = ref('')
const documentToDeleteId = ref('')
const selectedFiles = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
const statusFilter = ref('all')
const documentPagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 知识块管理
const chunks = ref([])
const selectedDocument = ref(null)
const chunkPagination = reactive({
  current: 1,
  size: 10,
  total: 0
})
const chunkSearchQuery = ref('')
const chunkStatusFilter = ref('all')
const selectAll = ref(false)
const selectedChunks = ref([])

// 分块弹窗
const showChunkModal = ref(false)
const chunkForm = reactive({
  docId: '',
  chunkStrategy: 'recursive',
  maxChunkSize: 512,
  maxOverlapSize: 0
})
const chunking = ref(false)

// 编辑知识块弹窗
const showEditChunkModal = ref(false)
const editingChunk = ref({})
const editForm = reactive({
  id: '',
  content: ''
})

// 新建知识块弹窗
const showCreateChunkModal = ref(false)
const createChunkForm = reactive({
  chunkText: ''
})

// 字符数和Token数
const charCount = ref(0)
const tokenCount = ref(0)

// 消息提示
const showMessage = ref(false)
const messageText = ref('')
const messageType = ref('success')

// 创建表单
const createForm = reactive({
  kbName: '',
  embeddingModel: 'text-embedding-v4',
  collection: ''
})

// 计算属性
const totalPages = computed(() => {
  return Math.ceil(pagination.total / pagination.size) || 1
})

// 知识库表格列配置
const knowledgeColumns = [
  { key: 'name', title: '名称' },
  { key: 'embeddingModel', title: 'Embedding模型' },
  { key: 'collection', title: 'Collection' },
  { key: 'owner', title: '负责人' },
  { key: 'createdAt', title: '创建时间' },
  { key: 'updatedAt', title: '修改时间' },
  { key: 'action', title: '操作', width: '160px' }
]

// 文档表格列配置
const documentColumns = [
  { key: 'documentName', title: '文档' },
  { key: 'source', title: '来源' },
  { key: 'processMode', title: '处理模式' },
  { key: 'status', title: '状态' },
  { key: 'enabled', title: '启用' },
  { key: 'chunkCount', title: '分块数' },
  { key: 'fileType', title: '类型' },
  { key: 'fileSize', title: '大小' },
  { key: 'updateTime', title: '更新时间' },
  { key: 'action', title: '操作', width: '160px' }
]

// 知识块表格列配置
const chunkColumns = [
  { key: 'index', title: '序号', width: '60px' },
  { key: 'content', title: '内容' },
  { key: 'status', title: '状态' },
  { key: 'charCount', title: '字符数' },
  { key: 'tokenCount', title: 'Token数' },
  { key: 'updateTime', title: '更新时间' },
  { key: 'action', title: '操作', width: '160px' }
]

// 切换到文档管理标签页
const navigateToDocuments = (kbId, kbName) => {
  selectedKnowledgeBase.value = {
    id: kbId,
    name: kbName
  }
  activeTab.value = 'documents'
  loadDocuments(kbId)
}

// 加载文档列表
const loadDocuments = async (kbId) => {
  try {
    const params = {
      kbId: kbId,
      current: documentPagination.current,
      size: documentPagination.size
    }
    // 添加文档名称搜索参数
    if (searchQuery.value && searchQuery.value.trim()) {
      params.documentName = searchQuery.value.trim()
    }
    
    // 添加状态筛选参数
    if (statusFilter.value === 'enabled') {
      params.enabled = '1'
    } else if (statusFilter.value === 'disabled') {
      params.enabled = '0'
    }
    // 全部状态时不传递enabled参数
    
    const response = await axios.get('/kb/doc/pageQuery', { params })
    
    if (response.data.code === '0') {
      documents.value = response.data.data.records || []
      documentPagination.total = response.data.data.total || 0
    } else {
      showToast('获取文档列表失败', 'error')
    }
  } catch (error) {
    console.error('获取文档列表失败:', error)
    showToast('获取文档列表失败，请稍后重试', 'error')
  }
}

// 返回知识库管理
const goBackToKnowledge = () => {
  activeTab.value = 'knowledge'
  selectedKnowledgeBase.value = null
}

// 处理文档搜索
const handleDocumentSearch = () => {
  // 实现搜索功能
  loadDocuments(selectedKnowledgeBase.value.id)
}

// 刷新文档列表
const refreshDocuments = () => {
  loadDocuments(selectedKnowledgeBase.value.id)
}

// 重置筛选条件
const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = 'all'
  if (selectedKnowledgeBase.value) {
    loadDocuments(selectedKnowledgeBase.value.id)
  }
}

// 监听状态筛选变化
watch(statusFilter, () => {
  if (selectedKnowledgeBase.value) {
    loadDocuments(selectedKnowledgeBase.value.id)
  }
})

// 导航到知识块管理
const navigateToChunks = (doc) => {
  selectedDocument.value = doc
  activeTab.value = 'chunks'
  loadChunks(doc.id)
}

// 加载知识块列表
const loadChunks = async (docId) => {
  try {
    const params = {
      docId: docId,
      current: chunkPagination.current,
      size: chunkPagination.size
    }
    
    const response = await axios.get('/kb/doc/chunk/pageQuery', { params })
    
    if (response.data.code === '0') {
      chunks.value = response.data.data.records || []
      chunkPagination.total = response.data.data.total || 0
    } else {
      showToast('获取知识块列表失败', 'error')
    }
  } catch (error) {
    console.error('加载知识块失败:', error)
    showToast('获取知识块列表失败，请稍后重试', 'error')
  }
}

// 知识块分页变更
const changeChunkPage = (page) => {
  chunkPagination.current = page
  if (selectedDocument.value) {
    loadChunks(selectedDocument.value.id)
  }
}

// 知识块每页显示条数变更
const handleChunkPageSizeChange = (size) => {
  chunkPagination.size = size
  chunkPagination.current = 1
  if (selectedDocument.value) {
    loadChunks(selectedDocument.value.id)
  }
}

// 全选功能
const handleSelectAll = () => {
  selectAll.value = !selectAll.value
  selectedChunks.value = selectAll.value ? chunks.value.map(chunk => chunk.id) : []
}

// 单个选择
const handleSelectChunk = (id) => {
  const index = selectedChunks.value.indexOf(id)
  if (index > -1) {
    selectedChunks.value.splice(index, 1)
  } else {
    selectedChunks.value.push(id)
  }
  selectAll.value = selectedChunks.value.length === chunks.value.length
}

// 编辑知识块
const editChunk = (chunk) => {
  editingChunk.value = { ...chunk }
  editForm.id = chunk.id
  editForm.content = chunk.chunkText || ''
  showEditChunkModal.value = true
}

// 保存编辑内容
const saveChunkEdit = async () => {
  try {
    const response = await axios.put('/kb/doc/chunk/mkdirText', {
      chunkId: editForm.id,
      chunkText: editForm.content
    })

    if (response.data.code === '0') {
      showEditChunkModal.value = false
      showToast('编辑成功', 'success')
      // 重新加载知识块列表
      if (selectedDocument.value) {
        loadChunks(selectedDocument.value.id)
      }
    } else {
      showToast(response.data.message || '编辑失败', 'error')
    }
  } catch (error) {
    console.error('编辑知识块失败:', error)
    showToast('编辑失败，请稍后重试', 'error')
  }
}

// 取消编辑
const cancelChunkEdit = () => {
  showEditChunkModal.value = false
  editingChunk.value = {}
  editForm.id = ''
  editForm.content = ''
}

// 打开新建知识块弹窗
const openCreateChunkModal = () => {
  createChunkForm.chunkText = ''
  charCount.value = 0
  tokenCount.value = 0
  showCreateChunkModal.value = true
}

// 处理新建知识块弹窗关闭
const handleCreateChunkModalClose = () => {
  showCreateChunkModal.value = false
  createChunkForm.chunkText = ''
  charCount.value = 0
  tokenCount.value = 0
}

// 更新字符数和Token数
const updateCharCount = () => {
  const text = createChunkForm.chunkText
  charCount.value = text.length
  tokenCount.value = estimateTokenCount(text)
}

// 估算Token数
const estimateTokenCount = (text) => {
  if (!text) return 0
  
  let cjkCharCount = 0
  let otherCharCount = 0
  
  for (let i = 0; i < text.length; i++) {
    const char = text[i]
    if (isCJKCharacter(char)) {
      cjkCharCount++
    } else {
      otherCharCount++
    }
  }
  
  // 其他字符每4个算1个token
  const otherTokens = Math.ceil(otherCharCount / 4)
  return cjkCharCount + otherTokens
}

// 判断是否为中日韩字符
const isCJKCharacter = (char) => {
  const code = char.charCodeAt(0)
  return (
    (code >= 0x4E00 && code <= 0x9FFF) || // 中日韩统一表意文字
    (code >= 0x3400 && code <= 0x4DBF) || // 扩展区 A
    (code >= 0x3040 && code <= 0x30FF) || // 日文假名
    (code >= 0xAC00 && code <= 0xD7AF)    // 韩文谚文
  )
}

// 新建知识块
const createChunk = async () => {
  if (!createChunkForm.chunkText.trim()) {
    showToast('知识块内容不能为空', 'error')
    return
  }
  
  // 验证Token数
  const estimatedTokens = estimateTokenCount(createChunkForm.chunkText)
  if (estimatedTokens > 512) {
    showToast(`Token数超过限制，当前${estimatedTokens}，限制512`, 'error')
    return
  }
  
  try {
    const response = await axios.put('/kb/doc/chunk/singleChunk', {
      kbId: selectedKnowledgeBase.value.id.toString(),
      docId: selectedDocument.value.id,
      chunkText: createChunkForm.chunkText
    })
    
    if (response.data.code === '0') {
      showCreateChunkModal.value = false
      showToast('创建成功', 'success')
      loadChunks(selectedDocument.value.id)
    } else {
      showToast(response.data.message || '创建失败', 'error')
    }
  } catch (error) {
    console.error('创建知识块失败:', error)
    const errorMessage = error.response?.data?.message || error.message || '创建失败'
    showToast(errorMessage, 'error')
  } finally {
    createChunkForm.chunkText = ''
  }
}

// 切换知识块启用状态
const toggleChunkEnabled = (chunk) => {
  // 实现启用/禁用功能
  console.log('切换知识块状态:', chunk)
  // 这里应该调用后端API来切换状态
  chunk.enabled = chunk.enabled === 1 ? 0 : 1
}

// 知识块删除确认
const showChunkDeleteConfirm = ref(false)
const chunkToDeleteId = ref('')
const chunkToDeleteContent = ref('')

// 确认删除知识块
const confirmDeleteChunk = (id, content) => {
  chunkToDeleteId.value = id
  chunkToDeleteContent.value = content
  showChunkDeleteConfirm.value = true
}

// 处理知识块删除确认
const handleChunkDeleteConfirm = async () => {
  try {
    const response = await axios.delete(`/kb/doc/chunk/deleteChunk/${chunkToDeleteId.value}`)
    
    if (response.data.code === '0') {
      showChunkDeleteConfirm.value = false
      showToast('删除成功', 'success')
      loadChunks(selectedDocument.value.id)
    } else {
      showToast('删除失败', 'error')
    }
  } catch (error) {
    console.error('删除知识块失败:', error)
    showToast('删除失败，请稍后重试', 'error')
  }
}

// 取消删除知识块
const handleChunkDeleteCancel = () => {
  showChunkDeleteConfirm.value = false
  chunkToDeleteId.value = ''
  chunkToDeleteContent.value = ''
}

// 防抖定时器
let toggleDebounceTimer = null

// 切换文档启用状态
const toggleDocumentEnabled = (id, enabled) => {
  // 清除之前的定时器
  if (toggleDebounceTimer) {
    clearTimeout(toggleDebounceTimer)
  }
  
  // 设置新的定时器，1s后执行
  toggleDebounceTimer = setTimeout(async () => {
    try {
      const response = await axios.post('/kb/doc/switch', null, {
        params: {
          docId: id,
          enabled: enabled
        }
      })
      
      if (response.data.code === '0') {
        const doc = documents.value.find(d => d.id === id)
        if (doc) {
          doc.enabled = enabled ? 1 : 0
        }
        showToast('操作成功', 'success')
      } else {
        showToast('操作失败', 'error')
      }
    } catch (error) {
      console.error('切换启用状态失败:', error)
      showToast('操作失败，请稍后重试', 'error')
    }
  }, 300)
}

// 编辑文档
const editDocument = (doc) => {
  // 实现编辑功能
  console.log('编辑文档:', doc)
}

// 查看文档
const viewDocument = (doc) => {
  // 实现查看功能
  console.log('查看文档:', doc)
}

// 下载文档
const downloadDocument = (doc) => {
  // 实现下载功能
  console.log('下载文档:', doc)
}

// 打开分块弹窗
const openChunkModal = (doc) => {
  chunkForm.docId = doc.id
  chunkForm.chunkStrategy = 'recursive'
  chunkForm.maxChunkSize = 512
  chunkForm.maxOverlapSize = 0
  showChunkModal.value = true
}

// 执行分块操作
const startChunking = async () => {
  chunking.value = true
  try {
    const response = await axios.post('/kb/doc/startChunk', {
      docId: chunkForm.docId,
      chunkOptions: {
        maxChunkSize: chunkForm.maxChunkSize,
        maxOverlapSize: chunkForm.maxOverlapSize
      }
    })
    
    if (response.data.code === '0') {
      showChunkModal.value = false
      showToast('分块成功', 'success')
      loadDocuments(selectedKnowledgeBase.value.id)
    } else {
      showToast('分块失败', 'error')
    }
  } catch (error) {
    console.error('分块失败:', error)
    showToast('分块失败，请稍后重试', 'error')
  } finally {
    chunking.value = false
  }
}

// 确认删除文档
const confirmDeleteDocument = (id, name) => {
  documentToDeleteId.value = id
  documentToDelete.value = name
  showDocumentDeleteConfirm.value = true
}

// 处理文档删除确认
const handleDocumentDeleteConfirm = async () => {
  try {
    const response = await axios.delete(`/kb/doc/deleteDoc/${documentToDeleteId.value}`)
    
    if (response.data.code === '0') {
      showDocumentDeleteConfirm.value = false
      showToast('删除成功', 'success')
      loadDocuments(selectedKnowledgeBase.value.id)
    } else {
      showToast('删除失败', 'error')
    }
  } catch (error) {
    console.error('删除文档失败:', error)
    showToast('删除失败，请稍后重试', 'error')
  }
}

// 取消删除文档
const handleDocumentDeleteCancel = () => {
  showDocumentDeleteConfirm.value = false
  documentToDelete.value = ''
}

// 处理文件选择
const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  if (files.length > 1) {
    showToast('当前只支持单文件上传', 'warning')
    selectedFiles.value = [files[0]] // 只取第一个文件
  } else {
    selectedFiles.value = files
  }
}

// 处理文件拖拽
const handleDrop = (event) => {
  event.preventDefault()
  const files = Array.from(event.dataTransfer.files)
  if (files.length > 1) {
    showToast('当前只支持单文件上传', 'warning')
    selectedFiles.value = [files[0]] // 只取第一个文件
  } else {
    selectedFiles.value = files
  }
}

// 开始上传
const startUpload = async () => {
  if (!selectedFiles.value.length) {
    showToast('请选择要上传的文件', 'warning')
    return
  }
  
  if (!selectedKnowledgeBase.value) {
    showToast('请先选择知识库', 'warning')
    return
  }
  
  const file = selectedFiles.value[0] // 只处理单个文件
  uploading.value = true
  uploadProgress.value = 0
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('kbId', selectedKnowledgeBase.value.id)
    
    // 调用后端上传接口
    const response = await axios.post('/kb/doc/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      }
    })
    
    if (response.data.code === '0') {
      uploading.value = false
      showUploadModal.value = false
      showToast('上传成功', 'success')
      loadDocuments(selectedKnowledgeBase.value.id)
      selectedFiles.value = [] // 清空选择的文件
    } else {
      uploading.value = false
      showToast(response.data.message || '上传失败', 'error')
    }
  } catch (error) {
    console.error('上传文件失败:', error)
    uploading.value = false
    const errorMessage = error.response?.data?.message || error.message || '上传失败，请稍后重试'
    showToast(errorMessage, 'error')
  }
}

// 格式化文件大小
const formatFileSize = (size) => {
  if (!size || size < 0) return '0 B'
  if (size < 1024) {
    return size + ' B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(1) + ' KB'
  } else if (size < 1024 * 1024 * 1024) {
    return (size / (1024 * 1024)).toFixed(1) + ' MB'
  } else {
    return (size / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
  }
}

const getMimeTypeSubtype = (mimeType) => {
  if (!mimeType) return '未知'
  const parts = mimeType.split('/')
  if (parts.length >= 2) {
    return parts[1]
  }
  return mimeType
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
    const errorMessage = error.response?.data?.message || error.message || '删除失败'
    showToast(errorMessage, 'error')
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
      createForm.embeddingModel = 'text-embedding-v4'
      createForm.collection = ''
      fetchKnowledgeList()
    } else {
      showToast(response.data.message || '创建失败', 'error')
    }
  } catch (error) {
    console.error('创建知识库失败:', error)
    const errorMessage = error.response?.data?.message || error.message || '创建失败'
    showToast(errorMessage, 'error')
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
    const errorMessage = error.response?.data?.message || error.message || '更新失败'
    showToast(errorMessage, 'error')
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

// 处理头像加载失败
const handleAvatarError = (event) => {
  // 头像加载失败时，隐藏图片，显示默认头像
  event.target.style.display = 'none'
  // 确保默认头像显示
  const avatarElement = event.target.parentElement
  const defaultAvatar = avatarElement.querySelector('span')
  if (defaultAvatar) {
    defaultAvatar.style.display = 'flex'
  }
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

// 切换用户下拉框
const toggleUserDropdown = () => {
  showUserDropdown.value = !showUserDropdown.value
}

// 切换文档分页
const changeDocumentPage = (page) => {
  documentPagination.current = page
  loadDocuments(selectedKnowledgeBase.value.id)
}
</script>

<style scoped>
/* 批量操作按钮样式 */
.batch-operations {
  display: flex;
  gap: 10px;
  margin: 16px 0;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.batch-button {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background-color: #ffffff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.batch-button:hover {
  border-color: #1890ff;
  color: #1890ff;
}

/* 知识块内容样式 */
.chunk-content {
  max-width: 400px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  line-height: 1.4;
}

/* 状态指示器样式 */
.status-indicator {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  vertical-align: middle;
}

.status-indicator.success {
  background-color: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.1);
}

.status-indicator.warning {
  background-color: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
  box-shadow: 0 2px 4px rgba(250, 140, 22, 0.1);
}

/* 表格操作按钮样式 */
.table-actions {
  display: flex;
  gap: 8px;
}

/* 面包屑导航样式 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0;
  font-size: 14px;
}

.breadcrumb-link {
  color: #1890ff;
  cursor: pointer;
}

.breadcrumb-link:hover {
  text-decoration: underline;
}

.separator {
  color: #999;
}

.current {
  color: #666;
}

/* 知识块管理页面样式 */
.chunk-management {
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  padding: 24px;
  margin-top: 20px;
}

.chunk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.chunk-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.chunk-description {
  font-size: 14px;
  color: #666;
  margin: 4px 0 0 0;
}

.header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.btn-back,
.btn-rebuild {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #e8e8e8;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-back:hover,
.btn-rebuild:hover {
  background-color: #e8e8e8;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.add-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.add-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #5a6fd8 0%, #6a409a 100%);
}

.btn-primary {
  padding: 6px 20px;
  border: none;
  border-radius: 4px;
  background: #1890ff;
  color: #ffffff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  font-weight: 500;
}

.btn-primary:hover {
  background: #40a9ff;
}

.chunk-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.section-description {
  font-size: 12px;
  color: #666;
  margin: 2px 0 0 0;
}

.controls-right {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.status-select {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
  background: #ffffff;
  cursor: pointer;
}

.btn-refresh {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #ffffff;
  color: #666;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-refresh:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.btn-batch {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #ffffff;
  color: #666;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.btn-batch:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 2px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
  width: 100%;
}

.search-box:hover {
  border-color: #d0d3d9;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.search-box input {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
  background: transparent;
}

.search-box input:focus {
  background-color: #f8f9fa;
  box-shadow: inset 0 0 0 1px rgba(102, 126, 234, 0.1);
  outline: none;
}

.search-box input::placeholder {
  color: #999;
  font-size: 14px;
}

.search-button {
  padding: 8px;
  background-color: transparent;
  color: #999;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  margin: 0 8px;
}

.search-box input {
  width: 100%;
  padding: 10px 16px;
  padding-right: 40px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
}

.search-box input:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.search-button:hover {
  background-color: #667eea;
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.chunk-list {
  margin-top: 20px;
}

.chunk-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.chunk-table th {
  background: #fafafa;
  text-align: left;
  padding: 12px 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #f0f0f0;
}

.chunk-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  vertical-align: top;
}

.chunk-table tr:hover {
  background: #f5f5f5;
}

.content-cell {
  width: 400px;
  height: 120px;
  color: #333;
  line-height: 1.4;
  overflow: hidden;
  display: flex;
  align-items: flex-start;
  padding: 12px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
}

.content-display {
  font-size: 14px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  max-width: 100%;
  min-height: 84px;
  white-space: normal;
  word-wrap: break-word;
  background-color: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
}

/* 状态节点样式 */
.status-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.status-node::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-enabled {
  background-color: #e6f7ff;
  color: #006633;
  border: 1px solid #91d5ff;
}

.status-enabled::before {
  background-color: #006633;
}

.status-enabled:hover {
  background-color: #bae7ff;
  border-color: #69c0ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 102, 51, 0.2);
}

.status-disabled {
  background-color: #fff2e8;
  color: #cc0000;
  border: 1px solid #ffd591;
}

.status-disabled::before {
  background-color: #cc0000;
}

.status-disabled:hover {
  background-color: #fff1f0;
  border-color: #ffccc7;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(204, 0, 0, 0.2);
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.status-enabled {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.status-disabled {
  background: #fffbe6;
  color: #faad14;
  border: 1px solid #ffe58f;
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

.btn-toggle {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #ffffff;
  color: #666;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s;
}

.btn-toggle:hover {
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

/* 页面标题样式 */
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

/* 知识块信息样式 */
.kb-name-title,
.kb-id-title {
  font-size: 16px;
  font-weight: 400;
  color: #666;
  margin-left: 8px;
}
</style>

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
          <div 
            class="menu-item"
            :class="{ active: activeMenu === 'intent' }"
            @click="setActiveMenu('intent')"
          >
            <span class="menu-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                <line x1="12" y1="22.08" x2="12" y2="12"/>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">意图管理</span>
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
            <div class="user-dropdown" @click="toggleUserDropdown">
              <div class="avatar">
                <img 
                  v-if="userInfo?.avatar" 
                  :src="userInfo.avatar" 
                  alt="avatar"
                  @error="handleAvatarError"
                />
                <span v-else>{{ userInfo?.username?.charAt(0).toUpperCase() || 'A' }}</span>
              </div>
              <span class="username">{{ userInfo?.username || 'admin' }}</span>
              <svg class="dropdown-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
              <!-- 下拉菜单 -->
              <div v-if="showUserDropdown" class="user-dropdown-menu">
                <div class="dropdown-item" @click="handleLogout">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                    <polyline points="16 17 21 12 16 7"></polyline>
                    <line x1="21" y1="12" x2="9" y2="12"></line>
                  </svg>
                  <span>退出登录</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </header>
      
      <!-- 面包屑 -->
      <div class="breadcrumb">
        <span class="breadcrumb-link" @click="setActiveMenu('overview')">首页</span>
        <span class="separator">/</span>
        <span v-if="activeMenu === 'knowledge' && (activeTab === 'documents' || activeTab === 'chunks')" class="breadcrumb-link" @click="activeTab = 'knowledge'">知识库管理</span>
        <span v-else class="current">{{ activeMenu === 'overview' ? '数据概览' : activeMenu === 'knowledge' ? '知识库管理' : activeMenu === 'users' ? '用户管理' : activeMenu === 'intent' ? '意图管理' : '系统设置' }}</span>
        <span v-if="activeMenu === 'knowledge' && (activeTab === 'documents' || activeTab === 'chunks')" class="separator">/</span>
        <span v-if="activeMenu === 'knowledge' && activeTab === 'chunks'" class="breadcrumb-link" @click="activeTab = 'documents'">文档管理</span>
        <span v-else-if="activeMenu === 'knowledge' && activeTab === 'documents'" class="current">文档管理</span>
        <span v-if="activeMenu === 'knowledge' && activeTab === 'chunks'" class="separator">/</span>
        <span v-if="activeMenu === 'knowledge' && activeTab === 'chunks'" class="current">知识块管理</span>
      </div>
      
      <!-- 内容区域 -->
      <div class="content-area">
        <!-- 这里根据activeMenu显示不同的内容 -->
        <div v-if="activeMenu === 'overview'" class="page-content">
          <h2>数据概览</h2>
          <p>欢迎使用 Eevee 管理后台</p>
        </div>
        <div v-else-if="activeMenu === 'knowledge'" class="page-content knowledge-page">
          <div v-if="activeTab === 'knowledge'">
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
                  <button class="search-button" @click="fetchKnowledgeList">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="11" cy="11" r="8"/>
                      <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                    </svg>
                  </button>
                </div>
                <button class="refresh-button" @click="refreshKnowledgeList" :disabled="loading">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :class="{ 'spin': loading }">
                    <path d="M21 12c0-4.97-4.03-9-9-9s-9 4.03-9 9 4.03 9 9 9"/>
                    <path d="M15 12l-3-3-3 3"/>
                    <path d="M9 12l3 3 3-3"/>
                  </svg>
                  刷新
                </button>
                <button class="add-button btn-hover-effect" @click="showCreateModal = true">
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
              <BaseCard>
                <template #default>
                  <div class="card-content">
                    <div class="card-icon">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="4" y="4" width="16" height="16" rx="2" ry="2"/>
                        <line x1="8" y1="8" x2="16" y2="8"/>
                        <line x1="8" y1="12" x2="16" y2="12"/>
                        <line x1="8" y1="16" x2="12" y2="16"/>
                      </svg>
                    </div>
                    <div class="card-value">{{ pagination.total }}</div>
                    <div class="card-label">知识库</div>
                  </div>
                </template>
              </BaseCard>
              <BaseCard>
                <template #default>
                  <div class="card-content">
                    <div class="card-icon">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                      </svg>
                    </div>
                    <div class="card-value">0</div>
                    <div class="card-label">文档数</div>
                  </div>
                </template>
              </BaseCard>
              <BaseCard>
                <template #default>
                  <div class="card-content">
                    <div class="card-icon">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="12" y1="8" x2="12" y2="12"/>
                        <line x1="12" y1="16" x2="12.01" y2="16"/>
                      </svg>
                    </div>
                    <div class="card-value">{{ pagination.total }}</div>
                    <div class="card-label">定义组标准</div>
                  </div>
                </template>
              </BaseCard>
              <BaseCard>
                <template #default>
                  <div class="card-content">
                    <div class="card-icon">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                        <circle cx="9" cy="7" r="4"/>
                      </svg>
                    </div>
                    <div class="card-value">1</div>
                    <div class="card-label">创建用户数</div>
                  </div>
                </template>
              </BaseCard>
            </div>
            
            <!-- 知识库表格 -->
            <div class="knowledge-table">
              <BaseTable 
                :columns="knowledgeColumns" 
                :data="knowledgeList" 
                :total="pagination.total"
                :currentPage="pagination.current"
                :pageSize="pagination.size"
                @page-change="changePage"
              >
                <template #name="{ row }">
                  <a href="#" class="kb-name-link" @click.prevent="navigateToDocuments(row.kbId, row.name)">{{ row.name }}</a>
                </template>
                <template #action="{ row }">
                  <div class="table-actions">
                    <BaseButton type="primary" size="small" @click="handleEdit(row)">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                      </svg>
                    </BaseButton>
                    <BaseButton type="danger" size="small" @click="handleDeleteClick(row)">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"/>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                        <line x1="10" y1="11" x2="10" y2="17"/>
                        <line x1="14" y1="11" x2="14" y2="17"/>
                      </svg>
                    </BaseButton>
                  </div>
                </template>
              </BaseTable>
            </div>
          </div>
          
          <div v-else-if="activeTab === 'documents'">
            <!-- 文档管理 -->
            <div class="document-management">

              
              <!-- 页面头部 -->
              <div class="page-header">
                <div class="header-left">
                  <h1 class="page-title">
                    文档管理
                    <span class="kb-name-title">{{ selectedKnowledgeBase?.name ? ' · ' + selectedKnowledgeBase.name : '' }}</span>
                  </h1>
                </div>
                <div class="header-right">
                  <button class="back-button" @click="goBackToKnowledge">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M19 12H5M12 19l-7-7 7-7"/>
                    </svg>
                    返回知识库
                  </button>
                  <button class="upload-button btn-hover-effect" @click="showUploadModal = true">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                      <polyline points="7 10 12 15 17 10"/>
                      <line x1="12" y1="15" x2="12" y2="3"/>
                    </svg>
                    上传文档
                  </button>
                </div>
              </div>

              <!-- 搜索和筛选 -->
              <div class="search-filter">
                <div class="search-box">
                  <input 
                    type="text" 
                    v-model="searchQuery" 
                    placeholder="搜索文档名称"
                    @keyup.enter="handleDocumentSearch"
                  />
                  <button class="search-button" @click="handleDocumentSearch">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="11" cy="11" r="8"/>
                      <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                    </svg>
                  </button>
                </div>
                <div class="filter-box">
                  <select v-model="statusFilter" class="status-select">
                    <option value="all">全部状态</option>
                    <option value="enabled">启用</option>
                    <option value="disabled">禁用</option>
                  </select>
                  <button class="refresh-button" @click="resetFilters">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
                    </svg>
                    重置
                  </button>
                </div>
              </div>

              <!-- 文档列表 -->
              <div class="document-list">
                <BaseTable 
                  :columns="documentColumns" 
                  :data="documents" 
                  :total="documentPagination.total"
                  :currentPage="documentPagination.current"
                  :pageSize="documentPagination.size"
                  @page-change="changeDocumentPage"
                >
                  <template #documentName="{ row }">
                    <a href="#" class="kb-name-link" @click.prevent="navigateToChunks(row)">{{ row.documentName }}</a>
                  </template>
                  <template #source="{ row }">
                    Local File
                  </template>
                  <template #processMode="{ row }">
                    chunk
                  </template>
                  <template #status="{ row }">
                    <span class="status-indicator success">success</span>
                  </template>
                  <template #enabled="{ row }">
                    <label class="switch">
                      <input type="checkbox" :checked="row.enabled === 1" @change="toggleDocumentEnabled(row.id, !row.enabled)">
                      <span class="slider"></span>
                    </label>
                  </template>
                  <template #chunkCount="{ row }">
                    <a href="#" class="kb-name-link" @click.prevent="navigateToChunks(row)">{{ row.chunkCount || 0 }}</a>
                  </template>
                  <template #fileType="{ row }">
                    <span class="file-type-tag">{{ getMimeTypeSubtype(row.fileType) }}</span>
                  </template>
                  <template #fileSize="{ row }">
                    <span class="file-size-text">{{ formatFileSize(row.fileSize) }}</span>
                  </template>
                  <template #updateTime="{ row }">
                    {{ formatDate(row.updateTime) }}
                  </template>
                  <template #action="{ row }">
                    <div class="table-actions">
                      <BaseButton type="primary" size="small" @click="editDocument(row)">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                        </svg>
                      </BaseButton>
                      <BaseButton type="info" size="small" @click="openChunkModal(row)">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <circle cx="12" cy="12" r="3"/>
                          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                        </svg>
                      </BaseButton>
                      <BaseButton type="danger" size="small" @click="confirmDeleteDocument(row.id, row.documentName)">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"/>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                          <line x1="10" y1="11" x2="10" y2="17"/>
                          <line x1="14" y1="11" x2="14" y2="17"/>
                        </svg>
                      </BaseButton>
                    </div>
                  </template>
                </BaseTable>
              </div>
            </div>
          </div>
          <div v-else-if="activeTab === 'chunks'">
            <!-- 页面头部 -->
            <div class="page-header">
              <div class="header-left">
                <h1 class="page-title">
                  分块管理
                  <span class="kb-name-title">{{ selectedDocument?.documentName ? ' · ' + selectedDocument.documentName : '' }}</span>
                  <span class="kb-id-title">{{ selectedDocument?.kbId ? ' (知识库: ' + selectedDocument.kbId + ')' : '' }}</span>
                </h1>
              </div>
              <div class="header-right">
                <button class="btn-back btn-hover-effect" @click="activeTab = 'documents'">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M19 12H5M12 19l-7-7 7-7"/>
                  </svg>
                  返回文档
                </button>
                <button class="btn-rebuild btn-hover-effect">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 12c0-4.97-4.03-9-9-9s-9 4.03-9 9 4.03 9 9 9"/>
                    <path d="M15 12l-3-3-3 3"/>
                    <path d="M9 12l3 3 3-3"/>
                  </svg>
                  重建分块
                </button>
                <button class="add-button btn-hover-effect" @click="openCreateChunkModal">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="12" y1="5" x2="12" y2="19"/>
                    <line x1="5" y1="12" x2="19" y2="12"/>
                  </svg>
                  新建分块
                </button>
              </div>
            </div>

            <!-- 知识块管理 -->
            <div class="chunk-management">
              <!-- 搜索和筛选 -->
              <div class="search-filter">
                <div class="search-box">
                  <input 
                    type="text" 
                    v-model="chunkSearchQuery" 
                    placeholder="搜索知识块内容"
                    @keyup.enter="loadChunks(selectedDocument.id)"
                  />
                  <button class="search-button" @click="loadChunks(selectedDocument.id)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="11" cy="11" r="8"/>
                      <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                    </svg>
                  </button>
                </div>
                <div class="filter-box">
                  <select v-model="chunkStatusFilter" class="status-select">
                    <option value="all">全部状态</option>
                    <option value="enabled">启用</option>
                    <option value="disabled">禁用</option>
                  </select>
                  <button class="refresh-button" @click="loadChunks(selectedDocument.id)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
                    </svg>
                    刷新
                  </button>
                  <button class="btn-batch">批量启用</button>
                  <button class="btn-batch">批量禁用</button>
                  <button class="btn-batch">全部启用</button>
                  <button class="btn-batch">全部禁用</button>
                </div>
              </div>

              <!-- 知识块列表 -->
              <div class="chunk-list">
                <BaseTable 
                  :columns="chunkColumns" 
                  :data="chunks" 
                  :total="chunkPagination.total"
                  :currentPage="chunkPagination.current"
                  :pageSize="chunkPagination.size"
                  @page-change="changeChunkPage"
                  @page-size-change="handleChunkPageSizeChange"
                >
                  <template #index="{ row, index }">
                    <div class="checkbox-cell">
                      <input type="checkbox" :checked="selectedChunks.includes(row.id)" @change="handleSelectChunk(row.id)" />
                    </div>
                  </template>
                  <template #content="{ row }">
                    <div class="content-cell">
                      <div class="content-display">
                        {{ row.chunkText || '' }}
                      </div>
                    </div>
                  </template>
                  <template #status="{ row }">
                    <span class="status-node {{ row.enabled === 1 ? 'status-enabled' : 'status-disabled' }}">
                      {{ row.enabled === 1 ? '启用' : '禁用' }}
                    </span>
                  </template>
                  <template #charCount="{ row }">
                    {{ row.charCount || 0 }}
                  </template>
                  <template #tokenCount="{ row }">
                    {{ row.tokenCount || 0 }}
                  </template>
                  <template #updateTime="{ row }">
                    {{ formatDate(row.updateTime) }}
                  </template>
                  <template #action="{ row }">
                    <div class="table-actions">
                      <BaseButton type="primary" size="small" @click="editChunk(row)">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                        </svg>
                      </BaseButton>
                      <BaseButton type="danger" size="small" @click="confirmDeleteChunk(row.id, row.chunkText)">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"/>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                          <line x1="10" y1="11" x2="10" y2="17"/>
                          <line x1="14" y1="11" x2="14" y2="17"/>
                        </svg>
                      </BaseButton>
                    </div>
                  </template>
                </BaseTable>
              </div>
            </div>
          </div>
        </div>
        <div v-else-if="activeMenu === 'intent'" class="page-content intent-page">
          <IntentManagement />
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
              <option value="text-embedding-v4">text-embedding-v4</option>
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

    <!-- 上传文档弹窗 -->
    <div v-if="showUploadModal" class="modal-overlay" @click.self="showUploadModal = false">
      <div class="modal-content upload-modal">
        <div class="modal-header">
          <h3>上传文档</h3>
          <button class="close-button" @click="showUploadModal = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="upload-area" @drop="handleDrop" @dragover.prevent @dragenter.prevent @dragleave.prevent>
            <div class="upload-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#667eea" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
            </div>
            <p class="upload-text">点击或拖拽文件到此处上传</p>
            <p class="upload-hint">支持 .md, .txt, .pdf 等文件格式</p>
            <input type="file" ref="fileInput" class="file-input" @change="handleFileSelect" />
            <button class="browse-button" @click="$refs.fileInput.click()">浏览文件</button>
          </div>
          
          <!-- 显示选择的文件 -->
          <div v-if="selectedFiles.length > 0" class="selected-files">
            <div class="file-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
                <polyline points="10 9 9 9 8 9"/>
              </svg>
              <div class="file-info">
                <div class="file-name">{{ selectedFiles[0].name }}</div>
                <div class="file-size">{{ formatFileSize(selectedFiles[0].size) }}</div>
              </div>
              <button class="remove-file" @click="selectedFiles = []">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
          </div>
          
          <!-- 文档信息表单 -->
          <div class="document-form" v-if="selectedFiles.length > 0">
            <div class="form-row">
              <div class="form-group">
                <label>文件名称</label>
                <input type="text" v-model="selectedFiles[0].name" class="form-input" readonly />
              </div>
              <div class="form-group">
                <label>文件大小</label>
                <input type="text" :value="formatFileSize(selectedFiles[0].size)" class="form-input" readonly />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>文件类型</label>
                <input type="text" :value="selectedFiles[0].type || '未知'" class="form-input" readonly />
              </div>
              <div class="form-group">
                <label>所属知识库</label>
                <input type="text" :value="selectedKnowledgeBase?.name" class="form-input" readonly />
              </div>
            </div>
          </div>
          
          <div v-if="uploading" class="upload-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
            </div>
            <span class="progress-text">{{ uploadProgress }}%</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="showUploadModal = false" :disabled="uploading">取消</button>
          <button class="confirm-button" @click="startUpload" :disabled="!selectedFiles.length || uploading">开始上传</button>
        </div>
      </div>
    </div>

    <!-- 删除文档确认弹窗 -->
    <div v-if="showDocumentDeleteConfirm" class="modal-overlay" @click.self="handleDocumentDeleteCancel">
      <div class="modal-content delete-modal">
        <div class="modal-header">
          <h3>确认删除</h3>
          <button class="close-button" @click="handleDocumentDeleteCancel">
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
          <p class="delete-message">确定要删除文档 <span class="delete-name">{{ documentToDelete }}</span> 吗？</p>
          <p class="delete-hint">此操作不可撤销，删除后相关数据将被永久清除。</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="handleDocumentDeleteCancel">取消</button>
          <button class="delete-confirm-button" @click="handleDocumentDeleteConfirm">删除</button>
        </div>
      </div>
    </div>

    <!-- 启动分块弹窗 -->
    <div v-if="showChunkModal" class="modal-overlay" @click.self="showChunkModal = false">
      <div class="modal-content chunk-modal">
        <div class="modal-header">
          <h3>启动分块</h3>
          <button class="close-button" @click="showChunkModal = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>分块策略 <span class="required">*</span></label>
            <select v-model="chunkForm.chunkStrategy" class="form-select">
              <option value="recursive">递归算法分块</option>
            </select>
          </div>
          <div class="form-group">
            <label>最大分块大小 (maxChunkSize) <span class="required">*</span></label>
            <input 
              v-model.number="chunkForm.maxChunkSize" 
              type="number" 
              class="form-input"
              placeholder="请输入最大分块大小"
              min="100"
              max="5000"
            />
          </div>
          <div class="form-group">
            <label>最大重叠大小 (maxOverlapSize) <span class="required">*</span></label>
            <input 
              v-model.number="chunkForm.maxOverlapSize" 
              type="number" 
              class="form-input"
              placeholder="请输入最大重叠大小"
              min="0"
              max="500"
            />
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="showChunkModal = false" :disabled="chunking">取消</button>
          <button class="confirm-button" @click="startChunking" :disabled="chunking">
            <svg v-if="chunking" class="loading-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" stroke-dasharray="32" stroke-dashoffset="32"/>
            </svg>
            {{ chunking ? '分块中...' : '启动分块' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 编辑知识块弹窗 -->
    <div v-if="showEditChunkModal" class="modal-overlay" @click.self="cancelChunkEdit">
      <div class="modal-content edit-chunk-modal">
        <div class="modal-header">
          <h3>编辑知识块</h3>
          <button class="close-button" @click="cancelChunkEdit">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>知识块内容</label>
            <textarea 
              v-model="editForm.content" 
              class="form-textarea"
              placeholder="请输入知识块内容"
              rows="10"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="cancelChunkEdit">取消</button>
          <button class="confirm-button" @click="saveChunkEdit">保存</button>
        </div>
      </div>
    </div>

    <!-- 新建知识块弹窗 -->
    <div v-if="showCreateChunkModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>新建知识块</h3>
          <button class="close-button" @click="handleCreateChunkModalClose">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>知识块内容 <span class="required">*</span></label>
            <textarea 
              v-model="createChunkForm.chunkText" 
              class="form-textarea"
              placeholder="请输入知识块内容"
              rows="10"
              maxlength="2048"
              @input="updateCharCount"
            ></textarea>
            <div class="char-count">
              字符数: {{ charCount }} | Token数: {{ tokenCount }} | 限制: 512 Token
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="handleCreateChunkModalClose">取消</button>
          <button class="confirm-button" @click="createChunk">创建</button>
        </div>
      </div>
    </div>
    
    <!-- 知识块删除确认弹窗 -->
    <div v-if="showChunkDeleteConfirm" class="modal-overlay" @click.self="handleChunkDeleteCancel">
      <div class="modal-content delete-modal">
        <div class="modal-header">
          <h3>确认删除</h3>
          <button class="close-button" @click="handleChunkDeleteCancel">
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
          <p class="delete-message">确定要删除知识块吗？</p>
          <p class="delete-hint">此操作不可撤销，删除后相关数据将被永久清除。</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-button" @click="handleChunkDeleteCancel">取消</button>
          <button class="delete-confirm-button" @click="handleChunkDeleteConfirm">删除</button>
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

/* 通用按钮悬停效果 */
.btn-hover-effect {
  position: relative;
  overflow: hidden;
}

.btn-hover-effect::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.1), transparent);
  transition: left 0.5s ease;
}

.btn-hover-effect:hover::before {
  left: 100%;
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
}

.user-dropdown {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  background-color: #f5f7fa;
  transition: all 0.2s ease;
  cursor: pointer;
}

.user-dropdown:hover {
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

.dropdown-icon {
  color: #666;
  transition: transform 0.3s ease;
}

.user-dropdown:hover .dropdown-icon {
  color: #667eea;
}

.user-dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  min-width: 160px;
  z-index: 1000;
  border: 1px solid #f0f0f0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  color: #ef4444;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 6px;
  margin: 4px;
}

.dropdown-item:hover {
  background: #fef2f2;
  color: #dc2626;
}

.kb-name-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s ease;
  display: inline-block;
  position: relative;
}

.kb-name-link:hover {
  color: #5a6fe6;
  text-decoration: underline;
  transform: translateY(-1px);
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
  padding: 0 24px 16px;
  background-color: transparent;
  border-radius: 0;
  border-bottom: none;
  margin-bottom: 16px;
  box-shadow: none;
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
  gap: 8px;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 0;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  height: 40px;
  margin: 0;
}

.page-header .search-box input {
  flex: 1;
  padding: 0 16px;
  border: none;
  border-radius: 8px 0 0 8px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
  background: transparent;
  width: 180px;
  height: 100%;
}

.page-header .search-button {
  padding: 0 16px;
  background-color: transparent;
  color: #999;
  border: none;
  border-left: 1px solid #e8e8e8;
  border-radius: 0 8px 8px 0;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  margin: 0;
}

.page-header .search-box:hover {
  border-color: #d0d3d9;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.page-header .search-box input:focus {
  background-color: #f8f9fa;
  box-shadow: inset 0 0 0 1px rgba(102, 126, 234, 0.1);
  outline: none;
}

.page-header .search-box input::placeholder {
  color: #999;
  font-size: 14px;
}

.refresh-button, .add-button {
  height: 40px;
  align-items: center;
}

.refresh-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  color: #666;
  font-size: 13px;
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
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 13px;
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
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  padding: 0 24px 24px;
  margin-bottom: 20px;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid #f0f0f0;
  position: relative;
  overflow: hidden;
}

.card:hover {
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
  border-color: #e8e8e8;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.card:hover .card-icon {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.3);
}

.card-content {
  flex: 1;
}

.card-value {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin-bottom: 2px;
  transition: color 0.3s ease;
}

.card:hover .card-value {
  color: #667eea;
}

.card-label {
  font-size: 13px;
  color: #999;
  margin-bottom: 0;
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
  transition: all 0.3s ease;
}

.knowledge-table .kb-name-link {
  color: #333;
  text-decoration: none;
  transition: all 0.3s ease;
  display: inline-block;
  position: relative;
}

.knowledge-table .kb-name-link:hover {
  color: #667eea;
}

.knowledge-table .kb-name-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background-color: #667eea;
  transition: width 0.3s ease;
}

.knowledge-table .kb-name-link:hover::after {
  width: 100%;
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
}

/* 分块弹窗 */
.chunk-modal {
  width: 450px;
}

.form-hint {
  font-size: 12px;
  color: #666;
  margin-top: 6px;
  line-height: 1.4;
}

.loading-icon {
  animation: spin 1s linear infinite;
  margin-right: 8px;
  display: inline-block;
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

/* 标签页容器 */
.tab-container {
  margin-top: 24px;
}

.tab-content {
  animation: fadeIn 0.3s ease;
}

/* 文档管理样式 */
.document-management {
  padding: 0;
}

/* 面包屑导航 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}

.breadcrumb-item {
  color: #666;
  text-decoration: none;
  transition: color 0.3s ease;
}

.breadcrumb-item:hover {
  color: #667eea;
}

.breadcrumb-item.current {
  color: #333;
  font-weight: 500;
}

.breadcrumb-separator {
  color: #ccc;
  margin: 0 4px;
}

/* 文档页面头部 */
.document-management .page-header,
.chunk-management .page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.document-management .header-left h1,
.chunk-management .header-left h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.document-management .kb-name-title,
.chunk-management .kb-name-title,
.chunk-management .kb-id-title {
  font-size: 16px;
  font-weight: 500;
  color: #666;
  margin-left: 8px;
}

.document-management .header-right,
.chunk-management .header-right {
  display: flex;
  gap: 12px;
}

.back-button,
.upload-button,
.btn-back,
.btn-rebuild,
.add-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.back-button {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #e8e8e8;
}

.back-button:hover {
  background-color: #e8e8e8;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.upload-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.upload-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #5a6fd8 0%, #6a409a 100%);
}

/* 搜索和筛选 */
.search-filter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8e8e8;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 2px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-box:hover {
  border-color: #d0d3d9;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.search-box input {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  width: 300px;
  transition: all 0.3s ease;
  background-color: transparent;
}

.search-box input:focus {
  outline: none;
  box-shadow: none;
}

.search-box input::placeholder {
  color: #999;
  font-size: 14px;
}

.search-button {
  padding: 8px;
  background-color: transparent;
  color: #999;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  margin: 0 8px;
}

.search-button:hover {
  background-color: #667eea;
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.search-button svg {
  margin: 0;
  vertical-align: middle;
}

.filter-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-select {
  padding: 10px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  background-color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.status-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.refresh-button {
  padding: 10px 16px;
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.refresh-button:hover {
  background-color: #e8e8e8;
  transform: translateY(-1px);
}

/* 文档列表 */
.document-list {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8e8e8;
  overflow: hidden;
  margin-bottom: 24px;
}

.list-header {
  display: grid;
  grid-template-columns: 220px 100px 120px 80px 60px 80px 100px 100px 150px 120px;
  padding: 16px 24px;
  background-color: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 500;
  font-size: 14px;
  color: #666;
  gap: 12px;
}

.document-item {
  display: grid;
  grid-template-columns: 220px 100px 120px 80px 60px 80px 100px 100px 150px 120px;
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  transition: all 0.3s ease;
  align-items: center;
  gap: 12px;
}

.header-item {
  text-align: left;
}

.list-body {
  min-height: 200px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #999;
}

.empty-state svg {
  margin-bottom: 16px;
}

.document-item:hover {
  background-color: #fafbff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.1);
}

.item-cell {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #333;
}

.document-name {
  font-weight: 500;
}

.status-indicator {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-indicator.success {
  background-color: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
  vertical-align: middle;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #f0f0f0;
  transition: .4s;
  border-radius: 24px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.slider:before {
  position: absolute;
  content: "";
  height: 20px;
  width: 20px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

input:checked + .slider {
  background-color: #667eea;
  box-shadow: inset 0 2px 4px rgba(102, 126, 234, 0.3);
}

input:focus + .slider {
  box-shadow: 0 0 2px #667eea, inset 0 2px 4px rgba(102, 126, 234, 0.3);
}

input:checked + .slider:before {
  transform: translateX(24px);
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.3);
}

.file-type-tag {
  display: inline;
  font-size: 14px;
  color: #666;
  font-weight: 400;
  word-break: break-all;
  max-width: 100%;
  box-sizing: border-box;
  line-height: 1.4;
}

.file-size-text {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.document-item .actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.document-item .action-button {
  padding: 6px 8px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  color: #666;
}

.document-item .action-button:hover {
  background-color: #e8e8e8;
  transform: translateY(-1px);
}

.document-item .action-button.edit:hover {
  background-color: #e6f7ff;
  color: #1890ff;
}

.document-item .action-button.view:hover {
  background-color: #f6ffed;
  color: #52c41a;
}

.document-item .action-button.download:hover {
  background-color: #fff7e6;
  color: #fa8c16;
}

.document-item .action-button.delete:hover {
  background-color: #fff1f0;
  color: #ff4d4f;
}

/* 文档分页 */
.document-management .pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8e8e8;
}

.document-management .page-info {
  font-size: 14px;
  color: #666;
}

/* 上传区域 */
.upload-area {
  border: 2px dashed #e8e8e8;
  border-radius: 12px;
  padding: 48px 24px;
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
  background-color: #fafafa;
}

.upload-area:hover {
  border-color: #667eea;
  background-color: #f0f4ff;
}

.upload-icon {
  margin-bottom: 16px;
}

.upload-text {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 14px;
  color: #999;
  margin-bottom: 24px;
}

.file-input {
  display: none;
}

.browse-button {
  padding: 10px 24px;
  background-color: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.browse-button:hover {
  background-color: #5a6fd8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 上传进度 */
.upload-progress {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

/* 文档信息表单 */
.document-form {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8e8e8;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.form-group {
  flex: 1;
  min-width: 200px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 10px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  background-color: #fafafa;
  color: #333;
  transition: all 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background-color: white;
}

.form-input[readonly] {
  cursor: not-allowed;
  background-color: #f5f5f5;
  color: #666;
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s ease;
  line-height: 1.4;
  background-color: #fafafa;
  color: #333;
}

.form-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background-color: white;
}

.char-count {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  text-align: right;
}

.char-count.exceeded {
  color: #ff4d4f;
}

.edit-chunk-modal {
  width: 600px;
  max-width: 90vw;
}

.progress-text {
  font-size: 14px;
  color: #666;
  text-align: right;
}

/* 上传弹窗 */
.upload-modal {
  width: 600px;
}

/* 选中文件显示 */
.selected-files {
  margin-top: 24px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background-color: #fafafa;
  transition: background-color 0.2s ease;
}

.file-item:hover {
  background-color: #f5f5f5;
}

.file-item svg {
  color: #667eea;
  margin-right: 12px;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 12px;
  color: #999;
}

.remove-file {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.remove-file:hover {
  color: #ff4d4f;
  background-color: rgba(255, 77, 79, 0.1);
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
