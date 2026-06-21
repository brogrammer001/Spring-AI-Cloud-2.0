import request from '@/utils/request'

// 查询首页轮播广告列表
export function listAdv(query) {
  return request({
    url: '/sms/adv/list',
    method: 'get',
    params: query
  })
}

// 查询首页轮播广告详细
export function getAdv(id) {
  return request({
    url: '/sms/adv/' + id,
    method: 'get'
  })
}

// 新增首页轮播广告
export function addAdv(data) {
  return request({
    url: '/sms/adv',
    method: 'post',
    data: data
  })
}

// 修改首页轮播广告
export function updateAdv(data) {
  return request({
    url: '/sms/adv',
    method: 'put',
    data: data
  })
}

// 删除首页轮播广告
export function delAdv(id) {
  return request({
    url: '/sms/adv/' + id,
    method: 'delete'
  })
}
