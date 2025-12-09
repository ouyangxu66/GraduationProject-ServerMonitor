import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
// 🟢 移除：import { useUserStore } from '@/stores/user'

const service = axios.create({
    baseURL: '/api',
    timeout: 5000
})

// 请求拦截器
service.interceptors.request.use(
    (config) => {
        // 🟢 暴力修改：直接从 localStorage 拿 Token
        // 这样可以避免 Pinia 初始化过早或过晚的问题
        const token = localStorage.getItem('token')

        // 🔍 调试：看看这行打印了什么？
        console.log('>>> [拦截器] LocalStorage Token:', token)

        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器 (保持不变，或确保 401 逻辑清除 localStorage)
service.interceptors.response.use(
    (response) => {
        const res = response.data
        if (res.code === 200) {
            return res.data
        }
        // ... 其他错误处理
        if (res.code === 401) {
            localStorage.removeItem('token') // 🟢 确保清除
            router.push('/login')
        }
        return Promise.reject(new Error(res.msg || 'Error'))
    },
    (error) => {
        // ... HTTP 错误处理
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('token') // 🟢 确保清除
            router.push('/login')
        }
        return Promise.reject(error)
    }
)

export default service