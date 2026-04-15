import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5177,
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false
      },
      '/agent': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false
      },
      '/kb': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false
      },
      '/intent-tree': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false
      }
    },
    strictPort: true
  }
})
