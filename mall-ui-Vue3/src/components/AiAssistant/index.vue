<template>
  <div class="ai-assistant">
    <!-- 悬浮机器人：可自由拖动，点击展开/收起聊天面板 -->
    <button
      ref="bubbleRef"
      class="ai-bubble"
      :class="{ 'is-open': panelVisible }"
      :style="bubbleStyle"
      type="button"
      title="AI 智能客服"
      @pointerdown="onBubbleDown"
      @click="onBubbleClick"
    >
      <span class="ai-bubble__glow" />
      <svg class="ai-bubble__robot" viewBox="0 0 32 32" aria-hidden="true">
        <line x1="16" y1="7" x2="16" y2="10" stroke="#7c8794" stroke-width="1.4" />
        <circle cx="16" cy="5.4" r="2" fill="#34d399" />
        <rect x="5.5" y="10" width="21" height="15.5" rx="6" fill="#3a4048" stroke="#78838f" stroke-width="1.1" />
        <circle cx="11.6" cy="17.4" r="2.3" fill="#3ddc84" />
        <circle cx="20.4" cy="17.4" r="2.3" fill="#3ddc84" />
        <rect x="12.4" y="21.6" width="7.2" height="1.5" rx="0.75" fill="#9aa5b1" />
      </svg>
    </button>

    <!-- 聊天面板 -->
    <transition name="ai-panel">
      <section v-if="panelVisible" ref="panelRef" class="ai-panel" :style="panelStyle">
        <header class="ai-panel__header" @pointerdown="onPanelDragDown">
          <button class="ai-icon-btn" type="button" title="收起" @pointerdown.stop @click="closePanel">
            <el-icon><Close /></el-icon>
          </button>
          <div class="ai-panel__title">
            <svg class="ai-panel__logo" viewBox="0 0 32 32" aria-hidden="true">
              <rect x="5.5" y="10" width="21" height="15.5" rx="6" fill="#3a4048" stroke="#78838f" stroke-width="1.1" />
              <circle cx="11.6" cy="17.4" r="2.3" fill="#3ddc84" />
              <circle cx="20.4" cy="17.4" r="2.3" fill="#3ddc84" />
            </svg>
            <span>AI 智能客服</span>
          </div>
          <button class="ai-icon-btn" type="button" title="最近对话" @pointerdown.stop @click="toggleHistory">
            <el-icon><ChatDotSquare /></el-icon>
          </button>
          <button class="ai-icon-btn" type="button" title="新增会话" @pointerdown.stop @click="newChat">
            <el-icon><Plus /></el-icon>
          </button>
        </header>

        <!-- 最近对话抽屉 -->
        <transition name="ai-drawer">
          <div v-if="historyOpen" class="ai-conv-drawer">
            <div class="ai-conv-head">
              <span>最近对话</span>
              <span class="ai-conv-count">{{ conversations.length }}</span>
            </div>
            <div class="ai-conv-list">
              <div
                v-for="conv in conversations"
                :key="conv.id"
                class="ai-conv-item"
                :class="{ 'is-active': conv.id === activeId }"
                @click="switchConversation(conv.id)"
              >
                <el-icon class="ai-conv-icon"><ChatLineRound /></el-icon>
                <span class="ai-conv-title">{{ conv.title }}</span>
                <button class="ai-conv-del" type="button" title="删除会话" @click.stop="deleteConversation(conv.id)">
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
              <div v-if="!conversations.length" class="ai-conv-empty">暂无对话记录</div>
            </div>
          </div>
        </transition>

        <div ref="listRef" class="ai-panel__body" @scroll="handleScroll">
          <div v-if="!messages.length" class="ai-welcome">
            <svg class="ai-welcome__robot" viewBox="0 0 32 32" aria-hidden="true">
              <line x1="16" y1="7" x2="16" y2="10" stroke="#7c8794" stroke-width="1.4" />
              <circle cx="16" cy="5.4" r="2" fill="#34d399" />
              <rect x="5.5" y="10" width="21" height="15.5" rx="6" fill="#3a4048" stroke="#78838f" stroke-width="1.1" />
              <circle cx="11.6" cy="17.4" r="2.3" fill="#3ddc84" />
              <circle cx="20.4" cy="17.4" r="2.3" fill="#3ddc84" />
              <rect x="12.4" y="21.6" width="7.2" height="1.5" rx="0.75" fill="#9aa5b1" />
            </svg>
            <h3 class="ai-welcome__title">你好，我是 AI 智能客服</h3>
            <p class="ai-welcome__sub">订单、物流、退换货、优惠活动，随时问我就好</p>
            <div class="ai-welcome__chips">
              <button v-for="item in quickQuestions" :key="item" type="button" @click="send(item)">
                {{ item }}
              </button>
              <button v-if="mockMode" type="button" class="is-demo" @click="loadStructuredDemo">能力演示</button>
            </div>
          </div>

          <div v-for="(msg, index) in messages" :key="index" class="ai-msg" :class="`is-${msg.role}`">
            <div v-if="msg.role === 'assistant'" class="ai-msg__avatar">
              <svg viewBox="0 0 32 32" aria-hidden="true">
                <rect x="5.5" y="10" width="21" height="15.5" rx="6" fill="#3a4048" stroke="#78838f" stroke-width="1.1" />
                <circle cx="11.6" cy="17.4" r="2.3" fill="#3ddc84" />
                <circle cx="20.4" cy="17.4" r="2.3" fill="#3ddc84" />
              </svg>
            </div>
            <div class="ai-msg__main">
              <div v-if="msg.role === 'user'" class="ai-msg__user">{{ msg.content }}</div>

              <template v-else>
                <!-- 知识库检索 -->
                <div v-if="msg.ragRetrieve" class="ai-block">
                  <div class="ai-block__title">
                    <el-icon><Document /></el-icon>
                    <span>知识库检索</span>
                    <span
                      class="ai-chip"
                      :class="msg.ragRetrieve.status === 'running' ? 'is-running' : msg.ragRetrieve.result === 'empty' ? 'is-empty' : 'is-success'"
                    >
                      <el-icon v-if="msg.ragRetrieve.status === 'running'" class="is-spin"><Loading /></el-icon>
                      <el-icon v-else-if="msg.ragRetrieve.result === 'empty'"><InfoFilled /></el-icon>
                      <el-icon v-else><Check /></el-icon>
                      {{ msg.ragRetrieve.status === 'running' ? '检索中' : msg.ragRetrieve.result === 'empty' ? '未检索到内容' : '已检索到内容' }}
                    </span>
                  </div>
                </div>

                <!-- 工具调用 -->
                <div v-if="msg.toolCalls && msg.toolCalls.length" class="ai-block">
                  <div class="ai-block__title">
                    <el-icon><MagicStick /></el-icon>
                    <span>工具调用</span>
                  </div>
                  <div class="ai-tool-summary" @click="toggleToolDetail(msg)">
                    <span>已调用工具</span>
                    <span class="ai-tool-count">{{ msg.toolCalls.length }}</span>
                    <span v-if="msg.toolCalls.some(t => t.status === 'running')" class="ai-chip is-running">
                      <el-icon class="is-spin"><Loading /></el-icon>调用中
                    </span>
                    <span v-else class="ai-chip is-success">
                      <el-icon><Check /></el-icon>完成
                    </span>
                    <el-icon class="ai-tool-chevron" :class="{ 'is-expanded': msg.toolDetailVisible }"><CaretDown /></el-icon>
                  </div>
                  <div v-show="msg.toolDetailVisible" class="ai-tool-list">
                    <div v-for="(tc, tcIdx) in msg.toolCalls" :key="tcIdx" class="ai-tool-item">
                      <el-icon class="ai-tool-item__icon"><Connection /></el-icon>
                      <div class="ai-tool-item__main">
                        <div class="ai-tool-item__head">
                          <span class="ai-tool-name">{{ tc.toolName }}</span>
                          <span v-if="tc.status === 'running'" class="ai-chip is-running">
                            <el-icon class="is-spin"><Loading /></el-icon>调用中
                          </span>
                          <span v-else class="ai-chip is-success">
                            <el-icon><Check /></el-icon>完成
                          </span>
                        </div>
                        <div v-if="tc.description" class="ai-tool-desc">{{ tc.description }}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 交互卡片：8003 确认 / 8002 表单补全 -->
                <div
                  v-if="msg.interaction"
                  class="ai-interaction"
                  :class="{ 'is-resolved': msg.interaction.resolved, 'is-cancelled': msg.interaction.cancelled }"
                >
                  <div class="ai-interaction__head">
                    <el-icon><Lock /></el-icon>
                    <span>{{ msg.interaction.title }}</span>
                    <span v-if="operationLabel(msg.interaction.operationType)" class="ai-interaction__op">
                      {{ operationLabel(msg.interaction.operationType) }}
                    </span>
                    <span v-if="msg.interaction.tool" class="ai-interaction__tool">{{ msg.interaction.tool }}</span>
                  </div>
                  <div v-if="msg.interaction.message" class="ai-interaction__message">{{ msg.interaction.message }}</div>

                  <template v-if="msg.interaction.type === 'confirm'">
                    <div class="ai-interaction__actions">
                      <button
                        class="ai-action-btn is-primary"
                        :disabled="msg.interaction.resolved || isSending"
                        @click="handleConfirmAction(index, true)"
                      >{{ msg.interaction.confirmText || '同意' }}</button>
                      <button
                        class="ai-action-btn is-ghost"
                        :disabled="msg.interaction.resolved || isSending"
                        @click="handleConfirmAction(index, false)"
                      >{{ msg.interaction.cancelText || '取消' }}</button>
                    </div>
                  </template>

                  <template v-else-if="msg.interaction.type === 'form'">
                    <div class="ai-interaction__form">
                      <div v-for="field in msg.interaction.fields" :key="field.name" class="ai-field">
                        <label class="ai-field__label">
                          {{ field.label || field.name }}
                          <span v-if="field.required" class="ai-field__required">*</span>
                        </label>
                        <input
                          v-if="field.inputType === 'text' || (!field.inputType && !(field.options && field.options.length))"
                          type="text"
                          class="ai-field__input"
                          :placeholder="field.placeholder || ''"
                          :disabled="msg.interaction.resolved"
                          v-model="ensureFormData(index)[field.name]"
                        />
                        <input
                          v-else-if="field.inputType === 'number'"
                          type="number"
                          class="ai-field__input"
                          :placeholder="field.placeholder || ''"
                          :disabled="msg.interaction.resolved"
                          v-model.number="ensureFormData(index)[field.name]"
                        />
                        <textarea
                          v-else-if="field.inputType === 'textarea'"
                          class="ai-field__input ai-field__textarea"
                          :placeholder="field.placeholder || ''"
                          :disabled="msg.interaction.resolved"
                          v-model="ensureFormData(index)[field.name]"
                        />
                        <select
                          v-else-if="field.inputType === 'select' || (!field.inputType && field.options && field.options.length)"
                          class="ai-field__input"
                          :disabled="msg.interaction.resolved"
                          v-model="ensureFormData(index)[field.name]"
                        >
                          <option value="" disabled>{{ field.placeholder || ('请选择' + (field.label || field.name)) }}</option>
                          <option v-for="option in field.options || []" :key="option.value" :value="option.value">
                            {{ option.label }}
                          </option>
                        </select>
                        <div v-else-if="field.inputType === 'radio'" class="ai-field__radios">
                          <label v-for="option in field.options || []" :key="option.value" class="ai-field__radio">
                            <input
                              type="radio"
                              :name="'ai-interaction-radio-' + index + '-' + field.name"
                              :value="option.value"
                              :disabled="msg.interaction.resolved"
                              v-model="ensureFormData(index)[field.name]"
                            />
                            <span>{{ option.label }}</span>
                          </label>
                        </div>
                        <span v-if="field.message" class="ai-field__hint">{{ field.message }}</span>
                      </div>
                      <div class="ai-interaction__actions">
                        <button
                          class="ai-action-btn is-primary"
                          :disabled="msg.interaction.resolved || isSending"
                          @click="handleFormSubmit(index)"
                        >{{ msg.interaction.submitText || '提交' }}</button>
                        <button
                          class="ai-action-btn is-ghost"
                          :disabled="msg.interaction.resolved || isSending"
                          @click="handleFormCancel(index)"
                        >取消</button>
                      </div>
                    </div>
                  </template>

                  <div v-if="msg.interaction.resolved" class="ai-interaction__resolved">
                    <el-icon>
                      <CircleClose v-if="msg.interaction.cancelled" />
                      <CircleCheck v-else />
                    </el-icon>
                    {{ msg.interaction.cancelled ? '已取消' : '已处理' }}
                  </div>
                </div>

                <!-- 业务结果条 -->
                <div v-if="msg.result" class="ai-result" :class="`is-${msg.result.kind}`">
                  <el-icon>
                    <CircleCheck v-if="msg.result.kind === 'success'" />
                    <CircleClose v-else-if="msg.result.kind === 'error'" />
                    <Link v-else />
                  </el-icon>
                  <span class="ai-result__text">{{ msg.result.msg }}</span>
                  <button
                    v-if="msg.result.kind === 'nav'"
                    class="ai-action-btn is-primary ai-result__btn"
                    @click="openRouteDialog(msg.result.routeUrl)"
                  >打开页面</button>
                </div>

                <div class="ai-msg__text markdown-body" v-html="renderMessage(msg)" />

                <!-- 9999 查询结果表格 -->
                <div
                  v-if="msg.dataTable && msg.dataTable.length"
                  class="ai-msg__text markdown-body"
                  v-html="renderDataTable(msg.dataTable)"
                />

                <!-- 9999 查询结果分页 -->
                <div v-if="msg.dataTable && msg.dataTable.length" class="ai-pager">
                  <span class="ai-pager__info">
                    第 {{ dataCurrentPage(msg) }} 页 · 已显示第 {{ dataShownRange(msg) }} 条<template v-if="msg.dataRowCount != null"> / 共 {{ msg.dataRowCount }} 条</template>
                  </span>
                  <span v-if="msg.moreLoading" class="ai-pager__loading">
                    <el-icon class="is-spin"><Loading /></el-icon>正在加载…
                  </span>
                  <button class="ai-pager__btn" :disabled="isSending || msg.moreLoading || !canPrevPage(msg)" @click="requestPrevPage(index)">
                    上一页
                  </button>
                  <button class="ai-pager__btn is-primary" :disabled="isSending || msg.moreLoading || !hasMoreRows(msg)" @click="requestMoreRows(index, false)">
                    下一页
                  </button>
                  <div class="ai-pager__custom">
                    <span>第</span>
                    <input type="number" min="1" class="ai-pager__input" v-model.number="msg.rangeStart" />
                    <span>–</span>
                    <input type="number" min="1" class="ai-pager__input" v-model.number="msg.rangeEnd" />
                    <span>条</span>
                    <button class="ai-pager__btn" :disabled="isSending || msg.moreLoading" @click="requestMoreRows(index, true)">查询</button>
                  </div>
                  <span class="ai-pager__hint">单次最多 10 条</span>
                </div>

                <div v-if="msg.loading" class="ai-msg__typing">
                  <span /><span /><span />
                  <em v-if="msg.content">正在输入…</em>
                </div>
                <div v-if="msg.error" class="ai-msg__error">{{ msg.error }}</div>
              </template>
            </div>
          </div>
        </div>

        <footer class="ai-panel__footer">
          <div v-if="attachment" class="ai-attachment">
            <span class="ai-attachment__name">{{ attachment.name }}</span>
            <button type="button" class="ai-attachment__remove" title="移除附件" @click="attachment = null">
              <el-icon><Close /></el-icon>
            </button>
          </div>
          <div class="ai-composer" :class="{ 'is-recording': recording }">
            <button class="ai-composer__plus" type="button" title="添加附件" @click="pickFile">
              <el-icon><Plus /></el-icon>
            </button>
            <input ref="fileRef" type="file" class="ai-file-input" @change="onFileChange" />
            <textarea
              ref="inputRef"
              v-model="draft"
              class="ai-composer__input"
              rows="1"
              placeholder="做什么事，直接跟我说"
              @keydown.enter.exact.prevent="send()"
              @input="autoGrow"
            />
            <button
              class="ai-composer__mic"
              :class="{ 'is-active': recording }"
              type="button"
              title="语音输入"
              @click="toggleVoice"
            >
              <el-icon><Microphone /></el-icon>
            </button>
            <button v-if="isSending" class="ai-composer__send is-stop" type="button" title="停止生成" @click="stopSending">
              <span class="ai-stop-square" />
            </button>
            <button
              v-else
              class="ai-composer__send"
              type="button"
              :disabled="!draft.trim()"
              title="发送"
              @click="send()"
            >
              <el-icon><Top /></el-icon>
            </button>
          </div>
          <p class="ai-panel__tip">Enter 发送 · Shift + Enter 换行 · 可拖动标题栏移动面板</p>
        </footer>
      </section>
    </transition>

    <!-- 菜单导航弹窗：直接懒加载目标页面组件 -->
    <el-dialog
      v-model="routeDialogVisible"
      :title="routeDialogTitle"
      width="90%"
      top="5vh"
      append-to-body
      destroy-on-close
    >
      <div class="ai-route-body">
        <component :is="routeDialogComponent" v-if="routeDialogComponent" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="AiAssistant">
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import router from '@/router'
import { createEventStream } from '@/utils/chatStream'
import { sendChatMessage, replayToolCall } from '@/api/ai/aichat/chat'
import {
  create as createConversationApi,
  deleteByConversationId,
  getConversationListByUserId as fetchConversationListApi
} from '@/api/ai/aichat/conversation'
import { getChatMemoryListByConversationId } from '@/api/ai/aichat/history'
import { isMockEnabled } from '@/utils/mock'
import { md } from '@/utils/markdown'

