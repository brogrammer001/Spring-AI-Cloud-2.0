<template>
    <div class="flex flex-col" style="height: calc(100vh - 84px);">
        <!-- 顶部导航栏 -->
        <header class="bg-white shadow-sm py-3 px-4 flex items-center justify-between">
            <div class="flex items-center">
                <div class="text-xl font-bold text-blue-600">AI志愿填报顾问</div>
            </div>
            <div class="flex items-center space-x-3">
                <button @click="startNewConversation" class="ml-2 p-3 rounded-lg bg-green-500 hover:bg-green-600 text-white" style="width: 50px">
                    <i class="fas fa-plus"></i>
                </button>
                <button @click="toggleDarkMode" class="p-2 rounded-full hover:bg-gray-100">
                    <i :class="darkMode ? 'fas fa-moon text-gray-600' : 'fas fa-sun text-gray-600'"></i>
                </button>
            </div>
        </header>

        <!-- 聊天内容区域 -->
        <main ref="chatContainer" class="flex-1 overflow-y-auto p-4 space-y-6" :class="{ 'bg-gray-800': darkMode }">
            <div v-for="(message, index) in messages" :key="index" class="max-w-3xl mx-auto">
                <div :class="['flex', message.role === 'user' ? 'justify-end' : 'justify-start']">
                    <div :class="['flex items-start space-x-3', message.role === 'user' ? 'flex-row-reverse space-x-reverse' : '']">
                        <div :class="['w-8 h-8 rounded-full flex items-center justify-center',
                                        message.role === 'user' ? 'bg-blue-100 text-blue-600' : 'bg-green-100 text-green-600',
                                        darkMode && message.role === 'assistant' ? 'bg-gray-700 text-green-400' : '']">
                            <i :class="message.role === 'user' ? 'fas fa-user' : 'fas fa-robot'"></i>
                        </div>
                        <div :class="['p-3 rounded-lg max-w-lg',
                                        message.role === 'user'
                                            ? 'bg-blue-500 text-white'
                                            : darkMode
                                                ? 'bg-gray-700 text-gray-100 border-gray-600'
                                                : 'bg-white shadow border border-gray-100']">
                            <div v-if="message.role === 'assistant' && message.isLoading" class="flex space-x-2">
                                <div :class="['w-2 h-2 rounded-full', darkMode ? 'bg-gray-400' : 'bg-gray-300', 'animate-pulse']"></div>
                                <div :class="['w-2 h-2 rounded-full', darkMode ? 'bg-gray-400' : 'bg-gray-300', 'animate-pulse delay-100']"></div>
                                <div :class="['w-2 h-2 rounded-full', darkMode ? 'bg-gray-400' : 'bg-gray-300', 'animate-pulse delay-200']"></div>
                            </div>
                            <div v-else class="whitespace-pre-wrap">
                                <span v-for="(char, charIndex) in message.content" :key="charIndex"
                                      :class="{'opacity-0': charIndex >= message.visibleChars, 'fade-in': charIndex < message.visibleChars}">
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
        <footer :class="['border-t p-4', darkMode ? 'bg-gray-800 border-gray-700' : 'bg-white border-gray-200']">
            <div class="max-w-3xl mx-auto relative">
                <div class="flex items-center">
                    <textarea
                        v-model="userInput"
                        @keydown.enter.exact.prevent="sendMessage"
                        @keydown.ctrl.enter.exact.prevent="sendMessage"
                        @keydown.esc.exact="stopResponse"
                        placeholder="输入您的问题..."
                        :class="['flex-1 border rounded-lg py-3 px-4 pr-12 focus:outline-none focus:ring-2 resize-none',
                            darkMode
                                ? 'bg-gray-700 border-gray-600 text-white focus:ring-blue-400 placeholder-gray-400'
                                : 'border-gray-300 focus:ring-blue-500 focus:border-transparent']"
                        rows="1"
                        ref="textarea"
                        @input="adjustTextareaHeight"
                    ></textarea>
                    <button
                        @click="isLoading ? stopResponse() : sendMessage()"
                        :disabled="!userInput.trim() && !isLoading"
                        :class="['ml-2 p-3 rounded-lg',
                                isLoading
                                    ? 'bg-red-500 hover:bg-red-600 text-white'
                                    : 'bg-blue-500 hover:bg-blue-600 text-white',
                                'disabled:opacity-50 disabled:cursor-not-allowed']"
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
import '@/assets/styles/all.scss';
import '@/assets/styles/tailwind.scss';

const messages = ref([]);
const userInput = ref('');
const isLoading = ref(false);
const chatContainer = ref(null);
const textarea = ref(null);
const darkMode = ref(false);
const memoeryId = ref(Date.now().toString());

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

const toggleDarkMode = () => {
    darkMode.value = !darkMode.value;
    localStorage.setItem('darkMode', darkMode.value);
};

const startTypingEffect = (messageIndex) => {
    const message = messages.value[messageIndex];
    if (!message || message.visibleChars >= message.content.length) {
        clearInterval(typingInterval);
        typingInterval = null;
        messages.value[messageIndex].isStreaming = false;
        return;
    }
    messages.value[messageIndex].visibleChars++;
    scrollToBottom();
};

const startNewConversation = () => {
    messages.value = [];
    memoeryId.value = Date.now().toString();
    messages.value.push({
        role: 'assistant',
        content: '你好！我是传智教育提供的AI志愿填报顾问，请问有什么能帮到您？',
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
        const response = await fetch(`/chat?message=${encodeURIComponent(content)}&memoryId=${memoeryId.value}`, {
            signal: controller.signal
        });

        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');
        let buffer = '';
        const messageIndex = messages.value.length - 1;

        if (typingInterval) { clearInterval(typingInterval); typingInterval = null; }

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });
            buffer += chunk;

            messages.value[messageIndex].content = buffer;
            messages.value[messageIndex].isLoading = false;

            if (!typingInterval) {
                typingInterval = setInterval(() => startTypingEffect(messageIndex), 20);
            }

            scrollToBottom();
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
        if (typingInterval) { clearInterval(typingInterval); typingInterval = null; }
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
        if (typingInterval) { clearInterval(typingInterval); typingInterval = null; }
    }
};

onMounted(() => {
    darkMode.value = localStorage.getItem('darkMode') === 'true' ||
        (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);

    messages.value.push({
        role: 'assistant',
        content: '你好！我是传智教育提供的AI志愿填报顾问，请问有什么能帮到您？',
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
/* 自定义滚动条 */
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

/* 输入框自适应高度 */
textarea {
    min-height: 44px;
    max-height: 200px;
    transition: height 0.2s;
}

/* 加载动画 */
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

/* 打字机效果 */
@keyframes blink {
    from, to { opacity: 1; }
    50% { opacity: 0; }
}
.typing-cursor::after {
    content: "|";
    animation: blink 1s step-end infinite;
}

/* 字符淡入效果 */
.fade-in {
    animation: fadeIn 0.05s ease-in forwards;
}
@keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}
</style>
