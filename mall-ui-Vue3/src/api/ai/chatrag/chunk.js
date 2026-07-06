import request from '@/utils/request'

// 查询文档切片列表
export function listChunk(query) {
  return request({
    url: '/ai-chat/chunk/list',
    method: 'get',
    params: query
  })
}

// 查询文档切片详细
export function getChunk(id) {
  return request({
    url: '/ai-chat/chunk/' + id,
    method: 'get'
  })
}

// 新增文档切片
export function addChunk(data) {
  return request({
    url: '/ai-chat/chunk',
    method: 'post',
    data: data
  })
}

// 修改文档切片
export function updateChunk(data) {
  return request({
    url: '/ai-chat/chunk',
    method: 'put',
    data: data
  })
}

// 删除文档切片
export function delChunk(id) {
  return request({
    url: '/ai-chat/chunk/' + id,
    method: 'delete'
  })
}
