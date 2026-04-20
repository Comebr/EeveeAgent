<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  currentAvatar: {
    type: String,
    default: ''
  },
  presetAvatars: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:visible', 'select'])

const selectedAvatar = ref(props.currentAvatar)
const customAvatarUrl = ref('')

// 关闭选择器
const close = () => {
  emit('update:visible', false)
}

// 选择头像
const selectAvatar = (avatar) => {
  selectedAvatar.value = avatar
  customAvatarUrl.value = ''
}

// 确认选择
const confirmSelect = () => {
  if (customAvatarUrl.value.trim()) {
    selectedAvatar.value = customAvatarUrl.value.trim()
  }
  emit('select', selectedAvatar.value)
  close()
}

// 判断是否为当前选中的头像
const isSelected = (avatar) => {
  return selectedAvatar.value === avatar
}
</script>

<template>
  <div v-if="visible" class="avatar-selector-overlay" @click.self="close">
    <div class="avatar-selector">
      <div class="selector-header">
        <h3>选择头像</h3>
        <button class="btn-close" @click="close">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      
      <div class="selector-body">
        <p class="selector-desc">请从以下预设头像中选择一个</p>
        
        <div class="avatar-grid">
          <div 
            v-for="(avatar, index) in presetAvatars" 
            :key="index"
            class="avatar-item"
            :class="{ selected: isSelected(avatar) }"
            @click="selectAvatar(avatar)"
          >
            <img :src="avatar" :alt="`头像 ${index + 1}`" />
            <div v-if="isSelected(avatar)" class="selected-indicator">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
          </div>
        </div>
        
        <div class="avatar-url-input">
          <label>自定义头像 URL</label>
          <input 
            type="text" 
            v-model="customAvatarUrl"
            placeholder="请输入头像 URL"
            @input="selectedAvatar = customAvatarUrl"
          />
        </div>
        
        <div class="current-preview">
          <span class="preview-label">当前选择:</span>
          <img :src="selectedAvatar" alt="当前头像" />
        </div>
      </div>
      
      <div class="selector-footer">
        <button class="btn btn-default" @click="close">取消</button>
        <button class="btn btn-primary" @click="confirmSelect">确定</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.avatar-selector-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}

.avatar-selector {
  background: white;
  border-radius: 12px;
  width: 480px;
  max-width: 90vw;
  max-height: 90vh;
  overflow: hidden;
  animation: dialogSlideIn 0.3s ease;
}

@keyframes dialogSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.selector-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #f5f5f5;
  color: #666;
}

.selector-body {
  padding: 24px;
}

.selector-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.avatar-item {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid #e8e8e8;
  transition: all 0.2s;
}

.avatar-item:hover {
  border-color: #667eea;
  transform: scale(1.05);
}

.avatar-item.selected {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

.avatar-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.selected-indicator {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(102, 126, 234, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-url-input {
  margin: 20px 0;
}

.avatar-url-input label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.avatar-url-input input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.avatar-url-input input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.current-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 20px;
}

.preview-label {
  font-size: 14px;
  color: #666;
}

.current-preview img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #667eea;
}

.selector-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-default {
  background: white;
  border: 1px solid #d9d9d9;
  color: #666;
}

.btn-default:hover {
  border-color: #667eea;
  color: #667eea;
}

@media (max-width: 600px) {
  .avatar-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
}
</style>
