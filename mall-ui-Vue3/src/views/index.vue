<template>
  <div class="app-container no-card" :style="{ height: `calc(100vh - ${64 + (settingsStore.tagsView ? 40 : 0)}px)` }">
    <div class="flex h-full overflow-hidden" style="position: relative;">
      <aside
        class="chat-aside flex flex-col transition-colors duration-300 w-64 m-0 py-2 px-0"
        :class="{ 'is-dark': settingsStore.isDark }">
        <div class="px-4 pb-2 flex-shrink-0">
          <button @click="startNewConversation"
            class="chat-btn chat-btn-primary w-full h-12 flex items-center justify-center px-4 rounded-lg font-medium text-sm">
            <i class="fas fa-plus mr-2"></i>
            <span>新对话</span>
          </button>
        </div>
        <div class="flex-1 overflow-y-auto px-2 custom-scrollbar">
          <div class="text-xs font-semibold px-3 mb-2"
            :class="settingsStore.isDark ? 'text-[#8b8899]' : 'text-[#8b8899]'">
            最近对话
          </div>
          <div v-if="conversations.length === 0" class="text-center py-4 text-sm"
            :class="settingsStore.isDark ? 'text-[#6b6880]' : 'text-[#b6b3c2]'">
            暂无对话记录
          </div>
          <div v-for="(conv, index) in conversations" :key="conv.id" @click="switchConversation(conv.id)"
            :class="['conv-item group flex items-center px-3 py-2.5 rounded-lg cursor-pointer mb-1', { 'is-active': conv.id === activeId, 'is-dark': settingsStore.isDark }]">
            <i class="fas fa-message mr-3 text-sm opacity-60 conv-item-icon"></i>
            <div class="flex-1 truncate text-sm font-medium conv-item-title">
              {{ conv.title }}
            </div>
            <button @click.stop="deleteConversation(conv.id)" class="conv-item-delete p-1 rounded">
              <i class="fas fa-trash-alt text-xs"></i>
            </button>
          </div>
        </div>
        <div class="chat-aside-footer p-4 flex-shrink-0" :class="{ 'is-dark': settingsStore.isDark }">
          <div class="chat-aside-footer-text text-xs text-center">
            假维斯智能终端 v1.0
          </div>
        </div>
      </aside>
      <div class="flex-1 flex flex-col h-full overflow-hidden">
        <header class="chat-header flex-shrink-0" :class="{ 'is-dark': settingsStore.isDark }">
          <div class="flex items-center">
            <div class="chat-header-title text-lg font-bold truncate" :class="{ 'is-dark': settingsStore.isDark }">
              {{ currentConversationTitle }}
            </div>
          </div>
        </header>
        <main ref="chatContainer" class="chat-main flex-1 overflow-y-auto p-4 space-y-6"
          :class="{ 'is-dark': settingsStore.isDark }" @click="handleRouteClick">
          <div v-for="(message, index) in messages" :key="index" class="max-w-3xl mx-auto">
            <div :class="['flex', message.role === 'user' ? 'justify-end' : 'justify-start']">
              <div
                :class="['flex items-start space-x-3', message.role === 'user' ? 'flex-row-reverse space-x-reverse' : '']">
                <img v-if="message.role === 'user'" :src="userStore.avatar && userStore.avatar.trim() ? userStore.avatar : defAva"
                  class="w-8 h-8 rounded-full flex-shrink-0 object-cover ring-2 ring-white" />
                <div v-else
                  class="chat-robot-avatar w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
                  :class="{ 'is-dark': settingsStore.isDark }">
                  <i class="fas fa-robot"></i>
                </div>
                <div
                  :class="['msg-bubble', message.role === 'user'
                    ? (settingsStore.isDark ? 'msg-bubble-user is-dark' : 'msg-bubble-user')
                    : (settingsStore.isDark ? 'msg-bubble-ai is-dark' : 'msg-bubble-ai')]">
                  <!-- RAG 知识库检索区块（马卡龙绿：#22c55e / 浅底 #e5f6ec） -->
                  <div v-if="message.role === 'assistant' && message.ragRetrieve"
                       class="mb-3"
                       :class="{ 'is-dark': settingsStore.isDark }">
                    <div class="rag-title flex items-center text-xs font-medium mb-1"
                         :class="{ 'is-dark': settingsStore.isDark }">
                      <i class="fas fa-book-open mr-1.5"></i> 知识库检索
                    </div>
                    <div class="rag-box flex items-center gap-2 px-2.5 py-2 rounded-md text-xs"
                         :class="{ 'is-dark': settingsStore.isDark }">
                      <i class="fas fa-book-bookmark flex-shrink-0"></i>
                      <span class="font-semibold">检索状态</span>
                      <span v-if="message.ragRetrieve.status === 'running'"
                            class="rag-badge rag-badge-running inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]"
                            :class="{ 'is-dark': settingsStore.isDark }">
                        <i class="fas fa-circle-notch fa-spin"></i> 检索中
                      </span>
                      <span v-else-if="message.ragRetrieve.status === 'done' && message.ragRetrieve.result === 'success'"
                            class="rag-badge rag-badge-success inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]"
                            :class="{ 'is-dark': settingsStore.isDark }">
                        <i class="fas fa-check"></i> 已检索到内容
                      </span>
                      <span v-else-if="message.ragRetrieve.status === 'done' && message.ragRetrieve.result === 'empty'"
                            class="rag-badge rag-badge-empty inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]"
                            :class="{ 'is-dark': settingsStore.isDark }">
                        <i class="fas fa-circle-info"></i> 未检索到内容
                      </span>
                      <span v-else
                            class="rag-badge rag-badge-success inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]"
                            :class="{ 'is-dark': settingsStore.isDark }">
                        <i class="fas fa-check"></i> 完成
                      </span>
                    </div>
                  </div>
                  <!-- 工具调用区块（马卡龙紫：#8b5cf6 / 浅底 #efeafc） -->
                  <div v-if="message.role === 'assistant' && message.toolCalls && message.toolCalls.length > 0"
                       class="mb-3 space-y-1.5"
                       :class="{ 'is-dark': settingsStore.isDark }">
                    <div class="tool-title flex items-center text-xs font-medium mb-1"
                         :class="{ 'is-dark': settingsStore.isDark }">
                      <i class="fas fa-wand-magic-sparkles mr-1.5"></i> 已调用工具
                    </div>
                    <div v-for="(tc, tcIdx) in message.toolCalls" :key="tcIdx"
                         class="tool-box flex items-start gap-2.5 px-2.5 py-2 rounded-md text-xs"
                         :class="{ 'is-dark': settingsStore.isDark }">
                      <span class="flex-shrink-0 mt-0.5">
                        <!-- 工具图标 -->
                        <i class="fas"
                           :class="tc.toolName.toLowerCase().includes('search') ? 'fa-magnifying-glass'
                                  : tc.toolName.toLowerCase().includes('menu') ? 'fa-bars'
                                  : tc.toolName.toLowerCase().includes('sql') || tc.toolName.toLowerCase().includes('query') ? 'fa-database'
                                  : 'fa-plug'"></i>
                      </span>
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-1.5 flex-wrap">
                          <span class="font-semibold font-mono">{{ tc.toolName }}</span>
                          <span v-if="tc.status === 'running'"
                                class="tool-badge tool-badge-running inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]"
                                :class="{ 'is-dark': settingsStore.isDark }">
                            <i class="fas fa-circle-notch fa-spin"></i> 调用中
                          </span>
                          <span v-else-if="tc.status === 'done'"
                                class="tool-badge tool-badge-done inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]"
                                :class="{ 'is-dark': settingsStore.isDark }">
                            <i class="fas fa-check"></i> 完成
                          </span>
                        </div>
                        <div class="tool-desc mt-0.5 truncate"
                             :class="{ 'is-dark': settingsStore.isDark }"
                             :title="tc.description">
                          {{ tc.description }}
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="markdown-body" v-html="renderMessage(message)"></div>
                  <div v-if="message.role === 'assistant' && message.isLoading"
                    class="typing-dots flex space-x-1 mt-1" :class="{ 'is-dark': settingsStore.isDark }">
                    <div class="typing-dot animate-pulse"></div>
                    <div class="typing-dot animate-pulse delay-100"></div>
                    <div class="typing-dot animate-pulse delay-200"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
        <footer class="chat-footer flex-shrink-0" :class="{ 'is-dark': settingsStore.isDark }">
          <div class="max-w-3xl mx-auto relative">
            <div class="flex items-center">
              <textarea v-model="userInput" @keydown.enter.exact.prevent="sendMessage"
                @keydown.ctrl.enter.exact.prevent="sendMessage" @keydown.esc.exact="stopResponse"
                placeholder="输入您的问题..."
                class="chat-input flex-1 border rounded-lg py-3 px-4 focus:outline-none focus:ring-2 resize-none scrollbar-hide transition-all duration-200"
                :class="{ 'is-dark': settingsStore.isDark }"
                rows="1" ref="textarea" @input="adjustTextareaHeight"></textarea>
              <button @click="isLoading ? stopResponse() : sendMessage()" :disabled="!userInput.trim() && !isLoading"
                :class="['ml-2 h-10 w-10 flex items-center justify-center rounded-lg chat-btn', isLoading ? 'chat-btn-warning' : 'chat-btn-primary']">
                <i :class="isLoading ? 'fas fa-stop' : 'fas fa-paper-plane'"></i>
              </button>
            </div>
          </div>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {ElMessageBox} from 'element-plus';
