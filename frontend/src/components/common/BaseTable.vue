<template>
  <div class="base-table">
    <!-- 表格头部 -->
    <div class="table-header">
      <div class="table-title" v-if="title">{{ title }}</div>
      <div class="table-actions" v-if="actions && actions.length > 0">
        <slot name="actions"></slot>
      </div>
    </div>
    
    <!-- 表格内容 -->
    <div class="table-container">
      <table class="table">
        <thead>
          <tr>
            <th v-for="column in columns" :key="column.key" :width="column.width" :class="{ 'sortable': column.sortable }">
              {{ column.title }}
              <span v-if="column.sortable" class="sort-icon">
                <svg v-if="sortField === column.key && sortOrder === 'asc'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="18 15 12 9 6 15"></polyline>
                </svg>
                <svg v-else-if="sortField === column.key && sortOrder === 'desc'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
                <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="18 15 12 9 6 15"></polyline>
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in data" :key="row.id || index" class="table-row">
            <td v-for="column in columns" :key="column.key" :class="column.className">
              <slot :name="column.key" :row="row" :index="index">
                {{ row[column.key] }}
              </slot>
            </td>
          </tr>
          <tr v-if="data.length === 0" class="empty-row">
            <td :colspan="columns.length" class="empty-cell">
              <div class="empty-content">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1.5">
                  <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                  <line x1="8" y1="21" x2="16" y2="21"></line>
                  <line x1="12" y1="17" x2="12" y2="21"></line>
                </svg>
                <p>{{ emptyText }}</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 分页 -->
    <div v-if="showPagination && total > 0" class="table-pagination">
      <div class="pagination-info">
        共 {{ total }} 条，第 {{ currentPage }} 页 / 共 {{ totalPages }} 页
      </div>
      <div class="pagination-controls">
        <!-- 每页显示条数选择器 -->
        <div class="page-size-selector">
          <label>每页:</label>
          <select 
            v-model="localPageSize" 
            class="page-size-select"
            @change="handlePageSizeChange"
          >
            <option value="10">10条</option>
            <option value="20">20条</option>
            <option value="50">50条</option>
            <option value="100">100条</option>
          </select>
        </div>
        
        <!-- 页码控制 -->
        <div class="page-navigation">
          <!-- 首页按钮 -->
          <button 
            class="page-button"
            :disabled="currentPage === 1"
            @click="handlePageChange(1)"
            title="首页"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
          </button>
          
          <!-- 上一页按钮 -->
          <button 
            class="page-button"
            :disabled="currentPage === 1"
            @click="handlePageChange(currentPage - 1)"
            title="上一页"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
          </button>
          
          <!-- 页码列表 -->
          <div class="page-numbers">
            <!-- 前面的省略号 -->
            <span v-if="showFirstEllipsis" class="page-ellipsis">...</span>
            
            <!-- 页码按钮 -->
            <button 
              v-for="page in pageNumbers" 
              :key="page"
              class="page-number-button"
              :class="{ active: page === currentPage }"
              @click="handlePageChange(page)"
            >
              {{ page }}
            </button>
            
            <!-- 后面的省略号 -->
            <span v-if="showLastEllipsis" class="page-ellipsis">...</span>
          </div>
          
          <!-- 下一页按钮 -->
          <button 
            class="page-button"
            :disabled="currentPage === totalPages"
            @click="handlePageChange(currentPage + 1)"
            title="下一页"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </button>
          
          <!-- 尾页按钮 -->
          <button 
            class="page-button"
            :disabled="currentPage === totalPages"
            @click="handlePageChange(totalPages)"
            title="尾页"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </button>
        </div>
        
        <!-- 页码快速跳转 -->
        <div class="page-jump">
          <label>跳至:</label>
          <input 
            type="number" 
            v-model="jumpPage"
            class="page-jump-input"
            @keyup.enter="handlePageJump"
            min="1"
            :max="totalPages"
          >
          <button 
            class="page-jump-button"
            @click="handlePageJump"
          >
            确定
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  columns: {
    type: Array,
    required: true
  },
  data: {
    type: Array,
    default: () => []
  },
  total: {
    type: Number,
    default: 0
  },
  pageSize: {
    type: Number,
    default: 10
  },
  currentPage: {
    type: Number,
    default: 1
  },
  showPagination: {
    type: Boolean,
    default: true
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  actions: {
    type: Array,
    default: () => []
  },
  sortField: {
    type: String,
    default: ''
  },
  sortOrder: {
    type: String,
    default: 'asc'
  }
})

const emit = defineEmits(['page-change', 'sort-change', 'page-size-change'])

// 本地状态
const localPageSize = ref(props.pageSize)
const jumpPage = ref(props.currentPage)

// 监听 props 变化
watch(() => props.pageSize, (newSize) => {
  localPageSize.value = newSize
})

watch(() => props.currentPage, (newPage) => {
  jumpPage.value = newPage
})

const totalPages = computed(() => {
  return Math.ceil(props.total / props.pageSize)
})

