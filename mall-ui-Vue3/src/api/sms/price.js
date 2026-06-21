import request from '@/utils/request'

// 查询商品会员价格列表
export function listPrice(query) {
  return request({
    url: '/sms/price/list',
    method: 'get',
    params: query
  })
}

// 查询商品会员价格详细
export function getPrice(id) {
  return request({
    url: '/sms/price/' + id,
    method: 'get'
  })
}

// 新增商品会员价格
export function addPrice(data) {
  return request({
    url: '/sms/price',
    method: 'post',
    data: data
  })
}

// 修改商品会员价格
export function updatePrice(data) {
  return request({
    url: '/sms/price',
    method: 'put',
    data: data
  })
}

// 删除商品会员价格
export function delPrice(id) {
  return request({
    url: '/sms/price/' + id,
    method: 'delete'
  })
}
