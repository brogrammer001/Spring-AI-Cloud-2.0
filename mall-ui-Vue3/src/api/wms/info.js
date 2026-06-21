import request from '@/utils/request'

// 查询仓库信息列表
export function listInfo(query) {
  return request({
    url: '/wms/info/list',
    method: 'get',
    params: query
  })
}

// 查询仓库信息详细
export function getInfo(id) {
  return request({
    url: '/wms/info/' + id,
    method: 'get'
  })
}

// 新增仓库信息
export function addInfo(data) {
  return request({
    url: '/wms/info',
    method: 'post',
    data: data
  })
}

// 修改仓库信息
export function updateInfo(data) {
  return request({
    url: '/wms/info',
    method: 'put',
    data: data
  })
}

// 删除仓库信息
export function delInfo(id) {
  return request({
    url: '/wms/info/' + id,
    method: 'delete'
  })
}