// 生成页码列表
const pageNumbers = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = props.currentPage
  
  // 始终显示第一页
  if (total > 0) {
    pages.push(1)
  }
  
  // 显示当前页附近的页码
  const start = Math.max(2, current - 2)
  const end = Math.min(total - 1, current + 2)
  
  // 前面的省略号
  if (start > 2) {
    pages.push('...')
  }
  
  // 中间的页码
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  
  // 后面的省略号
  if (end < total - 1) {
    pages.push('...')
  }
  
  // 始终显示最后一页
  if (total > 1) {
    pages.push(total)
  }
  
  return pages
})

// 是否显示前面的省略号
const showFirstEllipsis = computed(() => {
  return props.currentPage > 3
})

// 是否显示后面的省略号
const showLastEllipsis = computed(() => {
  return props.currentPage < totalPages.value - 2
})

const handlePageChange = (page) => {
  if (page === '...') return
  emit('page-change', page)
}

const handlePageSizeChange = () => {
  emit('page-size-change', Number(localPageSize.value))
}

const handlePageJump = () => {
  let page = Number(jumpPage.value)
  if (isNaN(page)) page = 1
  page = Math.max(1, Math.min(page, totalPages.value))
  emit('page-change', page)
}

const handleSortChange = (field) => {
  let newOrder = 'asc'
  if (props.sortField === field && props.sortOrder === 'asc') {
    newOrder = 'desc'
  }
  emit('sort-change', { field, order: newOrder })
}
</script>

<style scoped>
.base-table {
  width: 100%;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 12px 12px 0 0;
}

.table-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.table-actions {
  display: flex;
  gap: 12px;
}

.table-container {
  overflow-x: auto;
  border-bottom: 1px solid #f0f0f0;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th {
  padding: 16px 20px;
  text-align: left;
  font-weight: 600;
  font-size: 14px;
  color: #666;
  background-color: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
  position: relative;
  transition: all 0.3s ease;
}

.table th:hover {
  background-color: #f5f5f5;
}

.table td {
  padding: 16px 20px;
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.table td.content-cell {
  white-space: normal;
  width: 400px;
  min-height: 120px;
}

.table-row {
  transition: all 0.3s ease;
}

.table-row:hover {
  background-color: #fafbff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.sortable {
  cursor: pointer;
  user-select: none;
  display: flex;
  align-items: center;
  gap: 4px;
}

.sortable:hover {
  color: #667eea;
}

.sort-icon {
  display: inline-flex;
  align-items: center;
  transition: all 0.3s ease;
}

.empty-row {
  height: 240px;
}

.empty-cell {
  text-align: center;
  vertical-align: middle;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.empty-content svg {
  margin-bottom: 20px;
  opacity: 0.5;
  transition: all 0.3s ease;
}

.empty-content:hover svg {
  opacity: 0.8;
}

.empty-content p {
  color: #999;
  font-size: 14px;
  margin: 0;
  transition: all 0.3s ease;
}

.empty-content:hover p {
  color: #666;
}

.table-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 0 0 12px 12px;
  flex-wrap: wrap;
  gap: 16px;
}

.pagination-info {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  flex: 1;
  min-width: 200px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

/* 每页显示条数选择器 */
.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.page-size-select {
  padding: 6px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
}

.page-size-select:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
}

/* 页码导航 */
.page-navigation {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-button {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  background: #fff;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
}

.page-button:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.page-button:disabled {
  cursor: not-allowed;
  color: #ccc;
  border-color: #f0f0f0;
  transform: none;
  box-shadow: none;
}

/* 页码列表 */
.page-numbers {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-number-button {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  background: #fff;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-number-button:hover {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.page-number-button.active {
  background: #667eea;
  color: #fff;
  border-color: #667eea;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.page-ellipsis {
  padding: 8px 12px;
  font-size: 14px;
  color: #999;
  cursor: default;
}

/* 页码跳转 */
.page-jump {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.page-jump-input {
  padding: 6px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  width: 60px;
  text-align: center;
  transition: all 0.3s ease;
}

.page-jump-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.page-jump-button {
  padding: 6px 12px;
  border: 1px solid #e0e0e0;
  background: #fff;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.page-jump-button:hover {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .table-header {
    padding: 16px 20px;
  }
  
  .table-title {
    font-size: 16px;
  }
  
  .table th,
  .table td {
    padding: 12px 16px;
  }
  
  .table-pagination {
    padding: 16px 20px;
    flex-direction: column;
    align-items: flex-start;
  }
  
  .pagination-info {
    min-width: 100%;
  }
  
  .pagination-controls {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .page-navigation {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 8px;
  }
  
  .page-button {
    padding: 6px 10px;
    min-width: 32px;
    height: 32px;
  }
  
  .page-number-button {
    padding: 6px 10px;
    min-width: 32px;
    height: 32px;
  }
  
  .page-size-selector,
  .page-jump {
    width: 100%;
  }
  
  .page-size-select,
  .page-jump-input {
    flex: 1;
  }
}
</style>