import router from './index'
import { useUserStore } from '@/stores/user'

const whiteList = ['/login'] // 白名单

router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    const hasToken = userStore.token

    if (hasToken) {
        if (to.path === '/login') {
            // 已登录还去登录页，重定向到首页
            next({ path: '/' })
        } else {
            next()
        }
    } else {
        // 无 Token
        if (whiteList.indexOf(to.path) !== -1) {
            // 在白名单，直接放行
            next()
        } else {
            // 否则全部重定向到登录页
            next(`/login?redirect=${to.path}`)
        }
    }
})