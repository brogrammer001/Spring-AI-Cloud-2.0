import request from '@/utils/request'

// 查询首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】列表
export function listSubject(query) {
  return request({
    url: '/sms/subject/list',
    method: 'get',
    params: query
  })
}

// 查询首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】详细
export function getSubject(id) {
  return request({
    url: '/sms/subject/' + id,
    method: 'get'
  })
}

// 新增首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】
export function addSubject(data) {
  return request({
    url: '/sms/subject',
    method: 'post',
    data: data
  })
}

// 修改首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】
export function updateSubject(data) {
  return request({
    url: '/sms/subject',
    method: 'put',
    data: data
  })
}

// 删除首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】
export function delSubject(id) {
  return request({
    url: '/sms/subject/' + id,
    method: 'delete'
  })
}