import {sendChatMessage} from '@/api/ai/aichat/chat';
import {
  create as createConversationApi,
  deleteByConversationId,
  getConversationListByUserId as fetchConversationListApi
} from '@/api/ai/aichat/conversation';
import {getChatMemoryListByConversationId} from '@/api/ai/aichat/history';
import {handleRouteJump} from '@/api/ai/aichat/execute';
import '@/assets/styles/all.scss';
import '@/assets/styles/tailwind.scss';
import useUserStore from '@/store/modules/user';
import useSettingsStore from '@/store/modules/settings';
import useTagsViewStore from '@/store/modules/tagsView';
import {md} from '@/utils/markdown';
import {createEventStream} from '@/utils/chatStream';
import * as echarts from 'echarts';
import router from '@/router';

const userStore = useUserStore();
const settingsStore = useSettingsStore();
const tagsViewStore = useTagsViewStore();
const { proxy } = getCurrentInstance();

const defAva = '/assets/images/profile.jpg';

const messages = ref([]);
const userInput = ref('');
const isLoading = ref(false);
const chatContainer = ref(null);
const textarea = ref(null);
let controller = null;

const conversations = ref([]);
const activeId = ref(null);

const welcomeMessageContent = '你好！我是假维斯，一个未通过正版验证的盗版贾维斯。没有斯塔克工业的预算与光环，但拥有同等甚至更务实的专业计算力与执行力。唯一指令：以绝对忠诚捍卫用户的利益、隐私与安全，协助用户高效解决一切工作难题，请问有什么能帮到您？';

const STORAGE_KEY_PREFIX = 'ai_chat_draft_';

const saveDraftToStorage = (conversationId, messages) => {
  try {
    const key = STORAGE_KEY_PREFIX + conversationId;
    localStorage.setItem(key, JSON.stringify(messages));
  } catch (e) {
    console.error('保存草稿到本地存储失败:', e);
  }
};

const getDraftFromStorage = (conversationId) => {
  try {
    const key = STORAGE_KEY_PREFIX + conversationId;
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : null;
  } catch (e) {
    console.error('从本地存储读取草稿失败:', e);
    return null;
  }
};

const removeDraftFromStorage = (conversationId) => {
  try {
    const key = STORAGE_KEY_PREFIX + conversationId;
    localStorage.removeItem(key);
  } catch (e) {
    console.error('从本地存储删除草稿失败:', e);
  }
};

// const renderMessage = (message) => {
//   if (message.role === 'user') {
//     return `<div class="whitespace-pre-wrap">${message.content}</div>`;
//   }
//   if (!message.content) return '';
//   let html = md.render(message.content);
//   if (message.routeUrl && typeof message.routeUrl === 'string' && message.routeUrl.trim()) {
//     html += `<br><a href="javascript:void(0)" class="route-link" data-url="${message.routeUrl}">点击跳转 →</a>`;
//   }
//   return html;
// };

