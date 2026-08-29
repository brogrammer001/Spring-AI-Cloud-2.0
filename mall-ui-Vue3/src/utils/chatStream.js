/**
 * SSE 流式解析工具（基于 fetch ReadableStream + EventTarget 事件分发）
 *
 * 为什么不用原生 EventSource？
 *   EventSource 仅支持 GET 请求，无法携带 POST body 和 Authorization 头，
 *   而聊天接口是 POST，因此基于 fetch 实现符合 SSE 规范的解析与事件分发，
 *   对外暴露与 EventSource 一致的 addEventListener 用法：
 *
 *     const es = createEventStream(response);
 *     es.addEventListener('message',      e => appendText(e.text));
 *     es.addEventListener('thought',      e => showThinking(e.json));
 *     es.addEventListener('rag_retrieve', e => ...);
 *     es.addEventListener('tool_call',    e => ...);
 *     es.addEventListener('message_end',  e => finish(e.json));
 *     es.addEventListener('error',        e => showError(e.json ?? e.data));
 *     es.addEventListener('stream_error', e => console.error(e.error)); // 传输层错误
 *     es.addEventListener('complete',     () => ...);                   // 流关闭
 *     await es.start();
 *
 * 遵循 W3C SSE 规范（https://html.spec.whatwg.org/multipage/server-sent-events.html）：
 *   - 事件块以空行分隔；event:/data:/id:/retry: 为字段行
 *   - 字段名与值以第一个冒号分隔，值仅剥离一个前导空格，其余空白原样保留（Markdown 依赖）
 *   - 多行 data 以 \n 拼接；\r\n / \n / \r 均为合法换行
 *   - 未声明 event: 的事件块默认类型为 message
 *   - 以 : 开头的注释行忽略（常用于心跳保活）
 *
 * 兼容旧后端：data 为 JSON 且内嵌 event 字段（如 {"event":"message_end"}）时，
 *   按内嵌 event 类型二次分发，新旧协议可共存。
 */

/** 事件对象：e.data 原始字符串，e.json 解析后的 JSON（无法解析时为 null），e.text 常见文本字段 */
class SseEvent extends Event {
  constructor(type, data, json) {
    super(type);
    this.data = data;
    this.json = json;
  }

  /** 便捷取文本：优先 text（新协议），其次 content / msg（旧协议），否则原样 */
  get text() {
    const j = this.json;
    if (j == null) return this.data;
    if (typeof j.text === 'string') return j.text;
    if (typeof j.content === 'string') return j.content;
    if (typeof j.msg === 'string') return j.msg;
    return this.data;
  }
}

/**
 * 创建 SSE 事件流。
 * @param {Response} response fetch 返回的 response（body 为 ReadableStream）
 * @returns {{ addEventListener, removeEventListener, start: () => Promise<void> }}
 */
