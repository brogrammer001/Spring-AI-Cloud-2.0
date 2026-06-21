import request from '@/utils/request'

// 查询订单配置信息列表
export function listSetting(query) {
  return request({
    url: '/oms/setting/list',
    method: 'get',
    params: query
  })
}

// 查询订单配置信息详细
export function getSetting(id) {
  return request({
    url: '/oms/setting/' + id,
    method: 'get'
  })
}

// 新增订单配置信息
export function addSetting(data) {
  return request({
    url: '/oms/setting',
    method: 'post',
    data: data
  })
}

// 修改订单配置信息
export function updateSetting(data) {
  return request({
    url: '/oms/setting',
    method: 'put',
    data: data
  })
}

// 删除订单配置信息
export function delSetting(id) {
  return request({
    url: '/oms/setting/' + id,
    method: 'delete'
  })
}
