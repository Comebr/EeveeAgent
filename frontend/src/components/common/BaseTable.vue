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
        共 {{ total }} 条
      </div>
      <div class="pagination-controls">
        <button 
          class="page-button" 
          :disabled="currentPage === 1"
          @click="handlePageChange(currentPage - 1)"
        >
          上一页
        </button>
        <span class="page-info">
          {{ currentPage }} / {{ totalPages }}
        </span>
        <button 
          class="page-button" 
          :disabled="currentPage === totalPages"
          @click="handlePageChange(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

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

const emit = defineEmits(['page-change', 'sort-change'])

const totalPages = computed(() => {
  return Math.ceil(props.total / props.pageSize)
})

const handlePageChange = (page) => {
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
  white-space: nowrap;
  transition: all 0.3s ease;
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
}

.pagination-info {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-button {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  background: #fff;
  border-radius: 8px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
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

.page-info {
  font-size: 14px;
  color: #666;
  min-width: 100px;
  text-align: center;
  font-weight: 500;
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
  }
  
  .page-button {
    padding: 6px 12px;
  }
}
</style>