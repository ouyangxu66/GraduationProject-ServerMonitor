import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 🔴 关键：配置服务器代理
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 指向后端端口
        changeOrigin: true,
        // 如果你的后端 Controller 就叫 /api/auth，那么不需要 rewrite
        // 如果你的后端 Controller 叫 /auth (不带api)，则需要 rewrite: (path) => path.replace(/^\/api/, '')
        // 按照我们的教程，后端 Controller 是带 /api 的，所以这里不需要 rewrite！
      },
      // WebSocket 代理 (重要！为后面的 SSH 做准备)
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }
})