import request from '@/utils/request'

// 查询sku销售属性&值列表
export function listValue(query) {
  return request({
    url: '/pms/value/list',
    method: 'get',
    params: query
  })
}

// 查询sku销售属性&值详细
export function getValue(id) {
  return request({
    url: '/pms/value/' + id,
    method: 'get'
  })
}

// 新增sku销售属性&值
export function addValue(data) {
  return request({
    url: '/pms/value',
    method: 'post',
    data: data
  })
}

// 修改sku销售属性&值
export function updateValue(data) {
  return request({
    url: '/pms/value',
    method: 'put',
    data: data
  })
}

// 删除sku销售属性&值
export function delValue(id) {
  return request({
    url: '/pms/value/' + id,
    method: 'delete'
  })
}
