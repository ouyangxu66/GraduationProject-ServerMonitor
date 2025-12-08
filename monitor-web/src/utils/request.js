import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user' // 稍后创建
import router from '@/router'

// 1. 创建 axios 实例
const service = axios.create({
    baseURL: '/api', // 配合 Vite 代理，指向后端
    timeout: 5000    // 请求超时时间
})

// 2. 请求拦截器 (Request Interceptor)
service.interceptors.request.use(
    (config) => {
        // 在发送请求之前做些什么
        const userStore = useUserStore()
        // 如果存在 Token，则添加到 Header
        if (userStore.token) {
            config.headers['Authorization'] = `Bearer ${userStore.token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 3. 响应拦截器 (Response Interceptor)
service.interceptors.response.use(
    (response) => {
        const res = response.data
        // 假设后端 R 类成功码为 200
        if (res.code !== 200) {
            ElMessage({
                message: res.msg || '系统错误',
                type: 'error',
                duration: 5 * 1000
            })

            // 401: 未认证或 Token 过期
            if (res.code === 401) {
                // 可以在这里触发登出逻辑
                const userStore = useUserStore()
                userStore.logout()
                router.push('/login')
            }
            return Promise.reject(new Error(res.msg || 'Error'))
        } else {
            return res.data // 直接返回 R.data 部分，调用时更方便
        }
    },
    (error) => {
        // 处理 HTTP 状态码非 200 的情况
        console.error('err' + error)
        let msg = error.message
        if (error.response && error.response.status === 401) {
            msg = '登录已过期，请重新登录'
            const userStore = useUserStore()
            userStore.logout()
            router.push('/login')
        }
        ElMessage({
            message: msg,
            type: 'error',
            duration: 5 * 1000
        })
        return Promise.reject(error)
    }
)

export default service