<template>
  <div class="scrollable-container" :class="{ 'show-scrollbar': showScrollbar }">
    <slot></slot>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  // 滚动条颜色
  scrollbarColor: {
    type: String,
    default: '#444444'
  },
  // 滚动条悬停颜色
  scrollbarHoverColor: {
    type: String,
    default: '#333333'
  },
  // 滚动条隐藏延迟时间（毫秒）
  hideDelay: {
    type: Number,
    default: 1000
  },
  // 滚动条宽度
  scrollbarWidth: {
    type: String,
    default: '4px'
  }
})

const showScrollbar = ref(false)
let hideTimeout = null

// 处理滚动事件
const handleScroll = () => {
  showScrollbar.value = true
  
  // 清除之前的定时器
  if (hideTimeout) {
    clearTimeout(hideTimeout)
  }
  
  // 设置新的定时器
  hideTimeout = setTimeout(() => {
    showScrollbar.value = false
  }, props.hideDelay)
}

onMounted(() => {
  const container = document.querySelector('.scrollable-container')
  if (container) {
    container.addEventListener('wheel', handleScroll)
    container.addEventListener('mousedown', handleScroll)
  }
})

onUnmounted(() => {
  const container = document.querySelector('.scrollable-container')
  if (container) {
    container.removeEventListener('wheel', handleScroll)
    container.removeEventListener('mousedown', handleScroll)
  }
  
  if (hideTimeout) {
    clearTimeout(hideTimeout)
  }
})
</script>

<style scoped>
.scrollable-container {
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
  -ms-overflow-style: none;
  overflow: -moz-scrollbars-none;
  transition: all 0.3s ease;
}

/* 隐藏IE滚动条箭头 */
.scrollable-container {
  -ms-overflow-style: none;
}

/* 隐藏WebKit滚动条 */
.scrollable-container::-webkit-scrollbar {
  width: v-bind(scrollbarWidth);
  height: v-bind(scrollbarWidth);
  transition: width 0.3s ease;
}

.scrollable-container::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 2px;
}

.scrollable-container::-webkit-scrollbar-thumb {
  background-color: transparent;
  border-radius: 2px;
  transition: background-color 0.3s ease;
}

/* 隐藏WebKit滚动条箭头 */
.scrollable-container::-webkit-scrollbar-button {
  display: none;
  width: 0;
  height: 0;
}

/* 隐藏WebKit滚动条角落 */
.scrollable-container::-webkit-scrollbar-corner {
  display: none;
}

/* 鼠标悬停时滚动条仍然透明 */
.scrollable-container:hover {
  scrollbar-color: transparent transparent;
}

.scrollable-container:hover::-webkit-scrollbar-thumb {
  background-color: transparent;
}

/* 鼠标点击或滚动时显示滚动条 */
.scrollable-container:active {
  scrollbar-color: v-bind(scrollbarColor) transparent;
}

.scrollable-container:active::-webkit-scrollbar-thumb {
  background-color: v-bind(scrollbarColor);
}

/* 滚动时显示滚动条 */
.scrollable-container::-webkit-scrollbar-thumb:active {
  background-color: v-bind(scrollbarHoverColor);
}

/* 触摸设备支持 */
.scrollable-container::-webkit-scrollbar-thumb:hover {
  background-color: v-bind(scrollbarHoverColor);
}

/* 显示滚动条 */
.scrollable-container.show-scrollbar {
  scrollbar-color: v-bind(scrollbarColor) transparent;
}

.scrollable-container.show-scrollbar::-webkit-scrollbar-thumb {
  background-color: v-bind(scrollbarColor);
}
</style>