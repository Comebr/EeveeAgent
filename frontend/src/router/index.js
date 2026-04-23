import { createRouter, createWebHistory } from 'vue-router'
import Login from '../components/Login.vue'
import Home from '../components/Home.vue'
import Admin from '../components/Admin.vue'
import ComponentExample from '../components/common/ComponentExample.vue'

const routes = [
  {
    path: '/',
    name: 'Login',
    component: Login
  },
  {
    path: '/home',
    name: 'Home',
    component: Home
  },
  {
    path: '/admin',
    name: 'Admin',
    component: Admin
  },
  {
    path: '/components',
    name: 'ComponentExample',
    component: ComponentExample
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  // 登录页不需要校验
  if (to.path === '/') {
    next()
    return
  }
  
  // 检查是否已登录（实际项目中应该通过API校验登录状态）
  // 这里简化处理，假设如果能访问到用户信息则已登录
  next()
})

export default router