export function createEventStream(response) {
  const target = new EventTarget();
  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');

  // ---- 解析器状态：当前事件块 ----
  let buffer = '';        // 未消费的字节文本（可能含不完整的行）
  let eventType = '';     // 当前块的 event: 字段
  let dataLines = [];     // 当前块的 data: 行集合

  /** 空行 = 事件块结束，派发并重置 */
  function dispatchBlock() {
    // 规范：既无 data 也无 event 的块（如纯注释心跳）不派发
    if (dataLines.length === 0 && !eventType) {
      eventType = '';
      return;
    }
    const data = dataLines.join('\n');

    let json = null;
    if (data) {
      try { json = JSON.parse(data); } catch (_) { /* 非 JSON 载荷，原样保留在 data */ }
    }

    // 兼容旧协议：SSE 未声明 event 行，但 JSON 内嵌 event 字段 → 按其类型分发
    let type = eventType || 'message';
    if (!eventType && json && typeof json.event === 'string' && json.event) {
      type = json.event;
    }

    target.dispatchEvent(new SseEvent(type, data, json));
    eventType = '';
    dataLines = [];
  }

  /** 处理一行（已去除换行符） */
  function processLine(line) {
    if (line === '') {
      dispatchBlock();
      return;
    }
    // 注释行（: heartbeat 等）忽略
    if (line.startsWith(':')) return;

    const colon = line.indexOf(':');
    const field = colon === -1 ? line : line.slice(0, colon);
    let value = colon === -1 ? '' : line.slice(colon + 1);
    // 规范：冒号后仅剥离一个前导空格，其余空白保留
    if (value.startsWith(' ')) value = value.slice(1);

    switch (field) {
      case 'event': eventType = value; break;
      case 'data':  dataLines.push(value); break;
      // id / retry 字段当前场景无需消费，规范要求忽略即可
      default: break;
    }
  }

  /** 喂入一段解码后的文本，切分出所有完整行；兼容 \r\n / \n / \r 换行 */
  function feed(chunk) {
    buffer += chunk;
    let i = 0;
    while (i < buffer.length) {
      const nl = buffer.indexOf('\n', i);
      const cr = buffer.indexOf('\r', i);
      let end = -1;
      let sepLen = 0;

      if (nl !== -1 && (cr === -1 || nl < cr)) {
        end = nl; sepLen = 1;                       // \n
      } else if (cr !== -1) {
        if (cr === buffer.length - 1) break;        // \r 在缓冲末尾：可能是 \r\n 前半，等下一片
        end = cr; sepLen = buffer[cr + 1] === '\n' ? 2 : 1; // \r\n 或单独 \r
      } else {
        break;                                      // 无换行，行未完整
      }

      processLine(buffer.slice(i, end));
      i = end + sepLen;
    }
    buffer = buffer.slice(i);
  }

  /** 消费整个流；流自然关闭时冲刷残留块并派发 complete */
  async function start() {
    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        feed(decoder.decode(value, { stream: true }));
      }
      const tail = decoder.decode();
      if (tail) feed(tail);
      if (buffer) { processLine(buffer); buffer = ''; } // 末行无换行符的情况
      dispatchBlock();                                   // 冲刷最后一个未派发的块
      target.dispatchEvent(new Event('complete'));
    } catch (error) {
      // 传输层错误（网络中断 / AbortController 中止），与后端业务 error 事件区分开
      const ev = new Event('stream_error');
      ev.error = error;
      target.dispatchEvent(ev);
    } finally {
      try { reader.releaseLock(); } catch (_) { /* reader 已关闭时忽略 */ }
    }
  }

  return {
    addEventListener: target.addEventListener.bind(target),
    removeEventListener: target.removeEventListener.bind(target),
    start
  };
}

// ---------------------------------------------------------------------------
// 旧版回调式 API 兼容层：基于 createEventStream 实现，保留旧调用方不破坏
// ---------------------------------------------------------------------------
export async function parseStream({ response, onTextChange, onDone, onJsonChunk, onError }) {
  const es = createEventStream(response);
  let ended = false;

  const finish = () => {
    if (ended) return;
    ended = true;
    onDone && onDone();
  };

  es.addEventListener('message', (e) => {
    const json = e.json;
    if (json && json.code !== undefined) {
      onJsonChunk && onJsonChunk(json);
    }
    const text = e.text;
    if (typeof text === 'string' && text) {
      onTextChange && onTextChange(text);
    }
  });

  es.addEventListener('message_end', (e) => {
    if (e.json && e.json.code !== undefined) {
      onJsonChunk && onJsonChunk(e.json);
    }
    finish();
  });

  es.addEventListener('done', finish);

  es.addEventListener('error', (e) => {
    const json = e.json || null;
    if (json) onJsonChunk && onJsonChunk(json);
    onError && onError(new Error(json ? (json.msg || json.state || e.data) : (e.data || '服务端错误')));
  });

  es.addEventListener('stream_error', (e) => {
    onError && onError(e.error);
  });

  es.addEventListener('complete', finish);

  await es.start();
  finish();
}