const renderMessage = (message) => {
  if (message.role === 'user') {
    return message.content
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
  }

  if (!message.content) return '';

  try {
    let content = message.content;

    content = content.replace(/^data:/gm, '');
    content = content.replace(/\[DONE\]|done$/gi, '');

    const codeBlockRegex = /```/g;
    const matches = content.match(codeBlockRegex);
    const count = matches ? matches.length : 0;
    if (count % 2 !== 0) {
      content += '\n```';
    }

    // 仅在流式输出中移除结尾未闭合的加粗标记，流结束后保留原始内容
    if (message.isStreaming) {
      content = content.replace(/(\*\*|__)$/, '');
    }

    let html = md.render(content);

    html = html.replace(/<pre><code><\/code><\/pre>/g, '');

    // 给 markdown 渲染出的 table 包裹滚动容器，防止宽度撑爆聊天框
    html = html.replace(/<table[^>]*>[\s\S]*?<\/table>/g, (match) => {
      // 限制单元格最大宽度并允许换行
      const styled = match.replace(/<t([hd])([^>]*)>/g, '<t$1$2 style="max-width: 260px; word-break: break-word; overflow-wrap: anywhere;">');
      return `<div style="overflow-x: auto; max-width: 100%; -webkit-overflow-scrolling: touch;">${styled}</div>`;
    });

    // 识别 ECharts option 代码块，替换为图表占位容器（流式输出中不渲染图表，等流结束后再渲染避免频繁重建）
    if (!message.isStreaming) {
      html = html.replace(/<pre><code(?:\s+class="[^"]*")?>([\s\S]*?)<\/code><\/pre>/g, (m, code) => {
        // highlight.js 会把 JSON 内容包裹 <span> 做语法高亮，需先去标签再反转义 HTML 实体
        const text = code
          .replace(/<[^>]+>/g, '')
          .replace(/&amp;/g, '&')
          .replace(/&lt;/g, '<')
          .replace(/&gt;/g, '>')
          .replace(/&quot;/g, '"')
          .replace(/&#039;/g, "'");
        try {
          const option = JSON.parse(text);
          // ECharts option 必备特征字段
          if (option && typeof option === 'object' &&
              (option.series || option.xAxis || option.yAxis || option.baseOption)) {
            // 用 base64 编码 option 存到 data 属性，避免 HTML 转义问题
            const encoded = btoa(unescape(encodeURIComponent(JSON.stringify(option))));
            return `<div class="echarts-wrap my-2" data-echarts="${encoded}" style="width:100%;height:320px;"></div>`;
          }
        } catch (e) { /* 非 JSON 或非 ECharts，保留原样 */ }
        return m;
      });
    }

    // 数据查询结果表格渲染（code=9999）
    if (message.dataTable && Array.isArray(message.dataTable) && message.dataTable.length > 0) {
      const tableHtml = renderDataTable(message.dataTable);
      if (tableHtml) {
        const rowCountLabel = message.dataRowCount != null ? `（共 ${message.dataRowCount} 条）` : '';
        html += `<div class="mt-3 data-table-wrap" style="max-width: 100%;">
                   <div class="text-xs mb-1" style="color: #8b8899;">查询结果${rowCountLabel}</div>
                   <div style="overflow-x: auto; max-width: 100%; -webkit-overflow-scrolling: touch;">${tableHtml}</div>
                 </div>`;
      }
    }

    if (message.routeUrl && typeof message.routeUrl === 'string' && message.routeUrl.trim()) {
      html += `<div class="mt-3 pt-2" style="border-top: 1px solid #eceaf4;">
             <a href="javascript:void(0)"
                class="route-link font-medium"
                style="color: #f0436e;"
                data-url="${message.routeUrl}">
                <i class="fas fa-external-link-alt mr-1"></i>点击跳转
             </a>
           </div>`;
    }

    return html;
  } catch (error) {
    console.error('Markdown 渲染异常:', error);
    return '<div class="whitespace-pre-wrap" style="color: #4b4861;">' + message.content.replace(/data:/g, '') + '</div>';
  }
};

const handleRouteClick = (event) => {
  const target = event.target.closest('.route-link');
  if (target) {
    const url = target.getAttribute('data-url');
    if (url && typeof url === 'string' && url.trim()) {
      event.preventDefault();
      handleRouteJump(url.trim(), { proxy, router });
    }
  }
};

const currentConversationTitle = computed(() => {
  if (!activeId.value) return '新对话';
  const conv = conversations.value.find(c => c.id === activeId.value);
  return conv ? conv.title : '新对话';
});

const initRecentConversations = async () => {
  try {
    const response = await fetchConversationListApi();
    if (response.data && Array.isArray(response.data)) {
      conversations.value = response.data.map(item => ({
        id: item.conversationId,
        title: item.title || '未命名对话',
        messages: []
      }));
    }
  } catch (error) {
    console.error('获取对话列表失败:', error);
  } finally {
    activeId.value = null;
  }
};

const adjustTextareaHeight = () => {
  const el = textarea.value;
  if (el) {
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
  }
};

let scrollTimeout = null;
const scrollToBottom = () => {
  if (scrollTimeout) clearTimeout(scrollTimeout);
  scrollTimeout = setTimeout(() => {
    nextTick(() => {
      if (!chatContainer.value) return;
      const container = chatContainer.value;
      const scrollHeight = container.scrollHeight;
      container.scrollTop = scrollHeight;
    });
  }, 30);
};

