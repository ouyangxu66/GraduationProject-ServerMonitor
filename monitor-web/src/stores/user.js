import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
    // 状态：优先从 localStorage 读取
    const token = ref(localStorage.getItem('token') || '')
    const userInfo = ref({})

    // 动作：登录
    const login = async (loginForm) => {
        try {
            // 1. 调用 API
            const data = await loginApi(loginForm) // data 就是 R.data，即 { token: '...' }

            // 2. 更新状态
            token.value = data.token

            // 3. 持久化
            localStorage.setItem('token', data.token)

            return Promise.resolve()
        } catch (error) {
            return Promise.reject(error)
        }
    }

    // 动作：登出
    const logout = () => {
        token.value = ''
        userInfo.value = {}
        localStorage.removeItem('token')
    }

    return {
        token,
        userInfo,
        login,
        logout
    }
})