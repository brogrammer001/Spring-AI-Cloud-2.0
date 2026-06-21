import request from '@/utils/request'

// 查询sku图片列表
export function listImages(query) {
  return request({
    url: '/pms/images/list',
    method: 'get',
    params: query
  })
}

// 查询sku图片详细
export function getImages(id) {
  return request({
    url: '/pms/images/' + id,
    method: 'get'
  })
}

// 新增sku图片
export function addImages(data) {
  return request({
    url: '/pms/images',
    method: 'post',
    data: data
  })
}

// 修改sku图片
export function updateImages(data) {
  return request({
    url: '/pms/images',
    method: 'put',
    data: data
  })
}

// 删除sku图片
export function delImages(id) {
  return request({
    url: '/pms/images/' + id,
    method: 'delete'
  })
}
