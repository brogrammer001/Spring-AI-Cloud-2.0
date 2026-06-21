import request from '@/utils/request'

// 查询秒杀商品通知订阅列表
export function listNotice(query) {
  return request({
    url: '/sms/notice/list',
    method: 'get',
    params: query
  })
}

// 查询秒杀商品通知订阅详细
export function getNotice(id) {
  return request({
    url: '/sms/notice/' + id,
    method: 'get'
  })
}

// 新增秒杀商品通知订阅
export function addNotice(data) {
  return request({
    url: '/sms/notice',
    method: 'post',
    data: data
  })
}

// 修改秒杀商品通知订阅
export function updateNotice(data) {
  return request({
    url: '/sms/notice',
    method: 'put',
    data: data
  })
}

// 删除秒杀商品通知订阅
export function delNotice(id) {
  return request({
    url: '/sms/notice/' + id,
    method: 'delete'
  })
}
