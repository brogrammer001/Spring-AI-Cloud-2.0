<template>
  <div class="app-container">
    <div class="flex h-full overflow-hidden" style="position: relative;">
      <aside
        :class="['flex flex-col border-r transition-colors duration-300 w-64 m-0 py-2 px-0', settingsStore.isDark ? 'bg-gray-800 border-gray-700' : 'bg-gray-50 border-gray-200']">
        <div class="px-4 pb-2 flex-shrink-0">
          <button @click="startNewConversation"
            :class="['w-full flex items-center justify-center px-4 py-2.5 rounded-lg font-medium transition-colors duration-200', settingsStore.isDark ? 'bg-indigo-600 hover:bg-indigo-700 text-white' : 'bg-blue-500 hover:bg-blue-600 text-white']">
            <i class="fas fa-plus mr-2"></i>
            <span>新对话</span>
          </button>
        </div>
        <div class="flex-1 overflow-y-auto px-2 custom-scrollbar">
          <div class="text-xs font-semibold px-3 mb-2"
            :class="settingsStore.isDark ? 'text-gray-500' : 'text-gray-400'">
            最近对话
          </div>
          <div v-if="conversations.length === 0" class="text-center py-4 text-sm"
            :class="settingsStore.isDark ? 'text-gray-600' : 'text-gray-400'">
            暂无对话记录
          </div>
          <div v-for="(conv, index) in conversations" :key="conv.id" @click="switchConversation(conv.id)" :class="[
            'group flex items-center px-3 py-2.5 rounded-lg cursor-pointer mb-1 transition-colors duration-150',
            conv.id === activeId ? (settingsStore.isDark ? 'bg-gray-700 text-white' : 'bg-blue-100 text-blue-700') : (settingsStore.isDark ? 'text-gray-300 hover:bg-gray-800' : 'text-gray-700 hover:bg-gray-200')
          ]">
            <i class="fas fa-message mr-3 text-sm opacity-60"></i>
            <div class="flex-1 truncate text-sm font-medium">
              {{ conv.title }}
            </div>
            <button @click.stop="deleteConversation(conv.id)"
              :class="['opacity-0 group-hover:opacity-100 p-1 rounded transition-all', settingsStore.isDark ? 'text-gray-500 hover:text-red-400' : 'text-gray-400 hover:text-red-500']">
              <i class="fas fa-trash-alt text-xs"></i>
            </button>
          </div>
        </div>
        <div class="p-4 border-t flex-shrink-0" :class="settingsStore.isDark ? 'border-gray-700' : 'border-gray-200'">
          <div class="text-xs text-center" :class="settingsStore.isDark ? 'text-gray-600' : 'text-gray-400'">
            假维斯智能终端 v1.0
          </div>
        </div>
      </aside>
      <div class="flex-1 flex flex-col h-full overflow-hidden">
        <header
          :class="['shadow-sm py-3 px-4 flex items-center justify-between flex-shrink-0 z-10', settingsStore.isDark ? 'bg-gray-800' : 'bg-white']">
          <div class="flex items-center">
            <div :class="['text-lg font-bold truncate', settingsStore.isDark ? 'text-blue-400' : 'text-blue-600']">
              {{ currentConversationTitle }}
            </div>
          </div>
        </header>
        <main ref="chatContainer" class="flex-1 overflow-y-auto p-4 space-y-6"
          :class="{ 'bg-white': !settingsStore.isDark, 'bg-gray-800': settingsStore.isDark }" @click="handleRouteClick">
          <div v-for="(message, index) in messages" :key="index" class="max-w-3xl mx-auto">
            <div :class="['flex', message.role === 'user' ? 'justify-end' : 'justify-start']">
              <div
                :class="['flex items-start space-x-3', message.role === 'user' ? 'flex-row-reverse space-x-reverse' : '']">
                <img v-if="message.role === 'user'" :src="userStore.avatar && userStore.avatar.trim() ? userStore.avatar : defAva"
                  class="w-8 h-8 rounded-full flex-shrink-0 object-cover" />
                <div v-else
                  :class="['w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0', settingsStore.isDark ? 'bg-indigo-700 text-indigo-100' : 'bg-blue-100 text-blue-600']">
                  <i class="fas fa-robot"></i>
                </div>
                <div
                  :class="['p-3 rounded-lg max-w-lg', message.role === 'user' ? 'bg-blue-500 text-white' : settingsStore.isDark ? 'bg-gray-700 text-gray-100 border border-gray-600' : 'bg-white shadow border border-gray-200 text-gray-800']">
                  <!-- 工具调用区块（类似 Deepseek 风格：独立卡片、可折叠） -->
                  <div v-if="message.role === 'assistant' && message.toolCalls && message.toolCalls.length > 0"
                       class="mb-3 space-y-1.5">
                    <div class="flex items-center text-xs font-medium mb-1"
                         :class="settingsStore.isDark ? 'text-indigo-300' : 'text-indigo-600'">
                      <i class="fas fa-wand-magic-sparkles mr-1.5"></i> 已调用工具
                    </div>
                    <div v-for="(tc, tcIdx) in message.toolCalls" :key="tcIdx"
                         :class="['flex items-start gap-2.5 px-2.5 py-2 rounded-md text-xs',
                                  settingsStore.isDark ? 'bg-indigo-900/40 border border-indigo-700/50 text-indigo-200'
                                                        : 'bg-indigo-50 border border-indigo-100 text-indigo-700']">
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
                                :class="['inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]',
                                         settingsStore.isDark ? 'bg-amber-500/20 text-amber-300' : 'bg-amber-100 text-amber-700']">
                            <i class="fas fa-circle-notch fa-spin"></i> 调用中
                          </span>
                          <span v-else-if="tc.status === 'done'"
                                :class="['inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]',
                                         settingsStore.isDark ? 'bg-green-500/20 text-green-300' : 'bg-green-100 text-green-700']">
                            <i class="fas fa-check"></i> 完成
                          </span>
                        </div>
                        <div class="mt-0.5 truncate"
                             :class="settingsStore.isDark ? 'text-indigo-300/70' : 'text-indigo-500/80'"
                             :title="tc.description">
                          {{ tc.description }}
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="markdown-body" v-html="renderMessage(message)"></div>
                  <div v-if="message.role === 'assistant' && message.isLoading" class="flex space-x-1 mt-1">
                    <div
                      :class="['w-1.5 h-1.5 rounded-full', settingsStore.isDark ? 'bg-gray-400' : 'bg-gray-300', 'animate-pulse']">
                    </div>
                    <div
                      :class="['w-1.5 h-1.5 rounded-full', settingsStore.isDark ? 'bg-gray-400' : 'bg-gray-300', 'animate-pulse delay-100']">
                    </div>
                    <div
                      :class="['w-1.5 h-1.5 rounded-full', settingsStore.isDark ? 'bg-gray-400' : 'bg-gray-300', 'animate-pulse delay-200']">
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
        <footer
          :class="['border-t p-4 flex-shrink-0', settingsStore.isDark ? 'bg-gray-800 border-gray-700' : 'bg-white border-gray-200']">
          <div class="max-w-3xl mx-auto relative">
            <div class="flex items-center">
              <textarea v-model="userInput" @keydown.enter.exact.prevent="sendMessage"
                @keydown.ctrl.enter.exact.prevent="sendMessage" @keydown.esc.exact="stopResponse"
                placeholder="输入您的问题..."
                :class="['flex-1 border rounded-lg py-3 px-4 focus:outline-none focus:ring-2 resize-none scrollbar-hide', settingsStore.isDark ? 'bg-gray-800 border-gray-600 text-white focus:ring-blue-400 placeholder-gray-400' : 'bg-white border-gray-300 text-gray-800 focus:ring-blue-500 focus:border-transparent']"
                rows="1" ref="textarea" @input="adjustTextareaHeight"></textarea>
              <button @click="isLoading ? stopResponse() : sendMessage()" :disabled="!userInput.trim() && !isLoading"
                :class="['ml-2 p-3 rounded-lg', isLoading ? (settingsStore.isDark ? 'bg-red-700 hover:bg-red-800 text-red-100' : 'bg-red-500 hover:bg-red-600 text-white') : (settingsStore.isDark ? 'bg-indigo-700 hover:bg-indigo-800 text-indigo-100' : 'bg-blue-500 hover:bg-blue-600 text-white'), 'disabled:opacity-50 disabled:cursor-not-allowed']">
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
                   <div class="text-xs text-gray-500 dark:text-gray-400 mb-1">查询结果${rowCountLabel}</div>
                   <div style="overflow-x: auto; max-width: 100%; -webkit-overflow-scrolling: touch;">${tableHtml}</div>
                 </div>`;
      }
    }

    if (message.routeUrl && typeof message.routeUrl === 'string' && message.routeUrl.trim()) {
      html += `<div class="mt-3 pt-2 border-t border-gray-100 dark:border-gray-700">
             <a href="javascript:void(0)"
                class="route-link text-blue-600 dark:text-blue-400 hover:underline font-medium"
                data-url="${message.routeUrl}">
                <i class="fas fa-external-link-alt mr-1"></i>点击跳转
             </a>
           </div>`;
    }

    return html;
  } catch (error) {
    console.error('Markdown 渲染异常:', error);
    return '<div class="whitespace-pre-wrap text-gray-600 dark:text-gray-300">' + message.content.replace(/data:/g, '') + '</div>';
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
        messages.value = [{ role: 'assistant', content: '正在加载历史记录...', isLoading: true, visibleChars: 0, isStreaming: false, toolCalls: [] }];
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

          // 从历史记录还原 toolCalls（优先独立字段，其次 metadata 内，其次从 content 解析）
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
          messages.value = [{ role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: welcomeMessageContent.length, isStreaming: false, toolCalls: [] }];
        }
      }
    } catch (error) {
      console.error('获取历史记录失败:', error);
      if (!draftMessages || draftMessages.length === 0) {
        messages.value = [{ role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: welcomeMessageContent.length, isStreaming: false, toolCalls: [] }];
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
    await deleteByConversationId(id);
    const index = conversations.value.findIndex(c => c.id === id);
    if (index !== -1) {
      conversations.value.splice(index, 1);
      removeDraftFromStorage(id);
      if (activeId.value === id) {
        startNewConversation();
      }
    }
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
  const initialMsg = { role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: 0, isStreaming: false, toolCalls: [] };
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
      conv.title = content.substring(0, 15) + (content.length > 15 ? '...' : '');
    }
  }

  const userMessage = { role: 'user', content, isLoading: false, visibleChars: content.length, isStreaming: false };
  messages.value.push(userMessage);
  const assistantMessage = { role: 'assistant', content: '', isLoading: true, visibleChars: 0, isStreaming: true, toolCalls: [] };
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

    // await parseStream({
    //   response,
    //   onTextChange: (text) => {
    //     const msg = messages.value[messageIndex];
    //     msg.content += text;
    //     scrollToBottom();
    //   },
    //   onJsonChunk: (chunk) => {
    //     if (chunk.code !== undefined) {
    //       if (chunk.code === 8001 && chunk.data && typeof chunk.data === 'string' && chunk.data.trim()) {
    //         messages.value[messageIndex].routeUrl = chunk.data;
    //         setTimeout(() => {
    //           handleRouteJump(chunk.data.trim(), { proxy, router });
    //         }, 500);
    //       } else if (chunk.code === 0) {
    //         messages.value[messageIndex].isLoading = false;
    //         messages.value[messageIndex].isStreaming = false;
    //       } else if (chunk.code === 500) {
    //         messages.value[messageIndex].isLoading = false;
    //       }
    //     }
    //   },
    //   onDone: () => {
    //     if (messages.value[messageIndex].content === '') {
    //       messages.value[messageIndex].content = '(无返回内容)';
    //       messages.value[messageIndex].visibleChars = 6;
    //     }
    //     messages.value[messageIndex].isLoading = false;
    //     messages.value[messageIndex].isStreaming = false;
    //   },
    //   onError: (error) => {
    //     if (error.name !== 'AbortError') {
    //       console.error('流处理错误:', error);
    //       messages.value[messageIndex].content = error.message;
    //       messages.value[messageIndex].visibleChars = error.message.length;
    //     }
    //   }
    // });
    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let streamEnded = false;
    let innerBuffer = "";
    let isInnerJson = false;

    while (true) {
      if (streamEnded) break;
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() || "";

      for (const line of lines) {
        const trimmedLine = line.trim();
        if (!trimmedLine) continue;

        if (trimmedLine.startsWith("event:")) {
          const eventType = trimmedLine.substring(6).trim();
          if (eventType === "done" || eventType === "message_end") {
            streamEnded = true;
            break;
          }
          continue;
        }

        let content = trimmedLine;
        if (content.startsWith("data:")) {
          content = content.substring(5).trim();
        }
        if (!content) continue;

        try {
          const jsonData = JSON.parse(content);

          // 通过 JSON event 字段判断消息结束（新格式无 SSE event 行）
          if (jsonData.event === "message_end") {
            if (jsonData.messageId) {
              messages.value[messageIndex].messageId = jsonData.messageId;
            }
            if (jsonData.conversationId) {
              messages.value[messageIndex].conversationId = jsonData.conversationId;
            }
            streamEnded = true;
            break;
          }

          // 工具调用事件：单独存到 toolCalls 数组，不混入消息正文
          if (jsonData.event === "tool_call") {
            const rawText = (jsonData.content != null ? jsonData.content : jsonData.msg) || '';
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
                index: jsonData.index,
                startTime: Date.now()
              });
            }
            scrollToBottom();
            continue;
          }

          // 收到 message 事件（开始输出消息内容）时，把上一轮仍处于"调用中"的工具标记为"完成"
          if (jsonData.event === "message" && messages.value[messageIndex].toolCalls) {
            messages.value[messageIndex].toolCalls.forEach(t => {
              if (t.status === 'running') {
                t.status = 'done';
                t.endTime = Date.now();
              }
            });
          }

          // 提取文本：优先 content（新格式），其次 msg（旧格式兼容）
          const text = jsonData.content != null ? jsonData.content : jsonData.msg;

          if (text && typeof text === "string") {
            if (!isInnerJson && (text.startsWith('{') || text.startsWith('['))) {
              isInnerJson = true;
            }
            innerBuffer += text;
            if (tryParseInnerJson(innerBuffer, messageIndex, isInnerJson)) {
              innerBuffer = "";
            }
          }
        } catch (e) {
          console.error("解析 JSON 失败:", content, e);
        }
      }
    }

    processRemainingBuffer(innerBuffer, messageIndex, isInnerJson);

    // 流结束后的处理（保持原有逻辑）
    if (messages.value[messageIndex].content === '') {
      messages.value[messageIndex].content = '(无返回内容)';
      messages.value[messageIndex].visibleChars = 6;
    }
    // tool_call 状态标记完成
    if (messages.value[messageIndex].toolCalls && messages.value[messageIndex].toolCalls.length > 0) {
      messages.value[messageIndex].toolCalls.forEach(tc => { tc.status = 'done'; });
    }
    messages.value[messageIndex].isLoading = false;
    messages.value[messageIndex].isStreaming = false;
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

const processChunk = (dataPart, messageIndex, currentConversationId) => {
  try {
    const chunk = JSON.parse(dataPart);
    const code = chunk.code;
    const msg = chunk.msg || '';

    switch (code) {
      case 8000:
        messages.value[messageIndex].content += msg;
        messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
        messages.value[messageIndex].isLoading = false;
        scrollToBottom();
        break;

      case 8001:
        if (msg) {
          messages.value[messageIndex].content += msg;
          messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
        }
        if (chunk.data && typeof chunk.data === 'string' && chunk.data.trim()) {
          messages.value[messageIndex].routeUrl = chunk.data;
          setTimeout(() => {
            handleRouteJump(chunk.data.trim(), { proxy, router });
          }, 500);
        }
        messages.value[messageIndex].isLoading = false;
        scrollToBottom();
        break;

      case 200:
        messages.value[messageIndex].content += '\n\n' + msg;
        messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
        messages.value[messageIndex].isLoading = false;
        scrollToBottom();
        break;

      case 500:
        messages.value[messageIndex].content += '\n\n错误: ' + msg;
        messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
        messages.value[messageIndex].isLoading = false;
        scrollToBottom();
        break;

      case 0:
        messages.value[messageIndex].isLoading = false;
        messages.value[messageIndex].isStreaming = false;
        scrollToBottom();
        break;

      default:
        console.warn('未知的 code:', code);
        break;
    }
  } catch (e) {
    console.error('解析数据失败:', dataPart, e);
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

const tryParseInnerJson = (buffer, messageIndex, isInnerJson) => {
  if (!buffer) return false;

  if (!isInnerJson) {
    messages.value[messageIndex].content += buffer;
    scrollToBottom();
    return true;
  }

  const parsed = parseNestedJson(buffer);

  if (parsed) {
    const innerMsg = parsed.msg || '';
    const innerCode = parsed.code;
    const innerData = parsed.data;

    if (innerCode === 8001 && innerData && typeof innerData === 'string' && innerData.trim()) {
      messages.value[messageIndex].content += innerMsg;
      messages.value[messageIndex].routeUrl = innerData;
      scrollToBottom();
      setTimeout(() => {
        handleRouteJump(innerData.trim(), { proxy, router });
      }, 500);
    } else if (innerCode === 9999 && innerData && typeof innerData === 'object') {
      // 数据查询结果：data 为对象，含 result(数组)/summary/generatedSql/rowCount
      const rows = Array.isArray(innerData.result) ? innerData.result : [];
      const summary = innerData.summary || '';
      const rowCount = innerData.rowCount;
      messages.value[messageIndex].content += innerMsg;
      if (summary) {
        messages.value[messageIndex].content += '\n\n' + summary;
      }
      messages.value[messageIndex].dataTable = rows;
      messages.value[messageIndex].dataRowCount = rowCount;
      scrollToBottom();
    } else if (innerCode === 500) {
      messages.value[messageIndex].content += innerMsg;
      scrollToBottom();
    } else {
      if (innerMsg && typeof innerMsg === 'string') {
        messages.value[messageIndex].content += innerMsg;
        scrollToBottom();
      }
    }
    return true;
  }
  return false;
};

const processRemainingBuffer = (buffer, messageIndex, isInnerJson) => {
  if (!buffer) return;

  if (!isInnerJson) {
    messages.value[messageIndex].content += buffer;
    scrollToBottom();
    return;
  }

  const parsed = parseNestedJson(buffer);

  if (parsed) {
    const innerMsg = parsed.msg || '';
    const innerCode = parsed.code;
    const innerData = parsed.data;

    if (innerCode === 8001 && innerData && typeof innerData === 'string' && innerData.trim()) {
      messages.value[messageIndex].content += innerMsg;
      messages.value[messageIndex].routeUrl = innerData;
      scrollToBottom();
      setTimeout(() => {
        handleRouteJump(innerData.trim(), { proxy, router });
      }, 500);
    } else if (innerCode === 9999 && innerData && typeof innerData === 'object') {
      const rows = Array.isArray(innerData.result) ? innerData.result : [];
      const summary = innerData.summary || '';
      const rowCount = innerData.rowCount;
      messages.value[messageIndex].content += innerMsg;
      if (summary) {
        messages.value[messageIndex].content += '\n\n' + summary;
      }
      messages.value[messageIndex].dataTable = rows;
      messages.value[messageIndex].dataRowCount = rowCount;
      scrollToBottom();
    } else if (innerCode === 500) {
      messages.value[messageIndex].content += innerMsg;
      scrollToBottom();
    } else {
      if (innerMsg && typeof innerMsg === 'string') {
        messages.value[messageIndex].content += innerMsg;
        scrollToBottom();
      }
    }
  } else {
    messages.value[messageIndex].content += buffer;
    scrollToBottom();
  }
};

const renderDataTable = (data) => {
  if (!data || !data.length) return '';
  const headers = Object.keys(data[0]);
  // 不强制 width:100%，让列多时自然撑开由外层容器 overflow-x 滚动；
  // 单元格限制最大宽度并允许换行，避免长内容（hash/URL）撑爆
  let html = '<table border="1" cellpadding="4" cellspacing="0" style="border-collapse: collapse; min-width: 100%; width: auto; font-size: 12px; table-layout: auto;">';
  html += '<thead><tr>';
  headers.forEach(header => {
    html += `<th style="background: #f5f5f5; padding: 6px 8px; text-align: left; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${header}">${header}</th>`;
  });
  html += '</tr></thead><tbody>';
  data.forEach(row => {
    html += '<tr>';
    headers.forEach(header => {
      const value = row[header];
      const text = value !== undefined && value !== null ? String(value) : '';
      html += `<td style="padding: 6px 8px; border-top: 1px solid #eee; max-width: 220px; word-break: break-all; overflow-wrap: anywhere;">${text}</td>`;
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

::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

aside::-webkit-scrollbar {
  width: 4px;
}

aside::-webkit-scrollbar-thumb {
  background: #888;
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
</style>