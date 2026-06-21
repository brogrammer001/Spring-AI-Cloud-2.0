import request from '@/utils/request'

// 查询优惠券领取历史记录列表
export function listHistory(query) {
  return request({
    url: '/sms/history/list',
    method: 'get',
    params: query
  })
}

// 查询优惠券领取历史记录详细
export function getHistory(id) {
  return request({
    url: '/sms/history/' + id,
    method: 'get'
  })
}

// 新增优惠券领取历史记录
export function addHistory(data) {
  return request({
    url: '/sms/history',
    method: 'post',
    data: data
  })
}

// 修改优惠券领取历史记录
export function updateHistory(data) {
  return request({
    url: '/sms/history',
    method: 'put',
    data: data
  })
}

// 删除优惠券领取历史记录
export function delHistory(id) {
  return request({
    url: '/sms/history/' + id,
    method: 'delete'
  })
}
