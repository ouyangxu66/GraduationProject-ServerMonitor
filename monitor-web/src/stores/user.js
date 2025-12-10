import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getUserInfo as getUserInfoApi } from '@/api/auth' // 引入获取信息接口

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    // 🟢 新增：全局存储用户信息 (头像、昵称)
    const userInfo = ref({
        nickname: '',
        avatar: ''
    })

    const login = async (loginForm) => {
        try {
            const res = await loginApi(loginForm)

            // 🟢 核心修复：增加判空保护
            if (!res) {
                throw new Error('登录失败：接口未返回任何数据')
            }

            // 兼容逻辑：如果 res 已经是 token 字符串，或者 res.token 存在
            const tokenStr = res.token || res

            // 再次校验 token 是否为字符串
            if (typeof tokenStr !== 'string') {
                console.error('无效的 Token 格式:', tokenStr)
                throw new Error('登录失败：Token 格式错误')
            }

            token.value = tokenStr
            localStorage.setItem('token', tokenStr)
            return Promise.resolve()
        } catch (error) {
            return Promise.reject(error)
        }
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
        userInfo, // 导出 state
        login,
        logout,
        fetchUserInfo // 导出 action
    }
})