import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 👇 新增 server 配置
  server: {
    proxy: {
      // 意思：只要前端请求以 /api 开头，就自动转发给 http://localhost:8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // rewrite: 把路径里的 /api 去掉，因为后端接口是 /monitor/xxx，不是 /api/monitor/xxx
        // 如果你的后端接口就是 /monitor 开头，我们可以把 /api 替换为空，
        // 或者我们约定前端请求写 /api/monitor/xxx -> 后端 /monitor/xxx
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})