const STORAGE_BUBBLE = 'ai-assistant:bubble-pos'
const BUBBLE_SIZE = 62
const PANEL_GAP = 14
const MAX_PAGE_SIZE = 10

const OPERATION_LABELS = {
  add: '新增',
  update: '修改',
  delete: '删除',
  changeStatus: '状态修改',
  changestatus: '状态修改',
  run: '立即执行',
  auth: '权限分配'
}

const quickQuestions = ['查订单物流', '退换货政策', '优惠券怎么用', '联系人工客服']
const mockMode = isMockEnabled()

const panelVisible = ref(false)
const messages = ref([])
const draft = ref('')
const attachment = ref(null)
const isSending = ref(false)
const recording = ref(false)
const historyOpen = ref(false)

const bubbleRef = ref(null)
const panelRef = ref(null)
const listRef = ref(null)
const inputRef = ref(null)
const fileRef = ref(null)

const bubblePos = ref(readPos(STORAGE_BUBBLE))
const panelPos = ref(null) // 面板实际位置：由悬浮球位置 + 用户拖动偏移推导
const panelOffset = ref({ x: 0, y: 0 }) // 手动拖动面板后相对默认锚点的偏移

// 会话
const conversations = ref([])
const activeId = ref(null)
let conversationsLoaded = false

// 交互卡片表单数据：key = 消息索引
const interactionFormData = reactive({})

// 菜单导航弹窗
const routeDialogVisible = ref(false)
const routeDialogComponent = shallowRef(null)
const routeDialogTitle = ref('')

// 在面板内按路径懒加载 views 下的页面组件
const viewModules = import.meta.glob('../../views/**/*.vue')
const viewLookup = {}
Object.keys(viewModules).forEach((key) => {
  const normalized = key.replace('../../views/', '').replace(/\.vue$/, '')
  viewLookup[normalized] = viewModules[key]
})

let conversationId = ''
let abortController = null
let mockTimer = null
let mockResolve = null
let recognition = null
let pendingDataRange = null

const bubbleStyle = computed(() => {
  if (!bubblePos.value) return {}
  return { left: `${bubblePos.value.x}px`, top: `${bubblePos.value.y}px`, right: 'auto', bottom: 'auto' }
})

const panelStyle = computed(() => {
  if (!panelPos.value) return {}
  return { left: `${panelPos.value.x}px`, top: `${panelPos.value.y}px`, right: 'auto', bottom: 'auto' }
})

// ---------------- 位置与拖拽 ----------------
function readPos(key) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const pos = JSON.parse(raw)
    if (typeof pos.x !== 'number' || typeof pos.y !== 'number') return null
    return pos
  } catch (_) {
    return null
  }
}

function savePos(key, pos) {
  try { localStorage.setItem(key, JSON.stringify(pos)) } catch (_) { /* 忽略存储异常 */ }
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), Math.max(min, max))
}

function clampBubble(pos) {
  return {
    x: clamp(pos.x, 8, window.innerWidth - BUBBLE_SIZE - 8),
    y: clamp(pos.y, 8, window.innerHeight - BUBBLE_SIZE - 8)
  }
}

/** 取悬浮球当前坐标：优先使用已拖动的持久化位置，否则读取实际渲染位置 */
function getBubblePos() {
  if (bubblePos.value) return { x: bubblePos.value.x, y: bubblePos.value.y }
  const rect = bubbleRef.value?.getBoundingClientRect()
  if (!rect) return null
  return { x: rect.left, y: rect.top }
}

/** 以悬浮球为锚点计算面板默认位置：优先放在球的上方并右对齐，空间不足时放到下方，最后统一夹在视口内 */
function calcPanelPos(bubbleX, bubbleY, panelWidth, panelHeight) {
  let x = bubbleX + BUBBLE_SIZE - panelWidth
  let y = bubbleY - panelHeight - PANEL_GAP
  if (y < 8) y = bubbleY + BUBBLE_SIZE + PANEL_GAP
  return {
    x: clamp(x, 8, window.innerWidth - panelWidth - 8),
    y: clamp(y, 8, window.innerHeight - panelHeight - 8)
  }
}

