import request from '@/utils/request'

// 查询订单项信息列表
export function listItem(query) {
  return request({
    url: '/oms/item/list',
    method: 'get',
    params: query
  })
}

// 查询订单项信息详细
export function getItem(id) {
  return request({
    url: '/oms/item/' + id,
    method: 'get'
  })
}

// 新增订单项信息
export function addItem(data) {
  return request({
    url: '/oms/item',
    method: 'post',
    data: data
  })
}

// 修改订单项信息
export function updateItem(data) {
  return request({
    url: '/oms/item',
    method: 'put',
    data: data
  })
}

// 删除订单项信息
export function delItem(id) {
  return request({
    url: '/oms/item/' + id,
    method: 'delete'
  })
}
