import request from '@/utils/request'

// 查询秒杀活动场次列表
export function listSession(query) {
  return request({
    url: '/sms/session/list',
    method: 'get',
    params: query
  })
}

// 查询秒杀活动场次详细
export function getSession(id) {
  return request({
    url: '/sms/session/' + id,
    method: 'get'
  })
}

// 新增秒杀活动场次
export function addSession(data) {
  return request({
    url: '/sms/session',
    method: 'post',
    data: data
  })
}

// 修改秒杀活动场次
export function updateSession(data) {
  return request({
    url: '/sms/session',
    method: 'put',
    data: data
  })
}

// 删除秒杀活动场次
export function delSession(id) {
  return request({
    url: '/sms/session/' + id,
    method: 'delete'
  })
}
