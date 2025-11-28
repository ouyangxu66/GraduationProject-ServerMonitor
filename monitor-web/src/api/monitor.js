import axios from 'axios'

// 创建一个 axios 实例
const request = axios.create({
    // 注意：这里用 /api 前缀，触发 vite 的代理
    baseURL: '/api',
    timeout: 5000
})

// 定义 API 方法
export const getCpuHistory = () => {
    // 实际请求会被转发到: http://localhost:8080/monitor/cpu-history
    return request.get('/monitor/cpu-history')
}