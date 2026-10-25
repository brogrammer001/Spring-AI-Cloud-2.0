import request from '@/utils/request'

// 查询NL2SQL黄金评测集列表
export function listEval(query) {
  return request({
    url: '/ai-chat/eval/list',
    method: 'get',
    params: query
  })
}

// 查询NL2SQL黄金评测集详细
export function getEval(id) {
  return request({
    url: '/ai-chat/eval/' + id,
    method: 'get'
  })
}

// 新增NL2SQL黄金评测集
export function addEval(data) {
  return request({
    url: '/ai-chat/eval',
    method: 'post',
    data: data
  })
}

// 修改NL2SQL黄金评测集
export function updateEval(data) {
  return request({
    url: '/ai-chat/eval',
    method: 'put',
    data: data
  })
}

// 删除NL2SQL黄金评测集
export function delEval(id) {
  return request({
    url: '/ai-chat/eval/' + id,
    method: 'delete'
  })
}
