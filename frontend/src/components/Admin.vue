<script setup>
import { ref, onMounted, onUnmounted, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import UserManagement from './user/UserManagement.vue'
import DataOverview from './DataOverview.vue'
import SampleManagement from './SampleManagement.vue'
import BaseCard from './common/BaseCard.vue'
import BaseTable from './common/BaseTable.vue'
import BaseButton from './common/BaseButton.vue'
import BaseForm from './common/BaseForm.vue'
import FormField from './common/FormField.vue'
import ScrollableContainer from './common/ScrollableContainer.vue'
import logo from '../assets/logo.png'

const router = useRouter()
const userInfo = ref(null)
const activeMenu = ref('overview') // 默认显示数据概览
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

// 统计数据
const dashboardData = ref({
  knowledgeBaseCount: 0,
  knowledgeBaseGrowth: 0,
  documentCount: 0,
  documentGrowth: 0,
  chunkCount: 0,
  chunkGrowth: 0,
  userCount: 0,
  userGrowth: 0
})

// 弹窗控制
const showCreateModal = ref(false)
const showIntentCreateModal = ref(false)
const showEditModal = ref(false)
const showIntentEditModal = ref(false)
const showDeleteConfirm = ref(false)
const showIntentDeleteConfirm = ref(false)
const editingItem = ref({})
const itemToDelete = ref(null)

// 父节点选择状态
const parentSelectOpen = ref(false)

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
  maxOverlapSize: 24
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
  chunkForm.maxOverlapSize = 24
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
  fetchDashboardData()
}