/** 计算两个矩形的重叠面积（无重叠返回 0） */
function overlapArea(ax, ay, aw, ah, bx, by, bw, bh) {
  const w = Math.min(ax + aw, bx + bw) - Math.max(ax, bx)
  const h = Math.min(ay + ah, by + bh) - Math.max(ay, by)
  return w > 0 && h > 0 ? w * h : 0
}

/** 若面板与悬浮球重叠，则朝位移最小的方向把面板挪开，保证不遮挡机器人图标 */
function avoidBubble(x, y, panelWidth, panelHeight, bubbleX, bubbleY) {
  const left = clamp(x, 8, window.innerWidth - panelWidth - 8)
  const top = clamp(y, 8, window.innerHeight - panelHeight - 8)

  const pad = 8
  const bLeft = bubbleX - pad
  const bRight = bubbleX + BUBBLE_SIZE + pad
  const bTop = bubbleY - pad
  const bBottom = bubbleY + BUBBLE_SIZE + pad

  const overlapX = left < bRight && left + panelWidth > bLeft
  const overlapY = top < bBottom && top + panelHeight > bTop
  if (!overlapX || !overlapY) return { x: left, y: top }

  const candidates = [
    { x: left, y: bTop - panelHeight }, // 球的上方
    { x: left, y: bBottom }, // 球的下方
    { x: bLeft - panelWidth, y: top }, // 球的左侧
    { x: bRight, y: top } // 球的右侧
  ]
  let best = null
  let fallback = null
  candidates.forEach((item) => {
    const nx = clamp(item.x, 8, window.innerWidth - panelWidth - 8)
    const ny = clamp(item.y, 8, window.innerHeight - panelHeight - 8)
    const area = overlapArea(nx, ny, panelWidth, panelHeight, bLeft, bTop, bRight - bLeft, bBottom - bTop)
    const distance = Math.abs(nx - left) + Math.abs(ny - top)
    if (area === 0) {
      if (!best || distance < best.distance) best = { x: nx, y: ny, distance }
      return
    }
    // 视口过小时无法完全避开，退化为重叠面积最小的位置
    if (!fallback || area < fallback.area) fallback = { x: nx, y: ny, area }
  })
  if (best) return { x: best.x, y: best.y }
  return fallback ? { x: fallback.x, y: fallback.y } : { x: left, y: top }
}

/** 面板最终位置 = 悬浮球锚点位置 + 用户拖动偏移，再避让悬浮球 */
function computePanelPos(bubbleX, bubbleY, panelWidth, panelHeight) {
  const base = calcPanelPos(bubbleX, bubbleY, panelWidth, panelHeight)
  return avoidBubble(
    base.x + panelOffset.value.x,
    base.y + panelOffset.value.y,
    panelWidth,
    panelHeight,
    bubbleX,
    bubbleY
  )
}

/** 面板跟随悬浮球重新定位（保留拖动偏移，并避免遮挡球体） */
function placePanelNearBubble() {
  const panel = panelRef.value
  const bubble = getBubblePos()
  if (!panel || !bubble) return
  const width = panel.offsetWidth || 460
  const height = panel.offsetHeight || Math.min(700, window.innerHeight - 120)
  panelPos.value = computePanelPos(bubble.x, bubble.y, width, height)
}

let dragState = null
let suppressClickUntil = 0

function onBubbleDown(event) {
  if (event.button !== 0) return
  const rect = bubbleRef.value.getBoundingClientRect()
  startDrag('bubble', { x: rect.left, y: rect.top }, event)
}

/** 按住面板标题栏拖动面板；按钮区域不触发拖动 */
function onPanelDragDown(event) {
  if (event.button !== 0) return
  if (event.target.closest('button, input, textarea, select')) return
  const panel = panelRef.value
  if (!panel) return
  startDrag('panel', panelPos.value || { x: panel.offsetLeft, y: panel.offsetTop }, event)
}

function onBubbleClick() {
  if (Date.now() < suppressClickUntil) return
  togglePanel()
}