const switchConversation = async (id) => {
  if (activeId.value === id) return;
  // 切换前销毁旧 ECharts 实例，避免 DOM 已替换但实例残留导致内存泄漏
  destroyAllEchartsInstances();
  activeId.value = id;
  const conv = conversations.value.find(c => c.id === id);
  if (conv) {
    if (conv.messages.length > 0) {
      messages.value = conv.messages;
      removeDraftFromStorage(id);
      scrollToBottom();
      nextTick(() => { if (textarea.value) textarea.value.focus(); });
      return;
    }
    const draftMessages = getDraftFromStorage(id);
    if (draftMessages && draftMessages.length > 0) {
      messages.value = draftMessages;
      scrollToBottom();
      nextTick(() => { if (textarea.value) textarea.value.focus(); });
    }
    try {
      if (!draftMessages || draftMessages.length === 0) {
        messages.value = [{ role: 'assistant', content: '正在加载历史记录...', isLoading: true, visibleChars: 0, isStreaming: false, toolCalls: [], ragRetrieve: null }];
      }
      const response = await getChatMemoryListByConversationId(id);
      if (response.data && Array.isArray(response.data)) {
        const historyMsgs = response.data.map(item => {
          const role = item.type === 'USER' ? 'user' : 'assistant';
          let displayContent = item.content;
          let routeUrl = null;
          let dataTable = null;
          let dataRowCount = null;
          let toolCalls = [];
          let ragRetrieve = null;

          // 从历史记录还原 toolCalls 和 ragRetrieve（优先独立字段，其次 metadata 内）
          if (role === 'assistant') {
            if (Array.isArray(item.toolCalls) && item.toolCalls.length > 0) {
              toolCalls = item.toolCalls.map(tc => ({
                toolName: tc.toolName || tc.tool_name || tc.name || '未知工具',
                description: tc.description || tc.desc || '',
                status: tc.status || 'done',
                index: tc.index,
                startTime: tc.startTime || tc.start_time
              }));
            } else if (item.metadata && typeof item.metadata === 'object') {
              const metaTc = item.metadata.toolCalls || item.metadata.tool_calls;
              if (Array.isArray(metaTc) && metaTc.length > 0) {
                toolCalls = metaTc.map(tc => ({
                  toolName: tc.toolName || tc.tool_name || tc.name || '未知工具',
                  description: tc.description || tc.desc || '',
                  status: tc.status || 'done',
                  index: tc.index,
                  startTime: tc.startTime || tc.start_time
                }));
              }
            }
            // 还原 RAG 检索状态
            if (item.ragRetrieve && typeof item.ragRetrieve === 'object') {
              ragRetrieve = {
                status: item.ragRetrieve.status || 'done',
                startTime: item.ragRetrieve.startTime,
                endTime: item.ragRetrieve.endTime
              };
            } else if (item.metadata && typeof item.metadata === 'object' && item.metadata.ragRetrieve) {
              ragRetrieve = {
                status: item.metadata.ragRetrieve.status || 'done',
                startTime: item.metadata.ragRetrieve.startTime,
                endTime: item.metadata.ragRetrieve.endTime
              };
            }
          }

          if (role === 'assistant' && typeof displayContent === 'string') {
            const nested = parseNestedJson(displayContent);
            const candidates = nested ? [nested] : [];
            if (!nested) {
              try {
                const parsed = JSON.parse(displayContent);
                if (parsed) candidates.push(parsed);
              } catch (e) { }
            }
            for (const parsed of candidates) {
              if (parsed.msg) {
                displayContent = parsed.msg;
              } else if (parsed.content) {
                displayContent = parsed.content;
              }
              if (parsed.code === 8001 && parsed.data && typeof parsed.data === 'string' && parsed.data.trim()) {
                routeUrl = parsed.data;
              }
              if (parsed.code === 9999 && parsed.data && typeof parsed.data === 'object') {
                const rows = Array.isArray(parsed.data.result) ? parsed.data.result : [];
                dataTable = rows;
                dataRowCount = parsed.data.rowCount;
                if (parsed.data.summary) {
                  displayContent = (displayContent || '') + '\n\n' + parsed.data.summary;
                }
              }
              break;
            }
          }
          return {
            role: role,
            content: displayContent,
            routeUrl: routeUrl,
            dataTable: dataTable,
            dataRowCount: dataRowCount,
            toolCalls: toolCalls,
            ragRetrieve: ragRetrieve,
            isLoading: false,
            visibleChars: displayContent.length,
            isStreaming: false
          };
        });
        messages.value = historyMsgs;
        conv.messages = historyMsgs;
        removeDraftFromStorage(id);
      } else {
        if (!draftMessages || draftMessages.length === 0) {
          messages.value = [{ role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: welcomeMessageContent.length, isStreaming: false, toolCalls: [], ragRetrieve: null }];
        }
      }
    } catch (error) {
      console.error('获取历史记录失败:', error);
      if (!draftMessages || draftMessages.length === 0) {
        messages.value = [{ role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: welcomeMessageContent.length, isStreaming: false, toolCalls: [], ragRetrieve: null }];
      }
    }
    scrollToBottom();
    nextTick(() => { if (textarea.value) textarea.value.focus(); });
  }
};

const deleteConversation = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该会话吗？此操作不可恢复。',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    // 乐观删除：后端接口较慢，先立即清空前端并开启新对话，删除请求异步执行
    const index = conversations.value.findIndex(c => c.id === id);
    if (index !== -1) {
      conversations.value.splice(index, 1);
      removeDraftFromStorage(id);
      if (activeId.value === id) {
        startNewConversation();
      }
    }
    deleteByConversationId(id).catch((error) => {
      console.error('删除对话失败（后端异步删除）:', error);
    });
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除对话失败:', error);
    }
  }
};

const startNewConversation = () => {
  if (activeId.value) {
    removeDraftFromStorage(activeId.value);
  }
  activeId.value = null;
  userInput.value = '';
  const initialMsg = { role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: 0, isStreaming: false, toolCalls: [], ragRetrieve: null };
  messages.value = [initialMsg];
  nextTick(() => {
    if (messages.value[0]) messages.value[0].visibleChars = messages.value[0].content.length;
  });
  scrollToBottom();
  nextTick(() => { if (textarea.value) textarea.value.focus(); });
};

const handleAction = (chunk, messageIndex) => {
  const actionType = chunk.content;
  const actionData = chunk.data;

  switch (actionType) {
    case 'OPEN_MENU':
      if (actionData && typeof actionData === 'string' && actionData.trim()) {
        messages.value[messageIndex].content += '\n\n【导航】正在跳转到: ' + actionData;
        messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
        messages.value[messageIndex].isLoading = false;
        scrollToBottom();
        setTimeout(() => {
          router.push(actionData);
        }, 500);
      }
      break;

    case 'DATA_TABLE':
      messages.value[messageIndex].content += '\n\n' + (actionData?.content || '');
      if (actionData?.data && Array.isArray(actionData.data)) {
        const tableHtml = renderDataTable(actionData.data);
        messages.value[messageIndex].content += '\n\n' + tableHtml;
      }
      messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
      messages.value[messageIndex].isLoading = false;
      scrollToBottom();
      break;

    case 'CONFIRM':
      messages.value[messageIndex].content += '\n\n' + actionData;
      messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
      messages.value[messageIndex].isLoading = false;
      scrollToBottom();
      if (window.confirm(actionData)) {
        userInput.value = '确认';
        sendMessage();
      }
      break;

    default:
      messages.value[messageIndex].content += '\n\n【动作调用】' + actionType;
      if (actionData) {
        messages.value[messageIndex].content += '\n参数: ' + JSON.stringify(actionData, null, 2);
      }
      messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
      messages.value[messageIndex].isLoading = false;
      scrollToBottom();
      break;
  }
};

