import request from '@/utils/request'

// 查询NL2SQL指标定义列表
export function listMetric(query) {
  return request({
    url: '/aichat/metric/list',
    method: 'get',
    params: query
  })
}

// 查询NL2SQL指标定义详细
export function getMetric(id) {
  return request({
    url: '/aichat/metric/' + id,
    method: 'get'
  })
}

// 新增NL2SQL指标定义
export function addMetric(data) {
  return request({
    url: '/aichat/metric',
    method: 'post',
    data: data
  })
}

// 修改NL2SQL指标定义
export function updateMetric(data) {
  return request({
    url: '/aichat/metric',
    method: 'put',
    data: data
  })
}

// 删除NL2SQL指标定义
export function delMetric(id) {
  return request({
    url: '/aichat/metric/' + id,
    method: 'delete'
  })
}
