<template>
  <!-- 高度适配 RuoYi Tab 页面 -->
  <div ref="pageRoot" class="flex h-full overflow-hidden" style="position: relative;">
    
    <!-- 左侧侧边栏 -->
    <aside :class="['flex flex-col border-r transition-colors duration-300 w-64 m-0 py-2 px-0', darkMode ? 'bg-gray-800 border-gray-700' : 'bg-gray-50 border-gray-200']">
      <!-- 侧边栏顶部：新建对话 -->
      <div class="px-4 pb-2 flex-shrink-0">
        <button @click="startNewConversation" :class="['w-full flex items-center justify-center px-4 py-2.5 rounded-lg font-medium transition-colors duration-200', darkMode ? 'bg-indigo-600 hover:bg-indigo-700 text-white' : 'bg-blue-500 hover:bg-blue-600 text-white']">
          <i class="fas fa-plus mr-2"></i>
          <span>新对话</span>
        </button>
      </div>
      
      <!-- 对话列表区域 -->
      <div class="flex-1 overflow-y-auto px-2 custom-scrollbar">
        <div class="text-xs font-semibold px-3 mb-2" :class="darkMode ? 'text-gray-500' : 'text-gray-400'">
          最近对话
        </div>
        <div v-if="conversations.length === 0" class="text-center py-4 text-sm" :class="darkMode ? 'text-gray-600' : 'text-gray-400'">
          暂无对话记录
        </div>
        <div v-for="(conv, index) in conversations" :key="conv.id" 
             @click="switchConversation(conv.id)"
             :class="[
               'group flex items-center px-3 py-2.5 rounded-lg cursor-pointer mb-1 transition-colors duration-150',
               conv.id === activeId ? (darkMode ? 'bg-gray-700 text-white' : 'bg-blue-100 text-blue-700') : (darkMode ? 'text-gray-300 hover:bg-gray-800' : 'text-gray-700 hover:bg-gray-200')
             ]">
          <i class="fas fa-message mr-3 text-sm opacity-60"></i>
          <div class="flex-1 truncate text-sm font-medium">
            {{ conv.title }}
          </div>
          <button @click.stop="deleteConversation(conv.id)" :class="['opacity-0 group-hover:opacity-100 p-1 rounded transition-all', darkMode ? 'text-gray-500 hover:text-red-400' : 'text-gray-400 hover:text-red-500']">
             <i class="fas fa-trash-alt text-xs"></i>
          </button>
        </div>
      </div>
      
      <!-- 侧边栏底部 -->
      <div class="p-4 border-t flex-shrink-0" :class="darkMode ? 'border-gray-700' : 'border-gray-200'">
         <div class="text-xs text-center" :class="darkMode ? 'text-gray-600' : 'text-gray-400'">
            假维斯智能终端 v1.0
         </div>
      </div>
    </aside>

    <!-- 右侧主内容区 -->
    <div class="flex-1 flex flex-col h-full overflow-hidden">
      <!-- 顶部导航栏 -->
      <header :class="['shadow-sm py-3 px-4 flex items-center justify-between flex-shrink-0 z-10', darkMode ? 'bg-gray-800' : 'bg-white']">
        <div class="flex items-center">
          <div :class="['text-lg font-bold truncate', darkMode ? 'text-blue-400' : 'text-blue-600']">
            {{ currentConversationTitle }}
          </div>
        </div>
        <div class="flex items-center space-x-3">
          <button @click="toggleDarkMode" class="p-2 rounded-full hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors duration-300">
            <i :class="['text-xl transition-all duration-500', darkMode ? 'fas fa-sun text-yellow-400' : 'fas fa-moon text-gray-600']"></i>
          </button>
        </div>
      </header>

      <!-- 聊天内容区域 -->
      <main ref="chatContainer" class="flex-1 overflow-y-auto p-4 space-y-6" :class="{ 'bg-white': !darkMode, 'bg-gray-800': darkMode }">
        <div v-for="(message, index) in messages" :key="index" class="max-w-3xl mx-auto">
          <div :class="['flex', message.role === 'user' ? 'justify-end' : 'justify-start']">
            <div :class="['flex items-start space-x-3', message.role === 'user' ? 'flex-row-reverse space-x-reverse' : '']">
              <div :class="['w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0', message.role === 'user' ? (darkMode ? 'bg-indigo-700 text-indigo-100' : 'bg-blue-100 text-blue-600') : (darkMode ? 'bg-indigo-700 text-indigo-100' : 'bg-blue-100 text-blue-600')]">
                <i :class="message.role === 'user' ? 'fas fa-user' : 'fas fa-robot'"></i>
              </div>
              <div :class="['p-3 rounded-lg max-w-lg', message.role === 'user' ? 'bg-blue-500 text-white' : darkMode ? 'bg-gray-700 text-gray-100 border border-gray-600' : 'bg-white shadow border border-gray-200 text-gray-800']">
                <div v-if="message.role === 'assistant' && message.isLoading" class="flex space-x-2">
                  <div :class="['w-2 h-2 rounded-full', darkMode ? 'bg-gray-400' : 'bg-gray-300', 'animate-pulse']"></div>
                  <div :class="['w-2 h-2 rounded-full', darkMode ? 'bg-gray-400' : 'bg-gray-300', 'animate-pulse delay-100']"></div>
                  <div :class="['w-2 h-2 rounded-full', darkMode ? 'bg-gray-400' : 'bg-gray-300', 'animate-pulse delay-200']"></div>
                </div>
                <div v-else class="whitespace-pre-wrap">
                  <span v-for="(char, charIndex) in message.content" :key="charIndex" :class="{ 'opacity-0': charIndex >= message.visibleChars, 'fade-in': charIndex < message.visibleChars }">
                    {{ char }}
                  </span>
                  <span v-if="message.isStreaming" class="typing-cursor"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- 输入框区域 -->
      <footer :class="['border-t p-4 flex-shrink-0', darkMode ? 'bg-gray-800 border-gray-700' : 'bg-white border-gray-200']">
        <div class="max-w-3xl mx-auto relative">
          <div class="flex items-center">
            <textarea v-model="userInput" @keydown.enter.exact.prevent="sendMessage" @keydown.ctrl.enter.exact.prevent="sendMessage" @keydown.esc.exact="stopResponse" placeholder="输入您的问题..." :class="['flex-1 border rounded-lg py-3 px-4 focus:outline-none focus:ring-2 resize-none scrollbar-hide', darkMode ? 'bg-gray-800 border-gray-600 text-white focus:ring-blue-400 placeholder-gray-400' : 'bg-white border-gray-300 text-gray-800 focus:ring-blue-500 focus:border-transparent']" rows="1" ref="textarea" @input="adjustTextareaHeight"></textarea>
            <button @click="isLoading ? stopResponse() : sendMessage()" :disabled="!userInput.trim() && !isLoading" :class="['ml-2 p-3 rounded-lg', isLoading ? (darkMode ? 'bg-red-700 hover:bg-red-800 text-red-100' : 'bg-red-500 hover:bg-red-600 text-white') : (darkMode ? 'bg-indigo-700 hover:bg-indigo-800 text-indigo-100' : 'bg-blue-500 hover:bg-blue-600 text-white'), 'disabled:opacity-50 disabled:cursor-not-allowed']">
              <i :class="isLoading ? 'fas fa-stop' : 'fas fa-paper-plane'"></i>
            </button>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import {computed, nextTick, onMounted, ref, watch} from 'vue';
