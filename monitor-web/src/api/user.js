import request from '@/utils/request'

// 获取用户信息
export function getUserInfo() {
    return request.get('/user/profile')
}

// 修改用户信息
export function updateProfile(data) {
    return request.put('/user/profile', data)
}

// 修改密码
export function updatePassword(data) {
    return request.put('/user/password', data)
}

// 校验旧密码
export function checkOldPassword(password){
    return request.post('/user/check-password',{password})
}