import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
    baseURL: '/api',
    timeout: 5000
})

// 请求拦截器
service.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    (response) => {
        const res = response.data
        if (res.code === 200) {
            // 🟢 确保有返回值
            // 如果 res.data 有值，返回 data；否则返回 res 本身
            return (res.data !== undefined && res.data !== null) ? res.data : res
        }

        // 处理 401
        if (res.code === 401) {
            localStorage.removeItem('token')
            router.push('/login')
        }

        return Promise.reject(new Error(res.msg || 'Error'))
    },
    (error) => {
        console.error('Axios Error:', error)
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('token')
            router.push('/login')
        }
        return Promise.reject(error)
    }
)

export default service