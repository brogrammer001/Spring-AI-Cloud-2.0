import request from '@/utils/request'

// 查询【请填写功能名称】列表
export function listConversation(query) {
  return request({
    url: '/ai-chat/conversation/list',
    method: 'get',
    params: query
  })
}

// 查询【请填写功能名称】详细
export function getConversation(id) {
  return request({
    url: '/ai-chat/conversation/' + id,
    method: 'get'
  })
}

// 查询【请填写功能名称】详细
export function getConversationListByUserId() {
  return request({
    url: '/ai-chat/conversation/getConversationListByUserId',
    method: 'get'
  })
}

// 新增会话
export function create(question) {
  return request({
    url: '/ai-chat/conversation/create',
    method: 'get',
    params: {question}
  })
}

// 新增【请填写功能名称】
export function addConversation(data) {
  return request({
    url: '/ai-chat/conversation',
    method: 'post',
    data: data
  })
}

// 修改【请填写功能名称】
export function updateConversation(data) {
  return request({
    url: '/ai-chat/conversation',
    method: 'put',
    data: data
  })
}

// 删除【请填写功能名称】
export function delConversation(id) {
  return request({
    url: '/ai-chat/conversation/' + id,
    method: 'delete'
  })
}

// 删除【请填写功能名称】
export function deleteByConversationId(id) {
  return request({
    url: '/ai-chat/conversation/deleteByConversationId/' + id,
    method: 'delete'
  })
}