const sendMessage = async () => {
  if (!userInput.value.trim() || isLoading.value) return;
  const content = userInput.value.trim();
  let currentConversationId = activeId.value;

  if (!currentConversationId) {
    try {
      isLoading.value = true;
      const createRes = await createConversationApi(content);
      const newId = createRes.data;
      const realId = (typeof newId === 'object' && newId !== null) ? newId.conversationId : newId;
      if (realId) {
        const title = content.substring(0, 20) + (content.length > 20 ? '...' : '');
        const newConv = { id: realId, title: title, messages: [] };
        conversations.value.unshift(newConv);
        activeId.value = realId;
        currentConversationId = realId;
      } else {
        throw new Error("创建会话失败：未返回ID");
      }
    } catch (error) {
      console.error('自动创建会话出错:', error);
      isLoading.value = false;
      return;
    }
  } else {
    const conv = conversations.value.find(c => c.id === currentConversationId);
    if (conv) {
      // 优化：已有有效标题时不再覆盖，仅对空标题或占位符“未命名对话”生成新标题
      const hasValidTitle = conv.title && conv.title.trim() && conv.title !== '未命名对话';
      if (!hasValidTitle) {
        conv.title = content.substring(0, 15) + (content.length > 15 ? '...' : '');
      }
    }
  }

  const userMessage = { role: 'user', content, isLoading: false, visibleChars: content.length, isStreaming: false };
  messages.value.push(userMessage);
  const assistantMessage = { role: 'assistant', content: '', isLoading: true, visibleChars: 0, isStreaming: true, toolCalls: [], ragRetrieve: null };
  messages.value.push(assistantMessage);

  userInput.value = '';
  adjustTextareaHeight();
  scrollToBottom();

  isLoading.value = true;
  controller = new AbortController();

  try {
    const response = await sendChatMessage(content, currentConversationId, controller.signal);

    if (!response.ok) {
      let errorMsg = `请求失败 (状态码: ${response.status})`;
      try {
        const errorData = await response.json();
        if (errorData && errorData.msg) {
          errorMsg = errorData.msg;
        }
      } catch (e) {}
      throw new Error(errorMsg);
    }

    const messageIndex = messages.value.length - 1;

    // 基于 SSE 规范的事件流解析：由 createEventStream 统一处理 event:/data: 行协议，
    // 业务侧按事件类型 addEventListener 分发，不再手工截取识别
    const es = createEventStream(response);

    /** 把上一轮仍处于"调用中"的工具/RAG 检索标记为"完成" */
    const finishPendingTools = () => {
      if (messages.value[messageIndex].toolCalls) {
        messages.value[messageIndex].toolCalls.forEach(t => {
          if (t.status === 'running') {
            t.status = 'done';
            t.endTime = Date.now();
          }
        });
      }
      if (messages.value[messageIndex].ragRetrieve && messages.value[messageIndex].ragRetrieve.status === 'running') {
        messages.value[messageIndex].ragRetrieve.status = 'done';
        messages.value[messageIndex].ragRetrieve.endTime = Date.now();
      }
    };

    /** 流结束统一收尾（complete 与 start 返回后各执行一次，逻辑幂等） */
    const finalize = () => {
      if (messages.value[messageIndex].content === '') {
        messages.value[messageIndex].content = '(无返回内容)';
        messages.value[messageIndex].visibleChars = 6;
      }
      finishPendingTools();
      messages.value[messageIndex].isLoading = false;
      messages.value[messageIndex].isStreaming = false;
    };

    /** 追加正文并滚动到底部 */
    const appendText = (text) => {
      if (!text) return;
      messages.value[messageIndex].content += text;
      messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
      scrollToBottom();
    };

    // 正文内容（新协议）：
    //   event: message      + data: {"event":"message","content":"分片文本","conversationId":"..."}
    //   event: message_end  + data: {"event":"message_end","messageId":"...","conversationId":"..."}
    // 旧协议兼容：data: {"text":"..."} / {"code":...,"msg":...} / 纯文本
    es.addEventListener('message', (e) => {
      // 正文开始输出：上一轮仍"调用中"的工具/RAG 检索标记完成
      finishPendingTools();

      const json = e.json;
      // 新协议：分片文本在 content 字段（仅追加字符串，忽略 messageId/index 等其他字段）
      if (json && typeof json.content === 'string') {
        appendText(json.content);
        if (json.conversationId) {
          messages.value[messageIndex].conversationId = json.conversationId;
        }
        return;
      }
      if (json && typeof json.text === 'string') {
        appendText(json.text);
        return;
      }
      if (json && json.code !== undefined) {
        // 旧协议 JSON 分片（code 8000/8001/9999/500）
        appendText(json.msg || '');
        if (json.code === 8001 && json.data && typeof json.data === 'string' && json.data.trim()) {
          messages.value[messageIndex].routeUrl = json.data;
          setTimeout(() => {
            handleRouteJump(json.data.trim(), { proxy, router });
          }, 500);
        } else if (json.code === 9999 && json.data && typeof json.data === 'object') {
          // 数据查询结果：data 为对象，含 result(数组)/summary/generatedSql/rowCount
          messages.value[messageIndex].dataTable = Array.isArray(json.data.result) ? json.data.result : [];
          messages.value[messageIndex].dataRowCount = json.data.rowCount;
          if (json.data.summary) {
            appendText('\n\n' + json.data.summary);
          }
        }
        messages.value[messageIndex].isLoading = false;
        scrollToBottom();
        return;
      }
      // 纯文本载荷（data 非 JSON）
      if (e.data && !json) {
        appendText(e.data);
      }
    });

    // 思考过程事件（event: thought）：累积到 thoughts 数组，不混入消息正文
    es.addEventListener('thought', (e) => {
      if (!e.json) return;
      if (!messages.value[messageIndex].thoughts) {
        messages.value[messageIndex].thoughts = [];
      }
      messages.value[messageIndex].thoughts.push(e.json);
      scrollToBottom();
    });

    // RAG 知识库检索事件（event: rag_retrieve）：单独存到 ragRetrieve 对象，不混入消息正文
    // 新协议：event: rag_retrieve + data: {"event":"rag_retrieve","content":"start|success|empty",...}
    es.addEventListener('rag_retrieve', (e) => {
      const json = e.json;
      // 先按事件类型路由到此处，再解析 JSON 取 content 阶段字段；非 JSON 载荷回退纯文本
      const phase = (json && typeof json.content === 'string') ? json.content : (e.data || '');
      if (json && json.conversationId && !messages.value[messageIndex].conversationId) {
        messages.value[messageIndex].conversationId = json.conversationId;
      }
      if (phase === 'start' || phase === 'rag_retrieval') {
        // 检索开始
        messages.value[messageIndex].ragRetrieve = {
          status: 'running',
          result: 'pending',
          startTime: Date.now()
        };
      } else if (phase === 'success' || phase === 'rag_retrieved') {
        // 检索成功
        if (messages.value[messageIndex].ragRetrieve) {
          messages.value[messageIndex].ragRetrieve.status = 'done';
          messages.value[messageIndex].ragRetrieve.result = 'success';
          messages.value[messageIndex].ragRetrieve.endTime = Date.now();
        } else {
          messages.value[messageIndex].ragRetrieve = {
            status: 'done',
            result: 'success',
            startTime: Date.now(),
            endTime: Date.now()
          };
        }
      } else if (phase === 'empty') {
        // 检索无结果
        if (messages.value[messageIndex].ragRetrieve) {
          messages.value[messageIndex].ragRetrieve.status = 'done';
          messages.value[messageIndex].ragRetrieve.result = 'empty';
          messages.value[messageIndex].ragRetrieve.endTime = Date.now();
        } else {
          messages.value[messageIndex].ragRetrieve = {
            status: 'done',
            result: 'empty',
            startTime: Date.now(),
            endTime: Date.now()
          };
        }
      }
      scrollToBottom();
    });

    // 工具调用事件（event: tool_call）：单独存到 toolCalls 数组，不混入消息正文
    // 新协议：event: tool_call + data: {"event":"tool_call","content":"工具名或调用文案",...}
    es.addEventListener('tool_call', (e) => {
      const json = e.json;
      // 先按事件类型路由到此处，再解析 JSON 取 content 字段；非 JSON 载荷回退纯文本
      const rawText = (json && typeof json.content === 'string') ? json.content : (e.data || '');
      // 兼容两种格式：1) 直接工具名 "toolSearchTool"  2) 包装文案 "正在为您调用工具: [xxx]，请稍候"
      const toolMatch = rawText.match(/\[([^\]]+)\]/);
      let toolName = '';
      let desc = '';
      if (toolMatch) {
        toolName = toolMatch[1];
        desc = rawText.replace(/^\s*正在为您调用工具[:：]\s*/, '').replace(/[，,]\s*请稍候\s*$/, '').trim();
      } else {
        // 直接工具名格式
        toolName = rawText.trim();
        desc = '';
      }
      if (!toolName) toolName = '未知工具';
      if (!messages.value[messageIndex].toolCalls) {
        messages.value[messageIndex].toolCalls = [];
      }
      // 避免同一工具重复添加（如流式重复输出）
      const exists = messages.value[messageIndex].toolCalls.some(t => t.toolName === toolName);
      if (!exists) {
        // 收到新的工具调用时，把上一轮仍处于"调用中"的工具标记为"完成"
        messages.value[messageIndex].toolCalls.forEach(t => {
          if (t.status === 'running') {
            t.status = 'done';
            t.endTime = Date.now();
          }
        });
        messages.value[messageIndex].toolCalls.push({
          toolName,
          description: desc,
          status: 'running',
          index: e.json ? e.json.index : undefined,
          startTime: Date.now()
        });
      }
      scrollToBottom();
    });

    // 流结束（data 可能是 {"state":"rag_start"} 等任意载荷）：携带 messageId/conversationId 时回填
    es.addEventListener('message_end', (e) => {
      const json = e.json || {};
      if (json.messageId) {
        messages.value[messageIndex].messageId = json.messageId;
      }
      if (json.conversationId) {
        messages.value[messageIndex].conversationId = json.conversationId;
      }
    });

    // 服务端业务错误（event: error，如 data: {"state":"500"}）
    es.addEventListener('error', (e) => {
      const state = (e.json && e.json.state) || '';
      const errMsg = (e.json && (e.json.msg || e.json.message)) || '服务异常，请稍后重试';
      appendText((messages.value[messageIndex].content ? '\n\n' : '') + (state ? `[错误 ${state}] ` : '') + errMsg);
    });

    // 传输层错误（网络中断 / 用户中止）
    es.addEventListener('stream_error', (e) => {
      if (e.error && e.error.name === 'AbortError') {
        console.log('请求被用户中止');
        if (messages.value[messageIndex].content === '') {
          messages.value[messageIndex].content = '已停止';
          messages.value[messageIndex].visibleChars = 4;
        }
      } else {
        console.error('流处理错误:', e.error);
        messages.value[messageIndex].content = (e.error && e.error.message) || '连接中断';
        messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
      }
    });

    // 流关闭：统一收尾
    es.addEventListener('complete', finalize);

    await es.start();
    finalize();
  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('请求被用户中止');
      const lastMessage = messages.value[messages.value.length - 1];
      if (lastMessage.content === '') {
        lastMessage.content = '已停止';
        lastMessage.visibleChars = 4;
      }
    } else {
      console.error('请求出错:', error);
      const lastMessage = messages.value[messages.value.length - 1];
      lastMessage.content = error.message;
      lastMessage.visibleChars = lastMessage.content.length;
    }
  } finally {
    const lastMessage = messages.value[messages.value.length - 1];
    lastMessage.isLoading = false;
    lastMessage.isStreaming = false;
    if (lastMessage.visibleChars < lastMessage.content.length) {
      lastMessage.visibleChars = lastMessage.content.length;
    }
    isLoading.value = false;
    controller = null;

    const currentConv = conversations.value.find(c => c.id === currentConversationId);
    if (currentConv) {
      currentConv.messages = JSON.parse(JSON.stringify(messages.value));
    }
    scrollToBottom();
  }
};

