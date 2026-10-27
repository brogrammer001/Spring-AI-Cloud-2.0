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
            <button @click.stop="deleteConversation(conv.id)"
                    :disabled="isLoading"
                    :title="isLoading ? '正在回答中，暂不能删除会话' : '删除会话'"
                    class="conv-item-delete p-1 rounded"
                    :class="{ 'conv-item-delete-disabled': isLoading }">
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
          <div class="chat-header-title" :class="{ 'is-dark': settingsStore.isDark }">
            {{ currentConversationTitle }}
          </div>
        </header>
        <main ref="chatContainer" class="chat-main flex-1 overflow-y-auto"
              :class="{ 'is-dark': settingsStore.isDark }" @click="handleRouteClick" @scroll="handleChatScroll">
          <div class="chat-content">
            <div v-for="(message, index) in messages" :key="index" class="message-row"
              :class="[message.role === 'user' ? 'message-row-user' : 'message-row-ai']">
              <!-- 用户消息：右对齐气泡 + 用户头像 -->
              <template v-if="message.role === 'user'">
                <div class="user-bubble" :class="{ 'is-dark': settingsStore.isDark }">
                  <div class="markdown-body" v-html="renderMessage(message)"></div>
                </div>
                <img class="chat-user-avatar"
                  :src="userStore.avatar && userStore.avatar.trim() ? userStore.avatar : defAva" />
              </template>
              <!-- AI 消息：品牌头像 + 全宽正文，不再使用问答式气泡 -->
              <template v-else>
                <div class="chat-robot-avatar" :class="{ 'is-dark': settingsStore.isDark }">
                  <i class="fas fa-robot"></i>
                </div>
                <div class="ai-content" :class="{ 'is-dark': settingsStore.isDark }">
                  <!-- RAG 知识库检索区块（马卡龙绿：#22c55e / 浅底 #e5f6ec） -->
                  <div v-if="message.ragRetrieve"
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
                  <!-- 工具调用区块（马卡龙紫：#8b5cf6 / 浅底 #efeafc）：默认仅展示调用次数，点击图标展开/收起详情 -->
                  <div v-if="message.toolCalls && message.toolCalls.length > 0"
                       class="mb-3"
                       :class="{ 'is-dark': settingsStore.isDark }">
                    <!-- 汇总条：调用次数 + 状态 + 展开图标 -->
                    <div class="tool-summary flex items-center gap-2 px-2.5 py-2 rounded-md text-xs cursor-pointer select-none"
                         :class="{ 'is-dark': settingsStore.isDark }"
                         title="点击查看/收起工具调用详情"
                         @click="toggleToolDetail(message)">
                      <i class="fas fa-wand-magic-sparkles tool-title flex-shrink-0"
                         :class="{ 'is-dark': settingsStore.isDark }"></i>
                      <span class="font-medium tool-summary-text"
                            :class="{ 'is-dark': settingsStore.isDark }">已调用工具</span>
                      <span class="tool-count-badge inline-flex items-center justify-center px-1.5 h-[18px] min-w-[20px] rounded-full text-[10px] font-bold"
                            :class="{ 'is-dark': settingsStore.isDark }">{{ message.toolCalls.length }}</span>
                      <span v-if="message.toolCalls.some(t => t.status === 'running')"
                            class="tool-badge tool-badge-running inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]"
                            :class="{ 'is-dark': settingsStore.isDark }">
                        <i class="fas fa-circle-notch fa-spin"></i> 调用中
                      </span>
                      <span v-else
                            class="tool-badge tool-badge-done inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px]"
                            :class="{ 'is-dark': settingsStore.isDark }">
                        <i class="fas fa-check"></i> 完成
                      </span>
                      <span class="tool-toggle-icon ml-auto flex-shrink-0"
                            :class="{ 'is-expanded': message.toolDetailVisible, 'is-dark': settingsStore.isDark }">
                        <i class="fas fa-chevron-down"></i>
                      </span>
                    </div>
                    <!-- 详情列表：工具名称逐条展示（点击汇总条展开/收起） -->
                    <div v-show="message.toolDetailVisible" class="mt-1.5 space-y-1.5">
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
                  </div>

                  <!-- 交互卡片（马卡龙蓝：#3b82f6 / 浅底 #eef4ff）：后端 code=8003 确认 / code=8002 表单补全 -->
                  <div v-if="message.interaction"
                       class="interaction-card mb-3"
                       :class="{
                         'interaction-card-resolved': message.interaction.resolved,
                         'interaction-card-cancelled': message.interaction.cancelled,
                         'is-dark': settingsStore.isDark
                       }">
                    <!-- 标题行 -->
                    <div class="interaction-header flex items-center gap-2 mb-2">
                      <i class="fas"
                         :class="message.interaction.type === 'confirm' ? 'fa-shield-alt' : 'fa-edit'"></i>
                      <span class="font-medium text-sm">{{ message.interaction.title }}</span>
                      <span v-if="message.interaction.tool"
                            class="interaction-tool-tag ml-auto text-[10px] px-1.5 py-0.5 rounded font-mono opacity-70">
                        {{ message.interaction.tool }}
                      </span>
                    </div>

                    <!-- 提示正文（支持换行） -->
                    <div v-if="message.interaction.message"
                         class="interaction-message text-xs mb-2 whitespace-pre-wrap leading-relaxed"
                         :class="{ 'is-dark': settingsStore.isDark }">
                      {{ message.interaction.message }}
                    </div>

                    <!-- 类型 1：确认卡片（type=confirm） -->
                    <template v-if="message.interaction.type === 'confirm'">
                      <div class="interaction-actions flex gap-2 pt-1"
                           :class="{ 'interaction-actions-disabled': message.interaction.resolved }">
                        <button
                          class="interaction-btn interaction-btn-primary flex-1 h-9 rounded-md text-sm font-medium"
                          :disabled="message.interaction.resolved || isLoading"
                          @click="handleConfirmAction(index, true)">
                          <i class="fas fa-check mr-1.5"></i>
                          {{ message.interaction.confirmText || '同意' }}
                        </button>
                        <button
                          class="interaction-btn interaction-btn-ghost flex-1 h-9 rounded-md text-sm font-medium"
                          :disabled="message.interaction.resolved || isLoading"
                          @click="handleConfirmAction(index, false)">
                          <i class="fas fa-times mr-1.5"></i>
                          {{ message.interaction.cancelText || '取消' }}
                        </button>
                      </div>
                    </template>

                    <!-- 类型 2：表单补全卡片（type=form） -->
                    <template v-else-if="message.interaction.type === 'form'">
                      <div class="interaction-form space-y-2 pt-1"
                           :class="{ 'interaction-form-disabled': message.interaction.resolved }">
                        <div v-for="field in message.interaction.fields"
                             :key="field.name"
                             class="interaction-field">
                          <label class="interaction-field-label block text-xs mb-1">
                            {{ field.label || field.name }}
                            <span v-if="field.required" class="interaction-field-required">*</span>
                          </label>
                          <input
                            v-if="field.inputType === 'text' || !field.inputType"
                            type="text"
                            class="interaction-input w-full h-9 px-2.5 text-sm rounded-md border transition-colors"
                            :class="{ 'is-dark': settingsStore.isDark }"
                            :placeholder="field.placeholder || ''"
                            :disabled="message.interaction.resolved"
                            v-model="ensureFormData(index)[field.name]" />
                          <input
                            v-else-if="field.inputType === 'number'"
                            type="number"
                            class="interaction-input w-full h-9 px-2.5 text-sm rounded-md border transition-colors"
                            :class="{ 'is-dark': settingsStore.isDark }"
                            :placeholder="field.placeholder || ''"
                            :disabled="message.interaction.resolved"
                            v-model.number="ensureFormData(index)[field.name]" />
                          <textarea
                            v-else-if="field.inputType === 'textarea'"
                            class="interaction-input interaction-textarea w-full px-2.5 py-1.5 text-sm rounded-md border transition-colors resize-y min-h-[64px]"
                            :class="{ 'is-dark': settingsStore.isDark }"
                            :placeholder="field.placeholder || ''"
                            :disabled="message.interaction.resolved"
                            v-model="ensureFormData(index)[field.name]"></textarea>
                          <span v-if="field.message"
                                class="interaction-field-message text-[11px] mt-0.5 block">
                            {{ field.message }}
                          </span>
                        </div>
                        <div class="interaction-actions flex gap-2 pt-2">
                          <button
                            class="interaction-btn interaction-btn-primary flex-1 h-9 rounded-md text-sm font-medium"
                            :disabled="message.interaction.resolved || isLoading"
                            @click="handleFormSubmit(index)">
                            <i class="fas fa-paper-plane mr-1.5"></i>
                            {{ message.interaction.submitText || '提交' }}
                          </button>
                          <button
                            class="interaction-btn interaction-btn-ghost flex-1 h-9 rounded-md text-sm font-medium"
                            :disabled="message.interaction.resolved || isLoading"
                            @click="handleFormCancel(index)">
                            <i class="fas fa-times mr-1.5"></i>
                            取消
                          </button>
                        </div>
                      </div>
                    </template>

                    <!-- 已处理 / 已取消的状态提示 -->
                    <div v-if="message.interaction.resolved"
                         class="interaction-resolved-hint text-xs mt-2 pt-1.5"
                         :class="{
                           'is-cancelled': message.interaction.cancelled,
                           'is-dark': settingsStore.isDark
                         }">
                      <i class="fas"
                         :class="message.interaction.cancelled ? 'fa-ban' : 'fa-check-circle'"></i>
                      {{ message.interaction.cancelled ? '已取消' : '已处理' }}
                    </div>
                  </div>

                  <div class="markdown-body" v-html="renderMessage(message)"></div>
                  <div v-if="message.isLoading"
                    class="typing-dots flex space-x-1 mt-2" :class="{ 'is-dark': settingsStore.isDark }">
                    <div class="typing-dot animate-pulse"></div>
                    <div class="typing-dot animate-pulse delay-100"></div>
                    <div class="typing-dot animate-pulse delay-200"></div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </main>
        <footer class="chat-footer flex-shrink-0" :class="{ 'is-dark': settingsStore.isDark }">
          <div class="chat-input-wrap" :class="{ 'is-dark': settingsStore.isDark }">
            <textarea v-model="userInput" @keydown.enter.exact.prevent="sendMessage"
              @keydown.ctrl.enter.exact.prevent="sendMessage" @keydown.esc.exact="stopResponse"
              placeholder="给假维斯发送消息…（Enter 发送 / Shift+Enter 换行）"
              class="chat-input"
              :class="{ 'is-dark': settingsStore.isDark }"
              rows="1" ref="textarea" @input="adjustTextareaHeight"></textarea>
            <button @click="isLoading ? stopResponse() : sendMessage()" :disabled="!userInput.trim() && !isLoading"
              :class="['chat-send-btn', isLoading ? 'chat-btn-warning' : 'chat-btn-primary']">
              <i :class="isLoading ? 'fas fa-stop' : 'fas fa-paper-plane'"></i>
            </button>
          </div>
        </footer>
      </div>
    </div>

    <!-- 路由弹窗：直接渲染目标页面组件，不离开当前聊天窗口 -->
    <el-dialog
      v-model="routeDialogVisible"
      :title="routeDialogTitle"
      width="90%"
      top="5vh"
      destroy-on-close
      class="route-dialog">
      <div style="height: 75vh; overflow: auto;">
        <component :is="routeDialogComponent" v-if="routeDialogComponent" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, defineAsyncComponent, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {sendChatMessage, replayToolCall} from '@/api/ai/aichat/chat';
