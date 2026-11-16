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
 * 请求体与普通对话一致，仅 question 为工具调用信息的 JSON 字符串（toolName 合并进参数对象）：
 * {"deptName":"研发部","confirmed":true,"toolName":"deptCrud"}
 *
 * @param {string} toolName   - 工具名称（如 "deptCrud"）
 * @param {object} argsObj    - 合并后的完整参数（含 confirmed=true、用户填的表单字段等）
 * @param {string} conversationId - 会话记忆ID
 * @param {AbortSignal} signal - 用于中止请求的信号
 * @returns {Response} fetch Response 对象
 */
export async function replayToolCall(toolName, argsObj, conversationId, signal) {
  // 不额外传 arguments 参数：toolName 合并进参数对象后整体序列化为 JSON，放入 question
  const payload = JSON.stringify({ ...(argsObj || {}), toolName })
  const response = await fetch('/dev-api/ai-chat/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'Bearer ' + getToken()
    },
    body: `question=${encodeURIComponent(payload)}&conversationId=${conversationId}`,
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