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

        if (trimmedLine.startsWith('event:')) {
          const eventType = trimmedLine.substring(6).trim();
          if (eventType === 'done') {
            onDone && onDone();
            return;
          }
          continue;
        }

        let content = trimmedLine;
        if (content.startsWith('data:')) {
          content = content.substring(5).trim();
        }
        if (!content) continue;

        try {
          const jsonData = JSON.parse(content);
          const { msg } = jsonData;

          if (msg && typeof msg === 'string') {
            if (!isInnerJson && (msg.startsWith('{') || msg.startsWith('['))) {
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
          const { msg } = jsonData;

          if (msg && typeof msg === 'string') {
            if (!isInnerJson && (msg.startsWith('{') || msg.startsWith('['))) {
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

function parseNestedJson(buffer) {
  let parsed = null;
  try {
    parsed = JSON.parse(buffer);
  } catch (e) {
    return null;
  }

  if (parsed && typeof parsed === 'object') {
    if (parsed.msg !== undefined && parsed.code !== undefined) {
      return parsed;
    }
  }

  if (Array.isArray(parsed) && parsed.length > 0) {
    const firstItem = parsed[0];
    if (firstItem && firstItem.text) {
      let textParsed = null;
      try {
        textParsed = JSON.parse(firstItem.text);
      } catch (e) {
        return null;
      }
      if (textParsed && typeof textParsed === 'object') {
        return textParsed;
      }
    }
  }

  return null;
}

function tryParseInnerJson(buffer, isInnerJson, onTextChange, onJsonChunk) {
  if (!buffer) return false;

  if (!isInnerJson) {
    onTextChange && onTextChange(buffer);
    return true;
  }

  const parsed = parseNestedJson(buffer);

  if (parsed) {
    const innerMsg = parsed.msg || '';
    const innerCode = parsed.code;
    const innerData = parsed.data;

    if (innerCode === 8001 && innerData && typeof innerData === 'string' && innerData.trim()) {
      onTextChange && onTextChange(innerMsg);
      onJsonChunk && onJsonChunk({
        msg: innerMsg,
        code: innerCode,
        data: innerData
      });
    } else if (innerCode === 500) {
      onTextChange && onTextChange(innerMsg);
      onJsonChunk && onJsonChunk({
        msg: innerMsg,
        code: innerCode,
        data: innerData
      });
    } else {
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

  const parsed = parseNestedJson(buffer);

  if (parsed) {
    const innerMsg = parsed.msg || '';
    const innerCode = parsed.code;
    const innerData = parsed.data;

    if (innerCode === 8001 && innerData && typeof innerData === 'string' && innerData.trim()) {
      onTextChange && onTextChange(innerMsg);
      onJsonChunk && onJsonChunk({
        msg: innerMsg,
        code: innerCode,
        data: innerData
      });
    } else if (innerCode === 500) {
      onTextChange && onTextChange(innerMsg);
      onJsonChunk && onJsonChunk({
        msg: innerMsg,
        code: innerCode,
        data: innerData
      });
    } else {
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
    }
  } else {
    onTextChange && onTextChange(buffer);
  }
}
