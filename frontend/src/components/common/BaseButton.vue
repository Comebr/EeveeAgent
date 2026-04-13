<template>
  <button 
    :class="[
      'base-button',
      `button-${type}`,
      `button-${size}`,
      { 'button-block': block },
      { 'button-loading': loading },
      { 'button-disabled': disabled }
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="loading-spinner"></span>
    <slot v-if="!loading"></slot>
  </button>
</template>

<script setup>
const props = defineProps({
  type: {
    type: String,
    default: 'primary'
  },
  size: {
    type: String,
    default: 'medium'
  },
  block: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const handleClick = (event) => {
  emit('click', event)
}
</script>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.base-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.1), transparent);
  transition: left 0.5s ease;
}

.base-button:hover::before {
  left: 100%;
}

.button-block {
  width: 100%;
}

.button-loading {
  cursor: not-allowed;
}

.button-disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none !important;
  box-shadow: none !important;
}

/* 按钮类型 */
.button-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.button-primary:hover:not(:disabled):not(.button-loading) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6b46a0 100%);
  border-color: #5a6fd8;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.button-secondary {
  background: #fff;
  color: #666;
  border-color: #e0e0e0;
}

.button-secondary:hover:not(:disabled):not(.button-loading) {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.button-success {
  background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
  color: #fff;
  border-color: #4caf50;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.button-success:hover:not(:disabled):not(.button-loading) {
  background: linear-gradient(135deg, #43a047 0%, #3d8b40 100%);
  border-color: #43a047;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.4);
}

.button-warning {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
  color: #fff;
  border-color: #ff9800;
  box-shadow: 0 4px 12px rgba(255, 152, 0, 0.3);
}

.button-warning:hover:not(:disabled):not(.button-loading) {
  background: linear-gradient(135deg, #f57c00 0%, #e65100 100%);
  border-color: #f57c00;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 152, 0, 0.4);
}

.button-danger {
  background: linear-gradient(135deg, #ff4d4f 0%, #e63e41 100%);
  color: #fff;
  border-color: #ff4d4f;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.button-danger:hover:not(:disabled):not(.button-loading) {
  background: linear-gradient(135deg, #e63e41 0%, #d32f2f 100%);
  border-color: #e63e41;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 77, 79, 0.4);
}

/* 按钮大小 */
.button-small {
  padding: 6px 14px;
  font-size: 12px;
  border-radius: 6px;
}

.button-medium {
  padding: 10px 20px;
  font-size: 14px;
  border-radius: 8px;
}

.button-large {
  padding: 14px 28px;
  font-size: 16px;
  border-radius: 10px;
}

/* 加载动画 */
.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid #fff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.button-secondary .loading-spinner {
  border-color: rgba(102, 102, 102, 0.3);
  border-top-color: #666;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 点击效果 */
.base-button:active:not(:disabled):not(.button-loading) {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}
</style>