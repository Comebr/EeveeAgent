<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'

const emit = defineEmits(['login-success'])

const form = reactive({
  username: localStorage.getItem('rememberedUsername') || '',
  password: localStorage.getItem('rememberedPassword') || '',
  remember: localStorage.getItem('rememberedUsername') ? true : false
})

const loading = ref(false)
const error = ref('')
const showPassword = ref(false)
const showMessage = ref(false)
const messageText = ref('')
const messageType = ref('success')

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
  // 检查URL参数是否有消息提示
  const urlParams = new URLSearchParams(window.location.search)
  const message = urlParams.get('message')
  const messageType = urlParams.get('messageType')
  
  if (message) {
    showToast(message, messageType || 'error')
    // 清除URL参数，避免刷新页面后重复显示
    const newUrl = new URL(window.location.href)
    newUrl.searchParams.delete('message')
    newUrl.searchParams.delete('messageType')
    window.history.replaceState({}, '', newUrl.toString())
  }
})

// 表单验证错误
const formErrors = reactive({
  username: '',
  password: ''
})

// 验证表单
const validateForm = () => {
  let isValid = true
  
  // 重置错误信息
  formErrors.username = ''
  formErrors.password = ''
  
  // 验证用户名
  if (!form.username.trim()) {
    formErrors.username = '用户名不能为空'
    isValid = false
  }
  
  // 验证密码
  if (!form.password.trim()) {
    formErrors.password = '密码不能为空'
    isValid = false
  }
  
  return isValid
}


const handleLogin = async () => {
  error.value = ''
  
  // 验证表单
  if (!validateForm()) {
    return
  }
  
  loading.value = true
  
  try {
    const response = await axios.post('/api/auth/login', {
      username: form.username,
      password: form.password
    })
    
    // 检查后端返回的业务码
    const data = response.data
    if (data.code === '0') {
      // 登录成功，后端会设置HttpOnly Cookie
      // 直接跳转
      setTimeout(() => {
        window.location.href = '/home'
      }, 100)
    } else {
      // 显示后端返回的错误信息
      error.value = data.message || '用户名或密码错误'
    }
  } catch (err) {
    if (err.response) {
      // 服务器返回错误
      const errorData = err.response.data
      // 后端返回的错误信息在 message 字段
      error.value = errorData.message || '用户名或密码错误'
    } else if (err.request) {
      // 请求已发出但没有收到响应
      error.value = '网络错误，请检查后端服务是否正常'
    } else {
      // 其他错误
      error.value = err.message || '登录失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
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
    
    <h2>欢迎使用 EeveeAgent!</h2>
    
    <div class="login-content">
      <div class="form-group">
        <div class="input-wrapper" :class="{ 'error': formErrors.username }">
          <span class="icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </span>
          <input 
            v-model="form.username" 
            type="text" 
            placeholder="用户名"
            :disabled="loading"
            @keyup.enter="handleLogin"
          />
        </div>
        <div v-if="formErrors.username" class="error-text">{{ formErrors.username }}</div>
      </div>
      
      <div class="form-group">
        <div class="input-wrapper" :class="{ 'error': formErrors.password }">
          <span class="icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
          </span>
          <input 
            v-model="form.password" 
            :type="showPassword ? 'text' : 'password'" 
            placeholder="密码"
            :disabled="loading"
            @keyup.enter="handleLogin"
          />
          <button 
            type="button" 
            class="password-toggle" 
            @click="showPassword = !showPassword"
            :disabled="loading"
          >
            <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"></path>
              <circle cx="12" cy="12" r="3"></circle>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"></path>
              <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"></path>
              <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"></path>
              <line x1="2" y1="2" x2="22" y2="22"></line>
            </svg>
          </button>
        </div>
        <div v-if="formErrors.password" class="error-text">{{ formErrors.password }}</div>
      </div>
      

      
      <div v-if="error" class="error-message">
        {{ error }}
      </div>
      
      <button 
        class="login-button" 
        @click="handleLogin"
        :disabled="loading"
      >
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <div class="remember-container">
        <label class="remember-label">
          记住密码
          <label class="switch">
            <input
                v-model="form.remember"
                type="checkbox"
                :disabled="loading"
            />
            <span class="slider"></span>
          </label>
        </label>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: url('/images/background.png') !important;
  padding: 0;
  position: fixed;
  top: 0;
  left: 0;
  overflow: hidden;
  background-size: cover !important;
  background-position: center !important;
  background-repeat: no-repeat !important;
  margin: 0;
  z-index: 1;
  box-sizing: border-box;
}

.login-content {
  width: 100%;
  max-width: 320px;
  position: relative;
  z-index: 10;
  padding: 0 20px;
  box-sizing: border-box;
}

.login-container h2 {
  text-align: center;
  margin-bottom: 30px;
  color: white;
  font-size: 24px;
  font-weight: 600;
  font-family: 'SimSun', serif;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 10;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxkZWZzPjxwYXR0ZXJuIGlkPSJwYXR0ZXJuIiB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHBhdHRlcm5Vbml0cz0idXNlclNwYWNlT25Vc2UiPjxwYXRoIGQ9Ik0gNDAgMCBMIDAgMCAwIDQwIiBmaWxsPSJub25lIiBzdHJva2U9IiNmZmYiIHN0cm9rZS13aWR0aD0iMC41Ii8+PC9wYXR0ZXJuPjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI3BhdHRlcm4pIiAvPjwvc3ZnPg==');
  opacity: 0.1;
  z-index: 1;
}

.login-container::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 2;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
  z-index: 11;
}

.input-wrapper {
  position: relative;
  border: none;
  border-radius: 50px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.1) !important;
  backdrop-filter: blur(5px) !important;
  -webkit-backdrop-filter: blur(5px) !important;
  z-index: 11;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.2);
  /* 确保初始渲染时就应用玻璃态效果 */
  display: block;
  width: 100%;
  min-height: 44px;
  /* 防止样式被覆盖 */
  -webkit-box-shadow: none !important;
  -moz-box-shadow: none !important;
  box-shadow: none !important;
}

