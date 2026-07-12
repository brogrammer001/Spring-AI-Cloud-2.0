import request from '@/utils/request'

// 查询知识库文档列表
export function listDocument(query) {
  return request({
    url: '/ai-chat/document/list',
    method: 'get',
    params: query
  })
}

// 查询知识库文档详细
export function getDocument(id) {
  return request({
    url: '/ai-chat/document/' + id,
    method: 'get'
  })
}

// 新增知识库文档
export function addDocument(data) {
  return request({
    url: '/ai-chat/document',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 上传文件
export function uploadFile(data) {
  return request({
    url: '/file/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: data,
    timeout: 120000
  })
}

// 修改知识库文档
export function updateDocument(data) {
  return request({
    url: '/ai-chat/document',
    method: 'put',
    data: data
  })
}

// 删除知识库文档
export function delDocument(id) {
  return request({
    url: '/ai-chat/document/' + id,
    method: 'delete'
  })
}

// 下载知识库文档
export function downloadDocument(filePath) {
  return request({
    url: filePath.startsWith('http') ? filePath : '/file/download' + filePath,
    method: 'get',
    responseType: 'blob'
  })
}
