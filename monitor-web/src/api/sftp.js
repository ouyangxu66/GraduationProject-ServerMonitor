import request from '@/utils/request'

// 获取一次性 sftp ticket
export function getServerSftpTicket(serverId) {
  return request({
    url: `/server/${serverId}/sftp-ticket`,
    method: 'get'
  })
}

// 列目录（注意：ticket 一次性，所以每次操作都要重新取 ticket）
export function sftpList(params) {
  return request({
    url: '/sftp/list',
    method: 'get',
    params
  })
}

// 上传文件
export function sftpUpload(formData) {
  return request({
    url: '/sftp/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 下载文件：直接让浏览器下载（axios 返回 blob）
export function sftpDownload(params) {
  return request({
    url: '/sftp/download',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

