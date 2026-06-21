import request from '@/utils/request'

// 查询会员统计信息列表
export function listInfo(query) {
  return request({
    url: '/ums/info/list',
    method: 'get',
    params: query
  })
}

// 查询会员统计信息详细
export function getInfo(id) {
  return request({
    url: '/ums/info/' + id,
    method: 'get'
  })
}

// 新增会员统计信息
export function addInfo(data) {
  return request({
    url: '/ums/info',
    method: 'post',
    data: data
  })
}

// 修改会员统计信息
export function updateInfo(data) {
  return request({
    url: '/ums/info',
    method: 'put',
    data: data
  })
}

// 删除会员统计信息
export function delInfo(id) {
  return request({
    url: '/ums/info/' + id,
    method: 'delete'
  })
}
