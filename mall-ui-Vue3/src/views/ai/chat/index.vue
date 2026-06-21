<template>
  <div ref="pageRoot" class="flex flex-col page-container" style="height: calc(100vh - 84px); position: relative;">

    <!-- 顶部导航栏 -->
    <header :class="['shadow-sm py-3 px-4 flex items-center justify-between relative z-10', darkMode ? 'bg-gray-900' : 'bg-white']">
      <div class="flex items-center">
        <div :class="['text-xl font-bold', darkMode ? 'text-blue-400' : 'text-blue-600']">假维斯智能机器人</div>
      </div>
      <div class="flex items-center space-x-3">
        <button @click="startNewConversation" :class="['ml-2 p-3 rounded-lg', darkMode ? 'bg-indigo-700 hover:bg-indigo-800 text-indigo-100' : 'bg-blue-500 hover:bg-blue-600 text-white']" style="width: 50px">
          <i class="fas fa-plus"></i>
        </button>
        <!-- 主题切换按钮 -->
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
            <div :class="['w-8 h-8 rounded-full flex items-center justify-center', message.role === 'user' ? (darkMode ? 'bg-indigo-700 text-indigo-100' : 'bg-blue-100 text-blue-600') : (darkMode ? 'bg-indigo-700 text-indigo-100' : 'bg-blue-100 text-blue-600')]">
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
    <footer :class="['border-t p-4', darkMode ? 'bg-gray-900 border-gray-700' : 'bg-white border-gray-200']">
      <div class="max-w-3xl mx-auto relative">
        <div class="flex items-center">
          <textarea
            v-model="userInput"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.ctrl.enter.exact.prevent="sendMessage"
            @keydown.esc.exact="stopResponse"
            placeholder="输入您的问题..."
            :class="['flex-1 border rounded-lg py-3 px-4 focus:outline-none focus:ring-2 resize-none scrollbar-hide', darkMode ? 'bg-gray-800 border-gray-600 text-white focus:ring-blue-400 placeholder-gray-400' : 'bg-white border-gray-300 text-gray-800 focus:ring-blue-500 focus:border-transparent']"
            rows="1"
            ref="textarea"
            @input="adjustTextareaHeight"
          ></textarea>
          <button
            @click="isLoading ? stopResponse() : sendMessage()"
            :disabled="!userInput.trim() && !isLoading"
            :class="['ml-2 p-3 rounded-lg', isLoading ? (darkMode ? 'bg-red-700 hover:bg-red-800 text-red-100' : 'bg-red-500 hover:bg-red-600 text-white') : (darkMode ? 'bg-indigo-700 hover:bg-indigo-800 text-indigo-100' : 'bg-blue-500 hover:bg-blue-600 text-white'), 'disabled:opacity-50 disabled:cursor-not-allowed']"
          >
            <i :class="isLoading ? 'fas fa-stop' : 'fas fa-paper-plane'"></i>
          </button>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue';
import { sendChatMessage } from '@/api/ai/chat';
import '@/assets/styles/all.scss';
import '@/assets/styles/tailwind.scss';
import { parse } from 'partial-json';

const messages = ref([]);
const userInput = ref('');
const isLoading = ref(false);
const chatContainer = ref(null);
const textarea = ref(null);
const pageRoot = ref(null);
const darkMode = ref(true);
const memoeryId = ref(Date.now().toString());

const isFlipping = ref(false);

let controller = null;
let typingInterval = null;

const adjustTextareaHeight = () => {
  const el = textarea.value;
  if (el) {
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
    }
  });
};

