import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi} from '@/api/auth'
import {getUserInfo as getUserInfoApi} from '@/api/user'// 引入获取信息接口

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    const refreshToken = ref(localStorage.getItem('refreshToken') || '')
    // 🟢 新增：全局存储用户信息 (头像、昵称)
    const userInfo = ref({
        nickname: '',
        avatar: ''
    })

    const login = async (loginForm) => {
        const data = await loginApi(loginForm)
        // 后端现在返回 { accessToken: "...", refreshToken: "..." }
        token.value = data.accessToken
        refreshToken.value = data.refreshToken

        localStorage.setItem('token', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken) // 🟢 存入
    }

    // 🟢 更新 Token 的动作
    const setTokens = (newAccess, newRefresh) => {
        token.value = newAccess
        refreshToken.value = newRefresh
        localStorage.setItem('token', newAccess)
        localStorage.setItem('refreshToken', newRefresh)
    }

    // 🟢 新增：获取并更新用户信息的 Action
    const fetchUserInfo = async () => {
        try {
            const res = await getUserInfoApi()
            const data = res.data || res
            userInfo.value = data // 更新状态
            return data
        } catch (error) {
            console.error('获取用户信息失败', error)
        }
    }

    const logout = () => {
        token.value = ''
        userInfo.value = {} // 清空信息
        localStorage.removeItem('token')
    }

    return {
        token,
        refreshToken,
        setTokens,
        userInfo, // 导出 state
        login,
        logout,
        fetchUserInfo // 导出 action
    }
})