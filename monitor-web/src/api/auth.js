import request from '@/utils/request'

export function login(data) {
    return request({
        url: '/auth/login',
        method: 'post',
        data // { username, password }
    })
}

// 预留获取用户信息的接口
export function getUserInfo() {
    return request({
        url: '/user/info',
        method: 'get'
    })
}