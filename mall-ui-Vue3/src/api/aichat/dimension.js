import request from '@/utils/request'

// 查询NL2SQL维度定义列表
export function listDimension(query) {
  return request({
    url: '/ai-chat/dimension/list',
    method: 'get',
    params: query
  })
}

// 查询NL2SQL维度定义详细
export function getDimension(id) {
  return request({
    url: '/ai-chat/dimension/' + id,
    method: 'get'
  })
}

// 新增NL2SQL维度定义
export function addDimension(data) {
  return request({
    url: '/ai-chat/dimension',
    method: 'post',
    data: data
  })
}

// 修改NL2SQL维度定义
export function updateDimension(data) {
  return request({
    url: '/ai-chat/dimension',
    method: 'put',
    data: data
  })
}

// 删除NL2SQL维度定义
export function delDimension(id) {
  return request({
    url: '/ai-chat/dimension/' + id,
    method: 'delete'
  })
}
