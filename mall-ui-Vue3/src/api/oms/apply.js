import request from '@/utils/request'

// 查询订单退货申请列表
export function listApply(query) {
  return request({
    url: '/oms/apply/list',
    method: 'get',
    params: query
  })
}

// 查询订单退货申请详细
export function getApply(id) {
  return request({
    url: '/oms/apply/' + id,
    method: 'get'
  })
}

// 新增订单退货申请
export function addApply(data) {
  return request({
    url: '/oms/apply',
    method: 'post',
    data: data
  })
}

// 修改订单退货申请
export function updateApply(data) {
  return request({
    url: '/oms/apply',
    method: 'put',
    data: data
  })
}

// 删除订单退货申请
export function delApply(id) {
  return request({
    url: '/oms/apply/' + id,
    method: 'delete'
  })
}