import {sendChatMessage} from '@/api/ai/chat';
import { create as createConversationApi, getConversationListByUserId as fetchConversationListApi } from '@/api/ai/conversation';
import { getChatMemoryListByConversationId } from '@/api/ai/chatmemory';
import '@/assets/styles/all.scss';
import '@/assets/styles/tailwind.scss';
import {parse} from 'partial-json';

const messages = ref([]);
const userInput = ref('');
const isLoading = ref(false);
const chatContainer = ref(null);
const textarea = ref(null);
const pageRoot = ref(null);
const darkMode = ref(true);
const isFlipping = ref(false);
let controller = null;
let typingInterval = null;

const conversations = ref([]);
const activeId = ref(null);

const welcomeMessageContent = '你好！我是假维斯，一个未通过正版验证的盗版贾维斯。没有斯塔克工业的预算与光环，但拥有同等甚至更务实的专业计算力与执行力。唯一指令：以绝对忠诚捍卫用户的利益、隐私与安全，协助用户高效解决一切工作难题，请问有什么能帮到您？';

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
  }
};

const adjustTextareaHeight = () => {
  const el = textarea.value;
  if (el) {
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
  }
};

// --- 优化：智能滚动逻辑 ---
const scrollToBottom = () => {
  nextTick(() => {
    if (!chatContainer.value) return;
    const container = chatContainer.value;
    const currentTop = container.scrollTop;
    const scrollHeight = container.scrollHeight;
    const clientHeight = container.clientHeight;
    
    // 计算距离底部的距离
    // scrollHeight 是总高度，clientHeight 是可视高度，scrollTop 是滚动条位置
    // 底部位置 = scrollHeight - clientHeight
    // 当前距离底部的距离 = (scrollHeight - clientHeight) - scrollTop
    const distanceToBottom = scrollHeight - clientHeight - currentTop;

    // 阈值判断：如果距离底部超过 300px (大约一屏或大量新消息)，使用平滑滚动
    // 否则（流式输出增加几行），使用瞬间滚动以跟随打字效果
    if (distanceToBottom > 300) {
      container.scrollTo({
        top: scrollHeight,
        behavior: 'smooth'
      });
    } else {
      container.scrollTop = scrollHeight;
    }
  });
};