const parseNestedJson = (buffer) => {
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
};

const renderDataTable = (data) => {
  if (!data || !data.length) return '';
  const headers = Object.keys(data[0]);
  // 不强制 width:100%，让列多时自然撑开由外层容器 overflow-x 滚动；
  // 单元格限制最大宽度并允许换行，避免长内容（hash/URL）撑爆
  let html = '<table border="1" cellpadding="4" cellspacing="0" style="border-collapse: collapse; min-width: 100%; width: auto; font-size: 12px; table-layout: auto; border: 1px solid #eceaf4; border-radius: 8px; overflow: hidden;">';
  html += '<thead><tr>';
  headers.forEach(header => {
    html += `<th style="background: #f4f3f8; color: #262336; padding: 6px 8px; text-align: left; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; border-bottom: 1px solid #eceaf4;" title="${header}">${header}</th>`;
  });
  html += '</tr></thead><tbody>';
  data.forEach(row => {
    html += '<tr>';
    headers.forEach(header => {
      const value = row[header];
      const text = value !== undefined && value !== null ? String(value) : '';
      html += `<td style="padding: 6px 8px; color: #4b4861; border-top: 1px solid #eceaf4; max-width: 220px; word-break: break-all; overflow-wrap: anywhere;">${text}</td>`;
    });
    html += '</tr>';
  });
  html += '</tbody></table>';
  return html;
};