function startDrag(kind, origin, event) {
  dragState = {
    kind,
    originX: origin.x,
    originY: origin.y,
    startX: event.clientX,
    startY: event.clientY,
    moved: false,
    target: event.currentTarget
  }
  dragState.target.classList.add('is-dragging')
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

function onPointerMove(event) {
  if (!dragState) return
  const dx = event.clientX - dragState.startX
  const dy = event.clientY - dragState.startY
  if (!dragState.moved && Math.hypot(dx, dy) < 4) return
  dragState.moved = true

  if (dragState.kind === 'bubble') {
    bubblePos.value = clampBubble({ x: dragState.originX + dx, y: dragState.originY + dy })
    if (panelVisible.value) placePanelNearBubble()
    return
  }

  // 拖动面板：实时更新偏移，并保证不遮挡悬浮球
  const panel = panelRef.value
  const bubble = getBubblePos()
  if (!panel || !bubble) return
  const width = panel.offsetWidth
  const height = panel.offsetHeight
  const base = calcPanelPos(bubble.x, bubble.y, width, height)
  const next = avoidBubble(dragState.originX + dx, dragState.originY + dy, width, height, bubble.x, bubble.y)
  panelPos.value = next
  panelOffset.value = { x: next.x - base.x, y: next.y - base.y }
}

function onPointerUp() {
  if (!dragState) return
  const { moved, target } = dragState
  target.classList.remove('is-dragging')

  if (moved) {
    suppressClickUntil = Date.now() + 300
    if (bubblePos.value) savePos(STORAGE_BUBBLE, bubblePos.value)
  }

  dragState = null
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
}

// ---------------- 面板开关 ----------------
function togglePanel() {
  panelVisible.value ? closePanel() : openPanel()
}

async function openPanel() {
  // 先按悬浮球位置预估面板坐标（含拖动偏移），避免先出现在默认位置再跳动的闪动
  const rect = bubbleRef.value?.getBoundingClientRect()
  if (rect) {
    const width = window.innerWidth <= 560 ? window.innerWidth - 24 : 460
    const height = window.innerWidth <= 560
      ? Math.min(Math.round(window.innerHeight * 0.78), window.innerHeight - 100)
      : Math.min(700, window.innerHeight - 120)
    panelPos.value = computePanelPos(rect.left, rect.top, width, height)
  }
  panelVisible.value = true
  await nextTick()
  // 面板渲染后按真实尺寸再次对齐悬浮球
  placePanelNearBubble()
  if (!conversationsLoaded) loadConversations()
  scrollToBottom()
}

function closePanel() {
  panelVisible.value = false
  historyOpen.value = false
  stopVoice()
}

async function toggleHistory() {
  historyOpen.value = !historyOpen.value
  if (historyOpen.value && !conversationsLoaded) await loadConversations()
}

function newChat() {
  stopSending()
  destroyAllEcharts()
  clearInteractionData()
  conversationId = ''
  activeId.value = null
  messages.value = []
  draft.value = ''
  attachment.value = null
  historyOpen.value = false
  if (inputRef.value) inputRef.value.style.height = 'auto'
  nextTick(() => inputRef.value?.focus())
}

function clearInteractionData() {
  Object.keys(interactionFormData).forEach((k) => delete interactionFormData[k])
}

// ---------------- 会话列表 ----------------
async function loadConversations() {
  conversationsLoaded = true
  if (mockMode) return
  try {
    const response = await fetchConversationListApi()
    if (response.data && Array.isArray(response.data)) {
      conversations.value = response.data.map((item) => ({
        id: item.conversationId,
        title: item.title || '未命名对话',
        messages: []
      }))
    }
  } catch (error) {
    console.error('获取对话列表失败:', error)
  }
}

async function switchConversation(id) {
  if (id === activeId.value) {
    historyOpen.value = false
    return
  }
  stopSending()
  destroyAllEcharts()
  clearInteractionData()
  activeId.value = id
  conversationId = id
  historyOpen.value = false

  const conv = conversations.value.find((c) => c.id === id)
  if (conv && conv.messages && conv.messages.length) {
    messages.value = conv.messages
    scrollToBottom(true)
    return
  }

  if (mockMode) {
    messages.value = []
    return
  }

  messages.value = [{ role: 'assistant', content: '正在加载历史记录…', loading: true }]
  try {
    const response = await getChatMemoryListByConversationId(id)
    if (response.data && Array.isArray(response.data)) {
      const historyMessages = response.data.map(mapHistoryMessage)
      messages.value = historyMessages
      if (conv) conv.messages = historyMessages
    } else {
      messages.value = []
    }
  } catch (error) {
    console.error('获取历史记录失败:', error)
    messages.value = []
  }
  scrollToBottom(true)
}

/** 历史消息还原：toolCalls / ragRetrieve / 结构化结果 / 交互卡片 */
function mapHistoryMessage(item) {
  const role = item.type === 'USER' ? 'user' : 'assistant'
  let displayContent = item.content
  let dataTable = null
  let dataRowCount = null
  let dataStart = 1
  let rangeStart = null
  let rangeEnd = null
  let toolCalls = []
  let ragRetrieve = null
  let interaction = null
  let result = null

  if (role === 'assistant') {
    if (Array.isArray(item.toolCalls) && item.toolCalls.length) {
      toolCalls = item.toolCalls.map((tc) => ({
        toolName: tc.toolName || tc.tool_name || tc.name || '未知工具',
        description: tc.description || tc.desc || '',
        status: tc.status || 'done',
        startTime: tc.startTime || tc.start_time
      }))
    } else if (item.metadata && typeof item.metadata === 'object') {
      const metaTc = item.metadata.toolCalls || item.metadata.tool_calls
      if (Array.isArray(metaTc) && metaTc.length) {
        toolCalls = metaTc.map((tc) => ({
          toolName: tc.toolName || tc.tool_name || tc.name || '未知工具',
          description: tc.description || tc.desc || '',
          status: tc.status || 'done',
          startTime: tc.startTime || tc.start_time
        }))
      }
    }
    const rag = item.ragRetrieve || (item.metadata && item.metadata.ragRetrieve)
    if (rag && typeof rag === 'object') {
      ragRetrieve = {
        status: rag.status || 'done',
        result: rag.result || 'success'
      }
    }
  }

  if (role === 'assistant' && typeof displayContent === 'string') {
    const nested = parseNestedJson(displayContent)
    const candidates = nested ? [nested] : []
    if (!nested) {
      try {
        const parsed = JSON.parse(displayContent)
        if (parsed) candidates.push(parsed)
      } catch (_) { /* 非 JSON 内容 */ }
    }
    for (const parsed of candidates) {
      if (parsed.msg) displayContent = parsed.msg
      else if (parsed.content) displayContent = parsed.content

      if (parsed.code === 8001 && typeof parsed.data === 'string' && parsed.data.trim()) {
        result = { kind: 'nav', code: parsed.code, msg: parsed.msg || '已为您打开对应菜单。', routeUrl: parsed.data.trim() }
        displayContent = ''
      }
      if (parsed.code === 9999 && parsed.data && typeof parsed.data === 'object') {
        const rows = Array.isArray(parsed.data.result) ? parsed.data.result : []
        dataTable = rows
        dataRowCount = parsed.data.rowCount
        dataStart = 1
        const nextStart = rows.length + 1
        const exhausted = dataRowCount != null && nextStart > dataRowCount
        rangeStart = exhausted ? null : nextStart
        rangeEnd = exhausted ? null : Math.min(nextStart + MAX_PAGE_SIZE - 1, dataRowCount != null ? Math.max(dataRowCount, nextStart) : nextStart + MAX_PAGE_SIZE - 1)
        if (parsed.data.summary) displayContent = (displayContent || '') + '\n\n' + parsed.data.summary
      }
      if (parsed.code === 200) {
        result = { kind: 'success', code: parsed.code, msg: parsed.msg || '操作成功' }
        displayContent = ''
      }
      if (parsed.code === 500) {
        result = { kind: 'error', code: parsed.code, msg: parsed.msg || '操作失败' }
        displayContent = ''
      }
      if (parsed.code === 8003 && parsed.data && typeof parsed.data === 'object') {
        const d = parsed.data
        interaction = {
          type: 'confirm',
          title: d.title || '请确认',
          message: d.message || parsed.msg || '',
          confirmText: d.confirmText || '同意',
          cancelText: d.cancelText || '取消',
          tool: d.tool,
          arguments: d.arguments || {},
          values: d.values || {},
          operationType: d.operationType || (d.arguments && d.arguments.operationType) || '',
          resolved: true,
          cancelled: false
        }
        displayContent = ''
      }
      if (parsed.code === 8002 && parsed.data && typeof parsed.data === 'object') {
        const d = parsed.data
        interaction = {
          type: 'form',
          title: d.title || '请补充信息',
          message: d.message || parsed.msg || '',
          fields: Array.isArray(d.fields) ? d.fields : [],
          values: d.values || {},
          tool: d.tool,
          arguments: d.arguments || {},
          operationType: d.operationType || (d.values && d.values.operationType) || '',
          submitText: d.submitText || '提交',
          resolved: true,
          cancelled: false
        }
        displayContent = ''
      }
      break
    }
  }

  return {
    role,
    content: displayContent || '',
    dataTable,
    dataRowCount,
    dataStart,
    rangeStart,
    rangeEnd,
    toolCalls,
    ragRetrieve,
    interaction,
    result,
    loading: false,
    toolDetailVisible: false
  }
}

async function deleteConversation(id) {
  if (isSending.value) {
    ElMessage.warning('正在回答中，请等待回答完成后再删除会话')
    return
  }
  try {
    await ElMessageBox.confirm('确定要删除该会话吗？此操作不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const index = conversations.value.findIndex((c) => c.id === id)
    if (index !== -1) {
      conversations.value.splice(index, 1)
      if (activeId.value === id) newChat()
    }
    if (!mockMode) {
      deleteByConversationId(id).catch((error) => console.error('删除对话失败:', error))
    }
  } catch (error) {
    if (error !== 'cancel') console.error('删除对话失败:', error)
  }
}

// ---------------- Markdown 渲染 ----------------
const stripTags = (str) => str.replace(/<[^>]+>/g, '')
const decodeEntities = (str) => str
  .replace(/&amp;/g, '&')
  .replace(/&lt;/g, '<')
  .replace(/&gt;/g, '>')
  .replace(/&quot;/g, '"')
  .replace(/&#039;/g, "'")

function renderMessage(message) {
  try {
    let content = message.content || ''
    content = content.replace(/^data:/gm, '')
    const fences = (content.match(/```/g) || []).length
    if (fences % 2 !== 0) content += '\n```'
    if (message.loading) content = content.replace(/(\*\*|__)$/, '')

    let html = md.render(content)
    html = html.replace(/<pre><code><\/code><\/pre>/g, '')
    html = html.replace(/<table[^>]*>[\s\S]*?<\/table>/g, (match) => `<div class="ai-table-wrap">${match}</div>`)

    if (!message.loading) {
      html = html.replace(/<pre><code(?:\s+class="[^"]*")?>([\s\S]*?)<\/code><\/pre>/g, (match, code) => {
        const text = decodeEntities(stripTags(code))
        try {
          const option = JSON.parse(text)
          if (option && typeof option === 'object' && (option.series || option.xAxis || option.yAxis || option.baseOption)) {
            const encoded = btoa(unescape(encodeURIComponent(JSON.stringify(option))))
            return `<div class="echarts-wrap" data-echarts="${encoded}" style="width:100%;height:300px;"></div>`
          }
        } catch (_) { /* 非 JSON 代码块 */ }
        return match
      })
    }
    return html
  } catch (error) {
    console.error('Markdown 渲染异常:', error)
    return md.render(message.content || '')
  }
}

function renderDataTable(data) {
  if (!data || !data.length) return ''
  const escapeHtml = (str) => String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
  const headers = Object.keys(data[0])
  let html = '<table class="ai-data-table"><thead><tr>'
  headers.forEach((header) => {
    html += `<th>${escapeHtml(header)}</th>`
  })
  html += '</tr></thead><tbody>'
  data.forEach((row) => {
    html += '<tr>'
    headers.forEach((header) => {
      const value = row[header]
      html += `<td>${value !== undefined && value !== null ? escapeHtml(value) : ''}</td>`
    })
    html += '</tr>'
  })
  html += '</tbody></table>'
  return html
}

// ---------------- 发送 ----------------
async function ensureConversation(question) {
  if (conversationId) return conversationId
  const title = question.substring(0, 20) + (question.length > 20 ? '...' : '')

  if (mockMode) {
    conversationId = `local-${Date.now()}`
    conversations.value.unshift({ id: conversationId, title, messages: [] })
    activeId.value = conversationId
    return conversationId
  }

  const res = await createConversationApi(question)
  const data = res?.data
  const id = (data && typeof data === 'object') ? data.conversationId : data
  if (!id) throw new Error('创建会话失败：未返回会话ID')
  conversationId = id
  activeId.value = id
  conversations.value.unshift({ id, title, messages: [] })
  return id
}

async function send(override) {
  const text = (typeof override === 'string' ? override : draft.value).trim()
  if (isSending.value) return
  if (!text && !attachment.value) return

  const content = attachment.value
    ? `${text}${text ? '\n\n' : ''}[附件] ${attachment.value.name}`
    : text

  messages.value.push({ role: 'user', content })
  draft.value = ''
  attachment.value = null
  if (inputRef.value) inputRef.value.style.height = 'auto'

  const index = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    loading: true,
    error: '',
    toolCalls: [],
    ragRetrieve: null,
    interaction: null,
    result: null,
    dataTable: null,
    toolDetailVisible: false
  })
  isSending.value = true
  historyOpen.value = false
  scrollToBottom(true)

  if (mockMode) {
    await ensureConversation(content)
    await mockReply(content, index)
    cacheActiveConversation()
    return
  }

  const controller = new AbortController()
  abortController = controller
  try {
    await ensureConversation(content)
    const response = await sendChatMessage(content, conversationId, controller.signal)
    if (!response.ok) throw new Error(`请求失败（状态码 ${response.status}）`)
    await consumeStream(response, index, controller)
  } catch (error) {
    if (error.name === 'AbortError') return
    const msg = messages.value[index]
    if (msg) {
      msg.error = error.message || '发送失败，请稍后重试'
      msg.loading = false
    }
    isSending.value = false
    abortController = null
  }
  cacheActiveConversation()
}

/**
 * 统一消费 SSE 事件流：正文、思考、工具调用、RAG 检索、结构化结果
 */
async function consumeStream(response, index, controller) {
  const es = createEventStream(response)
  let pendingText = ''
  let flushScheduled = false
  let contentStarted = false
  let finalized = false

  const flushText = () => {
    flushScheduled = false
    if (!pendingText) return
    const msg = messages.value[index]
    if (!msg) { pendingText = ''; return }
    msg.content += pendingText
    pendingText = ''
  }

  const scheduleFlush = () => {
    if (flushScheduled) return
    flushScheduled = true
    if (typeof requestAnimationFrame === 'function') requestAnimationFrame(flushText)
    else setTimeout(flushText, 16)
  }

  const appendText = (text) => {
    if (!text) return
    pendingText += text
    scheduleFlush()
    scrollToBottom()
  }

  const finishPendingTools = () => {
    const msg = messages.value[index]
    if (msg?.toolCalls) {
      msg.toolCalls.forEach((t) => {
        if (t.status === 'running') t.status = 'done'
      })
    }
    if (msg?.ragRetrieve && msg.ragRetrieve.status === 'running') msg.ragRetrieve.status = 'done'
  }

  const finalize = () => {
    if (finalized) return
    finalized = true
    flushText()
    const msg = messages.value[index]
    if (!msg) return
    if (!msg.content && !msg.error && !msg.result && !msg.interaction && !(msg.dataTable && msg.dataTable.length)) {
      msg.content = '(无返回内容)'
    }
    finishPendingTools()
    msg.loading = false
    isSending.value = false
    if (abortController === controller) abortController = null
    scrollToBottom()
  }

  es.addEventListener('message', (e) => {
    const json = e.json
    if (!contentStarted) {
      contentStarted = true
      finishPendingTools()
    }
    if (json && typeof json.content === 'string') {
      const head = json.content.trimStart().charAt(0)
      const nested = (head === '{' || head === '[') ? parseNestedJson(json.content) : null
      if (nested && nested.code !== undefined) {
        const handled = applyStructuredResult(nested, index)
        if (handled.appendMsg) appendText(nested.msg || '')
        if (handled.extraText) appendText(handled.extraText)
        if (messages.value[index]) messages.value[index].loading = false
      } else {
        appendText(json.content)
      }
      if (json.conversationId) conversationId = json.conversationId
      return
    }
    if (json && typeof json.text === 'string') { appendText(json.text); return }
    if (json && json.code !== undefined) {
      const handled = applyStructuredResult(json, index)
      if (handled.appendMsg) appendText(json.msg || '')
      if (handled.extraText) appendText(handled.extraText)
      if (messages.value[index]) messages.value[index].loading = false
      return
    }
    if (e.data && !json) appendText(e.data)
  })

  es.addEventListener('thought', (e) => {
    if (!e.json) return
    const msg = messages.value[index]
    if (!msg) return
    if (!msg.thoughts) msg.thoughts = []
    msg.thoughts.push(e.json)
  })

  es.addEventListener('rag_retrieve', (e) => {
    const json = e.json
    const phase = (json && typeof json.content === 'string') ? json.content : (e.data || '')
    const msg = messages.value[index]
    if (!msg) return
    if (phase === 'start' || phase === 'rag_retrieval') {
      msg.ragRetrieve = { status: 'running', result: 'pending' }
    } else if (phase === 'success' || phase === 'rag_retrieved') {
      msg.ragRetrieve = { status: 'done', result: 'success' }
    } else if (phase === 'empty') {
      msg.ragRetrieve = { status: 'done', result: 'empty' }
    }
    scrollToBottom()
  })

  es.addEventListener('tool_call', (e) => {
    const json = e.json
    const rawText = (json && typeof json.content === 'string') ? json.content : (e.data || '')
    const toolMatch = rawText.match(/\[([^\]]+)\]/)
    let toolName = ''
    let description = ''
    if (toolMatch) {
      toolName = toolMatch[1]
      description = rawText.replace(/^\s*正在为您调用工具[:：]\s*/, '').replace(/[，,]\s*请稍候\s*$/, '').trim()
    } else {
      toolName = rawText.trim()
    }
    if (!toolName) toolName = '未知工具'
    const msg = messages.value[index]
    if (!msg) return
    if (!msg.toolCalls) msg.toolCalls = []
    if (!msg.toolCalls.some((t) => t.toolName === toolName)) {
      msg.toolCalls.forEach((t) => {
        if (t.status === 'running') t.status = 'done'
      })
      msg.toolCalls.push({ toolName, description, status: 'running', startTime: Date.now() })
    }
    scrollToBottom()
  })

  es.addEventListener('message_end', (e) => {
    const json = e.json || {}
    if (json.conversationId) conversationId = json.conversationId
    const isTerminal = !!json.messageId || json.metadata !== undefined || !json.state
    if (isTerminal) {
      finalize()
      es.close()
    }
  })

  es.addEventListener('error', (e) => {
    const state = (e.json && e.json.state) || ''
    const errMsg = (e.json && (e.json.msg || e.json.message)) || '服务异常，请稍后重试'
    const msg = messages.value[index]
    if (msg) msg.error = (msg.error ? msg.error + '\n' : '') + (state ? `[错误 ${state}] ` : '') + errMsg
    scrollToBottom()
  })

  es.addEventListener('stream_error', (e) => {
    flushText()
    const msg = messages.value[index]
    if (!msg) return
    if (e.error && e.error.name === 'AbortError') {
      if (!msg.content) msg.content = '已停止'
    } else {
      msg.error = (e.error && e.error.message) || '连接中断'
    }
  })

  es.addEventListener('complete', finalize)

  await es.start()
  finalize()
}

/** 离线演示模式：本地模拟逐字流式回复 */
function mockReply(question, index) {
  return new Promise((resolve) => {
    const reply = pickMockReply(question)
    let cursor = 0
    mockResolve = resolve
    mockTimer = setInterval(() => {
      const step = reply.slice(cursor, cursor + 2)
      cursor += 2
      const msg = messages.value[index]
      if (!msg) {
        clearInterval(mockTimer)
        mockTimer = null
        mockResolve = null
        resolve()
        return
      }
      msg.content += step
      scrollToBottom()
      if (cursor >= reply.length) {
        clearInterval(mockTimer)
        mockTimer = null
        mockResolve = null
        msg.loading = false
        isSending.value = false
        resolve()
      }
    }, 24)
  })
}

function pickMockReply(question) {
  if (/订单|物流|快递|发货/.test(question)) {
    return '您好，订单发货后可在【订单管理 → 订单列表】查看物流单号与实时轨迹。\n\n- 已发货：物流信息通常在 24 小时内同步\n- 未发货：可在订单详情中申请催发货\n\n如需我帮您查询具体订单，请提供订单编号（离线演示模式下为模拟回复）。'
  }
  if (/退换|退货|换货|退款/.test(question)) {
    return '退换货规则如下：\n\n1. 商品签收后 **7 天**内支持无理由退货；\n2. 商品需保持吊牌完好、不影响二次销售；\n3. 退款将在审核通过后 **1-3 个工作日**原路退回。\n\n可在【售后管理】中提交换货/退货申请，我会持续跟进处理进度。'
  }
  if (/优惠券|活动|折扣|满减/.test(question)) {
    return '优惠券可在【营销中心 → 优惠券】查看已领取与可用状态。\n\n- 每笔订单默认使用优惠力度最大的券\n- 优惠券不可叠加使用，过期后自动失效\n\n需要我帮您推荐当前最划算的组合吗？'
  }
  if (/人工|客服|电话/.test(question)) {
    return '已为您转接人工客服通道。\n\n- 服务时间：每日 9:00 - 22:00\n- 也可在【帮助中心】提交工单，平均 30 分钟内响应\n\n（离线演示模式：此处为模拟回复，接入后端后将由真实客服系统处理）'
  }
  return `已收到您的问题：**${question}**\n\n我可以在以下方面协助您：订单与物流查询、退换货政策、优惠券使用、账号与支付问题。\n\n> 当前为离线演示模式（\`src/settings.js\` 中 \`useBackend: false\`），回复为本地模拟内容。`
}

// ---------------- 结构化结果 ----------------
function parseNestedJson(buffer) {
  let parsed = null
  try { parsed = JSON.parse(buffer) } catch (_) { return null }
  if (parsed && typeof parsed === 'object' && parsed.msg !== undefined && parsed.code !== undefined) return parsed
  if (Array.isArray(parsed) && parsed.length && parsed[0] && parsed[0].text) {
    try {
      const textParsed = JSON.parse(parsed[0].text)
      if (textParsed && typeof textParsed === 'object') return textParsed
    } catch (_) { return null }
  }
  return null
}

function lastToolNameOf(msgRef) {
  const calls = (msgRef && msgRef.toolCalls) || []
  const last = calls[calls.length - 1]
  return (last && last.toolName) || ''
}

function applyStructuredResult(json, messageIndex, autoOpenRoute = true) {
  const msgRef = messages.value[messageIndex]
  if (!msgRef || !json || json.code === undefined) return { appendMsg: true, extraText: '' }

  const code = json.code
  const data = json.data

  if (code === 8002 && data && typeof data === 'object') {
    msgRef.interaction = {
      type: 'form',
      title: data.title || '请补充信息',
      message: data.message || json.msg || '',
      fields: Array.isArray(data.fields) ? data.fields : [],
      values: data.values || {},
      tool: data.tool || lastToolNameOf(msgRef),
      arguments: data.arguments || {},
      operationType: data.operationType || (data.values && data.values.operationType) || '',
      submitText: data.submitText || '提交',
      resolved: false,
      cancelled: false
    }
    delete interactionFormData[messageIndex]
    return { appendMsg: false, extraText: '' }
  }

  if (code === 8003 && data && typeof data === 'object') {
    msgRef.interaction = {
      type: 'confirm',
      title: data.title || '请确认',
      message: data.message || json.msg || '',
      confirmText: data.confirmText || '同意',
      cancelText: data.cancelText || '取消',
      tool: data.tool || lastToolNameOf(msgRef),
      arguments: data.arguments || {},
      values: data.values || {},
      operationType: data.operationType || (data.arguments && data.arguments.operationType) || '',
      resolved: false,
      cancelled: false
    }
    return { appendMsg: false, extraText: '' }
  }

  if (code === 8001 && typeof data === 'string' && data.trim()) {
    msgRef.result = { kind: 'nav', code, msg: json.msg || '已为您打开对应菜单。', routeUrl: data.trim() }
    if (autoOpenRoute) setTimeout(() => openRouteDialog(data.trim()), 500)
    return { appendMsg: false, extraText: '' }
  }

  if (code === 9999 && data && typeof data === 'object') {
    const start = pendingDataRange ? pendingDataRange.start : 1
    pendingDataRange = null
    applyQueryRows(msgRef, data, start)
    return { appendMsg: false, extraText: data.summary ? '\n\n' + data.summary : '' }
  }

  if (code === 200) {
    msgRef.result = { kind: 'success', code, msg: json.msg || '操作成功' }
    return { appendMsg: false, extraText: '' }
  }

  if (code === 500) {
    msgRef.result = { kind: 'error', code, msg: json.msg || '操作失败' }
    return { appendMsg: false, extraText: '' }
  }

  return { appendMsg: true, extraText: '' }
}

// ---------------- 数据分页 ----------------
function applyQueryRows(msgRef, data, start) {
  msgRef.dataTable = Array.isArray(data.result) ? data.result : []
  msgRef.dataRowCount = data.rowCount
  msgRef.dataStart = start
  const rows = msgRef.dataTable.length
  const total = data.rowCount
  const nextStart = start + rows
  const exhausted = total != null && nextStart > total
  msgRef.rangeStart = exhausted ? null : nextStart
  msgRef.rangeEnd = exhausted
    ? null
    : Math.min(nextStart + MAX_PAGE_SIZE - 1, total != null ? Math.max(total, nextStart) : nextStart + MAX_PAGE_SIZE - 1)
}

const dataShownRange = (message) => {
  const rows = message && message.dataTable ? message.dataTable.length : 0
  if (!rows) return ''
  const start = message.dataStart || 1
  return `${start}–${start + rows - 1}`
}

const hasMoreRows = (message) => {
  const rows = message && message.dataTable ? message.dataTable.length : 0
  if (!rows) return false
  const start = message.dataStart || 1
  if (message.dataRowCount == null) return rows >= MAX_PAGE_SIZE
  return start + rows - 1 < message.dataRowCount
}

const canPrevPage = (message) => {
  const rows = message && message.dataTable ? message.dataTable.length : 0
  return !!rows && (message.dataStart || 1) > 1
}

const dataCurrentPage = (message) => Math.floor(((message.dataStart || 1) - 1) / MAX_PAGE_SIZE) + 1

async function loadDataPage(messageIndex, start, end) {
  const msg = messages.value[messageIndex]
  if (!msg || isSending.value || msg.moreLoading) return

  if (mockMode) {
    ElMessage.info('离线演示模式：未连接后端，无法翻页')
    return
  }
  if (!conversationId) {
    ElMessage.warning('当前没有进行中的会话，请先发起一次查询')
    return
  }

  msg.moreLoading = true
  isSending.value = true
  let receivedData = null
  let replyText = ''

  try {
    const response = await sendChatMessage(
      `请继续查询第 ${start} 至 ${end} 条数据（单次最多 ${MAX_PAGE_SIZE} 条，返回字段与上次保持一致）。`,
      conversationId,
      new AbortController().signal
    )
    if (!response.ok) throw new Error(`请求失败 (状态码: ${response.status})`)

    const es = createEventStream(response)
    es.addEventListener('message', (e) => {
      const json = e.json
      if (!json) return
      let payload = json
      if (typeof json.content === 'string') {
        const text = json.content.trimStart()
        if (text.charAt(0) === '{' || text.charAt(0) === '[') payload = parseNestedJson(json.content) || json
      }
      if (payload && payload.code === 9999 && payload.data) receivedData = payload.data
      else if (typeof json.content === 'string' && !(payload && payload.code !== undefined)) replyText += json.content
    })
    await es.start()
  } catch (error) {
    console.error('翻页查询失败:', error)
  } finally {
    if (receivedData) applyQueryRows(msg, receivedData, start)
    else if (replyText.trim()) ElMessage.info(replyText.trim().slice(0, 200))
    else ElMessage.warning('未获取到数据，请稍后重试')
    msg.moreLoading = false
    isSending.value = false
    pendingDataRange = null
  }
}

function requestMoreRows(messageIndex, custom) {
  const msg = messages.value[messageIndex]
  if (!msg || !msg.dataTable || !msg.dataTable.length || isSending.value) return

  const loaded = msg.dataTable.length
  const total = msg.dataRowCount
  const start = custom ? Math.floor(Number(msg.rangeStart)) : (msg.dataStart || 1) + loaded
  let end = custom ? Math.floor(Number(msg.rangeEnd)) : start + MAX_PAGE_SIZE - 1

  if (!Number.isFinite(start) || !Number.isFinite(end) || start < 1) {
    ElMessage.warning('请输入正确的起止条数')
    return
  }
  if (end < start) {
    ElMessage.warning('结束条数不能小于起始条数')
    return
  }
  if (end - start + 1 > MAX_PAGE_SIZE) {
    ElMessage.warning(`单次最多查询 ${MAX_PAGE_SIZE} 条，请缩小查询范围`)
    return
  }
  if (total != null && start > total) {
    ElMessage.warning(`起始条数已超出总数（共 ${total} 条）`)
    return
  }
  if (total != null && end > total) end = total
  loadDataPage(messageIndex, start, end)
}

function requestPrevPage(messageIndex) {
  const msg = messages.value[messageIndex]
  if (!msg || !msg.dataTable || !msg.dataTable.length || isSending.value) return
  const currentStart = msg.dataStart || 1
  if (currentStart <= 1) return
  const end = currentStart - 1
  const start = Math.max(1, end - MAX_PAGE_SIZE + 1)
  loadDataPage(messageIndex, start, end)
}

// ---------------- 交互卡片 ----------------
function ensureFormData(idx) {
  if (!interactionFormData[idx]) {
    const msg = messages.value[idx]
    const values = (msg && msg.interaction && msg.interaction.values) || {}
    interactionFormData[idx] = { ...values }
  }
  return interactionFormData[idx]
}

function mergeInteractionArguments(interaction, formValues) {
  const base = interaction.arguments && typeof interaction.arguments === 'object' ? { ...interaction.arguments } : {}
  if (formValues && typeof formValues === 'object') {
    Object.keys(formValues).forEach((k) => {
      const v = formValues[k]
      if (v !== '' && v !== null && v !== undefined) base[k] = v
    })
  }
  return base
}

function operationLabel(operationType) {
  return OPERATION_LABELS[operationType] || ''
}

async function handleConfirmAction(messageIndex, approved) {
  const msg = messages.value[messageIndex]
  if (!msg || !msg.interaction || msg.interaction.resolved) return

  if (!approved) {
    msg.interaction.resolved = true
    msg.interaction.cancelled = true
    msg.interaction.message = (msg.interaction.message || '') + '\n\n（用户已取消）'
    notifyAiOfCancellation(msg.interaction)
    return
  }
  const interaction = msg.interaction
  if (!interaction.tool) {
    ElMessage.error('工具名称缺失，无法重放')
    return
  }
  await replayInteractionTool(messageIndex, interaction.tool, interaction.arguments || {})
}

async function handleFormSubmit(messageIndex) {
  const msg = messages.value[messageIndex]
  if (!msg || !msg.interaction || msg.interaction.resolved) return

  const interaction = msg.interaction
  const formValues = ensureFormData(messageIndex)
  for (const field of interaction.fields || []) {
    if (field.required) {
      const v = formValues[field.name]
      if (v === undefined || v === null || String(v).trim() === '') {
        ElMessage.warning(`请填写：${field.label || field.name}`)
        return
      }
    }
  }
  const mergedArgs = mergeInteractionArguments(interaction, formValues)
  await replayInteractionTool(messageIndex, interaction.tool, mergedArgs)
}

function handleFormCancel(messageIndex) {
  const msg = messages.value[messageIndex]
  if (!msg || !msg.interaction || msg.interaction.resolved) return
  msg.interaction.resolved = true
  msg.interaction.cancelled = true
  msg.interaction.message = (msg.interaction.message || '') + '\n\n（用户已取消）'
  notifyAiOfCancellation(msg.interaction)
}

async function notifyAiOfCancellation(interaction) {
  if (mockMode || !conversationId) return

  const actionText = operationLabel(interaction.operationType) || interaction.title || '该操作'
  const toolText = interaction.tool || '未知工具'
  const question = `我取消了「${actionText}」操作（工具：${toolText}），本次没有做任何修改，请确认取消并给出后续建议。`

  messages.value.push({ role: 'assistant', content: '', loading: true, error: '', toolCalls: [] })
  const index = messages.value.length - 1
  isSending.value = true
  scrollToBottom(true)

  const controller = new AbortController()
  abortController = controller
  try {
    const response = await sendChatMessage(question, conversationId, controller.signal)
    if (!response.ok) throw new Error(`请求失败 (状态码: ${response.status})`)
    await consumeStream(response, index, controller)
  } catch (error) {
    const msg = messages.value[index]
    if (msg) {
      msg.loading = false
      msg.error = error.message || '取消回传失败'
      if (!msg.content && !msg.error) msg.content = '已取消本次操作。'
    }
    isSending.value = false
  }
  cacheActiveConversation()
}

async function replayInteractionTool(messageIndex, toolName, argsObj) {
  const sourceMsg = messages.value[messageIndex]
  if (!sourceMsg) return

  if (mockMode) {
    ElMessage.warning('离线演示模式：未连接后端，已跳过工具重放')
    return
  }
  sourceMsg.interaction.resolved = true
  if (isSending.value) {
    ElMessage.warning('正在处理中，请稍候')
    return
  }
  if (!conversationId) {
    ElMessage.error('会话 ID 丢失，请刷新页面重试')
    return
  }

  messages.value.push({
    role: 'assistant',
    content: '',
    loading: true,
    error: '',
    toolCalls: [],
    toolDetailVisible: false
  })
  const index = messages.value.length - 1
  isSending.value = true
  scrollToBottom(true)

  const controller = new AbortController()
  abortController = controller
  try {
    const response = await replayToolCall(toolName, argsObj, conversationId, controller.signal)
    if (!response.ok) {
      let errorMsg = `请求失败 (状态码: ${response.status})`
      try {
        const errorData = await response.json()
        if (errorData && errorData.msg) errorMsg = errorData.msg
      } catch (_) { /* 忽略非 JSON 错误体 */ }
      throw new Error(errorMsg)
    }
    await consumeStream(response, index, controller)
  } catch (error) {
    if (error.name === 'AbortError') return
    const msg = messages.value[index]
    if (msg) {
      msg.loading = false
      msg.error = error.message || '工具重放失败'
    }
    isSending.value = false
  }
  cacheActiveConversation()
}

function toggleToolDetail(message) {
  message.toolDetailVisible = !message.toolDetailVisible
}

// ---------------- 菜单导航弹窗 ----------------
function openRouteDialog(componentPath) {
  if (!componentPath || typeof componentPath !== 'string') return
  const normalizedPath = componentPath.replace(/^\//, '')
  const loader = viewLookup[normalizedPath] || viewLookup[`${normalizedPath}/index`]

  if (!loader) {
    const allRoutes = router.getRoutes()
    const possiblePath = '/' + normalizedPath.replace(/\/index$/, '')
    const targetRoute = allRoutes.find((r) => r.path === possiblePath)
    if (targetRoute) ElMessage.error('页面组件加载失败: ' + normalizedPath)
    else ElMessage.error('该页面可能不在当前账号的访问权限内')
    return
  }

  const allRoutes = router.getRoutes()
  const possiblePath = '/' + normalizedPath.replace(/\/index$/, '')
  const targetRoute = allRoutes.find((r) => r.path === possiblePath)
  routeDialogTitle.value = targetRoute?.meta?.title || 'AI 推荐页面'

  routeDialogComponent.value = null
  routeDialogComponent.value = defineAsyncComponent(loader)
  routeDialogVisible.value = true
}

// ---------------- 离线能力演示 ----------------
function loadStructuredDemo() {
  stopSending()
  clearInteractionData()
  messages.value = []

  const push = (msg) => {
    messages.value.push({ role: 'assistant', content: '', loading: false, error: '', toolCalls: [], toolDetailVisible: false, ...msg })
  }

  push({
    content: '下面依次展示知识库检索、工具调用、用户确认、表单补全、结果条与数据表格的渲染效果。',
    ragRetrieve: { status: 'done', result: 'success' },
    toolCalls: [
      { toolName: 'orderQueryTool', description: '查询订单列表（离线演示）', status: 'done' },
      { toolName: 'logisticsTool', description: '查询物流轨迹（离线演示）', status: 'done' }
    ]
  })

  push({
    interaction: {
      type: 'confirm',
      title: '确认删除订单？',
      message: '即将执行删除操作（code=8003 确认卡片），请在真实环境中确认后重放工具。',
      confirmText: '同意',
      cancelText: '取消',
      tool: 'orderDeleteTool',
      arguments: { orderId: 'SO20261103001', confirmed: true },
      operationType: 'delete',
      resolved: false,
      cancelled: false
    }
  })

  push({
    interaction: {
      type: 'form',
      title: '补充订单信息',
      message: '请补充以下字段（code=8002 表单补全卡片）。',
      fields: [
        { name: 'receiver', label: '收货人', required: true, inputType: 'text', placeholder: '请输入收货人' },
        { name: 'channel', label: '下单渠道', inputType: 'select', options: [{ label: '线上', value: 'online' }, { label: '线下', value: 'offline' }] },
        { name: 'remark', label: '备注', inputType: 'textarea', placeholder: '选填' }
      ],
      values: { receiver: '张三' },
      tool: 'orderCreateTool',
      arguments: {},
      operationType: 'add',
      submitText: '提交',
      resolved: false,
      cancelled: false
    }
  })

  push({
    content: '查询结果如下：',
    result: { kind: 'success', code: 200, msg: '操作成功（离线演示）' },
    dataTable: [
      { 订单号: 'SO20261103001', 商品: '智能音箱', 金额: '¥299.00', 状态: '已发货' },
      { 订单号: 'SO20261103002', 商品: '机械键盘', 金额: '¥499.00', 状态: '待发货' },
      { 订单号: 'SO20261103003', 商品: '显示器支架', 金额: '¥159.00', 状态: '已完成' }
    ],
    dataRowCount: 23,
    dataStart: 1,
    rangeStart: 4,
    rangeEnd: 13
  })

  push({
    result: { kind: 'nav', code: 8001, msg: '已为您定位到用户管理页面。', routeUrl: 'system/user/index' }
  })

  scrollToBottom(true)
}

// ---------------- 滚动与缓存 ----------------
let userAtBottom = true
function handleScroll() {
  const el = listRef.value
  if (!el) return
  userAtBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80
}

function scrollToBottom(force = false) {
  if (!force && !userAtBottom) return
  if (force) userAtBottom = true
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function cacheActiveConversation() {
  const conv = conversations.value.find((c) => c.id === conversationId)
  if (conv) conv.messages = JSON.parse(JSON.stringify(messages.value))
}

function stopSending() {
  if (mockTimer) {
    clearInterval(mockTimer)
    mockTimer = null
  }
  if (mockResolve) {
    mockResolve()
    mockResolve = null
  }
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant' && last.loading) {
    if (!last.content) last.content = '(已停止生成)'
    last.loading = false
  }
  isSending.value = false
}

// ---------------- ECharts ----------------
const echartsInstances = new Map()

function initEcharts() {
  const container = listRef.value
  if (!container) return
  if (messages.value.some((m) => m.loading)) return
  container.querySelectorAll('.echarts-wrap[data-echarts]:not([data-echarts-init])').forEach((el) => {
    try {
      const encoded = el.getAttribute('data-echarts')
      const option = JSON.parse(decodeURIComponent(escape(atob(encoded))))
      const instance = echarts.init(el)
      instance.setOption(option)
      echartsInstances.set(el, instance)
      el.setAttribute('data-echarts-init', '1')
    } catch (error) {
      console.error('ECharts 初始化失败:', error)
      el.setAttribute('data-echarts-init', 'error')
    }
  })
}

function destroyAllEcharts() {
  echartsInstances.forEach((instance) => {
    try { instance.dispose() } catch (_) { /* 已销毁时忽略 */ }
  })
  echartsInstances.clear()
}

function handleEchartsResize() {
  echartsInstances.forEach((instance) => {
    try { instance.resize() } catch (_) { /* 忽略 */ }
  })
}

// ---------------- 附件与语音 ----------------
function pickFile() {
  fileRef.value?.click()
}

function onFileChange(event) {
  const file = event.target.files?.[0]
  if (file) attachment.value = { name: file.name }
  event.target.value = ''
}

function toggleVoice() {
  if (recording.value) {
    stopVoice()
    return
  }
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessage.info('当前浏览器不支持语音输入')
    return
  }
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.interimResults = true
  recognition.continuous = false
  recognition.onresult = (event) => {
    draft.value = Array.from(event.results).map((r) => r[0].transcript).join('')
    autoGrow()
  }
  recognition.onerror = () => stopVoice()
  recognition.onend = () => { recording.value = false }
  recording.value = true
  recognition.start()
}

function stopVoice() {
  if (recognition) {
    try { recognition.stop() } catch (_) { /* 已结束时忽略 */ }
    recognition = null
  }
  recording.value = false
}

function autoGrow() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 96)}px`
}

// ---------------- 生命周期 ----------------
function handleResize() {
  if (bubblePos.value) bubblePos.value = clampBubble(bubblePos.value)
  // 视口尺寸变化后，面板重新跟随悬浮球定位
  if (panelVisible.value) placePanelNearBubble()
  handleEchartsResize()
}

function handleKeydown(event) {
  if (event.key === 'Escape' && panelVisible.value) closePanel()
}

watch(messages, () => {
  nextTick(() => initEcharts())
}, { deep: true })

onMounted(() => {
  if (bubblePos.value) bubblePos.value = clampBubble(bubblePos.value)
  window.addEventListener('resize', handleResize)
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  stopSending()
  stopVoice()
  destroyAllEcharts()
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
})
</script>

<style lang="scss" scoped>
.ai-assistant {
  position: fixed;
  inset: 0;
  z-index: 3000;
  pointer-events: none;
}

/* ---------- 悬浮机器人 ---------- */
.ai-bubble {
  position: fixed;
  right: 28px;
  bottom: 96px;
  width: 62px;
  height: 62px;
  z-index: 3001;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: radial-gradient(circle at 50% 30%, #343a42 0%, #20242a 62%, #14171b 100%);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(0, 0, 0, 0.2);
  cursor: grab;
  pointer-events: auto;
  touch-action: none;
  user-select: none;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  animation: ai-float 3.2s ease-in-out infinite;

  &:hover {
    transform: translateY(-2px) scale(1.05);
    box-shadow: 0 14px 32px rgba(0, 0, 0, 0.42), 0 0 18px rgba(61, 220, 132, 0.22);
  }

  &:active,
  &.is-dragging {
    cursor: grabbing;
    animation: none;
    transform: scale(1.02);
  }

  &.is-open {
    animation: none;
    box-shadow: 0 10px 26px rgba(0, 0, 0, 0.35), 0 0 0 2px rgba(61, 220, 132, 0.4);
  }

  &.is-open .ai-bubble__glow {
    opacity: 1;
  }
}

.ai-bubble__robot {
  width: 34px;
  height: 34px;
  position: relative;
  z-index: 2;
  pointer-events: none;
}

.ai-bubble__glow {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(61, 220, 132, 0.22) 0%, rgba(61, 220, 132, 0) 70%);
  opacity: 0.65;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

@keyframes ai-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* ---------- 聊天面板 ---------- */
.ai-panel {
  position: fixed;
  right: 104px;
  bottom: 92px;
  width: 460px;
  height: min(700px, calc(100vh - 120px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 16px;
  border: 1px solid #2f2f2f;
  background: #1a1a1a;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.5);
  pointer-events: auto;
  color: #e6e6e6;
  font-size: 13px;
}

.ai-panel__header {
  position: relative;
  flex: 0 0 auto;
  height: 54px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border-bottom: 1px solid #262626;
  user-select: none;
  cursor: grab;
  touch-action: none;

  &.is-dragging {
    cursor: grabbing;
  }
}

.ai-panel__title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: #f2f2f2;
}

.ai-panel__logo {
  width: 20px;
  height: 20px;
}

.ai-icon-btn {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border-radius: 8px;
  border: 1px solid #333;
  background: #242424;
  color: #a8a8a8;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;

  &:hover {
    background: #2e2e2e;
    color: #f2f2f2;
  }

  :deep(.el-icon) {
    font-size: 15px;
  }
}

/* 最近对话抽屉 */
.ai-conv-drawer {
  position: absolute;
  top: 54px;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  background: #1c1c1c;
  border-top: 1px solid #262626;
}

.ai-conv-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px 8px;
  font-size: 12px;
  color: #8f8f8f;
}

.ai-conv-count {
  padding: 1px 6px;
  border-radius: 999px;
  background: #2c2c2c;
  color: #b9b9b9;
}

.ai-conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 10px 12px;
}

.ai-conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  margin-bottom: 4px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;

  &:hover {
    background: #242424;
  }

  &.is-active {
    background: #1f2a24;
    border-color: #33503f;
  }
}

.ai-conv-icon {
  flex: 0 0 auto;
  color: #7f7f7f;
  font-size: 14px;
}

.ai-conv-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #d0d0d0;
}

.ai-conv-del {
  flex: 0 0 auto;
  display: none;
  padding: 3px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #8a8a8a;
  cursor: pointer;

  &:hover { color: #f87171; background: #2e2222; }
}

.ai-conv-item:hover .ai-conv-del {
  display: inline-flex;
}

.ai-conv-empty {
  padding: 24px 0;
  text-align: center;
  color: #6f6f6f;
  font-size: 12px;
}

.ai-panel__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 14px 8px;
  scrollbar-width: thin;
  scrollbar-color: #3a3a3a transparent;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 3px;
    background: #3a3a3a;
  }
}

/* 欢迎区 */
.ai-welcome {
  padding: 26px 10px 10px;
  text-align: center;
}

.ai-welcome__robot {
  width: 58px;
  height: 58px;
  filter: drop-shadow(0 6px 16px rgba(61, 220, 132, 0.18));
}

.ai-welcome__title {
  margin: 14px 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #f3f3f3;
}

.ai-welcome__sub {
  margin: 0 0 18px;
  font-size: 12px;
  line-height: 1.7;
  color: #8f8f8f;
}

.ai-welcome__chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;

  button {
    padding: 7px 12px;
    border-radius: 999px;
    border: 1px solid #343434;
    background: #242424;
    color: #c9c9c9;
    font-size: 12px;
    cursor: pointer;
    transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;

    &:hover {
      border-color: #3f7a5a;
      background: #1f2a24;
      color: #5be39a;
    }

    &.is-demo {
      border-color: #3f5a7a;
      color: #8fbcff;

      &:hover {
        background: #1c2733;
        border-color: #4d78a8;
        color: #b5d4ff;
      }
    }
  }
}

/* 消息 */
.ai-msg {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;

  &.is-user {
    justify-content: flex-end;
  }
}

.ai-msg__avatar {
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #262b31;
  border: 1px solid #353b43;

  svg {
    width: 20px;
    height: 20px;
  }
}

.ai-msg__main {
  max-width: 88%;
  min-width: 0;
  flex: 1;
}

.ai-msg__user {
  margin-left: auto;
  width: fit-content;
  padding: 9px 12px;
  border-radius: 12px;
  border-bottom-right-radius: 3px;
  background: #2c2c2c;
  color: #ededed;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-msg__text {
  line-height: 1.75;
  color: #dcdcdc;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.ai-msg__typing {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;

  span {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: #3ddc84;
    opacity: 0.4;
    animation: ai-blink 1.2s infinite ease-in-out;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }

  em {
    margin-left: 6px;
    font-style: normal;
    font-size: 12px;
    color: #7d7d7d;
  }
}

@keyframes ai-blink {
  0%, 80%, 100% { opacity: 0.25; }
  40% { opacity: 1; }
}

.ai-msg__error {
  margin-top: 6px;
  padding: 7px 10px;
  border-radius: 8px;
  background: rgba(220, 38, 38, 0.12);
  border: 1px solid rgba(220, 38, 38, 0.3);
  color: #f87171;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 过程区块：知识库 / 工具调用 */
.ai-block {
  margin-bottom: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #2f2f2f;
  background: #1f1f1f;
}

.ai-block__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #a8a8a8;

  :deep(.el-icon) {
    color: #7fb18f;
  }
}

.ai-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 7px;
  border-radius: 999px;
  border: 1px solid #333;
  background: #262626;
  font-size: 11px;
  color: #a8a8a8;

  &.is-success {
    border-color: #2f5a44;
    background: #18251f;
    color: #5be39a;
  }

  &.is-running {
    border-color: #5a4a2f;
    background: #272218;
    color: #f0b95e;
  }

  &.is-empty {
    border-color: #4a3f33;
    background: #251f19;
    color: #d8a865;
  }

  .is-spin {
    animation: ai-spin 1s linear infinite;
  }
}

@keyframes ai-spin {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}

.ai-tool-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  padding: 6px 8px;
  border-radius: 6px;
  background: #242424;
  font-size: 12px;
  color: #b6b6b6;
  cursor: pointer;
  user-select: none;

  &:hover { background: #2a2a2a; }
}

.ai-tool-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #333;
  color: #e5e5e5;
  font-size: 11px;
  font-weight: 600;
}

.ai-tool-chevron {
  margin-left: auto;
  transition: transform 0.18s ease;

  &.is-expanded { transform: rotate(180deg); }
}

.ai-tool-list {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ai-tool-item {
  display: flex;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 6px;
  border: 1px solid #2e2e2e;
  background: #222;
}

.ai-tool-item__icon {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #8a8a8a;
}

.ai-tool-item__main {
  flex: 1;
  min-width: 0;
}

.ai-tool-item__head {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.ai-tool-name {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: #e2e2e2;
}

.ai-tool-desc {
  margin-top: 2px;
  font-size: 11px;
  color: #8c8c8c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 交互卡片 */
.ai-interaction {
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.35);
  background: rgba(59, 130, 246, 0.08);

  &.is-resolved {
    border-color: #333;
    background: #202020;
  }
}

.ai-interaction__head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #dbeafe;

  :deep(.el-icon) { color: #60a5fa; }
}

.ai-interaction__op {
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(59, 130, 246, 0.18);
  color: #93c5fd;
  font-size: 11px;
  font-weight: 400;
}

.ai-interaction__tool {
  margin-left: auto;
  padding: 1px 6px;
  border-radius: 4px;
  background: #262626;
  color: #8f8f8f;
  font-size: 10px;
  font-family: Consolas, Monaco, monospace;
  font-weight: 400;
}

.ai-interaction__message {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: #b5b5b5;
  white-space: pre-wrap;
}

.ai-interaction__actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.ai-action-btn {
  flex: 1;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0 12px;
  border-radius: 7px;
  border: 1px solid transparent;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s ease, opacity 0.15s ease;

  &.is-primary {
    background: #2563eb;
    color: #eff6ff;

    &:hover:not(:disabled) { background: #1d4ed8; }
  }

  &.is-ghost {
    background: #2a2a2a;
    border-color: #3a3a3a;
    color: #cfcfcf;

    &:hover:not(:disabled) { background: #333; }
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.ai-interaction__form {
  margin-top: 8px;
}

.ai-field {
  margin-bottom: 8px;
}

.ai-field__label {
  display: block;
  margin-bottom: 4px;
  font-size: 11px;
  color: #a8a8a8;
}

.ai-field__required {
  color: #f87171;
  margin-left: 2px;
}

.ai-field__input {
  width: 100%;
  height: 32px;
  padding: 0 9px;
  border-radius: 6px;
  border: 1px solid #3a3a3a;
  background: #232323;
  color: #ededed;
  font-size: 12px;
  outline: none;
  font-family: inherit;

  &:focus { border-color: #3b82f6; }
  &:disabled { opacity: 0.6; }
}

.ai-field__textarea {
  height: auto;
  min-height: 58px;
  padding: 6px 9px;
  resize: vertical;
}

.ai-field__radios {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ai-field__radio {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 6px 9px;
  border-radius: 6px;
  border: 1px solid #3a3a3a;
  font-size: 12px;
  color: #cfcfcf;
  cursor: pointer;
}

.ai-field__hint {
  display: block;
  margin-top: 3px;
  font-size: 11px;
  color: #8a8a8a;
}

.ai-interaction__resolved {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  font-size: 11px;
  color: #7d7d7d;

  :deep(.el-icon) { color: #5be39a; }
}

/* 结果条 */
.ai-result {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #333;
  background: #202020;
  font-size: 12px;
  color: #d0d0d0;

  :deep(.el-icon) { flex: 0 0 auto; }

  &.is-success :deep(.el-icon) { color: #4ade80; }
  &.is-error {
    border-color: rgba(220, 38, 38, 0.35);
    background: rgba(220, 38, 38, 0.1);
    color: #fca5a5;

    :deep(.el-icon) { color: #f87171; }
  }
  &.is-nav :deep(.el-icon) { color: #60a5fa; }
}

.ai-result__text {
  flex: 1;
  min-width: 0;
  line-height: 1.6;
}

.ai-result__btn {
  flex: 0 0 auto;
  height: 26px;
  padding: 0 10px;
  font-size: 11px;
}

/* 分页 */
.ai-pager {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #2f2f2f;
  background: #1f1f1f;
  font-size: 11px;
  color: #9a9a9a;
}

.ai-pager__info { margin-right: auto; }

.ai-pager__loading {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #f0b95e;

  .is-spin { animation: ai-spin 1s linear infinite; }
}

.ai-pager__btn {
  height: 24px;
  padding: 0 9px;
  border-radius: 6px;
  border: 1px solid #3a3a3a;
  background: #2a2a2a;
  color: #cfcfcf;
  font-size: 11px;
  cursor: pointer;

  &:hover:not(:disabled) { background: #333; }
  &:disabled { opacity: 0.4; cursor: not-allowed; }

  &.is-primary {
    border-color: #33503f;
    background: #1f2a24;
    color: #5be39a;
  }
}

.ai-pager__custom {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.ai-pager__input {
  width: 44px;
  height: 24px;
  padding: 0 5px;
  border-radius: 5px;
  border: 1px solid #3a3a3a;
  background: #232323;
  color: #ededed;
  font-size: 11px;
  outline: none;
}

.ai-pager__hint { color: #6f6f6f; }

/* 底部输入 */
.ai-panel__footer {
  flex: 0 0 auto;
  padding: 10px 12px 10px;
  border-top: 1px solid #262626;
  background: #1a1a1a;
}

.ai-attachment {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding: 5px 8px;
  border-radius: 8px;
  background: #242424;
  border: 1px solid #333;
  color: #bdbdbd;
  font-size: 12px;
  width: fit-content;
  max-width: 100%;
}

.ai-attachment__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-attachment__remove {
  display: inline-flex;
  padding: 0;
  border: none;
  background: none;
  color: #8a8a8a;
  cursor: pointer;

  &:hover { color: #f87171; }
}

.ai-composer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 14px;
  border: 1px solid #333;
  background: #262626;
  transition: border-color 0.15s ease;

  &:focus-within {
    border-color: #3f7a5a;
  }

  &.is-recording {
    border-color: #3ddc84;
  }
}

.ai-composer__plus,
.ai-composer__mic {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border-radius: 50%;
  border: 1px solid #3a3a3a;
  background: #2e2e2e;
  color: #b0b0b0;
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease;

  &:hover {
    background: #383838;
    color: #f2f2f2;
  }

  :deep(.el-icon) {
    font-size: 15px;
  }
}

.ai-composer__mic {
  border-color: transparent;
  background: transparent;

  &.is-active {
    color: #3ddc84;
    animation: ai-pulse 1.4s infinite;
  }
}

@keyframes ai-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(61, 220, 132, 0.35); }
  50% { box-shadow: 0 0 0 6px rgba(61, 220, 132, 0); }
}

.ai-composer__input {
  flex: 1;
  min-width: 0;
  max-height: 96px;
  padding: 5px 2px;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  color: #ededed;
  font-size: 13px;
  line-height: 1.6;
  font-family: inherit;
  scrollbar-width: thin;

  &::placeholder {
    color: #7a7a7a;
  }
}

.ai-composer__send {
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: #1d7a4c;
  color: #eafff2;
  cursor: pointer;
  transition: background 0.15s ease, opacity 0.15s ease;

  &:hover:not(:disabled) {
    background: #248f5a;
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  :deep(.el-icon) {
    font-size: 16px;
  }

  &.is-stop {
    background: #3a3a3a;
    color: #e0e0e0;

    &:hover {
      background: #4a4a4a;
    }
  }
}

.ai-stop-square {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  background: #e0e0e0;
}

.ai-file-input {
  display: none;
}

.ai-panel__tip {
  margin: 6px 2px 0;
  font-size: 11px;
  color: #666;
  text-align: center;
}

/* 面板出入场动画 */
.ai-panel-enter-active,
.ai-panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.ai-panel-enter-from,
.ai-panel-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.97);
}

.ai-drawer-enter-active,
.ai-drawer-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.ai-drawer-enter-from,
.ai-drawer-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

/* Markdown 内容（v-html 注入节点无 scoped 属性，需使用 :deep） */
.ai-msg__text.markdown-body {
  font-size: 13px;
  line-height: 1.75;
  color: #dcdcdc;
}

.ai-msg__text :deep(p),
.ai-msg__text :deep(ul),
.ai-msg__text :deep(ol),
.ai-msg__text :deep(pre),
.ai-msg__text :deep(blockquote),
.ai-msg__text :deep(table) {
  margin: 0 0 8px;
}

.ai-msg__text :deep(p:last-child),
.ai-msg__text :deep(ul:last-child),
.ai-msg__text :deep(ol:last-child),
.ai-msg__text :deep(pre:last-child) {
  margin-bottom: 0;
}

.ai-msg__text :deep(ul),
.ai-msg__text :deep(ol) {
  padding-left: 18px;
}

.ai-msg__text :deep(li) {
  margin: 3px 0;
}

.ai-msg__text :deep(pre) {
  padding: 9px 10px;
  border-radius: 8px;
  background: #111;
  border: 1px solid #2c2c2c;
  overflow-x: auto;
}

.ai-msg__text :deep(pre code) {
  font-size: 12px;
  line-height: 1.6;
}

.ai-msg__text :deep(:not(pre) > code) {
  padding: 1px 5px;
  border-radius: 4px;
  background: #2c2c2c;
  color: #7ee2a8;
  font-size: 12px;
}

.ai-msg__text :deep(blockquote) {
  padding: 4px 10px;
  border-left: 3px solid #3a3a3a;
  color: #9a9a9a;
  background: #202020;
  border-radius: 4px;
}

.ai-msg__text :deep(a) {
  color: #4ade80;
  text-decoration: none;
}

.ai-msg__text :deep(.ai-table-wrap) {
  overflow-x: auto;
  margin-bottom: 8px;
}

.ai-msg__text :deep(.ai-table-wrap table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.ai-msg__text :deep(.ai-table-wrap th),
.ai-msg__text :deep(.ai-table-wrap td) {
  border: 1px solid #3a3a3a;
  padding: 5px 8px;
  max-width: 240px;
  word-break: break-word;
  overflow-wrap: anywhere;
  text-align: left;
}

.ai-msg__text :deep(.ai-table-wrap th) {
  background: #2a2a2a;
  color: #e5e5e5;
  white-space: nowrap;
}

/* 9999 查询结果表格 */
.ai-msg__text :deep(.ai-data-table) {
  width: auto;
  min-width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  margin-top: 6px;
}

.ai-msg__text :deep(.ai-data-table th),
.ai-msg__text :deep(.ai-data-table td) {
  border: 1px solid #3a3a3a;
  padding: 5px 8px;
  text-align: left;
  max-width: 220px;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.ai-msg__text :deep(.ai-data-table th) {
  background: #2a2a2a;
  color: #e5e5e5;
  white-space: nowrap;
}

.ai-msg__text :deep(.ai-data-table td) {
  color: #cfcfcf;
}

.ai-msg__text :deep(.echarts-wrap) {
  margin: 6px 0;
}

.ai-route-body {
  height: 75vh;
  overflow: auto;
}

/* 响应式 */
@media (max-width: 560px) {
  .ai-bubble {
    right: 16px;
    bottom: 84px;
  }

  .ai-panel {
    right: 12px;
    bottom: 80px;
    width: calc(100vw - 24px);
    height: 78vh;
    max-height: calc(100vh - 100px);
  }

  .ai-panel__tip {
    display: none;
  }
}
</style>
