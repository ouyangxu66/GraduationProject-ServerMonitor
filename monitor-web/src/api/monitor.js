import request from "@/utils/request.js";
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