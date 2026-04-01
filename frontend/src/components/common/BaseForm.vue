<template>
  <div class="base-form">
    <form @submit.prevent="handleSubmit" class="form">
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
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

.form {
  width: 100%;
}

.form-content {
  margin-bottom: 24px;
}

.form-content :deep(.form-field:first-child) .field-label {
  font-weight: 600;
  color: #1a1a1a;
  padding-bottom: 8px;
  border-bottom: 2px solid #667eea;
  margin-bottom: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
}

.form-button {
  padding: 8px 20px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.form-button.primary {
  background: #667eea;
  color: #fff;
}

.form-button.primary:hover {
  background: #5a6fd8;
}

.form-button.primary:disabled {
  background: #c7d2fe;
  cursor: not-allowed;
}

.form-button.secondary {
  background: #fff;
  color: #666;
  border-color: #d9d9d9;
}

.form-button.secondary:hover {
  border-color: #667eea;
  color: #667eea;
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
</style>