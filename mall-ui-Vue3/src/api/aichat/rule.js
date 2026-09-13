import request from '@/utils/request'

// 查询NL2SQL业务规则列表
export function listRule(query) {
  return request({
    url: '/aichat/rule/list',
    method: 'get',
    params: query
  })
}

// 查询NL2SQL业务规则详细
export function getRule(id) {
  return request({
    url: '/aichat/rule/' + id,
    method: 'get'
  })
}

// 新增NL2SQL业务规则
export function addRule(data) {
  return request({
    url: '/aichat/rule',
    method: 'post',
    data: data
  })
}

// 修改NL2SQL业务规则
export function updateRule(data) {
  return request({
    url: '/aichat/rule',
    method: 'put',
    data: data
  })
}

// 删除NL2SQL业务规则
export function delRule(id) {
  return request({
    url: '/aichat/rule/' + id,
    method: 'delete'
  })
}