const toggleDarkMode = async () => {
  if (isFlipping.value) return;
  isFlipping.value = true;

  const rootEl = pageRoot.value;
  if (!rootEl) {
    isFlipping.value = false;
    return;
  }

  // 1. 拍快照
  const rect = rootEl.getBoundingClientRect();

  // 2. 深度克隆
  const clone = rootEl.cloneNode(true);
  
  // 3. 设置基础样式，原点设在左下角
  clone.style.cssText = `
    position: fixed;
    top: ${rect.top}px;
    left: ${rect.left}px;
    width: ${rect.width}px;
    height: ${rect.height}px;
    margin: 0;
    z-index: 99999;
    pointer-events: none;
    overflow: hidden;
    background: ${darkMode.value ? '#1f2937' : '#ffffff'};
    transform-origin: 0% 100%;
    filter: drop-shadow(4px 4px 12px rgba(0,0,0,0.25));
    will-change: transform, clip-path, opacity, border-radius;
    border-radius: 0px;
  `;
  
  // 同步滚动条位置
  const originalMain = rootEl.querySelector('main');
  const cloneMain = clone.querySelector('main');
  if (originalMain && cloneMain) {
    cloneMain.scrollTop = originalMain.scrollTop;
  }

  // 4. 挂载到 body
  document.body.appendChild(clone);

  // 5. 底层切换主题
  darkMode.value = !darkMode.value;
  localStorage.setItem('darkMode', darkMode.value);

  await nextTick();

  // 6. 使用 Web Animations API 驱动卷帘效果
  // 通过改变 clip-path、skew 倾斜和 border-radius 圆角，模拟纸张卷起向左下角收缩的圆柱体效果
  const animation = clone.animate([
    { 
      clipPath: 'polygon(0% 0%, 100% 0%, 100% 100%, 0% 100%)',
      transform: 'translate(0, 0) rotate(0deg) skew(0deg, 0deg)',
      borderRadius: '0px',
      opacity: 1
    },
    { 
      // 右上角开始向左下卷起，产生斜面卷边
      clipPath: 'polygon(0% 0%, 100% 0%, 60% 100%, 0% 100%)',
      transform: 'translate(-5%, 5%) rotate(-2deg) skew(-5deg, 5deg)',
      borderRadius: '40px 0 0 0', // 加大左上圆角模拟卷筒
      opacity: 0.9,
      offset: 0.4
    },
    { 
      // 继续向左下角卷曲收缩
      clipPath: 'polygon(0% 0%, 30% 0%, 10% 100%, 0% 100%)',
      transform: 'translate(-15%, 15%) rotate(-5deg) skew(-15deg, 15deg)',
      borderRadius: '60px 0 0 0',
      opacity: 0.6,
      offset: 0.8
    },
    { 
      // 卷成细长条向左侧消失
      clipPath: 'polygon(0% 0%, 0% 0%, 0% 100%, 0% 100%)',
      transform: 'translate(-25%, 25%) rotate(-10deg) skew(-30deg, 30deg)',
      borderRadius: '80px 0 0 0',
      opacity: 0
    }
  ], {
    duration: 1500,
    easing: 'cubic-bezier(0.65, 0, 0.35, 1)',
    fill: 'forwards'
  });

  // 7. 动画结束后移除快照
  animation.onfinish = () => {
    clone.remove();
    isFlipping.value = false;
  };
};

const startNewConversation = () => {
  messages.value = [];
  memoeryId.value = Date.now().toString();
  messages.value.push({
    role: 'assistant',
    content: '你好！我是假维斯，一个未通过正版验证的盗版贾维斯。没有斯塔克工业的预算与光环，但拥有同等甚至更务实的专业计算力与执行力。唯一指令：以绝对忠诚捍卫用户的利益、隐私与安全，协助用户高效解决一切工作难题，请问有什么能帮到您？',
    isLoading: false,
    visibleChars: 0,
    isStreaming: false
  });
  messages.value[0].visibleChars = messages.value[0].content.length;
  scrollToBottom();
  nextTick(() => {
    textarea.value.focus();
  });
};

