import request from '@/utils/request'

// 查询秒杀活动列表
export function listPromotion(query) {
  return request({
    url: '/sms/promotion/list',
    method: 'get',
    params: query
  })
}

// 查询秒杀活动详细
export function getPromotion(id) {
  return request({
    url: '/sms/promotion/' + id,
    method: 'get'
  })
}

// 新增秒杀活动
export function addPromotion(data) {
  return request({
    url: '/sms/promotion',
    method: 'post',
    data: data
  })
}

// 修改秒杀活动
export function updatePromotion(data) {
  return request({
    url: '/sms/promotion',
    method: 'put',
    data: data
  })
}

// 删除秒杀活动
export function delPromotion(id) {
  return request({
    url: '/sms/promotion/' + id,
    method: 'delete'
  })
}
