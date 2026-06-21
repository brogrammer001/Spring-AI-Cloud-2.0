import { getToken } from '@/utils/auth'

/**
 * 发送流式聊天消息
 * @param {string} question 用户输入的问题
 * @param {function} onChunk 收到每一段文字时的回调（打字机效果）
 * @param {function} onEnd 流式结束时的回调
 * @param {function} onError 报错时的回调
 */
export function sendChatMessage(question, { onChunk, onEnd, onError }) {
  fetch('/dev-api/chat/chat/send', {
    method: 'POST',
    // 后端是直接接收 String question，必须用表单格式
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'Bearer ' + getToken()
    },
    body: `question=${encodeURIComponent(question)}`,
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      
      // 用于缓存不完整的数据行
      let buffer = ''
      // 用于存储所有接收到的完整内容
      let fullContent = ''

      // 递归读取流数据
      function read() {
        reader.read().then(({ done, value }) => {
          if (done) {
            // 处理缓冲区中剩余的数据
            if (buffer.trim()) {
              processLine(buffer)
            }
            // 流结束，传递完整内容
            if (onEnd) onEnd(fullContent)
            return
          }

          // 解码当前 chunk 并追加到缓冲区
          const chunk = decoder.decode(value, { stream: true })
          buffer += chunk
          
          // 按行分割，保留最后一个不完整的行在缓冲区
          const lines = buffer.split('\n')
          // 最后一个元素可能是不完整的行，保留到下一次
          buffer = lines.pop() || ''
          
          // 处理完整的行
          for (const line of lines) {
            processLine(line)
          }
          
          // 继续读取下一段
          read()
        }).catch((err) => {
          if (onError) onError(err)
        })
      }
      
      // 处理单行 SSE 数据
      function processLine(line) {
        const trimmedLine = line.trim()
        if (trimmedLine.startsWith('data:')) {
          const pureText = trimmedLine.substring(5).trim()
          if (pureText) {
            fullContent += pureText
            if (onChunk) {
              onChunk(pureText)
            }
          }
        }
      }

      // 启动读取
      read()
    })
    .catch((error) => {
      console.error('请求失败:', error)
      if (onError) onError(error)
    })
}