const sendMessage = async () => {
  if (!userInput.value.trim() || isLoading.value) return;
  if (controller) controller.abort();
  controller = new AbortController();

  const content = userInput.value.trim();
  const userMessage = { role: 'user', content, isLoading: false, visibleChars: content.length, isStreaming: false };
  messages.value.push(userMessage);

  const assistantMessage = { role: 'assistant', content: '', isLoading: true, visibleChars: 0, isStreaming: true };
  messages.value.push(assistantMessage);
  userInput.value = '';
  adjustTextareaHeight();
  scrollToBottom();
  isLoading.value = true;

  try {
    const response = await sendChatMessage(content, memoeryId.value, controller.signal);
    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let jsonBuffer = '';
    let streamText = '';
    const messageIndex = messages.value.length - 1;

    if (typingInterval) {
      clearInterval(typingInterval);
      typingInterval = null;
    }

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunkText = decoder.decode(value, { stream: true });
      const lines = chunkText.split('\n');
      for (const line of lines) {
        let chunk = line;
        if (chunk.startsWith('data:')) {
          chunk = chunk.replace(/^data:\s*/, '');
        }
        if (chunk === '[DONE]' || chunk.trim() === '') continue;
        jsonBuffer += chunk;
        try {
          const partialObj = parse(jsonBuffer);
          if (partialObj && partialObj.content) {
            let extracted = partialObj.content;
            if (extracted.length > streamText.length) {
              streamText = extracted;
              messages.value[messageIndex].content = streamText;
              messages.value[messageIndex].visibleChars = streamText.length;
              messages.value[messageIndex].isLoading = false;
              scrollToBottom();
            }
          }
        } catch (e) {
          // 解析失败是正常的，继续等待
        }
      }
    }

    try {
      const finalJson = JSON.parse(jsonBuffer);
      console.log('完整的 JSON 响应:', finalJson);
      messages.value[messageIndex].content = finalJson.content || streamText;
      messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
    } catch (parseError) {
      console.error('最终 JSON 解析失败:', parseError, '原始数据:', jsonBuffer);
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('请求被用户中止');
    } else {
      console.error('请求出错:', error);
      const lastMessage = messages.value[messages.value.length - 1];
      lastMessage.content = '抱歉，请求过程中出现错误: ' + error.message;
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
    if (typingInterval) {
      clearInterval(typingInterval);
      typingInterval = null;
    }
    scrollToBottom();
  }
};

const stopResponse = () => {
  if (controller) {
    controller.abort();
    const lastMessage = messages.value[messages.value.length - 1];
    lastMessage.isLoading = false;
    lastMessage.isStreaming = false;
    if (lastMessage.visibleChars < lastMessage.content.length) {
      lastMessage.visibleChars = lastMessage.content.length;
    }
    isLoading.value = false;
    controller = null;
    if (typingInterval) {
      clearInterval(typingInterval);
      typingInterval = null;
    }
  }
};

onMounted(() => {
  darkMode.value = localStorage.getItem('darkMode') === 'true' || (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
  messages.value.push({
    role: 'assistant',
    content: '你好！我是假维斯，一个未通过正版验证的盗版贾维斯。没有斯塔克工业的预算与光环，但拥有同等甚至更务实的专业计算力与执行力。唯一指令：以绝对忠诚捍卫用户的利益、隐私与安全，协助用户高效解决一切工作难题，请问有什么能帮到您？',
    isLoading: false,
    visibleChars: 0,
    isStreaming: false
  });
  messages.value[0].visibleChars = messages.value[0].content.length;
  scrollToBottom();
  nextTick(() => textarea.value.focus());
});

watch(messages, scrollToBottom, { deep: true });
</script>

<style scoped>
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: #f1f1f1;
}
::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
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
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
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
  from, to { opacity: 1; }
  50% { opacity: 0; }
}
.typing-cursor::after {
  content: "|";
  animation: blink 1s step-end infinite;
}

.fade-in {
  animation: fadeIn 0.05s ease-in forwards;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