import {
  create as createConversationApi,
  deleteByConversationId,
  getConversationListByUserId as fetchConversationListApi
} from '@/api/ai/aichat/conversation';
import {getChatMemoryListByConversationId} from '@/api/ai/aichat/history';
import '@/assets/styles/all.scss';
import '@/assets/styles/tailwind.scss';
import useSettingsStore from '@/store/modules/settings';
import useTagsViewStore from '@/store/modules/tagsView';
import useUserStore from '@/store/modules/user';
import {md, mdUser} from '@/utils/markdown';
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

// 交互卡片表单数据：key = 消息在 messages 数组中的索引，value = { fieldName: inputValue }
// 使用 reactive 保证模板中 v-model 双向绑定能正确追踪
const interactionFormData = reactive({});

/** 安全获取/初始化指定消息索引下的表单数据。模板里 v-model 应通过此方法间接访问，
 * 避免历史回放时 interactionFormData[index] 缺失导致 "Cannot read properties of undefined" */
const ensureFormData = (idx) => {
  if (!interactionFormData[idx]) {
    interactionFormData[idx] = {};
  }
  return interactionFormData[idx];
};

/** 合并 arguments 与用户表单值，保留原始结构但用用户输入覆盖/补充同名字段 */
const mergeInteractionArguments = (interaction, formValues) => {
  const base = interaction.arguments && typeof interaction.arguments === 'object'
    ? { ...interaction.arguments }
    : {};
  if (formValues && typeof formValues === 'object') {
    for (const k of Object.keys(formValues)) {
      const v = formValues[k];
      if (v !== '' && v !== null && v !== undefined) {
        base[k] = v;
      }
    }
  }
  return base;
};

