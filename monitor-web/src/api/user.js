import request from '@/utils/request'

export function getUserInfo() {
    return request.get('/user/profile')
}

export function updateProfile(data) {
    return request.put('/user/profile', data)
}

export function updatePassword(data) {
    return request.put('/user/password', data)
}

export function checkOldPassword(password){
    return request.post('/user/check-password',{password})
}
// 登录接口保持不变...
export function login(data) { /* ... */ }