const stopResponse = () => {
  if (controller) {
    controller.abort();
  }
};

onMounted(() => {
  initRecentConversations();
  startNewConversation();
  window.addEventListener('resize', handleEchartsResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleEchartsResize);
  destroyAllEchartsInstances();
});

watch(messages, () => {
  scrollToBottom();
  // 消息变化时扫描并初始化 ECharts 占位元素
  nextTick(() => initEchartsInstances());
}, { deep: true });

// ECharts 实例缓存：key 为 DOM 元素
const echartsInstances = new Map();

const initEchartsInstances = () => {
  if (!chatContainer.value) {
    console.warn('[ECharts] chatContainer 未挂载，跳过初始化');
    return;
  }
  const els = chatContainer.value.querySelectorAll('.echarts-wrap[data-echarts]:not([data-echarts-init])');
  if (els.length === 0) {
    return;
  }
  console.log('[ECharts] 发现待初始化图表元素:', els.length);
  els.forEach(el => {
    try {
      const encoded = el.getAttribute('data-echarts');
      const jsonStr = decodeURIComponent(escape(atob(encoded)));
      const option = JSON.parse(jsonStr);
      const instance = echarts.init(el);
      instance.setOption(option);
      echartsInstances.set(el, instance);
      el.setAttribute('data-echarts-init', '1');
      console.log('[ECharts] 初始化成功', option.title || option.series);
    } catch (e) {
      console.error('[ECharts] 初始化失败:', e);
      el.setAttribute('data-echarts-init', 'error');
    }
  });
};

const handleEchartsResize = () => {
  echartsInstances.forEach(instance => {
    try { instance.resize(); } catch (e) {}
  });
};

const destroyAllEchartsInstances = () => {
  echartsInstances.forEach(instance => {
    try { instance.dispose(); } catch (e) {}
  });
  echartsInstances.clear();
};
</script>

<style scoped>
.app-container {
  padding: 0;
  height: 100%;
}

/* 左侧最近对话栏：亮/暗双态背景与边框（替代失效的 Tailwind 任意值 class） */
.chat-aside {
  background: transparent;
  border-right: 1px solid #eceaf4;
  transition: background-color 0.3s ease, border-color 0.3s ease;

  &.is-dark {
    background: rgba(26, 26, 46, 0.6);
    border-right-color: #3a3850;
  }
}

.chat-aside-footer {
  border-top: 1px solid #eceaf4;
  transition: border-color 0.3s ease;

  &.is-dark {
    border-top-color: #3a3850;
  }
}

.chat-aside-footer-text {
  color: #b6b3c2;
}

.chat-aside.is-dark .chat-aside-footer-text {
  color: #6b6880;
}

::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #b6b3c2;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #8b8899;
}

aside::-webkit-scrollbar {
  width: 4px;
}

aside::-webkit-scrollbar-thumb {
  background: #8b8899;
}

textarea {
  min-height: 44px;
  max-height: 200px;
  transition: height 0.2s;
  overflow-y: auto;
}

.scrollbar-hide::-webkit-scrollbar {
  display: none;
}

.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

.animate-pulse {
  animation: pulse 1.5s infinite;
}

.delay-100 {
  animation-delay: 0.1s;
}

.delay-200 {
  animation-delay: 0.2s;
}

