import request from '@/utils/request'

// 查询专题商品列表
export function listSpu(query) {
  return request({
    url: '/sms/spu/list',
    method: 'get',
    params: query
  })
}

// 查询专题商品详细
export function getSpu(id) {
  return request({
    url: '/sms/spu/' + id,
    method: 'get'
  })
}

// 新增专题商品
export function addSpu(data) {
  return request({
    url: '/sms/spu',
    method: 'post',
    data: data
  })
}

// 修改专题商品
export function updateSpu(data) {
  return request({
    url: '/sms/spu',
    method: 'put',
    data: data
  })
}

// 删除专题商品
export function delSpu(id) {
  return request({
    url: '/sms/spu/' + id,
    method: 'delete'
  })
}
