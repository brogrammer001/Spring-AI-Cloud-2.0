import request from '@/utils/request'

// 查询商品阶梯价格列表
export function listLadder(query) {
  return request({
    url: '/sms/ladder/list',
    method: 'get',
    params: query
  })
}

// 查询商品阶梯价格详细
export function getLadder(id) {
  return request({
    url: '/sms/ladder/' + id,
    method: 'get'
  })
}

// 新增商品阶梯价格
export function addLadder(data) {
  return request({
    url: '/sms/ladder',
    method: 'post',
    data: data
  })
}

// 修改商品阶梯价格
export function updateLadder(data) {
  return request({
    url: '/sms/ladder',
    method: 'put',
    data: data
  })
}

// 删除商品阶梯价格
export function delLadder(id) {
  return request({
    url: '/sms/ladder/' + id,
    method: 'delete'
  })
}
