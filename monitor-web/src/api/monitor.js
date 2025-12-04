import axios from 'axios'

// 1. 创建 axios 实例 (全文件只写这一次)
const request = axios.create({
    baseURL: '/api',
    timeout: 5000
})

// ===========================
//  Dashboard 监控大屏接口
// ===========================

// 获取 CPU 历史数据
export const getCpuHistory = () => {
    return request.get('/monitor/cpu-history')
}

// ===========================
//  Server 服务器管理接口
// ===========================

// 获取服务器列表
export const getServerList = () => {
    return request.get('/server/list')
}

// 新增或更新服务器
export const saveServer = (data) => {
    return request.post('/server/save', data)
}

// 删除服务器
export const deleteServer = (id) => {
    return request.delete(`/server/delete?id=${id}`)
}