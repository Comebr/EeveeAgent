<template>
  <div class="form-field" :class="{ 'has-error': error }">
    <label :for="name" class="field-label" v-if="label">
      {{ label }}
      <span class="required" v-if="required">*</span>
    </label>
    
    <div class="field-content">
      <!-- 文本输入 -->
      <input 
        v-if="type === 'text' || type === 'password' || type === 'email' || type === 'number'" 
        :type="type" 
        :id="name" 
        :name="name" 
        v-model="model" 
        :placeholder="placeholder" 
        :disabled="disabled"
        :maxlength="maxlength"
        class="field-input"
        @blur="handleBlur"
      />
      
      <!-- 文本域 -->
      <textarea 
        v-else-if="type === 'textarea'" 
        :id="name" 
        :name="name" 
        v-model="model" 
        :placeholder="placeholder" 
        :disabled="disabled"
        :rows="rows || 3"
        :maxlength="maxlength"
        class="field-textarea"
        @blur="handleBlur"
      ></textarea>
      
      <!-- 下拉选择 -->
      <select 
        v-else-if="type === 'select'" 
        :id="name" 
        :name="name" 
        v-model="model" 
        :disabled="disabled"
        class="field-select"
        @blur="handleBlur"
      >
        <option :value="''" v-if="showEmptyOption">{{ emptyOptionText }}</option>
        <option 
          v-for="option in options" 
          :key="option.value" 
          :value="option.value"
        >
          {{ option.label }}
        </option>
      </select>
      
      <!-- 开关 -->
      <label v-else-if="type === 'switch'" class="switch">
        <input 
          type="checkbox" 
          :id="name" 
          :name="name" 
          v-model="model" 
          :disabled="disabled"
        />
        <span class="slider"></span>
      </label>
      
      <!-- 单选框组 -->
      <div v-else-if="type === 'radio'" class="radio-group">
        <label 
          v-for="option in options" 
          :key="option.value" 
          class="radio-item"
        >
          <input 
            type="radio" 
            :name="name" 
            :value="option.value" 
            v-model="model" 
            :disabled="disabled"
          />
          <span class="radio-label">{{ option.label }}</span>
        </label>
      </div>
      
      <!-- 复选框组 -->
      <div v-else-if="type === 'checkbox'" class="checkbox-group">
        <label 
          v-for="option in options" 
          :key="option.value" 
          class="checkbox-item"
        >
          <input 
            type="checkbox" 
            :name="name" 
            :value="option.value" 
            v-model="model" 
            :disabled="disabled"
          />
          <span class="checkbox-label">{{ option.label }}</span>
        </label>
      </div>
      
      <!-- 日期选择器 -->
      <input 
        v-else-if="type === 'date' || type === 'datetime-local'" 
        :type="type" 
        :id="name" 
        :name="name" 
        v-model="model" 
        :disabled="disabled"
        class="field-input"
        @blur="handleBlur"
      />
      
      <!-- 自定义内容 -->
      <slot v-else></slot>
    </div>
    
    <div class="field-error" v-if="error">
      {{ error }}
    </div>
    <div class="field-help" v-if="help">
      {{ help }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean, Array, Date],
    default: ''
  },
  name: {
    type: String,
    required: true
  },
  label: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  placeholder: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  required: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  help: {
    type: String,
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  showEmptyOption: {
    type: Boolean,
    default: true
  },
  emptyOptionText: {
    type: String,
    default: '请选择'
  },
  maxlength: {
    type: Number,
    default: 0
  },
  rows: {
    type: Number,
    default: 3
  }
})

const emit = defineEmits(['update:modelValue', 'blur'])

const handleBlur = (event) => {
  emit('blur', event)
}

// 双向绑定
const model = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
</script>

<style scoped>
.form-field {
  margin-bottom: 20px;
}

.field-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.required {
  color: #ff4d4f;
  margin-left: 4px;
}

.field-content {
  position: relative;
}

.field-input,
.field-textarea,
.field-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.field-input:focus,
.field-textarea:focus,
.field-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.field-input:disabled,
.field-textarea:disabled,
.field-select:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  color: #999;
}

.field-textarea {
  resize: vertical;
  min-height: 80px;
}

.field-select {
  cursor: pointer;
}

.switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
  cursor: pointer;
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
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #667eea;
}

input:checked + .slider:before {
  transform: translateX(24px);
}

input:disabled + .slider {
  background-color: #e8e8e8;
  cursor: not-allowed;
}

.radio-group,
.checkbox-group {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.radio-item,
.checkbox-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.radio-item input[type="radio"],
.checkbox-item input[type="checkbox"] {
  cursor: pointer;
}

.radio-label,
.checkbox-label {
  font-size: 14px;
  color: #333;
  cursor: pointer;
}

.field-error {
  font-size: 12px;
  color: #ff4d4f;
  margin-top: 4px;
}

.field-help {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.form-field.has-error .field-input,
.form-field.has-error .field-textarea,
.form-field.has-error .field-select {
  border-color: #ff4d4f;
}

.form-field.has-error .field-input:focus,
.form-field.has-error .field-textarea:focus,
.form-field.has-error .field-select:focus {
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.1);
}
</style>