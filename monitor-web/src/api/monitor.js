import request from "@/utils/request.js";
// ===========================
//  Dashboard 监控大屏接口
// ===========================

// 获取 CPU 历史数据
export const getCpuHistory = (params) => {
    return request({
        url: '/monitor/cpu-history',
        method: 'get',
        params
    })
}

// 获取 磁盘 历史数据
export const getDiskHistory = (params) => {
    return request({
        url: '/monitor/disk-history',
        method: 'get',
        params
    })
}

// 获取 网络 历史数据
export const getNetHistory = (params) => {
    return request({
        url: '/monitor/net-history',
        method: 'get',
        params
    })
}

// 获取服务器基础信息
export const getServerBaseInfo = (params) => {
    return request({
        url: '/monitor/base-info',
        method: 'get',
        params
    })
}

// 获取服务器负载历史数据
export const getSystemLoadHistory = (params) => {
    return request({
        url: '/monitor/load-history',
        method: 'get',
        params
    })
}
// 获取磁盘IO历史数据
export const getDiskIoHistory = (params) => {
    return request({
        url: '/monitor/disk-io-history',
        method: 'get',
        params })
}

// ===========================
//  Server 服务器管理接口
// ===========================

// 获取服务器列表
export function getServerList() {
    return request({
        url: '/server/list',
        method: 'get'
    })
}

// 保存服务器 (新增/修改)
export function saveServer(data) {
    return request({
        url: '/server/save',
        method: 'post',
        data
    })
}

// 删除服务器
export function deleteServer(id) {
    return request({
        url: `/server/${id}`, // 注意反引号
        method: 'delete'
    })
}