// 获取统计数据
const fetchDashboardData = async () => {
  try {
    const response = await axios.get('/api/statistics/dashboard')
    if (response.data.code === '0') {
      dashboardData.value = {
        knowledgeBaseCount: response.data.data.knowledgeBaseCount,
        knowledgeBaseGrowth: response.data.data.knowledgeBaseGrowth || 0,
        documentCount: response.data.data.documentCount,
        documentGrowth: response.data.data.documentGrowth || 0,
        chunkCount: response.data.data.chunkCount,
        chunkGrowth: response.data.data.chunkGrowth || 0,
        userCount: response.data.data.userCount,
        userGrowth: response.data.data.userGrowth || 0
      }
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
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

onMounted(() => {
  // 实时获取用户信息
  fetchUserInfo()
  // 获取知识库列表
  fetchKnowledgeList()
  // 获取统计数据
  fetchDashboardData()
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

// 意图管理相关状态
const intentTree = ref([])
const selectedNode = ref(null)
const intentLoading = ref(false)
const expandedNodes = ref(new Set())

// 表单数据
const intentCreateForm = ref({
  kbId: '',
  intentCode: '',
  name: '',
  level: 0, // 0=DOMAIN,1=CATEGORY,2=INTENT
  parentCode: '', // 默认选择ROOT，ROOT对应的value是空字符串
  description: '',
  examples: [],
  examplesText: '',
  topK: 5,
  kind: 0, // 0=KB,1=SYSTEM
  sortOrder: 0,
  enabled: true,
  promptSnippet: '',
  promptTemplate: ''
})

// 示例问题标签
const exampleTags = ref([])
const newExampleTag = ref('')

// 编辑表单示例问题标签
const editExampleTags = ref([])
const newEditExampleTag = ref('')

// 表单展开状态
const descriptionExpanded = ref(true)
const promptExpanded = ref(false)
const advancedExpanded = ref(false)

const intentEditForm = ref({
  id: '',
  name: '',
  level: 0,
  parentCode: '',
  description: '',
  examples: [],
  examplesText: '',
  collectionName: '',
  topK: 5,
  kind: 0,
  sortOrder: 0,
  enabled: true,
  promptSnippet: '',
  promptTemplate: ''
})

// 示例问题
const newExample = ref('')

// 类型选项
const levelOptions = [
  { label: 'DOMAIN-顶层领域', value: 0 },
  { label: 'CATEGORY-业务分类', value: 1 },
  { label: 'INTENT-具体意图', value: 2 }
]

const kindOptions = [
  { label: 'KB-知识库检索', value: 0 },
  { label: 'SYSTEM-系统交互', value: 1 }
]
// 类型标签
const levelOptionsTag = [
  { label: 'DOMAIN', value: 0 },
  { label: 'CATEGORY', value: 1 },
  { label: 'INTENT', value: 2 }
]

const kindOptionsTag = [
  { label: 'KB', value: 0 },
  { label: 'SYSTEM', value: 1 }
]

// 加载意图树
const loadIntentTree = async () => {
  intentLoading.value = true
  try {
    const response = await axios.get('/intent-tree/trees')
    if (response.data.code === '0') {
      // 确保intentTree.value是一个数组，即使后端返回null或undefined
      intentTree.value = response.data.data || []
      // 默认选择第一个节点
      if (intentTree.value.length > 0) {
        selectedNode.value = intentTree.value[0]
        // 默认展开所有节点
        expandAllNodes(intentTree.value)
      }
    } else {
      showToast('获取意图树失败', 'error')
    }
  } catch (error) {
    console.error('获取意图树失败:', error)
    showToast('获取意图树失败，请稍后重试', 'error')
  } finally {
    intentLoading.value = false
  }
}

// 选择节点
const selectNode = (node) => {
  selectedNode.value = node
}

// 递归展开所有节点
const expandAllNodes = (nodes) => {
  nodes.forEach(node => {
    expandedNodes.value.add(node.id)
    if (node.children && node.children.length > 0) {
      expandAllNodes(node.children)
    }
  })
}

// 生成父节点选项列表（带完整路径）
const generateParentOptions = (nodes, prefix = '') => {
  let options = []
  
  // 添加ROOT选项
  if (prefix === '') {
    options.push({ label: 'ROOT', value: '' })
  }
  
  nodes.forEach(node => {
    // 生成当前节点的显示标签（包含完整路径）
    const label = prefix ? `${prefix} > ${node.name}` : node.name
    // 添加当前节点作为选项
    options.push({ label, value: node.intentCode })
    
    // 递归处理子节点
    if (node.children && node.children.length > 0) {
      const childOptions = generateParentOptions(node.children, label)
      options = [...options, ...childOptions]
    }
  })
  
  return options
}

// 计算属性：父节点选项列表
const parentOptions = computed(() => {
  return generateParentOptions(intentTree.value)
})

// 切换节点展开/收起状态
const toggleNode = (node) => {
  const expanded = expandedNodes.value
  if (expanded.has(node.id)) {
    expanded.delete(node.id)
  } else {
    expanded.add(node.id)
  }
  // 触发响应式更新
  expandedNodes.value = new Set(expanded)
}

// 添加示例问题标签
const addExampleTag = () => {
  if (newExampleTag.value.trim() && !exampleTags.value.includes(newExampleTag.value.trim())) {
    exampleTags.value.push(newExampleTag.value.trim())
    newExampleTag.value = ''
    // 更新examplesText
    intentCreateForm.value.examplesText = exampleTags.value.join('\n')
  }
}

// 删除示例问题标签
const removeExampleTag = (tag) => {
  exampleTags.value = exampleTags.value.filter(t => t !== tag)
  // 更新examplesText
  intentCreateForm.value.examplesText = exampleTags.value.join('\n')
}

// 同步标签和examplesText
const syncExampleTags = () => {
  if (intentCreateForm.value.examplesText) {
    const examplesText = intentCreateForm.value.examplesText
    try {
      // 尝试解析JSON数组字符串
      const parsed = JSON.parse(examplesText)
      if (Array.isArray(parsed)) {
        exampleTags.value = parsed.filter(item => item && item.trim())
        return
      }
    } catch (e) {
      // 如果不是JSON字符串，按换行符分割
    }
    // 按换行符分割
    exampleTags.value = examplesText.split('\n').filter(item => item.trim())
  } else {
    exampleTags.value = []
  }
}

// 添加编辑表单示例问题标签
const addEditExampleTag = () => {
  if (newEditExampleTag.value.trim() && !editExampleTags.value.includes(newEditExampleTag.value.trim())) {
    editExampleTags.value.push(newEditExampleTag.value.trim())
    newEditExampleTag.value = ''
    // 更新examplesText
    intentEditForm.value.examplesText = editExampleTags.value.join('\n')
  }
}

// 删除编辑表单示例问题标签
const removeEditExampleTag = (tag) => {
  editExampleTags.value = editExampleTags.value.filter(t => t !== tag)
  // 更新examplesText
  intentEditForm.value.examplesText = editExampleTags.value.join('\n')
}

// 同步编辑表单标签和examplesText
const syncEditExampleTags = () => {
  if (intentEditForm.value.examplesText) {
    editExampleTags.value = intentEditForm.value.examplesText.split('\n').filter(item => item.trim())
  } else {
    editExampleTags.value = []
  }
}

// 解析示例问题
const parseExamples = (examples) => {
  if (!examples) return []
  
  try {
    // 尝试解析JSON字符串
    const parsed = JSON.parse(examples)
    if (Array.isArray(parsed)) {
      return parsed.map(item => item.trim())
    }
  } catch (e) {
    // 如果不是JSON字符串，尝试按分号分割
    return examples.split(';').map(item => item.trim()).filter(item => item)
  }
  
  return []
}

// 新建节点
const handleIntentCreate = async () => {
  try {
    // 将examplesText转换为examples数组
    if (intentCreateForm.value.examplesText) {
      intentCreateForm.value.examples = intentCreateForm.value.examplesText.split('\n').filter(item => item.trim())
    } else {
      intentCreateForm.value.examples = []
    }
    
    // 将布尔值转换为数字，并处理parentCode和topK
    const formData = {
      ...intentCreateForm.value,
      parentCode: intentCreateForm.value.parentCode === '' ? null : intentCreateForm.value.parentCode,
      topK: intentCreateForm.value.topK === '' ? null : intentCreateForm.value.topK,
      enabled: intentCreateForm.value.enabled ? 1 : 0
    }
    
    const response = await axios.post('/intent-tree/createNode', formData)
    if (response.data.code === '0') {
      showToast('创建成功', 'success')
      showIntentCreateModal.value = false
      loadIntentTree()
      resetCreateForm()
    } else {
      showToast(response.data.message || '创建失败', 'error')
    }
  } catch (error) {
    console.error('创建节点失败:', error)
    showToast('创建失败，请稍后重试', 'error')
  }
}

// 更新节点
const handleIntentUpdate = async () => {
  try {
    // 将examplesText转换为examples数组
    if (intentCreateForm.value.examplesText) {
      intentCreateForm.value.examples = intentCreateForm.value.examplesText.split('\n').filter(item => item.trim())
    } else {
      intentCreateForm.value.examples = []
    }
    
    // 准备更新数据
    let formData = {
      id: intentCreateForm.value.id,
      topK: intentCreateForm.value.topK === '' ? null : intentCreateForm.value.topK,
      enabled: intentCreateForm.value.enabled ? 1 : 0
    }
    
    // 比较数据变化，只传递发生变化的字段
    const changedFields = {}
    
    // 比较基本字段
    if (intentCreateForm.value.name !== originalNodeData.value.name) {
      changedFields.name = intentCreateForm.value.name
    }
    
    if (intentCreateForm.value.description !== originalNodeData.value.description) {
      changedFields.description = intentCreateForm.value.description
    }
    
    // 比较examples
    const currentExamples = intentCreateForm.value.examples
    const originalExamples = originalNodeData.value.examples
    let examplesChanged = false
    
    if (Array.isArray(currentExamples) && Array.isArray(originalExamples)) {
      if (currentExamples.length !== originalExamples.length) {
        examplesChanged = true
      } else {
        for (let i = 0; i < currentExamples.length; i++) {
          if (currentExamples[i] !== originalExamples[i]) {
            examplesChanged = true
            break
          }
        }
      }
    } else if (typeof currentExamples === 'string' && typeof originalExamples === 'string') {
      if (currentExamples !== originalExamples) {
        examplesChanged = true
      }
    } else {
      examplesChanged = true
    }
    
    if (examplesChanged) {
      changedFields.examples = currentExamples
    }
    
    // 比较collectionName和kbId
    if (intentCreateForm.value.collectionName !== originalNodeData.value.collectionName) {
      changedFields.collectionName = intentCreateForm.value.collectionName
      changedFields.kbId = intentCreateForm.value.kbId
    }
    
    // 比较topK
    if (formData.topK !== originalNodeData.value.topK) {
      changedFields.topK = formData.topK
    }
    
    // 比较kind
    if (intentCreateForm.value.kind !== originalNodeData.value.kind) {
      changedFields.kind = intentCreateForm.value.kind
    }
    
    // 比较sortOrder
    if (intentCreateForm.value.sortOrder !== originalNodeData.value.sortOrder) {
      changedFields.sortOrder = intentCreateForm.value.sortOrder
    }
    
    // 比较enabled
    if (formData.enabled !== originalNodeData.value.enabled) {
      changedFields.enabled = formData.enabled
    }
    
    // 比较promptSnippet
    if (intentCreateForm.value.promptSnippet !== originalNodeData.value.promptSnippet) {
      changedFields.promptSnippet = intentCreateForm.value.promptSnippet
    }
    
    // 比较promptTemplate
    if (intentCreateForm.value.promptTemplate !== originalNodeData.value.promptTemplate) {
      changedFields.promptTemplate = intentCreateForm.value.promptTemplate
    }
    
    // 检查是否有字段发生变化
    const hasChanges = Object.keys(changedFields).length > 0
    
    if (!hasChanges) {
      showToast('未检测到数据变化', 'info')
      showIntentEditModal.value = false
      return
    }
    
    // 构建最终的formData
    formData = {
      ...formData,
      ...changedFields
    }
    
    const response = await axios.put(`/intent-tree/mkdir/${intentCreateForm.value.id}`, formData)
    if (response.data.code === '0') {
      showToast('更新成功', 'success')
      showIntentEditModal.value = false
      loadIntentTree()
      resetCreateForm()
    } else {
      showToast(response.data.message || '更新失败', 'error')
    }
  } catch (error) {
    console.error('更新节点失败:', error)
    showToast('更新失败，请稍后重试', 'error')
  }
}

// 删除节点
const handleDelete = async () => {
  if (!selectedNode.value) return
  
  try {
    const response = await axios.delete(`/intent-tree/remove/${selectedNode.value.id}`)
    if (response.data.code === '0') {
      showToast('删除成功', 'success')
      showIntentDeleteConfirm.value = false
      selectedNode.value = null
      loadIntentTree()
    } else {
      showToast(response.data.message || '删除失败', 'error')
    }
  } catch (error) {
    console.error('删除节点失败:', error)
    showToast('删除失败，请稍后重试', 'error')
  }
}

// 打开新建根节点模态框
const openCreateRootModal = () => {
  resetCreateForm()
  // 新建根节点时，parentCode 为空字符串（ROOT），level 为 0
  intentCreateForm.value.parentCode = ''
  intentCreateForm.value.level = 0
  showIntentCreateModal.value = true
}

// 打开新建子节点模态框
const openCreateChildModal = () => {
  resetCreateForm()
  if (selectedNode.value) {
    intentCreateForm.value.parentCode = selectedNode.value.intentCode
    intentCreateForm.value.level = selectedNode.value.level + 1
  }
  showIntentCreateModal.value = true
}

// 原始节点数据，用于比较变化
const originalNodeData = ref(null)

// 打开编辑节点模态框
const openEditModal = () => {
  if (!selectedNode.value) return
  
  // 找到当前collectionName对应的kbId
  const currentKb = knowledgeList.value.find(kb => kb.collection === selectedNode.value.collectionName)
  
  // 保存原始数据，用于后续比较
  let originalExamples = []
  if (selectedNode.value.examples) {
    if (Array.isArray(selectedNode.value.examples)) {
      originalExamples = selectedNode.value.examples
    } else if (typeof selectedNode.value.examples === 'string') {
      originalExamples = selectedNode.value.examples.split(';').filter(item => item.trim())
    }
  }
  
  originalNodeData.value = {
    id: selectedNode.value.id,
    name: selectedNode.value.name,
    intentCode: selectedNode.value.intentCode,
    level: selectedNode.value.level,
    parentCode: selectedNode.value.parentCode,
    description: selectedNode.value.description,
    examples: originalExamples,
    collectionName: selectedNode.value.collectionName,
    topK: selectedNode.value.topK,
    kind: selectedNode.value.kind,
    sortOrder: selectedNode.value.sortOrder,
    enabled: selectedNode.value.enabled,
    promptSnippet: selectedNode.value.promptSnippet,
    promptTemplate: selectedNode.value.promptTemplate
  }
  
  // 重置表单
  resetCreateForm()
  
  // 填充表单数据
  let examplesArray = []
  let examplesText = ''
  
  if (selectedNode.value.examples) {
    if (Array.isArray(selectedNode.value.examples)) {
      // 如果examples是数组，直接使用
      examplesArray = selectedNode.value.examples
      examplesText = examplesArray.join('\n')
    } else if (typeof selectedNode.value.examples === 'string') {
      // 如果examples是字符串，按分号分割
      examplesArray = selectedNode.value.examples.split(';').filter(item => item.trim())
      examplesText = examplesArray.join('\n')
    }
  }
  
  intentCreateForm.value = {
    id: selectedNode.value.id,
    name: selectedNode.value.name,
    intentCode: selectedNode.value.intentCode,
    level: selectedNode.value.level,
    parentCode: selectedNode.value.parentCode,
    description: selectedNode.value.description,
    examples: examplesArray,
    examplesText: examplesText,
    collectionName: selectedNode.value.collectionName,
    kbId: currentKb ? currentKb.kbId : '',
    topK: selectedNode.value.topK || '',
    kind: selectedNode.value.kind,
    sortOrder: selectedNode.value.sortOrder,
    enabled: selectedNode.value.enabled === 1,
    promptSnippet: selectedNode.value.promptSnippet,
    promptTemplate: selectedNode.value.promptTemplate
  }
  
  // 同步标签
  syncExampleTags()
  
  showIntentEditModal.value = true
}

// 重置创建表单
const resetCreateForm = () => {
  intentCreateForm.value = {
    kbId: '',
    intentCode: '',
    name: '',
    level: 0,
    parentCode: '',
    description: '',
    examples: [],
    examplesText: '',
    topK: '',
    kind: 0,
    sortOrder: 0,
    enabled: true,
    promptSnippet: '',
    promptTemplate: ''
  }
  
  // 重置示例问题标签
  exampleTags.value = []
  newExampleTag.value = ''
  
  // 重置展开状态
  descriptionExpanded.value = true
  promptExpanded.value = false
  advancedExpanded.value = false
}

// 添加示例问题
const addExample = () => {
  if (newExample.value && !intentCreateForm.value.examples.includes(newExample.value)) {
    intentCreateForm.value.examples.push(newExample.value)
    newExample.value = ''
  }
}

// 添加编辑示例问题
const addEditExample = () => {
  if (newExample.value && !intentEditForm.value.examples.includes(newExample.value)) {
    intentEditForm.value.examples.push(newExample.value)
    newExample.value = ''
  }
}

// 删除示例问题
const removeExample = (index) => {
  intentCreateForm.value.examples.splice(index, 1)
}

// 删除编辑示例问题
const removeEditExample = (index) => {
  intentEditForm.value.examples.splice(index, 1)
}

// 刷新意图树
const refreshIntentTree = () => {
  loadIntentTree()
}

// 切换父节点选择下拉框
const toggleParentSelect = () => {
  parentSelectOpen.value = !parentSelectOpen.value
}

// 选择父节点选项
const selectParentOption = (value) => {
  intentCreateForm.value.parentCode = value
  parentSelectOpen.value = false
}

// 获取父节点选项的标签
const getParentOptionLabel = (value) => {
  // 当value为空字符串或null时，直接返回'ROOT'
  if (value === '' || value === null) {
    return 'ROOT'
  }
  const option = parentOptions.value.find(opt => opt.value === value)
  return option ? option.label : '请选择父节点'
}

// 初始化意图树
onMounted(() => {
  // 从本地存储获取用户信息
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    userInfo.value = JSON.parse(storedUserInfo)
  }
  // 获取知识库列表
  fetchKnowledgeList()
  // 加载意图树
  loadIntentTree()
  
  // 添加全局点击事件监听器，用于关闭父节点选择下拉框
  document.addEventListener('click', handleClickOutside)
})

// 组件卸载时移除事件监听器
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 处理点击外部关闭下拉框
const handleClickOutside = (event) => {
  // 检查点击是否发生在自定义选择框外部
  const customSelect = document.querySelector('.custom-select')
  if (customSelect && !customSelect.contains(event.target)) {
    parentSelectOpen.value = false
  }
}

// 复制文本到剪贴板
const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text)
    .then(() => {
      showToast('复制成功', 'success')
    })
    .catch(err => {
      console.error('复制失败:', err)
      showToast('复制失败', 'error')
    })
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

/* 意图管理页面样式 */
.intent-page {
  padding: 24px;
}

.content-container {
  display: flex;
  gap: 24px;
  height: calc(100vh - 160px);
}

.tree-section {
  flex: 0 0 320px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.intent-tree {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tree-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.tree-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.tree-actions {
  display: flex;
  gap: 8px;
}

.tree-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.node-item {
  margin-bottom: 4px;
}

.node-content {
  display: flex;
  align-items: flex-start;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 2px;
}

.node-content:hover {
  background: #f5f7fa;
}

.node-content.selected {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
}

.node-toggle {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  font-size: 10px;
  color: #666;
  flex-shrink: 0;
  margin-top: 2px;
}

.node-info {
  flex: 1;
}

.node-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}

.node-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.node-children {
  margin-left: 24px;
  margin-top: 4px;
}

.detail-section {
  flex: 1;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.detail-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.node-basic-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
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

.examples-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.example-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.example-text {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.remove-example {
  flex-shrink: 0;
}

.add-example {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.add-example input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
}

.add-example input:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.prompt-section {
  margin-top: 8px;
}

.prompt-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  min-height: 120px;
  background: #f8f9fa;
  color: #333;
}

.prompt-textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 模态框样式 */
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

.create-modal {
  max-width: 700px;
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
  padding: 20px;
  overflow-y: auto;
}

.modal-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0 0 20px 0;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  background: #fafafa;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.form-row {
  margin-bottom: 16px;
}

.form-item.full-width {
  width: 100%;
}

.form-section {
  margin-bottom: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.section-header {
  padding: 12px 16px;
  background: #fafafa;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
}

.section-header:hover {
  background: #f0f0f0;
}

.section-toggle {
  font-size: 12px;
  color: #666;
  transition: transform 0.3s;
}

.section-content {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.delete-warning {
  text-align: center;
  padding: 24px;
}

.delete-warning svg {
  margin-bottom: 16px;
}

.delete-warning p {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #333;
}

.delete-hint {
  font-size: 12px;
  color: #999;
  margin-top: 8px !important;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-state p {
  margin: 0 0 8px 0;
  font-size: 14px;
}

/* 滚动容器样式 */
.scrollable-container {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

.scrollable-container::-webkit-scrollbar {
  width: 6px;
}

.scrollable-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.scrollable-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.scrollable-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 知识块信息样式 */
.kb-name-title,
.kb-id-title {
  font-size: 16px;
  font-weight: 400;
  color: #666;
  margin-left: 8px;
}

/* 表单样式 */
.form-item {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.required {
  color: #ff4d4f;
}

.form-input,
.form-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 可滚动选择框样式 */
.select-container {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.scrollable-select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%23666' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 16px;
  cursor: pointer;
  width: 100%;
  /* 确保下拉菜单在选项过多时滚动 */
  /* 注意：浏览器会自动处理下拉菜单的滚动 */
}

/* 为下拉选项添加滚动效果 */
.scrollable-select option {
  padding: 8px 12px;
  font-size: 14px;
}

/* 美化下拉箭头 */
.scrollable-select::-ms-expand {
  display: none;
}

/* 自定义下拉选择框样式 */
.custom-select {
  position: relative;
  width: 100%;
  cursor: pointer;
}

.custom-select-selected {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background-color: #ffffff;
  font-size: 14px;
  transition: all 0.3s;
}

.custom-select-selected:hover {
  border-color: #1890ff;
}

.custom-select-selected:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.select-arrow {
  font-size: 12px;
  color: #666;
  transition: transform 0.3s;
}

.custom-select-options {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #d9d9d9;
  border-radius: 0 0 4px 4px;
  background-color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  margin-top: -1px;
}

.custom-select-option {
  padding: 8px 12px;
  font-size: 14px;
  color: #333;
  transition: background-color 0.3s;
}

.custom-select-option:hover {
  background-color: #f5f7fa;
}

.custom-select-option.selected {
  background-color: #e6f7ff;
  color: #1890ff;
}

/* 自定义下拉菜单滚动条样式 */
.custom-select-options::-webkit-scrollbar {
  width: 6px;
}

.custom-select-options::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.custom-select-options::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.custom-select-options::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  box-sizing: border-box;
  transition: all 0.3s;
}

.form-textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 开关样式 */
.form-switch {
  display: inline-flex;
  align-items: center;
}

.form-switch input[type="checkbox"] {
  display: none;
}

.switch-label {
  display: block;
  width: 44px;
  height: 24px;
  background-color: #d9d9d9;
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: all 0.3s;
}

.switch-label::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background-color: white;
  border-radius: 50%;
  transition: all 0.3s;
}

.form-switch input[type="checkbox"]:checked + .switch-label {
  background-color: #1890ff;
}

.form-switch input[type="checkbox"]:checked + .switch-label::after {
  transform: translateX(20px);
}

/* 按钮样式 */
.btn {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-primary {
  background-color: #1890ff;
  color: white;
  border-color: #1890ff;
}

.btn-primary:hover {
  background-color: #40a9ff;
  border-color: #40a9ff;
}

.btn-secondary {
  background-color: white;
  color: #333;
  border-color: #d9d9d9;
}

.btn-secondary:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.btn-small {
  padding: 6px 12px;
  font-size: 12px;
}

/* 示例问题样式 */
.example-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.example-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background-color: #f0f0f0;
  border-radius: 12px;
  font-size: 12px;
  color: #666;
  border: 1px solid #d9d9d9;
}

.tag-remove {
  cursor: pointer;
  color: #999;
  font-size: 14px;
  line-height: 1;
}

.tag-remove:hover {
  color: #ff4d4f;
}

.example-input {
  display: flex;
  gap: 8px;
  align-items: center;
}

.example-input-field {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
}

.example-input-field:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.hidden-textarea {
  display: none;
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
              <svg width="20" height="20" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg">
                <path d="M979.2 158.848a32 32 0 0 0-38.4-23.936l-166.848 38.656a32 32 0 0 0-15.808 53.408L794.56 264.64l-194.56 173.568-101.024-95.68a32 32 0 0 0-45.152 1.152l-216.736 227.264A64 64 0 1 0 288 633.6c0-6.944-1.376-13.472-3.424-19.712l193.536-202.944 99.232 93.952a32 32 0 0 0 43.296 0.64L839.04 310.72l41.504 42.976a32 32 0 0 0 53.92-13.92l44.448-165.408a31.84 31.84 0 0 0 0.288-15.52z" fill="currentColor"/>
                <path d="M928 450.464a32 32 0 0 0-32 32V736a32 32 0 0 1-32 32H160a32 32 0 0 1-32-32V160a32 32 0 0 1 32-32h530.656a32 32 0 0 0 0-64H160a96 96 0 0 0-96 96v576a96 96 0 0 0 96 96h704a96 96 0 0 0 96-96v-253.536a32 32 0 0 0-32-32zM912 896h-800a32 32 0 0 0 0 64h800a32 32 0 0 0 0-64z" fill="currentColor"/>
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
              <svg width="18" height="18" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg">
                <path d="M931.507892 0A78.65427 78.65427 0 0 1 1010.162162 78.65427v314.644757a78.65427 78.65427 0 0 1-78.65427 78.65427H92.492108A78.65427 78.65427 0 0 1 13.837838 393.271351V78.65427A78.65427 78.65427 0 0 1 92.492108 0h839.015784z m0 55.351351H92.492108A23.302919 23.302919 0 0 0 69.189189 78.65427v314.644757c0 12.869189 10.43373 23.302919 23.302919 23.302919h839.015784A23.302919 23.302919 0 0 0 954.810811 393.271351V78.65427A23.302919 23.302919 0 0 0 931.507892 55.351351z m0 470.486487A78.65427 78.65427 0 0 1 1010.162162 604.492108v314.644757a78.65427 78.65427 0 0 1-78.65427 78.65427H92.492108A78.65427 78.65427 0 0 1 13.837838 919.109189V604.492108A78.65427 78.65427 0 0 1 92.492108 525.837838h839.015784z m0 55.351351H92.492108a23.302919 23.302919 0 0 0-23.302919 23.302919v314.644757c0 12.869189 10.43373 23.302919 23.302919 23.302919h839.015784A23.302919 23.302919 0 0 0 954.810811 919.109189V604.492108a23.302919 23.302919 0 0 0-23.302919-23.302919z" p-id="9678" fill="white"></path><path d="M249.081081 305.235027a69.189189 69.189189 0 1 1 0-138.378378 69.189189 69.189189 0 0 1 0 138.378378z m0 525.754811a69.189189 69.189189 0 1 1 0-138.378379 69.189189 69.189189 0 0 1 0 138.378379z" p-id="9679" fill="white"></path>
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
              <svg width="20" height="20" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg">
                <path d="M207.36 656H110.08a96 96 0 0 1-96-96V462.72a96 96 0 0 1 96-96h97.28a96 96 0 0 1 96 96v97.28a96 96 0 0 1-96 96zM110.08 430.72a32 32 0 0 0-32 32v97.28a32 32 0 0 0 32 32h97.28a32 32 0 0 0 32-32V462.72a32 32 0 0 0-32-32zM920.96 296.96h-97.92a96 96 0 0 1-96-96V103.68a96 96 0 0 1 96-96h97.92a96 96 0 0 1 95.36 96v97.28a96 96 0 0 1-95.36 96z m-97.92-225.28a32 32 0 0 0-32 32v97.28a32 32 0 0 0 32 32h97.92a31.36 31.36 0 0 0 31.36-32V103.68a31.36 31.36 0 0 0-31.36-32zM920.96 656h-97.92a96 96 0 0 1-96-96V462.72a96 96 0 0 1 96-96h97.92a96 96 0 0 1 95.36 96v97.28a96 96 0 0 1-95.36 96z m-97.92-225.28a32 32 0 0 0-32 32v97.28a32 32 0 0 0 32 32h97.92a31.36 31.36 0 0 0 31.36-32V462.72a31.36 31.36 0 0 0-31.36-32zM920.96 1015.04h-97.92a96 96 0 0 1-96-96v-97.28a96 96 0 0 1 96-96h97.92a96 96 0 0 1 95.36 96v97.28a96 96 0 0 1-95.36 96z m-97.92-225.28a32 32 0 0 0-32 32v97.28a32 32 0 0 0 32 32h97.92a31.36 31.36 0 0 0 31.36-32v-97.28a31.36 31.36 0 0 0-31.36-32zM673.92 912.64H478.08a32 32 0 0 1-32-32V152.32a32 32 0 0 1 32-32h195.84a32 32 0 0 1 0 64H512v664.32h163.84a32 32 0 0 1 32 32 32 32 0 0 1-33.92 32z" p-id="17194" fill="white"></path><path d="M664.96 548.48h-320a32.64 32.64 0 0 1-32-32 32 32 0 0 1 32-32h320a32 32 0 0 1 32 32 32 32 0 0 1-32 32z" p-id="17195" fill="white"></path>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">意图管理</span>
          </div>
          <div 
            class="menu-item"
            :class="{ active: activeMenu === 'samples' }"
            @click="setActiveMenu('samples')"
          >
            <span class="menu-icon">
              <svg width="20" height="20" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg">
                <path d="M544 789.333h105.173a10.667 10.667 0 0 0 10.667-10.666v-0.363L656.608 684a32 32 0 0 1 14.293-27.765C750.933 603.189 800 513.792 800 416c0-159.061-128.939-288-288-288S224 256.939 224 416c0 97.813 49.099 187.232 129.141 240.267a32 32 0 0 1 14.315 25.578l3.339 97.184a10.667 10.667 0 0 0 10.666 10.304H480v-216.64l-76.64-81.429a32 32 0 0 1 46.613-43.861l63.36 67.328 72.694-68.662a32 32 0 0 1 43.946 46.528L544 573.792v215.541zM864 416a351.53 351.53 0 0 1-142.827 283.147l2.635 76.96a74.667 74.667 0 0 1-74.635 77.226H381.44a74.667 74.667 0 0 1-74.624-72.106l-2.784-81.195A351.541 351.541 0 0 1 160 416c0-194.4 157.6-352 352-352s352 157.6 352 352zM416 960a32 32 0 0 1 0-64h192a32 32 0 0 1 0 64H416z" p-id="5447" fill="white"></path>
              </svg>
            </span>
            <span class="menu-text" v-show="!sidebarCollapsed">示例管理</span>
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
              <svg width="20" height="20" viewBox="0 0 1035 1024" version="1.1" xmlns="http://www.w3.org/2000/svg">
                <path d="M540.842667 932.977778H170.666667a170.666667 170.666667 0 0 1-170.666667-170.666667V284.444444a170.666667 170.666667 0 0 1 170.666667-170.666666h682.666666a170.666667 170.666667 0 0 1 170.666667 170.666666v113.379556a34.133333 34.133333 0 0 1-68.266667 0V284.444444a102.4 102.4 0 0 0-102.4-102.4H170.666667a102.4 102.4 0 0 0-102.4 102.4v477.866667a102.4 102.4 0 0 0 102.4 102.4h370.176a34.133333 34.133333 0 0 1 0 68.266667z" p-id="20352" fill="white"></path><path d="M200.988444 365.112889m34.133334 0l272.782222 0q34.133333 0 34.133333 34.133333l0-0.056889q0 34.133333-34.133333 34.133334l-272.782222 0q-34.133333 0-34.133334-34.133334l0 0.056889q0-34.133333 34.133334-34.133333Z" p-id="20353" fill="white"></path><path d="M200.931556 607.288889m34.133333 0l136.362667 0q34.133333 0 34.133333 34.133333l0-0.056889q0 34.133333-34.133333 34.133334l-136.362667 0q-34.133333 0-34.133333-34.133334l0 0.056889q0-34.133333 34.133333-34.133333Z" p-id="20354" fill="white"></path><path d="M1019.221333 721.692444l-93.184 168.96a37.717333 37.717333 0 0 1-32.995555 19.569778h-185.002667a37.717333 37.717333 0 0 1-32.995555-19.569778l-93.184-168.96a38.513778 38.513778 0 0 1 0-37.148444l93.184-168.96a37.717333 37.717333 0 0 1 32.995555-19.569778h185.059556a37.717333 37.717333 0 0 1 32.995555 19.569778l93.184 168.96a38.513778 38.513778 0 0 1-0.056889 37.148444z m-28.444444-15.928888a5.688889 5.688889 0 0 0 0-5.290667l-93.013333-168.96a5.347556 5.347556 0 0 0-4.721778-2.787556h-184.888889a5.347556 5.347556 0 0 0-4.721778 2.787556l-93.013333 168.96a5.688889 5.688889 0 0 0 0 5.290667l93.013333 168.96a5.347556 5.347556 0 0 0 4.721778 2.787555h184.718222a5.347556 5.347556 0 0 0 4.721778-2.787555z m-191.488 82.432a87.210667 87.210667 0 1 1 87.210667-87.210667 87.210667 87.210667 0 0 1-87.381334 87.267555z m0-32.711112a54.499556 54.499556 0 1 0-54.499556-54.499555 54.499556 54.499556 0 0 0 54.328889 54.556444z" p-id="20355" fill="white"></path>
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
                  v-if="userInfo?.avatar && userInfo.avatar.trim()"
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
        <span v-else class="current">{{ activeMenu === 'overview' ? '数据概览' : activeMenu === 'knowledge' ? '知识库管理' : activeMenu === 'users' ? '用户管理' : activeMenu === 'intent' ? '意图管理' : activeMenu === 'samples' ? '示例管理' : '系统设置' }}</span>
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
          <DataOverview />
        </div>
        <div v-else-if="activeMenu === 'intent'" class="page-content intent-page">
          <!-- 页面标题 -->
          <div class="page-header">
            <div class="header-left">
              <h1 class="page-title">意图树配置</h1>
              <p class="page-subtitle">配置意图层级、类型和节点关系</p>
            </div>
            <div class="header-actions">
              <BaseButton type="secondary" size="small" @click="refreshIntentTree" class="refresh-button">
                <svg width="16" height="16" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg">
                  <path d="M512 85.333333c102.869333 0 199.509333 36.693333 275.029333 100.437334l93.866667-94.037334a21.333333 21.333333 0 0 1 36.437333 15.061334V384a21.333333 21.333333 0 0 1-21.333333 21.333333h-276.693333a21.333333 21.333333 0 0 1-15.104-36.394666l122.325333-122.496a341.333333 341.333333 0 1 0 118.314667 341.632 42.666667 42.666667 0 1 1 83.2 18.901333A426.794667 426.794667 0 0 1 512 938.666667C276.352 938.666667 85.333333 747.648 85.333333 512S276.352 85.333333 512 85.333333z" fill="currentColor"/>
                </svg>
                刷新
              </BaseButton>
              <BaseButton type="primary" size="small" @click="openCreateRootModal" class="create-button">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="5" x2="12" y2="19"/>
                  <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                新建根节点
              </BaseButton>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="intent-management-container">
            <!-- 左侧意图树结构 -->
            <div class="intent-tree-panel">
              <div class="panel-header">
                <h3 class="panel-title">意图树结构</h3>
                <p class="panel-subtitle">点击节点查看详情或进行编辑</p>
              </div>
              <div class="panel-content">
                <div v-if="intentLoading" class="loading-state">
                  <div class="loading-spinner"></div>
                  <p>加载中...</p>
                </div>
                <div v-else-if="intentTree.length === 0" class="empty-state">
                  <div class="empty-icon">📁</div>
                  <p>暂无意图节点</p>
                  <p>点击右上角"新建根节点"开始创建</p>
                </div>
                <div v-else class="tree-content">
                  <div v-for="node in intentTree" :key="node.id" class="tree-node">
                    <div
                      class="node-item"
                      :class="{ selected: selectedNode?.id === node.id }"
                      @click="selectNode(node)"
                    >
                      <div
                        class="node-toggle"
                        v-if="node.children && node.children.length > 0"
                        @click.stop="toggleNode(node)"
                      >
                        {{ expandedNodes.has(node.id) ? '▼' : '▶' }}
                      </div>
                      <div class="node-toggle" v-else></div>
                      <div class="node-name">{{ node.name }}</div>
                      <div class="node-badges">
                        <span class="badge level-badge">{{ levelOptionsTag.find(opt => opt.value === node.level)?.label }}</span>
                        <span class="badge kind-badge">{{ kindOptionsTag.find(opt => opt.value === node.kind)?.label }}</span>
                      </div>
                    </div>
                    <div v-if="node.children && node.children.length > 0 && expandedNodes.has(node.id)" class="node-children">
                      <div v-for="child in node.children" :key="child.id" class="tree-node">
                        <div
                          class="node-item"
                          :class="{ selected: selectedNode?.id === child.id }"
                          @click="selectNode(child)"
                        >
                          <div
                            class="node-toggle"
                            v-if="child.children && child.children.length > 0"
                            @click.stop="toggleNode(child)"
                          >
                            {{ expandedNodes.has(child.id) ? '▼' : '▶' }}
                          </div>
                          <div class="node-toggle" v-else></div>
                          <div class="node-name">{{ child.name }}</div>
                          <div class="node-badges">
                            <span class="badge level-badge">{{ levelOptionsTag.find(opt => opt.value === child.level)?.label }}</span>
                            <span class="badge kind-badge">{{ kindOptionsTag.find(opt => opt.value === child.kind)?.label }}</span>
                          </div>
                        </div>
                        <div v-if="child.children && child.children.length > 0 && expandedNodes.has(child.id)" class="node-children">
                          <div v-for="grandchild in child.children" :key="grandchild.id" class="tree-node">
                            <div
                              class="node-item"
                              :class="{ selected: selectedNode?.id === grandchild.id }"
                              @click="selectNode(grandchild)"
                            >
                              <div class="node-toggle"></div>
                              <div class="node-name">{{ grandchild.name }}</div>
                              <div class="node-badges">
                                <span class="badge level-badge">{{ levelOptionsTag.find(opt => opt.value === grandchild.level)?.label }}</span>
                                <span class="badge kind-badge">{{ kindOptionsTag.find(opt => opt.value === grandchild.kind)?.label }}</span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧节点详情 -->
            <div class="node-detail-panel">
              <div class="panel-header">
                <div>
                  <h3 class="panel-title">节点详情</h3>
                  <p class="panel-subtitle">查看并管理当前选择的节点</p>
                </div>
                <div class="panel-actions" v-if="selectedNode">
                  <BaseButton type="primary" size="small" @click="openCreateChildModal" class="action-button">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="12" y1="5" x2="12" y2="19"/>
                      <line x1="5" y1="12" x2="19" y2="12"/>
                    </svg>
                    新增子节点
                  </BaseButton>
                  <BaseButton type="secondary" size="small" @click="openEditModal" class="action-button">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                    </svg>
                    编辑节点
                  </BaseButton>
                  <BaseButton type="danger" size="small" @click="showIntentDeleteConfirm = true" class="action-button">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"/>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                      <line x1="10" y1="11" x2="10" y2="17"/>
                      <line x1="14" y1="11" x2="14" y2="17"/>
                    </svg>
                    删除节点
                  </BaseButton>
                </div>
              </div>
              <div class="panel-content">
                <div v-if="!selectedNode" class="empty-state">
                  <div class="empty-icon">📋</div>
                  <p>请选择一个节点查看详情</p>
                </div>
                <div v-else class="node-details">
                  <!-- 节点基本信息 -->
                  <div class="node-header-section">
                    <h3 class="node-title">{{ selectedNode.name }}</h3>
                    <div class="node-meta">
                      <span class="badge level-badge">{{ levelOptionsTag.find(opt => opt.value === selectedNode.level)?.label }}</span>
                      <span class="badge kind-badge">{{ kindOptionsTag.find(opt => opt.value === selectedNode.kind)?.label }}</span>
                      <span class="badge status-badge" :class="selectedNode.enabled === 1 ? 'status-enabled' : 'status-disabled'">
                        {{ selectedNode.enabled === 1 ? '启用' : '禁用' }}
                      </span>
                      <span class="node-code" style="font-weight: bold;">{{ selectedNode.intentCode }}</span>
                    </div>
                  </div>

                  <!-- 节点操作 - 已移动到面板头部 -->

                  <!-- 详细信息 -->
                  <div class="detail-section">
                    <h4 class="section-title">基本信息</h4>
                    <div class="info-grid">
                      <div class="info-item">
                        <span class="info-label">父节点</span>
                        <span class="info-value">{{ selectedNode.parentCode || 'ROOT' }}</span>
                      </div>
                      <div class="info-item">
                        <span class="info-label">排序</span>
                        <span class="info-value">{{ selectedNode.sortOrder }}</span>
                      </div>
                      <div class="info-item">
                        <span class="info-label">Collection</span>
                        <span class="info-value">{{ selectedNode.collectionName || '默认' }}</span>
                      </div>
                      <div class="info-item">
                        <span class="info-label">节点 TopK</span>
                        <span class="info-value">{{ selectedNode.topK || '默认（全局）' }}</span>
                      </div>
                    </div>
                  </div>

                  <div class="detail-section">
                    <h4 class="section-title">描述</h4>
                    <div class="description-content">
                      {{ selectedNode.description || '无描述' }}
                    </div>
                  </div>

                  <div class="detail-section">
                    <h4 class="section-title">示例问题</h4>
                    <div class="example-tags">
                      <span
                        v-for="(example, index) in parseExamples(selectedNode.examples)"
                        :key="index"
                        class="example-tag"
                        @dblclick="copyToClipboard(example)"
                      >
                        {{ example }}
                      </span>
                      <div v-if="!selectedNode.examples || parseExamples(selectedNode.examples).length === 0" class="empty-state">
                        <p>暂无示例问题</p>
                      </div>
                    </div>
                  </div>

                  <!-- 提示词模板 - 已隐藏 -->
                  <!-- <div class="detail-section">
                    <h4 class="section-title">提示词模板</h4>
                    <div class="prompt-section">
                      <textarea
                        class="prompt-textarea"
                        :value="selectedNode.promptTemplate || '无模板'"
                        readonly
                      ></textarea>
                    </div>
                  </div> -->
                </div>
              </div>
            </div>
          </div>

          <!-- 新建节点模态框 -->
          <div v-if="showIntentCreateModal" class="modal-overlay">
            <div class="modal create-modal">
              <div class="modal-header">
                <h3 class="modal-title">新建意图节点</h3>
                <BaseButton type="secondary" size="small" @click="showIntentCreateModal = false" class="close-button">&times;</BaseButton>
              </div>
              <ScrollableContainer>
                <div class="modal-body">
                  <p class="modal-subtitle">配置意图节点的层级、类型与描述信息</p>
                  <form @submit.prevent="handleIntentCreate">
                    <div class="form-grid">
                      <div class="form-item">
                        <label class="form-label">节点名称 <span class="required">*</span></label>
                        <input
                          type="text"
                          v-model="intentCreateForm.name"
                          placeholder="例如：OA系统"
                          class="form-input"
                          required
                        />
                      </div>
                      <div class="form-item">
                        <label class="form-label">意图标识</label>
                        <input
                          type="text"
                          v-model="intentCreateForm.intentCode"
                          placeholder="例如：biz-oa"
                          class="form-input"
                        />
                      </div>
                      <div class="form-item">
                        <label class="form-label">层级 <span class="required">*</span></label>
                        <select
                          v-model="intentCreateForm.level"
                          class="form-select"
                          required
                        >
                          <option v-for="option in levelOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                          </option>
                        </select>
                      </div>
                      <div class="form-item">
                        <label class="form-label">类型 <span class="required">*</span></label>
                        <select
                          v-model="intentCreateForm.kind"
                          class="form-select"
                          required
                        >
                          <option v-for="option in kindOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                          </option>
                        </select>
                      </div>
                    </div>

                    <!-- 父节点 -->
                    <div class="form-row">
                      <div class="form-item full-width">
                        <label class="form-label">父节点</label>
                        <div class="custom-select" @click="toggleParentSelect">
                          <div class="custom-select-selected">
                            {{ getParentOptionLabel(intentCreateForm.parentCode) }}
                            <span class="select-arrow">{{ parentSelectOpen ? '▼' : '▶' }}</span>
                          </div>
                          <div v-if="parentSelectOpen" class="custom-select-options">
                            <div
                              v-for="option in parentOptions"
                              :key="option.value"
                              class="custom-select-option"
                              :class="{ selected: intentCreateForm.parentCode === option.value }"
                              @click="selectParentOption(option.value)"
                            >
                              {{ option.label }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 知识库 -->
                    <div class="form-row">
                      <div class="form-item full-width">
                        <label class="form-label">知识库</label>
                        <select
                          v-model="intentCreateForm.kbId"
                          class="form-select"
                        >
                          <option value="" disabled selected>请选择知识库</option>
                          <option v-for="kb in knowledgeList" :key="kb.kbId" :value="kb.kbId">
                            {{ kb.name }} ({{ kb.collection }})
                          </option>
                        </select>
                      </div>
                    </div>

                    <!-- 描述与示例 -->
                    <div class="form-section">
                      <div class="section-header" @click="descriptionExpanded = !descriptionExpanded">
                        <span class="section-title">描述与示例</span>
                        <span class="section-toggle">{{ descriptionExpanded ? '▼' : '▶' }}</span>
                      </div>
                      <div v-if="descriptionExpanded" class="section-content">
                        <div class="form-item">
                          <label class="form-label">描述</label>
                          <textarea
                            v-model="intentCreateForm.description"
                            placeholder="节点的语义说明与说明场景"
                            class="form-textarea"
                            rows="4"
                          ></textarea>
                        </div>
                        <div class="form-item">
                          <label class="form-label">示例问题</label>
                          <!-- 标签显示区域 -->
                          <div class="example-tags">
                            <span
                              v-for="tag in exampleTags"
                              :key="tag"
                              class="example-tag"
                            >
                              {{ tag }}
                              <span class="tag-remove" @click="removeExampleTag(tag)">×</span>
                            </span>
                          </div>
                          <!-- 输入区域 -->
                          <div class="example-input">
                            <input
                              type="text"
                              v-model="newExampleTag"
                              placeholder="输入示例问题并按回车添加"
                              @keyup.enter="addExampleTag"
                              class="example-input-field"
                            />
                            <BaseButton type="primary" size="small" @click="addExampleTag">添加</BaseButton>
                          </div>
                          <!-- 隐藏的textarea用于保持数据绑定 -->
                          <textarea
                            v-model="intentCreateForm.examplesText"
                            class="hidden-textarea"
                            @input="syncExampleTags"
                          ></textarea>
                        </div>
                      </div>
                    </div>

                    <!-- Prompt 配置 -->
                    <div class="form-section">
                      <div class="section-header" @click="promptExpanded = !promptExpanded">
                        <span class="section-title">Prompt 配置</span>
                        <span class="section-toggle">{{ promptExpanded ? '▼' : '▶' }}</span>
                      </div>
                      <div v-if="promptExpanded" class="section-content">
                        <div class="form-item">
                          <label class="form-label">短规则片段（可选）</label>
                          <textarea
                            v-model="intentCreateForm.promptSnippet"
                            placeholder="多意图场景下的特定规则，会添加到整体提示词中"
                            class="form-textarea"
                            rows="3"
                          ></textarea>
                        </div>
                        <div class="form-item">
                          <label class="form-label">Prompt模板（可选）</label>
                          <textarea
                            v-model="intentCreateForm.promptTemplate"
                            placeholder="场景用的完整Prompt模板，KB和MCP节点都可配置"
                            class="form-textarea"
                            rows="5"
                          ></textarea>
                        </div>
                      </div>
                    </div>

                    <!-- 高级设置 -->
                    <div class="form-section">
                      <div class="section-header" @click="advancedExpanded = !advancedExpanded">
                        <span class="section-title">高级设置</span>
                        <span class="section-toggle">{{ advancedExpanded ? '▼' : '▶' }}</span>
                      </div>
                      <div v-if="advancedExpanded" class="section-content">
                        <div class="form-grid">
                          <div class="form-item">
                            <label class="form-label">节点 TopK（可选）</label>
                            <input
                              type="number"
                              v-model="intentCreateForm.topK"
                              placeholder="留空则使用全局 TopK"
                              class="form-input"
                            />
                          </div>
                          <div class="form-item">
                            <label class="form-label">排序</label>
                            <input
                              type="number"
                              v-model="intentCreateForm.sortOrder"
                              placeholder="0"
                              class="form-input"
                            />
                          </div>
                        </div>
                        <div class="form-item">
                          <label class="form-label">启用节点</label>
                          <div class="form-switch">
                            <input
                              type="checkbox"
                              v-model="intentCreateForm.enabled"
                              id="enabled-switch"
                            />
                            <label for="enabled-switch" class="switch-label"></label>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div class="form-actions">
                      <BaseButton type="secondary" @click="showIntentCreateModal = false">取消</BaseButton>
                      <BaseButton type="primary" buttonType="submit">创建</BaseButton>
                    </div>
                  </form>
                </div>
              </ScrollableContainer>
            </div>
          </div>

          <!-- 编辑节点模态框 -->
          <div v-if="showIntentEditModal" class="modal-overlay">
            <div class="modal">
              <div class="modal-header">
                <h3 class="modal-title">编辑节点</h3>
                <BaseButton type="secondary" size="small" @click="showIntentEditModal = false" class="close-button">&times;</BaseButton>
              </div>
              <ScrollableContainer>
                <div class="modal-body">
                  <p class="modal-subtitle">配置意图节点的层级、类型与描述信息</p>
                  <form @submit.prevent="handleIntentUpdate">
                    <div class="form-grid">
                      <div class="form-item">
                        <label class="form-label">节点名称 <span class="required">*</span></label>
                        <input
                          type="text"
                          v-model="intentCreateForm.name"
                          placeholder="例如：OA系统"
                          class="form-input"
                          required
                        />
                      </div>
                      <div class="form-item">
                        <label class="form-label">意图标识</label>
                        <input
                          type="text"
                          v-model="intentCreateForm.intentCode"
                          placeholder="例如：biz-oa"
                          class="form-input"
                        />
                      </div>
                      <div class="form-item">
                        <label class="form-label">层级 <span class="required">*</span></label>
                        <select
                          v-model="intentCreateForm.level"
                          class="form-select"
                          required
                        >
                          <option v-for="option in levelOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                          </option>
                        </select>
                      </div>
                      <div class="form-item">
                        <label class="form-label">类型 <span class="required">*</span></label>
                        <select
                          v-model="intentCreateForm.kind"
                          class="form-select"
                          required
                        >
                          <option v-for="option in kindOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                          </option>
                        </select>
                      </div>
                    </div>

                    <!-- 父节点 -->
                    <div class="form-row">
                      <div class="form-item full-width">
                        <label class="form-label">父节点</label>
                        <div class="custom-select" @click="toggleParentSelect">
                          <div class="custom-select-selected">
                            {{ getParentOptionLabel(intentCreateForm.parentCode) }}
                            <span class="select-arrow">{{ parentSelectOpen ? '▼' : '▶' }}</span>
                          </div>
                          <div v-if="parentSelectOpen" class="custom-select-options">
                            <div
                              v-for="option in parentOptions"
                              :key="option.value"
                              class="custom-select-option"
                              :class="{ selected: intentCreateForm.parentCode === option.value }"
                              @click="selectParentOption(option.value)"
                            >
                              {{ option.label }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 知识库 -->
                    <div class="form-row">
                      <div class="form-item full-width">
                        <label class="form-label">Collection名称</label>
                        <select
                          v-model="intentCreateForm.kbId"
                          class="form-select"
                        >
                          <option value="">请选择知识库</option>
                          <option v-for="kb in knowledgeList" :key="kb.kbId" :value="kb.kbId">
                            {{ kb.collection }}
                          </option>
                        </select>
                      </div>
                    </div>

                    <!-- 描述与示例 -->
                    <div class="form-section">
                      <div class="section-header" @click="descriptionExpanded = !descriptionExpanded">
                        <span class="section-title">描述与示例</span>
                        <span class="section-toggle">{{ descriptionExpanded ? '▼' : '▶' }}</span>
                      </div>
                      <div v-if="descriptionExpanded" class="section-content">
                        <div class="form-item">
                          <label class="form-label">描述</label>
                          <textarea
                            v-model="intentCreateForm.description"
                            placeholder="节点的语义说明与说明场景"
                            class="form-textarea"
                            rows="4"
                          ></textarea>
                        </div>
                        <div class="form-item">
                          <label class="form-label">示例问题</label>
                          <!-- 标签显示区域 -->
                          <div class="example-tags">
                            <span
                              v-for="tag in exampleTags"
                              :key="tag"
                              class="example-tag"
                            >
                              {{ tag }}
                              <span class="tag-remove" @click="removeExampleTag(tag)">×</span>
                            </span>
                          </div>
                          <!-- 输入区域 -->
                          <div class="example-input">
                            <input
                              type="text"
                              v-model="newExampleTag"
                              placeholder="输入示例问题并按回车添加"
                              @keyup.enter="addExampleTag"
                              class="example-input-field"
                            />
                            <BaseButton type="primary" size="small" @click="addExampleTag">添加</BaseButton>
                          </div>
                          <!-- 隐藏的textarea用于保持数据绑定 -->
                          <textarea
                            v-model="intentCreateForm.examplesText"
                            class="hidden-textarea"
                            @input="syncExampleTags"
                          ></textarea>
                        </div>
                      </div>
                    </div>

                    <!-- Prompt 配置 -->
                    <div class="form-section">
                      <div class="section-header" @click="promptExpanded = !promptExpanded">
                        <span class="section-title">Prompt 配置</span>
                        <span class="section-toggle">{{ promptExpanded ? '▼' : '▶' }}</span>
                      </div>
                      <div v-if="promptExpanded" class="section-content">
                        <div class="form-item">
                          <label class="form-label">短规则片段（可选）</label>
                          <textarea
                            v-model="intentCreateForm.promptSnippet"
                            placeholder="多意图场景下的特定规则，会添加到整体提示词中"
                            class="form-textarea"
                            rows="3"
                          ></textarea>
                        </div>
                        <div class="form-item">
                          <label class="form-label">Prompt模板（可选）</label>
                          <textarea
                            v-model="intentCreateForm.promptTemplate"
                            placeholder="场景用的完整Prompt模板，KB和MCP节点都可配置"
                            class="form-textarea"
                            rows="5"
                          ></textarea>
                        </div>
                      </div>
                    </div>

                    <!-- 高级设置 -->
                    <div class="form-section">
                      <div class="section-header" @click="advancedExpanded = !advancedExpanded">
                        <span class="section-title">高级设置</span>
                        <span class="section-toggle">{{ advancedExpanded ? '▼' : '▶' }}</span>
                      </div>
                      <div v-if="advancedExpanded" class="section-content">
                        <div class="form-grid">
                          <div class="form-item">
                            <label class="form-label">节点 TopK（可选）</label>
                            <input
                              type="number"
                              v-model="intentCreateForm.topK"
                              placeholder="留空则使用全局 TopK"
                              class="form-input"
                            />
                          </div>
                          <div class="form-item">
                            <label class="form-label">排序</label>
                            <input
                              type="number"
                              v-model="intentCreateForm.sortOrder"
                              placeholder="数值越小越靠前"
                              class="form-input"
                            />
                          </div>
                        </div>
                        <div class="form-item">
                          <label class="form-label">状态</label>
                          <div class="form-switch">
                            <input
                              type="checkbox"
                              v-model="intentCreateForm.enabled"
                              id="enabled-switch"
                            />
                            <label for="enabled-switch" class="switch-label"></label>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div class="form-actions">
                      <BaseButton type="secondary" @click="showIntentEditModal = false">取消</BaseButton>
                      <BaseButton type="primary" buttonType="submit">保存</BaseButton>
                    </div>
                  </form>
                </div>
              </ScrollableContainer>
            </div>
          </div>

          <!-- 删除确认模态框 -->
          <div v-if="showIntentDeleteConfirm" class="modal-overlay">
            <div class="modal">
              <div class="modal-header">
                <h3 class="modal-title">确认删除</h3>
                <BaseButton type="secondary" size="small" @click="showIntentDeleteConfirm = false" class="close-button">&times;</BaseButton>
              </div>
              <div class="modal-body">
                <div class="delete-warning">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="15" y1="9" x2="9" y2="15"/>
                    <line x1="9" y1="9" x2="15" y2="15"/>
                  </svg>
                  <p>确定要删除节点 <strong>{{ selectedNode?.name }}</strong> 吗？此操作不可撤销。</p>
                  <p class="delete-hint">删除后，与此节点相关的所有子节点也将被删除。</p>
                </div>
              </div>
              <div class="modal-footer">
                <BaseButton type="secondary" @click="showIntentDeleteConfirm = false">取消</BaseButton>
                <BaseButton type="danger" @click="handleDelete">删除</BaseButton>
              </div>
            </div>
          </div>
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
                  <svg width="16" height="16" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" :class="{ 'spin': loading }">
                    <path d="M512 85.333333c102.869333 0 199.509333 36.693333 275.029333 100.437334l93.866667-94.037334a21.333333 21.333333 0 0 1 36.437333 15.061334V384a21.333333 21.333333 0 0 1-21.333333 21.333333h-276.693333a21.333333 21.333333 0 0 1-15.104-36.394666l122.325333-122.496a341.333333 341.333333 0 1 0 118.314667 341.632 42.666667 42.666667 0 1 1 83.2 18.901333A426.794667 426.794667 0 0 1 512 938.666667C276.352 938.666667 85.333333 747.648 85.333333 512S276.352 85.333333 512 85.333333z" fill="currentColor"/>
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
            <div class="data-cards" style="margin-bottom: 24px;">
              <div class="card">
                <div class="card-icon knowledge-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
                </div>
                <div class="card-content">
                  <div class="card-title">知识库数量</div>
                  <div class="card-value">{{ dashboardData.knowledgeBaseCount }}</div>
                  <div class="card-change positive">{{ (dashboardData.knowledgeBaseGrowth || 0).toFixed(3) }}% 增长</div>
                </div>
              </div>

              <div class="card">
                <div class="card-icon document-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>
                </div>
                <div class="card-content">
                  <div class="card-title">文档数量</div>
                  <div class="card-value">{{ dashboardData.documentCount }}</div>
                  <div class="card-change positive">{{ (dashboardData.documentGrowth || 0).toFixed(3) }}% 增长</div>
                </div>
              </div>

              <div class="card">
                <div class="card-icon chunk-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
                </div>
                <div class="card-content">
                  <div class="card-title">知识块数量</div>
                  <div class="card-value">{{ dashboardData.chunkCount }}</div>
                  <div class="card-change positive">{{ (dashboardData.chunkGrowth || 0).toFixed(3) }}% 增长</div>
                </div>
              </div>
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
                    <svg width="16" height="16" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg">
                      <path d="M524.8 106.666667c-106.666667 0-209.066667 42.666667-285.866667 110.933333l-8.533333-68.266667c0-25.6-21.333333-42.666667-46.933333-38.4-25.6 0-42.666667 21.333333-38.4 46.933334l8.533333 115.2c4.266667 55.466667 51.2 98.133333 106.666667 98.133333h8.533333L384 362.666667c25.6 0 42.666667-21.333333 38.4-46.933334 0-25.6-21.333333-42.666667-46.933333-38.4l-85.333334 4.266667c64-55.466667 145.066667-89.6 230.4-89.6 187.733333 0 341.333333 153.6 341.333334 341.333333s-153.6 341.333333-341.333334 341.333334-341.333333-153.6-341.333333-341.333334c0-25.6-17.066667-42.666667-42.666667-42.666666s-42.666667 17.066667-42.666666 42.666666c0 234.666667 192 426.666667 426.666666 426.666667s426.666667-192 426.666667-426.666667c4.266667-234.666667-187.733333-426.666667-422.4-426.666666z" fill="currentColor"/>
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
                        <svg width="14" height="14" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg">
                          <path d="M515.311 956.738c-59.938 0-118.089-11.739-172.84-34.894-52.876-22.363-100.357-54.373-141.125-95.14-40.77-40.771-72.779-88.252-95.141-141.126-23.153-54.747-34.893-112.898-34.893-172.84 0-59.941 11.74-118.093 34.893-172.84 22.361-52.875 54.371-100.356 95.141-141.125 40.77-40.769 88.25-72.779 141.125-95.141 54.746-23.153 112.898-34.893 172.84-34.893 59.943 0 118.096 11.74 172.843 34.893 52.875 22.362 100.355 54.373 141.126 95.141 40.771 40.772 72.78 88.254 95.138 141.125 23.154 54.75 34.896 112.902 34.896 172.841 0 59.938-11.741 118.091-34.896 172.841-22.358 52.873-54.368 100.354-95.138 141.126-40.769 40.767-88.249 72.776-141.126 95.14-54.752 23.153-112.905 34.892-172.843 34.892z m0-778.647c-184.552 0-334.697 150.122-334.697 334.647 0 184.526 150.145 334.648 334.697 334.648 184.555 0 334.701-150.122 334.701-334.648 0-184.525-150.147-334.647-334.701-334.647z" fill="currentColor"/>
                          <path d="M642.863 541.624L465.189 652.961l-0.127-0.17a38.689 38.689 0 0 1-3.944 2.12c-0.087 0.043-0.087 0.043-0.171 0.043-1.314 0.635-2.713 1.146-4.156 1.57-0.086 0.04-0.17 0.04-0.298 0.04a15.41 15.41 0 0 1-1.909 0.511c-0.084 0.042-0.168 0.042-0.338 0.084-0.679 0.128-1.399 0.256-2.078 0.382-0.085 0-0.254 0.043-0.34 0.043-0.636 0.084-1.229 0.17-1.866 0.212-0.168 0-0.339 0.043-0.551 0.043-0.721 0-1.441 0.042-2.207 0.042-18.448 0-33.462-14.974-33.462-33.465V401.104c0-18.493 15.014-33.508 33.462-33.508 7.042 0 13.532 2.164 18.919 5.811l176.444 110.279a33.437 33.437 0 0 1 16.796 29.053 33.465 33.465 0 0 1-16.5 28.885" fill="currentColor"/>
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
                    <svg width="16" height="16" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg">
                      <path d="M512 85.333333c102.869333 0 199.509333 36.693333 275.029333 100.437334l93.866667-94.037334a21.333333 21.333333 0 0 1 36.437333 15.061334V384a21.333333 21.333333 0 0 1-21.333333 21.333333h-276.693333a21.333333 21.333333 0 0 1-15.104-36.394666l122.325333-122.496a341.333333 341.333333 0 1 0 118.314667 341.632 42.666667 42.666667 0 1 1 83.2 18.901333A426.794667 426.794667 0 0 1 512 938.666667C276.352 938.666667 85.333333 747.648 85.333333 512S276.352 85.333333 512 85.333333z" fill="currentColor"/>
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
        <div v-else-if="activeMenu === 'samples'" class="page-content">
          <SampleManagement @back="setActiveMenu('overview')" />
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
/* 数据卡片样式 */
.data-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  width: 300px;
  flex-shrink: 0;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: white;
}

.knowledge-icon {
  background: linear-gradient(135deg, #1890ff, #36cfc9);
}

.document-icon {
  background: linear-gradient(135deg, #faad14, #fadb14);
}

.chunk-icon {
  background: linear-gradient(135deg, #52c41a, #73d13d);
}

.user-icon {
  background: linear-gradient(135deg, #722ed1, #9254de);
}

.card-content {
  flex: 1;
}

.card-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.card-change {
  font-size: 12px;
  font-weight: 500;
}

.card-change.positive {
  color: #52c41a;
}

.card-change.negative {
  color: #ff4d4f;
}

/* 原有样式 */
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
  background: radial-gradient(circle at top left, #374151, #111827);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1), min-width 0.4s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.4s ease;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.sidebar.collapsed {
  width: 70px;
  min-width: 70px;
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
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 500;
  color: white;
  white-space: nowrap;
  opacity: 1;
  transition: opacity 0.3s ease, width 0.3s ease;
  overflow: hidden;
}

.sidebar.collapsed .logo-text {
  opacity: 0;
  width: 0;
}

/* 侧边栏内容区域 */
.sidebar-section {
  padding: 20px 12px;
}

.sidebar-section .section-title {
  font-size: 12px;
  font-weight: 500;
  color: white !important;
  margin-bottom: 12px;
  padding: 0 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  opacity: 1;
  transition: opacity 0.3s ease 0.1s;
  white-space: nowrap;
}

.sidebar.collapsed .section-title {
  opacity: 0;
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
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.3s ease;
  color: white;
  font-size: 15px;
  position: relative;
}

.menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
}

.menu-item:hover .menu-text {
  transform: translateX(4px);
}

.menu-item.active {
  background-color: rgba(102, 126, 234, 0.2);
  color: white;
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
  transition: height 0.3s ease;
}

.menu-item:hover.active::before {
  height: 80%;
}

.sidebar.collapsed .menu-item {
  justify-content: center;
  padding: 10px;
}

.sidebar.collapsed .menu-item .menu-text {
  opacity: 0;
  width: 0;
  overflow: hidden;
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
  flex-shrink: 0;
}

.menu-text {
  white-space: nowrap;
  opacity: 1;
  transition: opacity 0.3s ease, transform 0.3s ease;
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
  border: 1px solid #b8860b;
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
  transition: background-color 0.3s ease, color 0.3s ease, border-color 0.3s ease;
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

.edit-chunk-modal {
  width: 700px;
  max-width: 95%;
}

.edit-chunk-modal .modal-body {
  max-height: 75vh;
}

.edit-chunk-modal .form-textarea {
  min-height: 300px;
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
  flex-shrink: 0;
}

.close-button:hover {
  background-color: #f0f0f0;
  color: #333;
  transform: rotate(90deg);
}

.modal-body {
  padding: 20px 28px;
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

/* 意图管理页面样式 */
.intent-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
  background: transparent;
}

/* 页面标题 */
.intent-page .page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.intent-page .header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.intent-page .page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.intent-page .page-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.intent-page .header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.intent-page .refresh-button {
  margin-right: 8px;
}

/* 内容容器 */
.intent-page .content-container {
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 600px;
  overflow: hidden;
  padding: 0 24px;
}

.intent-page .content-container > div {
  flex: 1;
  min-width: 0;
}

/* 卡片样式 */
.intent-page .card {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  width: 100%;
  height: 100%;
}

.intent-page .card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  background-color: #fafafa;
}

.intent-page .card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.intent-page .card-subtitle {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.intent-page .card-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

/* 意图树卡片 */
.intent-page .tree-card {
  min-width: 300px;
}

/* 节点详情卡片 */
.intent-page .detail-card {
  min-width: 300px;
}

/* BaseCard 自定义样式 */
/* 内容容器样式 */
.intent-page .content-container {
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 600px;
  overflow: hidden;
  padding: 0 24px;
}

/* 卡片样式 */
.intent-page .card {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  flex: 1;
  min-width: 300px;
  max-width: 50%;
}

/* 意图管理容器 */
.intent-management-container {
  display: flex;
  gap: 24px;
  height: calc(100vh - 350px);
  margin-top: 0;
}

/* 左侧意图树面板 */
.intent-tree-panel {
  flex: 1;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-width: 300px;
}

/* 右侧节点详情面板 */
.node-detail-panel {
  flex: 1;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-width: 300px;
}

/* 面板头部 */
.panel-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafafa;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
  flex: 1;
}

.panel-subtitle {
  font-size: 12px;
  color: #999;
  margin: 0;
  flex: 1;
  min-width: 200px;
}

.panel-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 面板内容 */
.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #666;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 树节点样式 */
.tree-node {
  margin-bottom: 4px;
}

.node-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 2px;
  position: relative;
}

.node-item:hover {
  background: #f5f7fa;
}

.node-item.selected {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
}

.node-toggle {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  font-size: 10px;
  color: #666;
  flex-shrink: 0;
  cursor: pointer;
  transition: transform 0.3s;
}

.node-toggle:hover {
  color: #1890ff;
}

.node-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.node-badges {
  display: flex;
  gap: 4px;
  margin-left: 4px;
  align-items: center;
}

.badge {
  padding: 1px 6px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 500;
  background: #f0f0ff;
  color: #667eea;
  border: 1px solid #e0e0ff;
}

.level-badge {
  background: #f0f0ff;
  color: #667eea;
  border: 1px solid #e0e0ff;
}

.kind-badge {
  background: #f0f0ff;
  color: #667eea;
  border: 1px solid #e0e0ff;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
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

/* 子节点缩进 */
.node-children {
  margin-left: 24px;
  margin-top: 4px;
}

/* 节点详情 */
.node-details {
  display: flex;
  flex-direction: column;
}

.node-header-section {
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.node-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
}

.node-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.node-code {
  font-size: 14px;
  color: #999;
  font-family: monospace;
  background: #f8f9fa;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
}

/* 节点操作 - 已移动到面板头部 */
.action-button {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 示例问题标签样式 */
.example-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.example-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: #f0f0ff;
  color: #667eea;
  border: 1px solid #e0e0ff;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.example-tag:hover {
  background: #e6e6ff;
  border-color: #667eea;
}

.tag-remove {
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  padding: 0 4px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.tag-remove:hover {
  background: #667eea;
  color: white;
}

.example-input {
  display: flex;
  gap: 8px;
  align-items: center;
}

.example-input-field {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.example-input-field:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.hidden-textarea {
  display: none;
}

/* 详细信息区域 */
.detail-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.description-content {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.examples-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.example-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.example-text {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.prompt-section {
  margin-top: 8px;
}

.prompt-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  min-height: 120px;
  background: #f8f9fa;
  color: #333;
  font-family: monospace;
}

.prompt-textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #999;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-state p {
  margin: 0 0 8px 0;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .intent-management-container {
    flex-direction: column;
    height: auto;
  }
  
  .intent-tree-panel {
    flex: none;
    height: 400px;
  }
  
  .node-detail-panel {
    flex: none;
    min-height: 500px;
  }
}

@media (max-width: 768px) {
  .intent-tree-panel {
    flex: 0 0 280px;
  }
  
  .panel-header {
    padding: 16px 20px;
  }
  
  .panel-content {
    padding: 16px;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
}









</style>
