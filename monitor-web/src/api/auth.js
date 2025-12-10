import request from '@/utils/request'

// 登录接口
export function login(data) {
    // 🟢 必须有 return！必须有 return！必须有 return！
    // 如果没有 return，外部 await 拿到的就是 undefined，且不会等待请求完成
    return request({
        url: '/auth/login',
        method: 'post',
        data
    })
}

// 获取用户信息
export function getUserInfo() {
    return request({
        url: '/user/profile',
        method: 'get'
    })
}