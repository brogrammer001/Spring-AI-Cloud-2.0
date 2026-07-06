import request from '@/utils/request'

// 查询知识库列表
export function listBase(query) {
  return request({
    url: '/ai-chat/base/list',
    method: 'get',
    params: query
  })
}

// 查询知识库详细
export function getBase(id) {
  return request({
    url: '/ai-chat/base/' + id,
    method: 'get'
  })
}

// 新增知识库
export function addBase(data) {
  return request({
    url: '/ai-chat/base',
    method: 'post',
    data: data
  })
}

// 修改知识库
export function updateBase(data) {
  return request({
    url: '/ai-chat/base',
    method: 'put',
    data: data
  })
}

// 删除知识库
export function delBase(id) {
  return request({
    url: '/ai-chat/base/' + id,
    method: 'delete'
  })
}
