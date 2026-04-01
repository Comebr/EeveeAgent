<template>
  <div class="component-example">
    <h1>通用组件示例</h1>
    
    <!-- 卡片组件示例 -->
    <div class="example-section">
      <h2>卡片组件</h2>
      <BaseCard title="示例卡片">
        <p>这是一个卡片组件的示例内容</p>
      </BaseCard>
    </div>
    
    <!-- 按钮组件示例 -->
    <div class="example-section">
      <h2>按钮组件</h2>
      <div class="button-group">
        <BaseButton type="primary">主要按钮</BaseButton>
        <BaseButton type="secondary">次要按钮</BaseButton>
        <BaseButton type="success">成功按钮</BaseButton>
        <BaseButton type="warning">警告按钮</BaseButton>
        <BaseButton type="danger">危险按钮</BaseButton>
      </div>
      <div class="button-group" style="margin-top: 12px;">
        <BaseButton type="primary" size="small">小按钮</BaseButton>
        <BaseButton type="primary" size="medium">中按钮</BaseButton>
        <BaseButton type="primary" size="large">大按钮</BaseButton>
      </div>
    </div>
    
    <!-- 表单组件示例 -->
    <div class="example-section">
      <h2>表单组件</h2>
      <BaseForm :model="formModel" :rules="formRules" @submit="handleSubmit">
        <FormField 
          v-model="formModel.name" 
          name="name" 
          label="姓名" 
          type="text" 
          placeholder="请输入姓名"
          required
        />
        <FormField 
          v-model="formModel.email" 
          name="email" 
          label="邮箱" 
          type="email" 
          placeholder="请输入邮箱"
          required
        />
        <FormField 
          v-model="formModel.gender" 
          name="gender" 
          label="性别" 
          type="radio"
          :options="genderOptions"
          required
        />
        <FormField 
          v-model="formModel.hobbies" 
          name="hobbies" 
          label="爱好" 
          type="checkbox"
          :options="hobbyOptions"
        />
        <FormField 
          v-model="formModel.status" 
          name="status" 
          label="状态" 
          type="select"
          :options="statusOptions"
          required
        />
        <FormField 
          v-model="formModel.enabled" 
          name="enabled" 
          label="启用"
          type="switch"
        />
        <FormField 
          v-model="formModel.remarks" 
          name="remarks" 
          label="备注" 
          type="textarea" 
          placeholder="请输入备注"
        />
      </BaseForm>
    </div>
    
    <!-- 表格组件示例 -->
    <div class="example-section">
      <h2>表格组件</h2>
      <BaseTable 
        title="用户列表" 
        :columns="tableColumns" 
        :data="tableData" 
        :total="tableTotal"
        :currentPage="currentPage"
        :pageSize="pageSize"
        @page-change="handlePageChange"
      >
        <template #actions>
          <BaseButton type="primary" size="small">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            新增
          </BaseButton>
        </template>
        <template #action="{ row }">
          <div class="table-actions">
            <BaseButton type="primary" size="small">编辑</BaseButton>
            <BaseButton type="danger" size="small">删除</BaseButton>
          </div>
        </template>
      </BaseTable>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import BaseCard from './BaseCard.vue'
import BaseButton from './BaseButton.vue'
import BaseForm from './BaseForm.vue'
import FormField from './FormField.vue'
import BaseTable from './BaseTable.vue'

// 表单数据
const formModel = reactive({
  name: '',
  email: '',
  gender: '',
  hobbies: [],
  status: '',
  enabled: true,
  remarks: ''
})

// 表单规则
const formRules = {
  name: [
    { required: true, message: '请输入姓名' },
    { min: 2, max: 20, message: '姓名长度在2-20个字符之间' }
  ],
  email: [
    { required: true, message: '请输入邮箱' },
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确' }
  ],
  gender: [
    { required: true, message: '请选择性别' }
  ],
  status: [
    { required: true, message: '请选择状态' }
  ]
}

// 选项数据
const genderOptions = [
  { value: 'male', label: '男' },
  { value: 'female', label: '女' }
]

const hobbyOptions = [
  { value: 'reading', label: '阅读' },
  { value: 'sports', label: '运动' },
  { value: 'music', label: '音乐' },
  { value: 'travel', label: '旅行' }
]

const statusOptions = [
  { value: 'active', label: '活跃' },
  { value: 'inactive', label: '非活跃' },
  { value: 'pending', label: '待审核' }
]

// 表格数据
const tableColumns = [
  { key: 'id', title: 'ID', width: '80px' },
  { key: 'name', title: '姓名' },
  { key: 'email', title: '邮箱' },
  { key: 'gender', title: '性别' },
  { key: 'status', title: '状态' },
  { key: 'createdAt', title: '创建时间' },
  { key: 'action', title: '操作', width: '120px' }
]

const tableData = [
  { id: 1, name: '张三', email: 'zhangsan@example.com', gender: '男', status: '活跃', createdAt: '2026-03-01' },
  { id: 2, name: '李四', email: 'lisi@example.com', gender: '女', status: '非活跃', createdAt: '2026-03-02' },
  { id: 3, name: '王五', email: 'wangwu@example.com', gender: '男', status: '待审核', createdAt: '2026-03-03' }
]

const tableTotal = 3
const currentPage = ref(1)
const pageSize = ref(10)

// 方法
const handleSubmit = (formData) => {
  console.log('表单提交:', formData)
  alert('表单提交成功！')
}

const handlePageChange = (page) => {
  console.log('页码变化:', page)
  currentPage.value = page
}
</script>

<style scoped>
.component-example {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 32px;
}

h2 {
  font-size: 18px;
  font-weight: 500;
  color: #666;
  margin-bottom: 16px;
  margin-top: 40px;
}

.example-section {
  margin-bottom: 40px;
}

.button-group {
  display: flex;
  gap: 12px;
}

.table-actions {
  display: flex;
  gap: 8px;
}
</style>