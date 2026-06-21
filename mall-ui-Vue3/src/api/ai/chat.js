import { getToken } from '@/utils/auth'

/**
 * 发送聊天消息（流式响应）
 * @param {string} question - 用户问题
 * @param {string} memoryId - 会话记忆ID
 * @param {AbortSignal} signal - 用于中止请求的信号
 * @returns {Response} fetch Response 对象，可用于读取流式数据
 */
export async function sendChatMessage(question, memoryId, signal) {
  const response = await fetch('/dev-api/ai-chat/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'Bearer ' + getToken()
    },
    body: `question=${encodeURIComponent(question)}&memoryId=${memoryId}`,
    signal
  })
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  return response
}
