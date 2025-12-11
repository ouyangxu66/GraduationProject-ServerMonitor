import { useUserStore } from '@/stores/user.js'

export default {
    mounted(el, binding) {
        const { value } = binding // 指令接收到的值，例如: ['server:delete']
        const userStore = useUserStore()

        // 1. 获取当前用户角色
        const role = userStore.userInfo.role

        // 2. 🟢 获取当前用户拥有的所有权限 (从后端返回的 permission 字段)
        const permission = userStore.userInfo.permission || []

        if (value && value instanceof Array && value.length > 0) {
            const requiredPerms = value

            // 判断逻辑：
            // 如果是超级管理员，或者拥有所需权限中的任意一个，则通过
            const hasPermission = role === 'ROLE_ADMIN' ||
                permission.some(perm => requiredPerms.includes(perm))

            if (!hasPermission) {
                // 没有权限，移除 DOM 元素
                el.parentNode && el.parentNode.removeChild(el)
            }
        } else {
            throw new Error(`need roles! Like v-permission="['server:delete']"`)
        }
    }
}