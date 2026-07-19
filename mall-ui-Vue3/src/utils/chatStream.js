export async function parseStream({ response, onTextChange, onDone, onJsonChunk, onError }) {
  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let innerBuffer = '';
  let isInnerJson = false;

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        const trimmedLine = line.trim();
        if (!trimmedLine) continue;

        let content = trimmedLine;
        if (content.startsWith('data:')) {
          content = content.substring(5).trim();
        }
        if (!content) continue;

        try {
          const jsonData = JSON.parse(content);
          const { msg, code } = jsonData;

          if (code === -1) {
            onDone && onDone();
            return;
          }

          if (msg && typeof msg === 'string') {
            if (!isInnerJson && msg.startsWith('{')) {
              isInnerJson = true;
            }
            innerBuffer += msg;
            if (tryParseInnerJson(innerBuffer, isInnerJson, onTextChange, onJsonChunk)) {
              innerBuffer = '';
            }
          }

          onJsonChunk && onJsonChunk(jsonData);
        } catch (e) {
          console.error('解析 JSON 失败:', content, e);
        }
      }
    }

    if (buffer.trim()) {
      let content = buffer.trim();
      if (content.startsWith('data:')) {
        content = content.substring(5).trim();
      }
      if (content) {
        try {
          const jsonData = JSON.parse(content);
          const { msg, code } = jsonData;

          if (code === -1) {
            onDone && onDone();
            return;
          }

          if (msg && typeof msg === 'string') {
            if (!isInnerJson && msg.startsWith('{')) {
              isInnerJson = true;
            }
            innerBuffer += msg;
            if (tryParseInnerJson(innerBuffer, isInnerJson, onTextChange, onJsonChunk)) {
              innerBuffer = '';
            }
          }

          onJsonChunk && onJsonChunk(jsonData);
        } catch (e) {
          console.error('解析残留 JSON 失败:', content, e);
        }
      }
    }

    processRemainingBuffer(innerBuffer, isInnerJson, onTextChange, onJsonChunk);
    onDone && onDone();
  } catch (error) {
    onError && onError(error);
    throw error;
  }
}

function tryParseInnerJson(buffer, isInnerJson, onTextChange, onJsonChunk) {
  if (!buffer) return false;

  if (!isInnerJson) {
    onTextChange && onTextChange(buffer);
    return true;
  }

  let parsed = null;
  try {
    parsed = JSON.parse(buffer);
  } catch (e) {
    return false;
  }

  if (parsed && typeof parsed === 'object') {
    const innerMsg = parsed.msg || '';
    const innerCode = parsed.code;
    const innerData = parsed.data;

    if (innerMsg && typeof innerMsg === 'string') {
      onTextChange && onTextChange(innerMsg);
    }

    if (innerCode !== undefined) {
      onJsonChunk && onJsonChunk({
        msg: innerMsg,
        code: innerCode,
        data: innerData
      });
    }
    return true;
  }
  return false;
}

function processRemainingBuffer(buffer, isInnerJson, onTextChange, onJsonChunk) {
  if (!buffer) return;

  if (!isInnerJson) {
    onTextChange && onTextChange(buffer);
    return;
  }

  let parsed = null;
  try {
    parsed = JSON.parse(buffer);
  } catch (e) {
    onTextChange && onTextChange(buffer);
    return;
  }

  if (parsed && typeof parsed === 'object') {
    const innerMsg = parsed.msg || '';
    const innerCode = parsed.code;
    const innerData = parsed.data;

    if (innerMsg && typeof innerMsg === 'string') {
      onTextChange && onTextChange(innerMsg);
    }

    if (innerCode !== undefined) {
      onJsonChunk && onJsonChunk({
        msg: innerMsg,
        code: innerCode,
        data: innerData
      });
    }
  } else {
    onTextChange && onTextChange(buffer);
  }
}
