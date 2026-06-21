import request from '@/utils/request'

// 查询优惠券分类关联列表
export function listRelation(query) {
  return request({
    url: '/sms/relation/list',
    method: 'get',
    params: query
  })
}

// 查询优惠券分类关联详细
export function getRelation(id) {
  return request({
    url: '/sms/relation/' + id,
    method: 'get'
  })
}

// 新增优惠券分类关联
export function addRelation(data) {
  return request({
    url: '/sms/relation',
    method: 'post',
    data: data
  })
}

// 修改优惠券分类关联
export function updateRelation(data) {
  return request({
    url: '/sms/relation',
    method: 'put',
    data: data
  })
}

// 删除优惠券分类关联
export function delRelation(id) {
  return request({
    url: '/sms/relation/' + id,
    method: 'delete'
  })
}
