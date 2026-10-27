import { getToken } from '@/utils/auth'

/**
 * 发送聊天消息（流式响应）
 * @param {string} question - 用户问题
 * @param {string} conversationId - 会话记忆ID
 * @param {AbortSignal} signal - 用于中止请求的信号
 * @returns {Response} fetch Response 对象，可用于读取流式数据
 */
export async function sendChatMessage(question, conversationId, signal) {
  const response = await fetch('/dev-api/ai-chat/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'Bearer ' + getToken()
    },
    body: `question=${encodeURIComponent(question)}&conversationId=${conversationId}`,
    signal
  })
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  return response
}

/**
 * 重放工具调用（流式响应）
 * 当后端返回 code=8002/8003 需要用户补全字段或确认时，前端使用此接口携带工具名和参数重放。
 *
 * @param {string} toolName   - 工具名称（如 "deptCrud"）
 * @param {object} argsObj    - 合并后的完整参数（含 confirmed=true、用户填的表单字段等）
 * @param {string} conversationId - 会话记忆ID
 * @param {AbortSignal} signal - 用于中止请求的信号
 * @returns {Response} fetch Response 对象
 */
export async function replayToolCall(toolName, argsObj, conversationId, signal) {
  const response = await fetch('/dev-api/ai-chat/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + getToken()
    },
    body: JSON.stringify({
      question: '',
      conversationId: conversationId,
      toolName: toolName,
      arguments: argsObj
    }),
    signal
  })
  if (!response.ok) {
    let errorMsg = `HTTP error! status: ${response.status}`
    try {
      const errorData = await response.json()
      if (errorData && errorData.msg) errorMsg = errorData.msg
    } catch (_) { /* 忽略非 JSON 错误体 */ }
    throw new Error(errorMsg)
  }
  return response
}