const switchConversation = async (id) => {
  if (activeId.value === id) return;
  activeId.value = id;
  const conv = conversations.value.find(c => c.id === id);
  if (conv) {
    if (conv.messages.length > 0) {
      messages.value = conv.messages;
      scrollToBottom(); // 这里的距离通常很大，会触发 smooth
      nextTick(() => { if(textarea.value) textarea.value.focus(); });
      return;
    }

    try {
      messages.value = [{ role: 'assistant', content: '正在加载历史记录...', isLoading: true, visibleChars: 0, isStreaming: false }];
      
      const response = await getChatMemoryListByConversationId(id);
      if (response.data && Array.isArray(response.data)) {
        const historyMsgs = response.data.map(item => {
          const role = item.type === 'USER' ? 'user' : 'assistant';
          let displayContent = item.content;

          if (role === 'assistant' && typeof displayContent === 'string') {
            try {
              const parsed = JSON.parse(displayContent);
              if (parsed && parsed.content) {
                displayContent = parsed.content;
              }
            } catch (e) {}
          }

          return {
            role: role,
            content: displayContent,
            isLoading: false,
            visibleChars: displayContent.length,
            isStreaming: false
          };
        });

        messages.value = historyMsgs;
        conv.messages = historyMsgs;
      } else {
        messages.value = [{ role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: welcomeMessageContent.length, isStreaming: false }];
      }
    } catch (error) {
      console.error('获取历史记录失败:', error);
      messages.value = [{ role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: welcomeMessageContent.length, isStreaming: false }];
    }

    scrollToBottom(); // 加载历史后，必定是大距离滚动，触发 smooth
    nextTick(() => { if(textarea.value) textarea.value.focus(); });
  }
};

