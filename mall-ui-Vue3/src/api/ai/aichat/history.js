import request from '@/utils/request'

// 查询【请填写功能名称】列表
export function listHistory(query) {
  return request({
    url: '/ai-chat/history/list',
    method: 'get',
    params: query
  })
}

// 查询【请填写功能名称】详细
export function getHistory(conversationId) {
  return request({
    url: '/ai-chat/history/' + conversationId,
    method: 'get'
  })
}

// 新增【请填写功能名称】
export function addHistory(data) {
  return request({
    url: '/ai-chat/history',
    method: 'post',
    data: data
  })
}

// 修改【请填写功能名称】
export function updateHistory(data) {
  return request({
    url: '/ai-chat/history',
    method: 'put',
    data: data
  })
}

// 删除【请填写功能名称】
export function delHistory(conversationId) {
  return request({
    url: '/ai-chat/history/' + conversationId,
    method: 'delete'
  })
}

// 查询会话历史
export function getChatMemoryListByConversationId(conversationId) {
  return request({
    url: '/ai-chat/history/getChatMemoryListByConversationId/' + conversationId,
    method: 'get'
  })
}