// 路由弹窗状态
const routeDialogVisible = ref(false);
const routeDialogComponent = shallowRef(null);
const routeDialogTitle = ref('');

// 动态加载视图组件映射（Vite 构建时解析，运行时按需懒加载）
const viewModules = import.meta.glob('./**/*.vue');

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

// HTML 转义工具：\x26 为 & 的十六进制写法，避免实体串被编辑器/工具链二次解码
const escapeHtml = (str) => String(str)
  .replace(/&/g, '\x26amp;')
  .replace(/</g, '\x26lt;')
  .replace(/>/g, '\x26gt;')
  .replace(/"/g, '\x26quot;')
  .replace(/'/g, '\x26#039;');

const renderMessage = (message) => {
  if (message.role === 'user') {
    // 用户消息同样走 Markdown 渲染（mdUser 实例 html:false，原生 HTML 会被自动转义，防 XSS）
    try {
      let content = String(message.content ?? '');

      // 修复未闭合代码块，避免流式/复制粘贴场景下渲染错乱
      const fenceCount = (content.match(/```/g) || []).length;
      if (fenceCount % 2 !== 0) {
        content += '\n```';
      }

      let html = mdUser.render(content);

      // 用户气泡（粉色）内的表格也包一层横向滚动容器，防止撑爆气泡
      html = html.replace(/<table[^>]*>[\s\S]*?<\/table>/g, (match) => {
        const styled = match.replace(/<t([hd])([^>]*)>/g, '<t$1$2 style="max-width: 260px; word-break: break-word; overflow-wrap: anywhere;">');
        return `<div style="overflow-x: auto; max-width: 100%; -webkit-overflow-scrolling: touch;">${styled}</div>`;
      });

      return html;
    } catch (error) {
      console.error('用户消息 Markdown 渲染异常:', error);
      // 渲染异常时回退为纯文本转义显示
      return escapeHtml(String(message.content ?? ''));
    }
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
                <i class="fas fa-external-link-alt mr-1"></i>点击查看
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
      openRouteDialog(url.trim());
    }
  }
};

/**
 * 打开路由弹窗：根据组件路径动态加载对应 Vue 组件，直接在弹窗内渲染
 */
const openRouteDialog = (componentPath) => {
  // 去掉前缀斜杠，统一为相对路径格式（如 system/user/index）
  const normalizedPath = componentPath.replace(/^\//, '');

  // 尝试多种路径格式匹配视图文件
  const candidates = [
    `./${normalizedPath}.vue`,
    `./${normalizedPath}/index.vue`,
    `./${normalizedPath.replace(/\/index$/, '')}.vue`
  ];

  let loader = null;
  for (const key of candidates) {
    if (viewModules[key]) {
      loader = viewModules[key];
      break;
    }
  }

  if (!loader) {
    // 回退：从路由表中查找标题，再报错
    const allRoutes = router.getRoutes();
    const possiblePath = '/' + normalizedPath.replace(/\/index$/, '');
    const targetRoute = allRoutes.find(r => r.path === possiblePath);
    if (targetRoute) {
      ElMessage.error('页面组件加载失败: ' + normalizedPath);
    } else {
      ElMessage.error('AI 找到了路径，但当前账号可能没有该页面的访问权限');
    }
    return;
  }

  // 从路由表获取页面标题
  const allRoutes = router.getRoutes();
  const possiblePath = '/' + normalizedPath.replace(/\/index$/, '');
  const targetRoute = allRoutes.find(r => r.path === possiblePath);
  routeDialogTitle.value = targetRoute?.meta?.title || 'AI 推荐页面';

  // 动态加载组件并渲染
  routeDialogComponent.value = defineAsyncComponent(loader);
  routeDialogVisible.value = true;
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

/** 用户是否停留在滚动底部（距底 < 80px 视为在底部）。流式输出时仅当用户在底部才自动跟随，用户上翻阅读时不打断 */
const userAtBottom = ref(true);
const handleChatScroll = () => {
  const container = chatContainer.value;
  if (!container) return;
  userAtBottom.value = container.scrollHeight - container.scrollTop - container.clientHeight < 80;
};

let scrollTimeout = null;
const scrollToBottom = (force = false) => {
  // 非强制滚动时，用户不在底部则跳过，避免打断用户阅读历史内容
  if (!force && !userAtBottom.value) return;
  // 强制滚动（发送消息/切换会话）时同步标记为在底部，保证后续流式内容持续跟随
  if (force) userAtBottom.value = true;
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

/** 切换工具调用详情列表展开/收起（默认收起，仅显示调用次数汇总条） */
const toggleToolDetail = (message) => {
  message.toolDetailVisible = !message.toolDetailVisible;
};

const switchConversation = async (id) => {
  if (activeId.value === id) return;
  // 切换前销毁旧 ECharts 实例，避免 DOM 已替换但实例残留导致内存泄漏
  destroyAllEchartsInstances();
  // 清理交互卡片表单缓存（旧会话的消息索引不匹配新会话）
  Object.keys(interactionFormData).forEach(k => delete interactionFormData[k]);
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

          let interaction = null;

          // 从历史记录还原 toolCalls、ragRetrieve、interaction（优先独立字段，其次 metadata 内）
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
              // 恢复交互卡片（历史回放时统一标记为 resolved，防止用户点击旧按钮）
              if (parsed.code === 8003 && parsed.data && typeof parsed.data === 'object') {
                const d = parsed.data;
                interaction = {
                  type: 'confirm',
                  title: d.title || '请确认',
                  message: d.message || parsed.msg || '',
                  confirmText: d.confirmText || '同意',
                  cancelText: d.cancelText || '取消',
                  tool: d.tool,
                  arguments: d.arguments || {},
                  values: d.values || {},
                  resolved: true,
                  cancelled: false
                };
              }
              if (parsed.code === 8002 && parsed.data && typeof parsed.data === 'object') {
                const d = parsed.data;
                interaction = {
                  type: 'form',
                  title: d.title || '请补充信息',
                  message: d.message || parsed.msg || '',
                  fields: Array.isArray(d.fields) ? d.fields : [],
                  values: d.values || {},
                  tool: d.tool,
                  arguments: d.arguments || {},
                  submitText: d.submitText || '提交',
                  resolved: true,
                  cancelled: false
                };
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
            interaction: interaction,
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
    // 切换会话强制回到底部
    scrollToBottom(true);
    nextTick(() => { if (textarea.value) textarea.value.focus(); });
  }
};

const deleteConversation = async (id) => {
  // 正在回答（流式输出中）时不允许删除会话，避免中断回答后状态错乱
  if (isLoading.value) {
    ElMessage.warning('正在回答中，请等待回答完成后再删除会话');
    return;
  }
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
  // 清理交互卡片表单缓存
  Object.keys(interactionFormData).forEach(k => delete interactionFormData[k]);
  activeId.value = null;
  userInput.value = '';
  const initialMsg = { role: 'assistant', content: welcomeMessageContent, isLoading: false, visibleChars: 0, isStreaming: false, toolCalls: [], ragRetrieve: null };
  messages.value = [initialMsg];
  nextTick(() => {
    if (messages.value[0]) messages.value[0].visibleChars = messages.value[0].content.length;
  });
  // 新会话强制回到底部
  scrollToBottom(true);
  nextTick(() => { if (textarea.value) textarea.value.focus(); });
};

const handleAction = (chunk, messageIndex) => {
  const actionType = chunk.content;
  const actionData = chunk.data;

  switch (actionType) {
    case 'OPEN_MENU':
      if (actionData && typeof actionData === 'string' && actionData.trim()) {
        messages.value[messageIndex].content += '\n\n【导航】正在打开: ' + actionData;
        messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
        messages.value[messageIndex].isLoading = false;
        scrollToBottom();
        setTimeout(() => {
          openRouteDialog(actionData.trim());
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

// ============ 交互卡片（confirm / form）处理 ============

/**
 * 确认按钮回调：把 data.arguments（含 confirmed=true）原样重放 data.tool
 * 或取消则标记为 cancelled
 */
const handleConfirmAction = async (messageIndex, approved) => {
  const msg = messages.value[messageIndex];
  if (!msg || !msg.interaction || msg.interaction.resolved) return;

  if (!approved) {
    // 用户取消：标记已解决，不再重放
    msg.interaction.resolved = true;
    msg.interaction.cancelled = true;
    msg.interaction.message = (msg.interaction.message || '') + '\n\n（用户已取消）';
    scrollToBottom();
    return;
  }

  const interaction = msg.interaction;
  const toolName = interaction.tool;
  // arguments 已在后端返回时自动注入 confirmed=true，直接用
  const args = interaction.arguments || {};

  if (!toolName) {
    ElMessage.error('工具名称缺失，无法重放');
    return;
  }

  await replayInteractionTool(messageIndex, toolName, args);
};

/**
 * 表单提交回调：把用户填写的表单值合并进 interaction.arguments，然后重放
 */
const handleFormSubmit = async (messageIndex) => {
  const msg = messages.value[messageIndex];
  if (!msg || !msg.interaction || msg.interaction.resolved) return;

  const interaction = msg.interaction;
  const formValues = ensureFormData(messageIndex);

  // 必填校验
  for (const field of interaction.fields || []) {
    if (field.required) {
      const v = formValues[field.name];
      if (v === undefined || v === null || String(v).trim() === '') {
        ElMessage.warning(`请填写：${field.label || field.name}`);
        return;
      }
    }
  }

  const mergedArgs = mergeInteractionArguments(interaction, formValues);
  await replayInteractionTool(messageIndex, interaction.tool, mergedArgs);
};

/**
 * 表单取消回调：标记 cancelled
 */
const handleFormCancel = (messageIndex) => {
  const msg = messages.value[messageIndex];
  if (!msg || !msg.interaction || msg.interaction.resolved) return;
  msg.interaction.resolved = true;
  msg.interaction.cancelled = true;
  msg.interaction.message = (msg.interaction.message || '') + '\n\n（用户已取消）';
  scrollToBottom();
};

/**
 * 通用重放方法：调用 replayToolCall → 消费 SSE 流 → 新增 assistant 消息承接后续回复
 * 新消息自动追加到 messages 末尾，由 SSE 事件处理逻辑填充内容
 */
const replayInteractionTool = async (messageIndex, toolName, argsObj) => {
  const sourceMsg = messages.value[messageIndex];
  if (!sourceMsg) return;

  // 标记当前交互卡片已处理完成（防止双击重放）
  sourceMsg.interaction.resolved = true;

  if (isLoading.value) {
    ElMessage.warning('正在处理中，请稍候');
    return;
  }

  let currentConversationId = activeId.value;
  if (!currentConversationId) {
    ElMessage.error('会话 ID 丢失，请刷新页面重试');
    return;
  }

  // 先在当前 assistant 消息正文里追加一行提示，再新起一个 assistant 消息承接 SSE 输出
  sourceMsg.content += `\n\n✅ 已确认，正在执行「${toolName}」...`;
  sourceMsg.isLoading = false;
  sourceMsg.visibleChars = sourceMsg.content.length;

  const assistantMessage = {
    role: 'assistant',
    content: '',
    isLoading: true,
    visibleChars: 0,
    isStreaming: true,
    toolCalls: [],
    ragRetrieve: null
  };
  messages.value.push(assistantMessage);
  scrollToBottom(true);

  isLoading.value = true;
  controller = new AbortController();
  const newMsgIndex = messages.value.length - 1;

  try {
    const response = await replayToolCall(toolName, argsObj, currentConversationId, controller.signal);

    if (!response.ok) {
      let errorMsg = `请求失败 (状态码: ${response.status})`;
      try {
        const errorData = await response.json();
        if (errorData && errorData.msg) errorMsg = errorData.msg;
      } catch (_) { /* 忽略 */ }
      throw new Error(errorMsg);
    }

    // 复用 sendMessage 中同样的 SSE 事件监听逻辑
    const es = createEventStream(response);
    let pendingText = '';
    let flushScheduled = false;
    let contentStarted = false;

    const flushText = () => {
      flushScheduled = false;
      if (!pendingText) return;
      messages.value[newMsgIndex].content += pendingText;
      messages.value[newMsgIndex].visibleChars = messages.value[newMsgIndex].content.length;
      pendingText = '';
    };
    const scheduleFlush = () => {
      if (flushScheduled) return;
      flushScheduled = true;
      if (typeof requestAnimationFrame === 'function') {
        requestAnimationFrame(flushText);
      } else {
        setTimeout(flushText, 16);
      }
    };
    const appendText = (text) => {
      if (!text) return;
      pendingText += text;
      scheduleFlush();
      scrollToBottom();
    };
    const finishPendingTools = () => {
      if (messages.value[newMsgIndex].toolCalls) {
        messages.value[newMsgIndex].toolCalls.forEach(t => {
          if (t.status === 'running') { t.status = 'done'; t.endTime = Date.now(); }
        });
      }
    };
    const finalize = () => {
      flushText();
      if (messages.value[newMsgIndex].content === '') {
        messages.value[newMsgIndex].content = '(无返回内容)';
        messages.value[newMsgIndex].visibleChars = 6;
      }
      finishPendingTools();
      messages.value[newMsgIndex].isLoading = false;
      messages.value[newMsgIndex].isStreaming = false;
      scrollToBottom();
    };

    // 正文事件：与 sendMessage 里的 message 事件处理保持一致
    es.addEventListener('message', (e) => {
      const json = e.json;
      if (!contentStarted) { contentStarted = true; finishPendingTools(); }

      if (json && typeof json.content === 'string') {
        const head = json.content.trimStart().charAt(0);
        const nested = (head === '{' || head === '[') ? parseNestedJson(json.content) : null;
        if (nested && nested.code !== undefined) {
          appendText(nested.msg || '');
          handleStructuredCode(nested, newMsgIndex);
          messages.value[newMsgIndex].isLoading = false;
          scrollToBottom();
        } else {
          appendText(json.content);
        }
        if (json.conversationId) messages.value[newMsgIndex].conversationId = json.conversationId;
        return;
      }
      if (json && typeof json.text === 'string') { appendText(json.text); return; }
      if (json && json.code !== undefined) {
        appendText(json.msg || '');
        handleStructuredCode(json, newMsgIndex);
        messages.value[newMsgIndex].isLoading = false;
        scrollToBottom();
        return;
      }
      if (e.data && !json) appendText(e.data);
    });

    // tool_call / rag_retrieve 事件（复用 sendMessage 的简化版）
    es.addEventListener('tool_call', (e) => {
      const json = e.json;
      const rawText = (json && typeof json.content === 'string') ? json.content : (e.data || '');
      const toolMatch = rawText.match(/\[([^\]]+)\]/);
      let toolName2 = '';
      let desc = '';
      if (toolMatch) {
        toolName2 = toolMatch[1];
        desc = rawText.replace(/^\s*正在为您调用工具[:：]\s*/, '').replace(/[，,]\s*请稍候\s*$/, '').trim();
      } else {
        toolName2 = rawText.trim();
      }
      if (!toolName2) toolName2 = '未知工具';
      if (!messages.value[newMsgIndex].toolCalls) messages.value[newMsgIndex].toolCalls = [];
      const exists = messages.value[newMsgIndex].toolCalls.some(t => t.toolName === toolName2);
      if (!exists) {
        messages.value[newMsgIndex].toolCalls.forEach(t => {
          if (t.status === 'running') { t.status = 'done'; t.endTime = Date.now(); }
        });
        messages.value[newMsgIndex].toolCalls.push({
          toolName: toolName2, description: desc, status: 'running', startTime: Date.now()
        });
      }
      scrollToBottom();
    });

    es.addEventListener('rag_retrieve', (e) => {
      const json = e.json;
      const phase = (json && typeof json.content === 'string') ? json.content : (e.data || '');
      if (phase === 'start' || phase === 'rag_retrieval') {
        messages.value[newMsgIndex].ragRetrieve = { status: 'running', result: 'pending', startTime: Date.now() };
      } else if (phase === 'success' || phase === 'rag_retrieved') {
        messages.value[newMsgIndex].ragRetrieve = messages.value[newMsgIndex].ragRetrieve || { status: 'done', result: 'success', startTime: Date.now(), endTime: Date.now() };
        messages.value[newMsgIndex].ragRetrieve.status = 'done';
        messages.value[newMsgIndex].ragRetrieve.result = 'success';
        messages.value[newMsgIndex].ragRetrieve.endTime = Date.now();
      } else if (phase === 'empty') {
        messages.value[newMsgIndex].ragRetrieve = messages.value[newMsgIndex].ragRetrieve || { status: 'done', result: 'empty', startTime: Date.now(), endTime: Date.now() };
        messages.value[newMsgIndex].ragRetrieve.status = 'done';
        messages.value[newMsgIndex].ragRetrieve.result = 'empty';
        messages.value[newMsgIndex].ragRetrieve.endTime = Date.now();
      }
      scrollToBottom();
    });

    es.addEventListener('message_end', (e) => {
      const json = e.json || {};
      if (json.messageId) messages.value[newMsgIndex].messageId = json.messageId;
      if (json.conversationId) messages.value[newMsgIndex].conversationId = json.conversationId;
      const isTerminal = !!json.messageId || json.metadata !== undefined || !json.state;
      if (isTerminal) { finalize(); es.close(); }
    });
    es.addEventListener('error', (e) => {
      const state = (e.json && e.json.state) || '';
      const errMsg = (e.json && (e.json.msg || e.json.message)) || '服务异常，请稍后重试';
      appendText((messages.value[newMsgIndex].content ? '\n\n' : '') + (state ? `[错误 ${state}] ` : '') + errMsg);
    });
    es.addEventListener('stream_error', (e) => {
      flushText();
      if (e.error && e.error.name === 'AbortError') {
        if (messages.value[newMsgIndex].content === '') {
          messages.value[newMsgIndex].content = '已停止';
          messages.value[newMsgIndex].visibleChars = 4;
        }
      } else {
        messages.value[newMsgIndex].content = (e.error && e.error.message) || '连接中断';
        messages.value[newMsgIndex].visibleChars = messages.value[newMsgIndex].content.length;
      }
    });
    es.addEventListener('complete', finalize);

    await es.start();
    finalize();
  } catch (error) {
    const lastMessage = messages.value[messages.value.length - 1];
    if (error.name === 'AbortError') {
      if (lastMessage.content === '') {
        lastMessage.content = '已停止';
        lastMessage.visibleChars = 4;
      }
    } else {
      console.error('工具重放请求出错:', error);
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

/**
 * 结构化业务码统一处理（code=8001/8002/8003/9999 等）
 * 抽成独立函数避免 sendMessage 和 replayInteractionTool 里重复写判断
 */
const handleStructuredCode = (json, messageIndex) => {
  if (!json || json.code === undefined) return;
  const msgRef = messages.value[messageIndex];

  if (json.code === 8001 && json.data && typeof json.data === 'string' && json.data.trim()) {
    msgRef.routeUrl = json.data;
    setTimeout(() => { openRouteDialog(json.data.trim()); }, 500);
  } else if (json.code === 9999 && json.data && typeof json.data === 'object') {
    msgRef.dataTable = Array.isArray(json.data.result) ? json.data.result : [];
    msgRef.dataRowCount = json.data.rowCount;
    if (json.data.summary) {
      msgRef.content += '\n\n' + json.data.summary;
      msgRef.visibleChars = msgRef.content.length;
    }
  } else if (json.code === 8003 && json.data && typeof json.data === 'object') {
    const d = json.data;
    msgRef.interaction = {
      type: 'confirm',
      title: d.title || '请确认',
      message: d.message || json.msg || '',
      confirmText: d.confirmText || '同意',
      cancelText: d.cancelText || '取消',
      tool: d.tool,
      arguments: d.arguments || {},
      values: d.values || {},
      resolved: false
    };
  } else if (json.code === 8002 && json.data && typeof json.data === 'object') {
    const d = json.data;
    msgRef.interaction = {
      type: 'form',
      title: d.title || '请补充信息',
      message: d.message || json.msg || '',
      fields: Array.isArray(d.fields) ? d.fields : [],
      values: d.values || {},
      tool: d.tool,
      arguments: d.arguments || {},
      submitText: d.submitText || '提交',
      resolved: false
    };
    interactionFormData[messageIndex] = { ...(d.values || {}) };
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
  scrollToBottom(true);

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

    // ---- 正文合批提交 ----
    // 后端按极短分片推送正文（如 "```bash"、"\nE"、":\\ll"、"ama-b"，单条仅 1~10 字符），
    // 若每片都直接写响应式数据，会触发整篇 Markdown 反复重渲染（含代码高亮），
    // 造成明显卡顿与代码块闪烁。此处先写入本地缓冲，按动画帧合并提交，
    // 既保证逐字输出的实时观感，又把渲染频率收敛到每帧最多一次。
    let pendingText = '';
    let flushScheduled = false;

    /** 立即把缓冲中的分片合并进消息正文（供收尾/错误等需要同步读取正文的场景调用） */
    const flushText = () => {
      flushScheduled = false;
      if (!pendingText) return;
      messages.value[messageIndex].content += pendingText;
      messages.value[messageIndex].visibleChars = messages.value[messageIndex].content.length;
      pendingText = '';
    };

    const scheduleFlush = () => {
      if (flushScheduled) return;
      flushScheduled = true;
      if (typeof requestAnimationFrame === 'function') {
        requestAnimationFrame(flushText);
      } else {
        setTimeout(flushText, 16);
      }
    };

    /** 流结束统一收尾（message_end / complete / start 返回后均可能触发，逻辑幂等） */
    const finalize = () => {
      flushText();
      if (messages.value[messageIndex].content === '') {
        messages.value[messageIndex].content = '(无返回内容)';
        messages.value[messageIndex].visibleChars = 6;
      }
      finishPendingTools();
      messages.value[messageIndex].isLoading = false;
      messages.value[messageIndex].isStreaming = false;
      scrollToBottom();
    };

    /** 追加正文（写入缓冲，按帧合并提交）并滚动到底部 */
    const appendText = (text) => {
      if (!text) return;
      pendingText += text;
      scheduleFlush();
      scrollToBottom();
    };

    // 正文内容（新协议）：
    //   event: message      + data: {"event":"message","content":"分片文本","conversationId":"..."}
    //   event: message_end  + data: {"event":"message_end","messageId":"...","conversationId":"..."}
    // 旧协议兼容：data: {"text":"..."} / {"code":...,"msg":...} / 纯文本
    let contentStarted = false;
    es.addEventListener('message', (e) => {
      const json = e.json;
      // 正文开始输出：仅首次分片时把上一轮仍"调用中"的工具/RAG 检索标记完成
      // （原实现每片都遍历一次，高频碎片下属于无效开销）
      if (!contentStarted) {
        contentStarted = true;
        finishPendingTools();
      }
      // 新协议：分片文本在 content 字段（仅追加字符串，忽略 messageId/index 等其他字段）
      if (json && typeof json.content === 'string') {
        // 结构化数据（code 8001/9999）才会以 { 或 [ 开头；
        // 纯文本分片先做首字符判断，避免高频碎片每次都 JSON.parse 抛异常
        const head = json.content.trimStart().charAt(0);
        const nested = (head === '{' || head === '[') ? parseNestedJson(json.content) : null;
        if (nested && nested.code !== undefined) {
          // 结构化协议：展示 msg 文本 + 处理业务码
          appendText(nested.msg || '');
          if (nested.code === 8001 && nested.data && typeof nested.data === 'string' && nested.data.trim()) {
            messages.value[messageIndex].routeUrl = nested.data;
            setTimeout(() => {
              openRouteDialog(nested.data.trim());
            }, 500);
          } else if (nested.code === 9999 && nested.data && typeof nested.data === 'object') {
            messages.value[messageIndex].dataTable = Array.isArray(nested.data.result) ? nested.data.result : [];
            messages.value[messageIndex].dataRowCount = nested.data.rowCount;
            if (nested.data.summary) {
              appendText('\n\n' + nested.data.summary);
            }
          } else if (nested.code === 8003 && nested.data && typeof nested.data === 'object') {
            // 确认卡片（type=confirm）：存结构化数据，模板原生渲染按钮
            const d = nested.data;
            messages.value[messageIndex].interaction = {
              type: 'confirm',
              title: d.title || '请确认',
              message: d.message || nested.msg || '',
              confirmText: d.confirmText || '同意',
              cancelText: d.cancelText || '取消',
              tool: d.tool,
              arguments: d.arguments || {},
              values: d.values || {},
              resolved: false
            };
          } else if (nested.code === 8002 && nested.data && typeof nested.data === 'object') {
            // 表单补全卡片（type=form）：存结构化数据，模板原生渲染表单
            const d = nested.data;
            messages.value[messageIndex].interaction = {
              type: 'form',
              title: d.title || '请补充信息',
              message: d.message || nested.msg || '',
              fields: Array.isArray(d.fields) ? d.fields : [],
              values: d.values || {},
              tool: d.tool,
              arguments: d.arguments || {},
              submitText: d.submitText || '提交',
              resolved: false
            };
            // 初始化该消息索引下的表单数据（用 values 回填已有值）
            interactionFormData[messageIndex] = { ...(d.values || {}) };
          }
          messages.value[messageIndex].isLoading = false;
          scrollToBottom();
        } else {
          // 普通文本分片，直接追加
          appendText(json.content);
        }
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
        // 旧协议 JSON 分片（code 8000/8001/8002/8003/9999/500）
        appendText(json.msg || '');
        if (json.code === 8001 && json.data && typeof json.data === 'string' && json.data.trim()) {
          messages.value[messageIndex].routeUrl = json.data;
          setTimeout(() => {
            openRouteDialog(json.data.trim());
          }, 500);
        } else if (json.code === 9999 && json.data && typeof json.data === 'object') {
          // 数据查询结果：data 为对象，含 result(数组)/summary/generatedSql/rowCount
          messages.value[messageIndex].dataTable = Array.isArray(json.data.result) ? json.data.result : [];
          messages.value[messageIndex].dataRowCount = json.data.rowCount;
          if (json.data.summary) {
            appendText('\n\n' + json.data.summary);
          }
        } else if (json.code === 8003 && json.data && typeof json.data === 'object') {
          // 确认卡片（type=confirm）
          const d = json.data;
          messages.value[messageIndex].interaction = {
            type: 'confirm',
            title: d.title || '请确认',
            message: d.message || json.msg || '',
            confirmText: d.confirmText || '同意',
            cancelText: d.cancelText || '取消',
            tool: d.tool,
            arguments: d.arguments || {},
            values: d.values || {},
            resolved: false
          };
        } else if (json.code === 8002 && json.data && typeof json.data === 'object') {
          // 表单补全卡片（type=form）
          const d = json.data;
          messages.value[messageIndex].interaction = {
            type: 'form',
            title: d.title || '请补充信息',
            message: d.message || json.msg || '',
            fields: Array.isArray(d.fields) ? d.fields : [],
            values: d.values || {},
            tool: d.tool,
            arguments: d.arguments || {},
            submitText: d.submitText || '提交',
            resolved: false
          };
          interactionFormData[messageIndex] = { ...(d.values || {}) };
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
    // message_end 即代表本轮正文已输出完毕，可能不再发送 complete：
    // 立即冲刷缓冲并收尾，避免连接保活导致界面一直停留在"生成中"
    es.addEventListener('message_end', (e) => {
      const json = e.json || {};
      if (json.messageId) {
        messages.value[messageIndex].messageId = json.messageId;
      }
      if (json.conversationId) {
        messages.value[messageIndex].conversationId = json.conversationId;
      }
      // 兼容旧后端把状态载荷（如 {"state":"rag_start"}）也塞进 message_end 的情况：
      // 仅当是真正的结束载荷（带 messageId / metadata，或不带 state）时才收尾
      const isTerminal = !!json.messageId || json.metadata !== undefined || !json.state;
      if (isTerminal) {
        finalize();
        // 正文已结束：主动结束 SSE 读取，避免服务端 keep-alive 让按钮长期停在"生成中"
        es.close();
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
      // 先合批落地已收到的分片，保证下面的"是否有正文"判断准确
      flushText();
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
  justify-content: center;
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
  font-size: 16px;
  font-weight: 600;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.is-dark {
    color: #f3f4f6;
  }
}

/* 消息主区域：亮色白底 / 暗色深底 */
.chat-main {
  background: #ffffff;
  transition: background-color 0.3s ease;

  &.is-dark {
    background: rgba(20, 18, 30, 0.6);
  }
}

/* 消息内容容器：居中、限制阅读宽度（GLM / DeepSeek 阅读宽度） */
.chat-content {
  max-width: 46rem;
  margin: 0 auto;
  padding: 28px 16px 16px;
}

/* 单条消息行 */
.message-row {
  margin-bottom: 28px;
}

/* 用户消息：整体右对齐（气泡 + 头像） */
.message-row-user {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;
}

/* AI 消息：左头像 + 正文 */
.message-row-ai {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

/* 用户头像 */
.chat-user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  flex-shrink: 0;
  object-fit: cover;
}

/* 用户气泡：浅色圆角块、无尾巴、无边框（贴近 DeepSeek：仅背景色） */
.user-bubble {
  max-width: 78%;
  background: rgba(240, 67, 110, 0.08);
  color: #262336;
  padding: 10px 16px;
  border-radius: 14px;
  border-top-right-radius: 4px;
  font-size: 15px;
  line-height: 1.7;
  word-break: break-word;

  &.is-dark {
    background: rgba(240, 67, 110, 0.18);
    color: #f3f4f6;
  }
}

/* AI 正文：纯文本无卡片（贴近 DeepSeek：无边框/无背景/无阴影，直铺页面底色；16px / 1.7） */
.ai-content {
  flex: 1;
  min-width: 0;
  color: #262336;
  font-size: 16px;
  line-height: 1.7;
  word-break: break-word;

  &.is-dark {
    color: #e8e6f0;
  }
}

/* AI 机器人头像 */
.chat-robot-avatar {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 14px;
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

/* 工具调用汇总条：默认仅显示调用次数，点击展开详情 */
.tool-summary {
  background: #efeafc;
  border: 1px solid rgba(139, 92, 246, 0.30);
  color: #8b5cf6;
  transition: background 0.2s ease, border-color 0.2s ease;

  &:hover {
    border-color: rgba(139, 92, 246, 0.55);
    background: #e7e1fb;
  }

  &.is-dark {
    background: rgba(139, 92, 246, 0.10);
    color: #a78bfa;

    &:hover {
      background: rgba(139, 92, 246, 0.18);
      border-color: rgba(167, 139, 250, 0.45);
    }
  }
}

.tool-summary-text {
  color: #8b5cf6;

  &.is-dark {
    color: #a78bfa;
  }
}

/* 调用次数徽章（马卡龙紫饱和底 + 白字） */
.tool-count-badge {
  background: #8b5cf6;
  color: #ffffff;
  line-height: 1;

  &.is-dark {
    background: #7c3aed;
    color: #ffffff;
  }
}

/* 展开/收起箭头图标：展开时旋转 180° */
.tool-toggle-icon {
  color: #8b5cf6;
  font-size: 10px;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), color 0.2s ease;

  &:hover {
    color: #7c3aed;
  }

  &.is-expanded {
    transform: rotate(180deg);
  }

  &.is-dark {
    color: #a78bfa;

    &:hover {
      color: #c4b5fd;
    }
  }
}

/* ===== 交互卡片（马卡龙蓝：#3b82f6 / 浅底 #eef4ff）===== */
.interaction-card {
  background: #eef4ff;
  border: 1px solid rgba(59, 130, 246, 0.30);
  border-radius: 10px;
  padding: 12px 14px;
  color: #2563eb;
  transition: background 0.2s ease, border-color 0.2s ease, opacity 0.25s ease;

  &.is-dark {
    background: rgba(59, 130, 246, 0.10);
    border-color: rgba(96, 165, 250, 0.35);
    color: #60a5fa;
  }

  &.interaction-card-resolved {
    opacity: 0.72;
    pointer-events: none;
  }

  &.interaction-card-cancelled {
    background: #fdf2f8;
    border-color: rgba(240, 67, 110, 0.25);
    color: #f0436e;

    &.is-dark {
      background: rgba(240, 67, 110, 0.10);
      border-color: rgba(240, 67, 110, 0.30);
      color: #fb7185;
    }
  }
}

.interaction-header {
  color: #3b82f6;

  .is-dark & { color: #60a5fa; }
  .interaction-card-cancelled & { color: #be123c; .is-dark & { color: #fb7185; } }
}

.interaction-tool-tag {
  background: rgba(59, 130, 246, 0.15);
  color: #2563eb;
  padding: 2px 6px;

  .interaction-card-cancelled & { background: rgba(240, 67, 110, 0.12); color: #f0436e; }
  .is-dark & { background: rgba(96, 165, 250, 0.20); color: #93c5fd; }
  .is-dark .interaction-card-cancelled & { background: rgba(240, 67, 110, 0.18); color: #fb7185; }
}

.interaction-message {
  color: #4b5563;

  .is-dark & { color: #cbd5e1; }
  .interaction-card-cancelled & { color: #be123c; .is-dark & { color: #fb7185; } }
}

.interaction-actions {
  opacity: 1;
  transition: opacity 0.25s ease;

  &.interaction-actions-disabled {
    opacity: 0.72;
    pointer-events: none;
  }
}

.interaction-btn {
  border: none;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:disabled { opacity: 0.5; cursor: not-allowed; }
  &:not(:disabled):hover { transform: translateY(-1px); }

  &.interaction-btn-primary {
    background: #3b82f6;
    color: #fff;
    box-shadow: 0 2px 6px rgba(59, 130, 246, 0.25);

    &:not(:disabled):hover { background: #2563eb; }
  }

  &.interaction-btn-ghost {
    background: rgba(255, 255, 255, 0.7);
    color: #4b5563;
    border: 1px solid rgba(156, 163, 175, 0.4);

    &:not(:disabled):hover {
      background: rgba(255, 255, 255, 0.95);
      border-color: rgba(107, 114, 128, 0.6);
    }

    .is-dark & {
      background: rgba(55, 65, 81, 0.6);
      color: #cbd5e1;
      border-color: rgba(107, 114, 128, 0.4);

      &:not(:disabled):hover {
        background: rgba(55, 65, 81, 0.85);
        border-color: rgba(148, 163, 184, 0.5);
      }
    }
  }
}

.interaction-field-label { color: #374151; .is-dark & { color: #cbd5e1; } }
.interaction-field-required { color: #f0436e; margin-left: 2px; }
.interaction-field-message { color: #f59e0b; .is-dark & { color: #fbbf24; } }

.interaction-input {
  border: 1px solid rgba(156, 163, 175, 0.4);
  background: rgba(255, 255, 255, 0.9);
  color: #1f2937;
  outline: none;

  &::placeholder { color: #9ca3af; }
  &:focus { border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15); }
  &:disabled { opacity: 0.6; cursor: not-allowed; }

  .is-dark & {
    background: rgba(30, 41, 59, 0.85);
    border-color: rgba(100, 116, 139, 0.5);
    color: #e2e8f0;

    &::placeholder { color: #64748b; }
    &:focus { border-color: #60a5fa; box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.25); }
  }
}

.interaction-textarea { font-family: inherit; }
.interaction-form-disabled { pointer-events: none; opacity: 0.72; }

.interaction-resolved-hint {
  border-top: 1px dashed rgba(59, 130, 246, 0.30);
  color: #2563eb;
  opacity: 0.75;

  &.is-cancelled { border-top-color: rgba(240, 67, 110, 0.30); color: #be123c; .is-dark & { color: #fb7185; } }
  .is-dark & { border-top-color: rgba(96, 165, 250, 0.30); color: #93c5fd; }
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
  padding: 12px 16px 16px;
  backdrop-filter: blur(12px);
  background: rgba(251, 251, 253, 0.85);

  &.is-dark {
    background: rgba(26, 26, 46, 0.7);
    border-top-color: #3a3850;
  }
}

/* 输入容器：圆角卡片 + 聚焦光环（GLM / DeepSeek 输入条） */
.chat-input-wrap {
  max-width: 46rem;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 8px 8px 16px;
  background: #ffffff;
  border: 1px solid #eceaf4;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(124, 116, 160, 0.08);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:focus-within {
    border-color: #f0436e;
    box-shadow: 0 2px 16px rgba(240, 67, 110, 0.18);
  }

  &.is-dark {
    background: rgba(40, 38, 54, 0.9);
    border-color: #3a3850;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.30);

    &:focus-within {
      border-color: #f0436e;
    }
  }
}

/* 发送按钮 */
.chat-send-btn {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  font-size: 15px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  &:not(:disabled):hover {
    transform: translateY(-1px);
  }
}

/* 输入框：无边框，由外层容器统一控制聚焦态 */
.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: #262336;
  font-size: 15px;
  line-height: 1.6;
  resize: none;
  padding: 6px 4px;

  &::placeholder {
    color: #b6b3c2;
  }

  &.is-dark {
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

    /* 回答中禁用：置灰且不可点击 */
    &.conv-item-delete-disabled {
      cursor: not-allowed;
      color: #d4d0de;

      &:hover {
        color: #d4d0de;
      }
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

/* 旧的 .msg-bubble 问答式气泡样式已移除，改为 .message-row / .user-bubble / .ai-content */

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

<!-- 路由弹窗样式（el-dialog teleport 到 body，scoped 不生效，需单独非 scoped 块） -->
<style>
.route-dialog .el-dialog__body {
  padding: 0;
  overflow: hidden;
}
.route-dialog .el-dialog__header {
  border-bottom: 1px solid #eceaf4;
  margin-right: 0;
  padding: 16px 20px;
}
.route-dialog .el-dialog {
  border-radius: 12px;
  overflow: hidden;
}

/* ===== 用户气泡内 Markdown 元素适配 =====
   用户消息走 Markdown 渲染（mdUser），v-html 注入的子元素无 scoped 属性，
   故本块样式需放在非 scoped 样式中才能命中 */
.user-bubble .markdown-body {
  font-size: 15px;
}

/* AI 消息 Markdown 排版（贴近 DeepSeek：16px / 1.7）
   all.scss 的 .markdown-body(15px/1.6) 会覆盖容器继承值，这里按 AI 场景覆盖回来 */
.ai-content .markdown-body {
  font-size: 16px;
  line-height: 1.7;
}

/* 代码块对齐 DeepSeek 官方 token --dsw-font-markdown-code-block（13px / 22px） */
.ai-content .markdown-body pre code {
  font-size: 13px;
  line-height: 22px;
}

.user-bubble .markdown-body p:last-child,
.user-bubble .markdown-body ul:last-child,
.user-bubble .markdown-body ol:last-child,
.user-bubble .markdown-body pre:last-child,
.user-bubble .markdown-body blockquote:last-child {
  margin-bottom: 0;
}

.user-bubble .markdown-body p:first-child,
.user-bubble .markdown-body ul:first-child,
.user-bubble .markdown-body ol:first-child {
  margin-top: 0;
}

.user-bubble .markdown-body a {
  color: #d9305c;
  text-decoration: underline;
}
</style>