const deleteConversation = (id) => {
  const index = conversations.value.findIndex(c => c.id === id);
  if (index !== -1) {
    conversations.value.splice(index, 1);
    if (activeId.value === id) {
      activeId.value = null;
      messages.value = [{ role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: welcomeMessageContent.length, isStreaming: false }];
    }
  }
};

const toggleDarkMode = async () => {
  if (isFlipping.value) return;
  isFlipping.value = true;
  const rootEl = pageRoot.value;
  if (!rootEl) { isFlipping.value = false; return; }

  rootEl.classList.add('no-transition');
  const rect = rootEl.getBoundingClientRect();
  const clone = rootEl.cloneNode(true);
  clone.style.cssText = `
    position: fixed; top: ${rect.top}px; left: ${rect.left}px;
    width: ${rect.width}px; height: ${rect.height}px; margin: 0;
    z-index: 99999; pointer-events: none; overflow: hidden;
    background: ${darkMode.value ? '#1f2937' : '#ffffff'}; 
    transform-origin: 0% 100%;
    will-change: transform, clip-path;
  `;
  
  const originalMain = rootEl.querySelector('main');
  const cloneMain = clone.querySelector('main');
  if (originalMain && cloneMain) cloneMain.scrollTop = originalMain.scrollTop;

  document.body.appendChild(clone);
  darkMode.value = !darkMode.value;
  localStorage.setItem('darkMode', darkMode.value);
  await nextTick();
  rootEl.classList.remove('no-transition');

  const animation = clone.animate([
    { clipPath: 'polygon(0% 0%, 100% 0%, 100% 100%, 0% 100%)', transform: 'translate(0, 0) rotate(0deg) skew(0deg, 0deg)', opacity: 1 },
    { clipPath: 'polygon(0% 0%, 50% 0%, 20% 100%, 0% 100%)', transform: 'translate(-8%, 8%) rotate(-1deg) skew(-5deg, 5deg)', opacity: 1, offset: 0.6 },
    { clipPath: 'polygon(0% 0%, 0% 0%, 0% 100%, 0% 100%)', transform: 'translate(-15%, 15%) rotate(-2deg) skew(-10deg, 10deg)', opacity: 1 }
  ], { duration: 800, easing: 'cubic-bezier(0.4, 0, 0.2, 1)', fill: 'forwards' });

  animation.onfinish = () => { clone.remove(); isFlipping.value = false; };
};

const startNewConversation = async () => {
  try {
    const res = await createConversationApi();
    const newId = res.data;
    if (newId) {
      const newConv = { id: newId, title: '新对话', messages: [] };
      conversations.value.unshift(newConv);
      activeId.value = newId;
      const initialMsg = { role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: 0, isStreaming: false };
      messages.value = [initialMsg];
      nextTick(() => {
        if(messages.value[0]) messages.value[0].visibleChars = messages.value[0].content.length;
      });
      scrollToBottom();
      nextTick(() => { if(textarea.value) textarea.value.focus(); });
    }
  } catch (error) {
    console.error("创建新会话失败:", error);
  }
};

