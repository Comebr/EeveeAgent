<template>
  <div class="base-card" :class="{ 'card-shadow': shadow }">
    <div class="card-header" v-if="showHeader">
      <div class="card-title" v-if="title">
        {{ title }}
      </div>
      <div class="card-actions" v-if="$slots.actions">
        <slot name="actions"></slot>
      </div>
    </div>
    <div class="card-body">
      <slot></slot>
    </div>
    <div class="card-footer" v-if="$slots.footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  showHeader: {
    type: Boolean,
    default: true
  },
  shadow: {
    type: Boolean,
    default: true
  }
})
</script>

<style scoped>
.base-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
}

.card-shadow {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.base-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 12px 12px 0 0;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.card-actions {
  display: flex;
  gap: 12px;
}

.card-body {
  padding: 24px;
}

.card-footer {
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 0 0 12px 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .card-header {
    padding: 16px 20px;
  }
  
  .card-title {
    font-size: 16px;
  }
  
  .card-body {
    padding: 20px;
  }
  
  .card-footer {
    padding: 16px 20px;
  }
}
</style>