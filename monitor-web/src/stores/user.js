import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')

    const login = async (loginForm) => {
        try {
            // data 就是 request.js 返回的 res.data
            const data = await loginApi(loginForm)

            // 🔍 调试：看看后端到底返回了什么？
            console.log('>>> [登录成功] 后端返回数据:', data)

            // 假设后端返回的是 { token: "..." }
            // 如果后端返回的是字符串，这里要改！
            const tokenStr = data.token || data // 兼容处理

            token.value = tokenStr
            localStorage.setItem('token', tokenStr)

            console.log('>>> [登录成功] 已写入 LocalStorage:', localStorage.getItem('token'))

            return Promise.resolve()
        } catch (error) {
            return Promise.reject(error)
        }
    }

    const logout = () => {
        token.value = ''
        localStorage.removeItem('token')
    }

    return { token, login, logout }
})