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

export default router
