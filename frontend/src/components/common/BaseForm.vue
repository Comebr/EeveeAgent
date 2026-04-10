<template>
  <div class="base-form">
    <form @submit.prevent="handleSubmit" class="form">
      <div class="form-header" v-if="title">
        <h3 class="form-title">{{ title }}</h3>
        <div class="form-subtitle" v-if="subtitle">{{ subtitle }}</div>
      </div>
      <div class="form-content">
        <slot></slot>
      </div>
      <div class="form-actions" v-if="showActions">
        <button 
          type="button" 
          class="form-button secondary"
          @click="handleCancel"
        >
          {{ cancelText }}
        </button>
        <button 
          type="submit" 
          class="form-button primary"
          :disabled="loading"
        >
          <span v-if="loading" class="loading-spinner"></span>
          {{ submitText }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const props = defineProps({
  model: {
    type: Object,
    default: () => ({})
  },
  rules: {
    type: Object,
    default: () => ({})
  },
  showActions: {
    type: Boolean,
    default: true
  },
  submitText: {
    type: String,
    default: '提交'
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  loading: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['submit', 'cancel', 'validate'])

const errors = reactive({})

const validateField = (field, value) => {
  const fieldRules = props.rules[field]
  if (!fieldRules) return true
  
  for (const rule of fieldRules) {
    if (rule.required && (!value || (Array.isArray(value) && value.length === 0))) {
      errors[field] = rule.message || '此项为必填项'
      return false
    }
    if (rule.min && value && value.length < rule.min) {
      errors[field] = rule.message || `最少需要${rule.min}个字符`
      return false
    }
    if (rule.max && value && value.length > rule.max) {
      errors[field] = rule.message || `最多允许${rule.max}个字符`
      return false
    }
    if (rule.pattern && value && !rule.pattern.test(value)) {
      errors[field] = rule.message || '格式不正确'
      return false
    }
    if (rule.validator && typeof rule.validator === 'function') {
      const error = rule.validator(value, props.model)
      if (error) {
        errors[field] = error
        return false
      }
    }
  }
  delete errors[field]
  return true
}

const validate = () => {
  let isValid = true
  Object.keys(props.rules).forEach(field => {
    if (!validateField(field, props.model[field])) {
      isValid = false
    }
  })
  emit('validate', isValid, errors)
  return isValid
}

const handleSubmit = () => {
  if (validate()) {
    emit('submit', props.model)
  }
}

const handleCancel = () => {
  emit('cancel')
}

// 暴露方法给父组件
defineExpose({
  validate,
  validateField,
  errors
})
</script>

<style scoped>
.base-form {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.form {
  width: 100%;
}

.form-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px 32px 24px;
  color: white;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 8px 0;
  line-height: 1.3;
}

.form-subtitle {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
  line-height: 1.5;
}

.form-content {
  padding: 32px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 24px 32px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 0 0 12px 12px;
}

.form-button {
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 100px;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.form-button.primary {
  background: #667eea;
  color: #fff;
}

.form-button.primary:hover {
  background: #5a6fd8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.form-button.primary:disabled {
  background: #c7d2fe;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.form-button.secondary {
  background: #fff;
  color: #666;
  border-color: #e0e0e0;
}

.form-button.secondary:hover {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid #fff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .form-header {
    padding: 24px 24px 16px;
  }
  
  .form-title {
    font-size: 20px;
  }
  
  .form-content {
    padding: 24px;
  }
  
  .form-actions {
    padding: 20px 24px;
    flex-direction: column;
  }
  
  .form-button {
    width: 100%;
  }
}
</style>