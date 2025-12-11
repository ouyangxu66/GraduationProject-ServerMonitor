import { useUserStore } from '@/stores/user'

export default {
    mounted(el, binding) {
        const { value } = binding // 例如: ['server:delete']
        const userStore = useUserStore()

        // 获取当前用户角色
        const role = userStore.userInfo.role

        // 逻辑：
        // 1. 如果是超级管理员 (ROLE_ADMIN)，拥有所有权限
        // 2. 如果是普通用户 (ROLE_USER)，则根据具体的业务规则判断
        //    根据之前的 SQL 设计，普通用户只有 server:list，没有 server:delete

        if (value && value instanceof Array && value.length > 0) {
            const requiredPerms = value

            let hasPermission = false

            if (role === 'ROLE_ADMIN') {
                hasPermission = true
            } else {
                // 对于普通用户，检查是否拥有 requiredPerms 中的任意一个
                // 这里简化处理：假设 store 中暂存的是 role，
                // 实际商业项目中，login接口通常返回 permissions: ['server:list', ...] 数组
                // 这里我们在前端做一个简单的映射模拟数据库逻辑：
                const userPerms = ['server:list'] // 普通用户拥有的权限
                hasPermission = userPerms.some(p => requiredPerms.includes(p))
            }

            if (!hasPermission) {
                // 没有权限，移除 DOM 元素，防止用户点击
                el.parentNode && el.parentNode.removeChild(el)
            }
        } else {
            throw new Error(`need roles! Like v-permission="['server:delete']"`)
        }
    }
}