import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const service = axios.create({
    baseURL: '/api',
    timeout: 5000
})

// --- 核心变量 ---
let isRefreshing = false // 是否正在刷新 Token
let requests = []        // 存储刷新期间挂起的请求队列

// --- 请求拦截器 ---
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

// --- 响应拦截器 ---
service.interceptors.response.use(
    async (response) => {
        // 0. 文件下载等二进制响应：直接返回，不走业务 code 解析
        const rt = response.config?.responseType
        if (rt === 'blob' || rt === 'arraybuffer') {
            return response.data
        }

        const res = response.data

        // 1. 成功响应
        if (res.code === 200) {
            return (res.data !== undefined && res.data !== null) ? res.data : res
        }

        // 2. 业务逻辑 401 (Token 过期)
        if (res.code === 401) {
            // 进入无感刷新流程，传入当前请求的配置 config
            return handleRefreshToken(response.config)
        }

        // 其他业务错误
        return Promise.reject(new Error(res.msg || 'Error'))
    },
    async (error) => {
        // 3. HTTP 状态码 401
        if (error.response && error.response.status === 401) {
            return handleRefreshToken(error.config)
        }

        console.error('Axios Error:', error)
        return Promise.reject(error)
    }
)

/**
 * 核心：处理 Token 刷新逻辑
 */
async function handleRefreshToken(config) {
    const userStore = useUserStore()

    // A. 如果已经有请求在刷新了，则把当前请求挂起，存入队列
    if (isRefreshing) {
        return new Promise((resolve) => {
            requests.push((newToken) => {
                // 回调：刷新成功后，用新 Token 重发请求
                config.headers['Authorization'] = `Bearer ${newToken}`
                resolve(service(config))
            })
        })
    }

    // B. 开始刷新
    isRefreshing = true
    const refreshToken = localStorage.getItem('refreshToken')

    // 如果没有 RefreshToken，直接登出
    if (!refreshToken) {
        handleLogout()
        return Promise.reject(new Error('会话已过期，请重新登录'))
    }

    try {
        // 1. 调用后端刷新接口 (注意：这里使用原生 axios 实例，避免死循环)
        const { data } = await axios.post('/api/auth/refresh', { refreshToken })

        if (data.code === 200) {
            // 2. 拿到新的双 Token
            const { accessToken, refreshToken: newRefresh } = data.data

            // 3. 更新 Store 和 LocalStorage
            userStore.setTokens(accessToken, newRefresh)

            // 4. 执行队列中的挂起请求
            requests.forEach(cb => cb(accessToken))
            requests = [] // 清空队列

            // 5. 重试当前这个触发 401 的请求
            config.headers['Authorization'] = `Bearer ${accessToken}`
            return service(config)
        } else {
            // 刷新失败 (比如 RefreshToken 也过期了)
            throw new Error('刷新 Token 失败')
        }
    } catch (err) {
        console.error('无感刷新失败:', err)
        // 彻底清理并跳转登录
        handleLogout()
        return Promise.reject(err)
    } finally {
        isRefreshing = false
    }
}

/**
 * 辅助：强制登出
 */
function handleLogout() {
    // 清空状态
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')

    // 清空队列
    requests = []
    isRefreshing = false

    // 跳转
    router.push('/login')
    ElMessage.error('登录已过期，请重新登录')
}

export default service