import request from '@/utils/request'

// 查询支付信息列表
export function listInfo(query) {
  return request({
    url: '/oms/info/list',
    method: 'get',
    params: query
  })
}

// 查询支付信息详细
export function getInfo(id) {
  return request({
    url: '/oms/info/' + id,
    method: 'get'
  })
}

// 新增支付信息
export function addInfo(data) {
  return request({
    url: '/oms/info',
    method: 'post',
    data: data
  })
}

// 修改支付信息
export function updateInfo(data) {
  return request({
    url: '/oms/info',
    method: 'put',
    data: data
  })
}

// 删除支付信息
export function delInfo(id) {
  return request({
    url: '/oms/info/' + id,
    method: 'delete'
  })
}