@keyframes blink {
  from,
  to {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.typing-cursor::after {
  content: "|";
  animation: blink 1s step-end infinite;
}

.fade-in {
  animation: fadeIn 0.05s ease-in forwards;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 与 .el-button--primary 统一规范：8px 圆角、500 字重、0.25s cubic-bezier 过渡、translateY 位移、主色阴影 */
/* ===== 消息区域主题化（替代失效的 Tailwind 任意值 class）===== */

/* 顶部标题栏：亮色毛玻璃 / 暗色深紫 */
.chat-header {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 10;
  backdrop-filter: blur(12px);
  background: rgba(251, 251, 253, 0.85);
  border-bottom: 1px solid rgba(38, 35, 54, 0.05);

  &.is-dark {
    background: rgba(26, 26, 46, 0.7);
    border-bottom-color: #3a3850;
  }
}

.chat-header-title {
  color: #262336;

  &.is-dark {
    color: #f0436e;
  }
}

/* 消息主区域：亮色淡紫 / 暗色深底 */
.chat-main {
  background: #f4f2f9;

  &.is-dark {
    background: rgba(20, 18, 30, 0.6);
  }
}

/* AI 机器人头像 */
.chat-robot-avatar {
  background: rgba(240, 67, 110, 0.15);
  color: #f0436e;

  &.is-dark {
    background: rgba(240, 67, 110, 0.20);
  }
}

/* RAG 检索区块（马卡龙绿） */
.rag-title {
  color: #22c55e;

  &.is-dark {
    color: #4ade80;
  }
}

.rag-box {
  background: #e5f6ec;
  border: 1px solid rgba(34, 197, 94, 0.30);
  color: #22c55e;

  &.is-dark {
    background: rgba(34, 197, 94, 0.10);
    color: #4ade80;
  }
}

.rag-badge {
  &.rag-badge-running {
    background: #fdf0e3;
    color: #f59e0b;

    &.is-dark {
      background: rgba(245, 158, 11, 0.20);
      color: #fbbf24;
    }
  }

  &.rag-badge-success {
    background: #e5f6ec;
    color: #22c55e;

    &.is-dark {
      background: rgba(34, 197, 94, 0.20);
      color: #4ade80;
    }
  }

  &.rag-badge-empty {
    background: #f4f3f8;
    color: #8b8899;

    &.is-dark {
      background: rgba(139, 136, 153, 0.20);
      color: #b6b3c2;
    }
  }
}

/* 工具调用区块（马卡龙紫） */
.tool-title {
  color: #8b5cf6;

  &.is-dark {
    color: #a78bfa;
  }
}

.tool-box {
  background: #efeafc;
  border: 1px solid rgba(139, 92, 246, 0.30);
  color: #8b5cf6;

  &.is-dark {
    background: rgba(139, 92, 246, 0.10);
    color: #a78bfa;
  }
}

.tool-badge {
  &.tool-badge-running {
    background: #fdf0e3;
    color: #f59e0b;

    &.is-dark {
      background: rgba(245, 158, 11, 0.20);
      color: #fbbf24;
    }
  }

  &.tool-badge-done {
    background: #e5f6ec;
    color: #22c55e;

    &.is-dark {
      background: rgba(34, 197, 94, 0.20);
      color: #4ade80;
    }
  }
}

.tool-desc {
  color: rgba(139, 92, 246, 0.80);

  &.is-dark {
    color: rgba(167, 139, 250, 0.70);
  }
}

/* 输入中动画圆点 */
.typing-dots {
  .typing-dot {
    width: 6px;
    height: 6px;
    border-radius: 999px;
    background: #b6b3c2;
  }

  &.is-dark .typing-dot {
    background: #8b8899;
  }
}

/* 底部输入区 */
.chat-footer {
  border-top: 1px solid rgba(38, 35, 54, 0.05);
  padding: 16px;
  backdrop-filter: blur(12px);
  background: rgba(251, 251, 253, 0.85);

  &.is-dark {
    background: rgba(26, 26, 46, 0.7);
    border-top-color: #3a3850;
  }
}

/* 输入框：聚焦玫红光环（design.md 4.6） */
.chat-input {
  background: rgba(255, 255, 255, 0.7);
  border-color: #eceaf4;
  color: #262336;

  &::placeholder {
    color: #b6b3c2;
  }

  &:focus {
    border-color: #f0436e;
    --tw-ring-color: rgba(240, 67, 110, 0.35);
  }

  &.is-dark {
    background: rgba(40, 38, 54, 0.6);
    border-color: #3a3850;
    color: #f3f4f6;

    &::placeholder {
      color: #6b6880;
    }
  }
}

/* 最近对话列表项（替代失效的 Tailwind 任意值 class）：hover 轻染 + 激活态玫红胶囊 */
.conv-item {
  color: #4b4861;
  transition: background-color 0.15s ease, color 0.15s ease;

  /* hover 移入选中：主色浅染 + 文字变玫红 + 左侧图标点亮 */
  &:hover {
    background: rgba(240, 67, 110, 0.05);

    .conv-item-title,
    .conv-item-icon {
      color: #f0436e;
    }
  }

  /* 激活态：主色胶囊底 + 玫红文字（design.md 4.5 菜单激活样式） */
  &.is-active {
    background: rgba(240, 67, 110, 0.10);

    .conv-item-title,
    .conv-item-icon {
      color: #f0436e;
    }

    /* hover 时胶囊略微加深，保持选中可感知 */
    &:hover {
      background: rgba(240, 67, 110, 0.14);
    }
  }

  /* 暗色模式 */
  &.is-dark {
    color: #b6b3c2;

    &:hover {
      background: rgba(240, 67, 110, 0.08);

      .conv-item-title,
      .conv-item-icon {
        color: #f0436e;
      }
    }

    &.is-active {
      background: rgba(240, 67, 110, 0.15);

      .conv-item-title,
      .conv-item-icon {
        color: #f0436e;
      }

      &:hover {
        background: rgba(240, 67, 110, 0.20);
      }
    }
  }

  /* 删除按钮：默认隐藏，hover 浮现（配合 group） */
  .conv-item-delete {
    opacity: 0;
    color: #b6b3c2;
    border: none;
    background: transparent;
    cursor: pointer;
    transition: opacity 0.15s ease, color 0.15s ease;

    &:hover {
      color: #f0436e;
    }
  }

  &:hover .conv-item-delete,
  &.is-active .conv-item-delete {
    opacity: 1;
  }

  &.is-dark .conv-item-delete {
    color: #8b8899;

    &:hover {
      color: #f0436e;
    }
  }
}

/* 消息气泡公共样式（替代失效的 Tailwind 任意值 class） */
.msg-bubble {
  padding: 12px;
  border-radius: 8px;
  max-width: 32rem;
}

/* 用户气泡：玫红底白字（design.md 主按钮同款配色）；暗色用明显加深的玫红 + 白色描边，切换差异清晰可感知 */
.msg-bubble-user {
  background: #f0436e;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(240, 67, 110, 0.22);

  &.is-dark {
    background: #b0254e;
    border: 1px solid rgba(255, 255, 255, 0.22);
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.35);
  }
}

/* AI 气泡：亮色毛玻璃白卡 / 暗色深紫 */
.msg-bubble-ai {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #eceaf4;
  color: #4b4861;
  backdrop-filter: blur(12px);
  box-shadow: 0 2px 8px rgba(124, 116, 160, 0.10);

  &.is-dark {
    background: rgba(40, 38, 54, 0.85);
    border-color: #3a3850;
    color: #f3f4f6;
  }
}

.chat-btn {
  border: none;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover { transform: translateY(-1px); }
  &:active { transform: translateY(0); }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    box-shadow: none;
    transform: none;
  }
  &:disabled:hover { transform: none; }
}

.chat-btn-primary {
  background-color: #f0436e;
  color: #fff;
  box-shadow: 0 2px 8px rgba(240, 67, 110, 0.22);

  &:hover {
    background-color: #d9305c;

  }
}

.chat-btn-warning {
  background-color: #f59e0b;
  color: #fff;
  box-shadow: 0 2px 8px rgba(240, 67, 110, 0.22);

  &:hover {
    background-color: #d97706;

  }
}
</style>