const sendMessage = async () => {
  if (!userInput.value.trim() || isLoading.value) return;
  const content = userInput.value.trim();
  
  let currentConversationId = activeId.value;

  if (!currentConversationId) {
    try {
      isLoading.value = true;
      const createRes = await createConversationApi();
      const newId = createRes.data;
      const realId = (typeof newId === 'object' && newId !== null) ? newId.conversationId : newId;

      if (realId) {
        const title = content.substring(0, 15) + (content.length > 15 ? '...' : '');
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
  const assistantMessage = { role: 'assistant', content: '', isLoading: true, visibleChars: 0, isStreaming: true };
  messages.value.push(assistantMessage);
  
  userInput.value = '';
  adjustTextareaHeight();
  scrollToBottom(); // 发送消息瞬间增加两条，如果是长对话列表，会触发 smooth
  isLoading.value = true;

  try {
    const response = await sendChatMessage(content, currentConversationId, controller?.signal);

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

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let jsonBuffer = '';
    let streamText = '';
    const messageIndex = messages.value.length - 1;
    if (typingInterval) { clearInterval(typingInterval); typingInterval = null; }

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunkText = decoder.decode(value, { stream: true });
      const lines = chunkText.split('\n');
      for (const line of lines) {
        let chunk = line;
        if (chunk.startsWith('data:')) chunk = chunk.replace(/^data:\s*/, '');
        if (chunk === '[DONE]' || chunk.trim() === '') continue;
        jsonBuffer += chunk;
        try {
          const partialObj = parse(jsonBuffer);
          if (partialObj && partialObj.code && partialObj.code !== 200 && partialObj.msg) {
             throw new Error(partialObj.msg);
          }
          if (partialObj && partialObj.content) {
            let extracted = partialObj.content;
            if (extracted.length > streamText.length) {
              streamText = extracted;
              messages.value[messageIndex].content = streamText;
              messages.value[messageIndex].visibleChars = streamText.length;
              messages.value[messageIndex].isLoading = false;
              scrollToBottom(); // 流式输出，距离很小，触发瞬间滚动
            }
          }
        } catch (e) { 
           if (!(e instanceof Error && e.message)) {
             /* continue parsing */
           } else {
             throw e;
           }
        }
      }
    }
    try {
      const finalJson = JSON.parse(jsonBuffer);
      messages.value[messageIndex].content = finalJson.content || streamText;
      messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
    } catch (parseError) { console.error('Final parse error:', parseError); }

  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('请求被中止');
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
    if (typingInterval) { clearInterval(typingInterval); typingInterval = null; }

    const currentConv = conversations.value.find(c => c.id === currentConversationId);
    if (currentConv) {
      currentConv.messages = JSON.parse(JSON.stringify(messages.value));
    }
    scrollToBottom();
  }
};

const stopResponse = () => {
  if (controller) {
    controller.abort();
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage) {
      lastMessage.isLoading = false;
      lastMessage.isStreaming = false;
      if (lastMessage.visibleChars < lastMessage.content.length) lastMessage.visibleChars = lastMessage.content.length;
    }
    isLoading.value = false;
    controller = null;
    if (typingInterval) { clearInterval(typingInterval); typingInterval = null; }

    const currentConv = conversations.value.find(c => c.id === activeId.value);
    if (currentConv) {
      currentConv.messages = JSON.parse(JSON.stringify(messages.value));
    }
  }
};

onMounted(() => {
  darkMode.value = localStorage.getItem('darkMode') === 'true' || (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
  initRecentConversations();
  activeId.value = null;
  const initialMsg = { role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: 0, isStreaming: false };
  messages.value = [initialMsg];
  nextTick(() => {
    if(messages.value[0]) messages.value[0].visibleChars = messages.value[0].content.length;
  });
});

watch(messages, scrollToBottom, { deep: true });
</script>

<style scoped>
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #c1c1c1; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #a8a8a8; }

aside::-webkit-scrollbar { width: 4px; }
aside::-webkit-scrollbar-thumb { background: #888; }

textarea { min-height: 44px; max-height: 200px; transition: height 0.2s; overflow-y: auto; }
.scrollbar-hide::-webkit-scrollbar { display: none; }
.scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
.no-transition, .no-transition * { transition: none !important; }

@keyframes pulse { 0%, 100% { opacity: 0.5; } 50% { opacity: 1; } }
.animate-pulse { animation: pulse 1.5s infinite; }
.delay-100 { animation-delay: 0.1s; }
.delay-200 { animation-delay: 0.2s; }

@keyframes blink { from, to { opacity: 1; } 50% { opacity: 0; } }
.typing-cursor::after { content: "|"; animation: blink 1s step-end infinite; }

.fade-in { animation: fadeIn 0.05s ease-in forwards; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
</style>
