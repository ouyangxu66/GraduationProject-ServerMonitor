import request from '@/utils/request'

// 获取用户列表
export function getUserList(params) {
    return request({
        url: '/admin/user/list',
        method: 'get',
        params
    })
}

// 添加用户
export function addUser(data) {
    return request({
        url: '/admin/user/add',
        method: 'post',
        data
    })
}

// 更新用户
export function updateUser(data) {
    return request({
        url: '/admin/user/update',
        method: 'put',
        data
    })
}

// 删除用户
export function deleteUser(id) {
    return request({
        url: `/admin/user/${id}`,
        method: 'delete'
    })
}

// 重置用户密码
export function resetUserPwd(id) {
    return request({
        url: `/admin/user/reset-pwd/${id}`,
        method: 'put'
    })
}