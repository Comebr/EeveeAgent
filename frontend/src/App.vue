<script setup>
import { ref, onMounted } from 'vue'
import Login from './components/Login.vue'
import Home from './components/Home.vue'

const isLoggedIn = ref(false)

onMounted(() => {
  // 检查本地存储中是否有token
  const token = localStorage.getItem('token')
  isLoggedIn.value = !!token
})

const handleLoginSuccess = () => {
  isLoggedIn.value = true
}
</script>

<template>
  <div v-if="isLoggedIn">
    <Home />
  </div>
  <div v-else>
    <Login @login-success="handleLoginSuccess" />
  </div>
</template>
