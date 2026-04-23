import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import axios from 'axios'

// 配置axios
axios.defaults.baseURL = ''

// 请求拦截器
axios.interceptors.request.use(
  config => {
    // 从本地存储获取token
    const token = localStorage.getItem('token')
    if (token) {
      // 添加token到请求头
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    // 处理401错误
    if (error.response && error.response.status === 401) {
      // 清除本地存储
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 跳转到登录页并传递消息参数
      window.location.href = '/?message=登录已失效，请重新登录&messageType=info'
    }
    return Promise.reject(error)
  }
)

// 将axios添加到全局
const app = createApp(App)
app.config.globalProperties.$axios = axios
app.use(router).mount('#app')
