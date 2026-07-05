import request from '@/utils/request'

// 查询【请填写功能名称】列表
export function listChatmemory(query) {
  return request({
    url: '/ai-chat/chatmemory/list',
    method: 'get',
    params: query
  })
}

// 查询【请填写功能名称】详细
export function getChatmemory(conversationId) {
  return request({
    url: '/ai-chat/chatmemory/' + conversationId,
    method: 'get'
  })
}

// 新增【请填写功能名称】
export function addChatmemory(data) {
  return request({
    url: '/ai-chat/chatmemory',
    method: 'post',
    data: data
  })
}

// 修改【请填写功能名称】
export function updateChatmemory(data) {
  return request({
    url: '/ai-chat/chatmemory',
    method: 'put',
    data: data
  })
}

// 删除【请填写功能名称】
export function delChatmemory(conversationId) {
  return request({
    url: '/ai-chat/chatmemory/' + conversationId,
    method: 'delete'
  })
}
