<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isLoggedIn = ref(false)

onMounted(() => {
  // 检查本地存储中是否有token
  const token = localStorage.getItem('token')
  isLoggedIn.value = !!token
  
  // 如果未登录且不在登录页，重定向到登录页
  if (!isLoggedIn.value && router.currentRoute.value.path !== '/') {
    router.push('/')
  }
})

const handleLoginSuccess = () => {
  isLoggedIn.value = true
  router.push('/home')
}
</script>

<template>
  <router-view />
</template>