.password-toggle {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent !important;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  padding: 5px;
  border-radius: 50%;
  transition: all 0.3s ease;
  z-index: 12;
  box-sizing: border-box;
}

.password-toggle:hover {
  background: rgba(255, 255, 255, 0.1) !important;
}

.password-toggle:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.input-wrapper:focus-within {
  background: rgba(255, 255, 255, 0.15) !important;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.input-wrapper.error {
  border: 1px solid rgba(255, 100, 100, 0.8) !important;
  background: rgba(255, 100, 100, 0.1) !important;
}

.error-text {
  color: rgba(255, 100, 100, 0.9);
  font-size: 12px;
  margin-top: 5px;
  margin-left: 10px;
  position: relative;
  z-index: 11;
}

.icon {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  z-index: 12;
}

input {
  width: 100%;
  padding: 12px 15px 12px 40px;
  border: none;
  outline: none;
  font-size: 13px;
  color: white !important;
  background: transparent !important;
  box-shadow: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  appearance: none !important;
  z-index: 12;
  position: relative;
  box-sizing: border-box;
}



input::placeholder {
  color: rgba(255, 255, 255, 0.6) !important;
}

input:focus {
  color: white !important;
}

.remember-container {
  margin-top: 10px;
  display: flex;
  justify-content: flex-start;
  position: relative;
  z-index: 11;
}

.remember-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  position: relative;
  z-index: 11;
}

.remember-label .switch {
  margin-left: 8px;
}

/* 开关样式 */
.switch {
  position: relative;
  display: inline-block;
  width: 40px;
  height: 20px;
  z-index: 11;
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
  background-color: rgba(255, 255, 255, 0.2);
  transition: .4s;
  border-radius: 20px;
  z-index: 1;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
  z-index: 2;
}

input:checked + .slider {
  background-color: rgba(102, 126, 234, 0.6);
}

input:checked + .slider:before {
  transform: translateX(20px);
}

.error-message {
  background-color: rgba(255, 170, 170, 0.3);
  color: #fff;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 20px;
  font-size: 14px;
  border: 1px solid rgba(255, 170, 170, 0.5);
  position: relative;
  z-index: 11;
}

.login-button {
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(5px) !important;
  -webkit-backdrop-filter: blur(5px) !important;
  color: white !important;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 50px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 11;
  position: relative;
  box-sizing: border-box;
}

.login-button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.login-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 消息提示样式 */
.toast-message {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  animation: slideDown 0.3s ease-out;
}

.toast-message.success {
  background-color: #f0fdf4;
  border: 1px solid #dcfce7;
  color: #166534;
}

.toast-message.error {
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  color: #b91c1c;
}

.toast-message.info {
  background-color: #eff6ff;
  border: 1px solid #dbeafe;
  color: #1d4ed8;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translate(-50%, -20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

/* 全局样式重置，确保没有其他样式干扰 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* 确保登录容器始终覆盖整个屏幕 */
.login-container {
  display: flex !important;
  opacity: 1 !important;
  visibility: visible !important;
}

/* 确保输入框始终保持玻璃态 */
.input-wrapper,
.login-button {
  transition: none !important;
  animation: none !important;
}

@media (max-width: 480px) {
  .login-container h2 {
    font-size: 24px;
    margin-bottom: 30px;
  }
  
  .login-content {
    padding: 0 15px;
  }
  
  input {
    padding: 12px 15px 12px 45px;
  }
  
  .login-button {
    padding: 12px;
  }
}
</style>