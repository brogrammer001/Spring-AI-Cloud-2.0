import request from '@/utils/request'

// 查询知识库文档列表
export function listDocument(query) {
  return request({
    url: '/chat-rag/document/list',
    method: 'get',
    params: query
  })
}

// 查询知识库文档详细
export function getDocument(id) {
  return request({
    url: '/chat-rag/document/' + id,
    method: 'get'
  })
}

// 新增知识库文档
export function addDocument(data) {
  return request({
    url: '/chat-rag/document',
    method: 'post',
    data: data
  })
}

// 新增知识库文档
export function uploadFile(data) {
  return request({
    url: '/file/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: data
  })
}

// 修改知识库文档
export function updateDocument(data) {
  return request({
    url: '/chat-rag/document',
    method: 'put',
    data: data
  })
}

// 删除知识库文档
export function delDocument(id) {
  return request({
    url: '/chat-rag/document/' + id,
    method: 'delete'
  })
}
