<template>
  <div class="app-container">
    <div class="doc-container">
      <!-- 背景氛围 -->
      <div class="bg-atmosphere"></div>
      <div class="grid-pattern"></div>
      <div class="floating-particles" id="particles"></div>

      <!-- 主内容区 -->
      <main class="main-content">
        <!-- 页面头部 -->
        <header class="page-header">
          <h1 class="page-title font-display">{{ pageTitle }}</h1>
          <div class="header-actions">
            <div class="search-container">
              <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input v-model="queryParams.fileName" class="search-input" placeholder="搜索文档..." @keyup.enter="handleQuery" />
            </div>
            <div class="view-toggle" role="group" aria-label="视图切换">
              <button class="view-btn" :class="{ active: !isListView }" data-view="grid" aria-label="网格视图" @click="isListView = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="7" height="7"/>
                  <rect x="14" y="3" width="7" height="7"/>
                  <rect x="3" y="14" width="7" height="7"/>
                  <rect x="14" y="14" width="7" height="7"/>
                </svg>
              </button>
              <button class="view-btn" :class="{ active: isListView }" data-view="list" aria-label="列表视图" @click="isListView = true">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="8" y1="6" x2="21" y2="6"/>
                  <line x1="8" y1="12" x2="21" y2="12"/>
                  <line x1="8" y1="18" x2="21" y2="18"/>
                  <line x1="3" y1="6" x2="3.01" y2="6"/>
                  <line x1="3" y1="12" x2="3.01" y2="12"/>
                  <line x1="3" y1="18" x2="3.01" y2="18"/>
                </svg>
              </button>
            </div>
          </div>
        </header>

        <!-- 统计卡片 -->
        <div class="stats-bar">
          <div class="stat-item">
            <div class="stat-icon accent-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
            </div>
            <div>
              <div class="stat-value font-display">{{ total }}</div>
              <div class="stat-label">总文档</div>
            </div>
          </div>
        </div>

        <!-- 上传区域 -->
        <div class="upload-zone" :class="{ dragover: isDragover }" @dragover.prevent="isDragover = true" @dragleave="isDragover = false" @drop.prevent="handleDrop" @click="triggerUpload">
          <input type="file" ref="fileInput" style="display: none" multiple @change="handleFileSelect" />
          <div class="upload-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </div>
          <div class="upload-title">拖拽文件到此处上传</div>
          <div class="upload-hint">或点击选择文件，选择后需配置切片参数并提交</div>
          <div class="upload-decoration">
            <span class="deco-line"></span>
            <span class="deco-dot"></span>
            <span class="deco-line"></span>
          </div>
        </div>

        <!-- 文档网格 -->
        <div class="doc-grid" :class="{ 'list-view': isListView }" v-loading="loading">
          <div v-for="(doc, index) in documentList" :key="doc.id" class="doc-card" :style="{ transitionDelay: (index * 50) + 'ms' }">
            <div class="doc-card-header">
              <div class="doc-icon" :class="getIconClass(doc.fileType)">
                <img v-if="isImageType(doc.fileType)" :src="getImageUrl(doc.filePath)" :alt="doc.fileName" class="doc-image-preview" />
                <span v-else v-html="getFileIcon(doc.fileType)" />
              </div>
              <div class="doc-info">
                <div class="doc-title" :title="doc.fileName">{{ doc.fileName }}</div>
                <div class="doc-meta">
                  <span>{{ formatFileSize(doc.fileSize) }}</span>
                  <span>{{ formatDate(doc.createTime) }}</span>
                </div>
              </div>
            </div>
            <div class="doc-card-body">
              <div class="doc-status">
                <dict-tag class="status-badge" :class="getStatusClass(doc.status)" :options="sys_normal_disable" :value="doc.status" />
              </div>
            </div>
            <div class="doc-card-footer">
              <div class="doc-tags">
                <span class="doc-tag type-tag" :class="'type-' + getIconClass(doc.fileType)" v-if="doc.fileType">
                  {{ doc.fileType.toUpperCase() }}
                </span>
<!--                <span class="doc-tag">ID: {{ doc.id }}</span>-->
              </div>
              <div class="doc-actions">
                <button class="doc-action-btn accent-btn" @click="handleChunks(doc)" v-hasPermi="['chatrag:chunk:list']" title="切片管理">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="7" height="7"/>
                    <rect x="14" y="3" width="7" height="7"/>
                    <rect x="3" y="14" width="7" height="7"/>
                    <rect x="14" y="14" width="7" height="7"/>
                  </svg>
                </button>
                <button class="doc-action-btn" @click="handleDownload(doc)" title="下载附件">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7 10 12 15 17 10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                </button>
                <button class="doc-action-btn danger-btn" @click="handleDelete(doc)" v-hasPermi="['chatrag:document:remove']" title="删除">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="documentList.length === 0 && !loading" class="empty-state">
            <div class="empty-icon">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
            </div>
            <h3>暂无文档</h3>
            <p>上传您的第一个文档开始使用</p>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" id="fileData" v-show="total > 0">
          <div class="pagination-info">
            显示 {{ (queryParams.pageNum - 1) * queryParams.pageSize + 1 }} - {{ Math.min(queryParams.pageNum * queryParams.pageSize, total) }} 条，共 {{ total }} 条
          </div>
          <div class="pagination-controls">
            <div class="page-buttons">
              <button class="page-btn" :disabled="queryParams.pageNum === 1" @click="goToPage(1)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="11 17 6 12 11 7"/>
                  <polyline points="18 17 13 12 18 7"/>
                </svg>
              </button>
              <button class="page-btn" :disabled="queryParams.pageNum === 1" @click="goToPage(queryParams.pageNum - 1)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="15 18 9 12 15 6"/>
                </svg>
              </button>
              <span class="page-current">{{ queryParams.pageNum }} / {{ totalPages }}</span>
              <button class="page-btn" :disabled="queryParams.pageNum >= totalPages" @click="goToPage(queryParams.pageNum + 1)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </button>
              <button class="page-btn" :disabled="queryParams.pageNum >= totalPages" @click="goToPage(totalPages)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="13 17 18 12 13 7"/>
                  <polyline points="6 17 11 12 6 7"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </main>

      <!-- 添加/编辑弹窗 -->
      <el-dialog :title="title" v-model="open" width="500px" append-to-body>
        <el-form ref="documentRef" :model="form" label-width="100px">
          <el-form-item label="待传文件" v-if="form.rawFile">
            <span style="color: var(--accent)">{{ form.fileName }} (已选择，未上传)</span>
          </el-form-item>
          <el-form-item label="文件名" v-else-if="form.fileName">
            <span>{{ form.fileName }}</span>
          </el-form-item>
          <el-form-item label="分块大小" prop="chunkSize">
            <el-input-number v-model="form.chunkSize" :min="1" placeholder="请输入分块大小（字符数）" />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="cancel">取消</el-button>
            <el-button type="primary" @click="submitForm">上传并保存</el-button>
          </div>
        </template>
      </el-dialog>

      <!-- Toast 容器 -->
      <div class="toast-container" id="toastContainer"></div>
    </div>
  </div>
</template>

<script setup name="Document">
import {addDocument, delDocument, listDocument, uploadFile, downloadDocument} from "@/api/ai/chatrag/document"
import "@/utils/css/document.css";

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

const router = useRouter()
const route = useRoute()

const documentList = ref([])
const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")
const isListView = ref(false)
const isDragover = ref(false)
const pageTitle = ref("全部文档")
const fileInput = ref(null)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 12,
    knowledgeId: undefined,
    fileName: undefined,
    fileType: undefined,
    status: undefined,
  }
})

if (route.query.knowledgeId) {
  data.queryParams.knowledgeId = route.query.knowledgeId
}

const { queryParams, form } = toRefs(data)

const totalPages = computed(() => Math.ceil(total.value / queryParams.value.pageSize) || 1)

function getIconClass(type) {
  const typeMap = { 'pdf': 'pdf', 'txt': 'txt', 'md': 'md', 'doc': 'doc', 'docx': 'doc', 'jpg': 'image', 'jpeg': 'image', 'png': 'image', 'gif': 'image', 'webp': 'image' }
  return typeMap[type?.toLowerCase()] || 'other'
}

function isImageType(type) {
  const imageTypes = ['jpg', 'jpeg', 'png', 'gif', 'webp']
  return imageTypes.includes(type?.toLowerCase())
}

function getImageUrl(filePath) {
  if (!filePath) return ''
  return filePath.startsWith('http') ? filePath : window.location.origin + filePath
}

function getFileIcon(type) {
  const icons = {
    pdf: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>',
    txt: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>',
    md: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><path d="M9 15l2 2 4-4"/></svg>',
    doc: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>',
    image: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>',
    other: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>'
  }
  return icons[type?.toLowerCase()] || icons.other
}

function getStatusClass(status) {
  const classMap = { 0: 'status-success', 1: 'status-error' };
  return classMap[status] ?? 'status-pending'
}

function formatFileSize(size) {
  if (!size) return '-';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  let s = parseFloat(size);
  while (s >= 1024 && i < units.length - 1) { s /= 1024; i++ };
  return s.toFixed(1) + ' ' + units[i]
}

function formatDate(date) {
  if (!date) return '-';
  return new Date(date).toLocaleDateString('zh-CN')
}

function toggleView() {
  isListView.value = !isListView.value
}

function goToPage(page) {
  queryParams.value.pageNum = page;
  getList()
}

function triggerUpload() {
  fileInput.value?.click()
}

/**
 * 通用文件选择处理函数
 * 暂存文件信息，打开配置弹窗等待用户输入切片参数
 */
function handleFileSelect(e) {
  const files = e.target.files;
  if (files && files.length > 0) {
    prepareDocumentConfig(files[0]);
  }
  e.target.value = ''; // 清空input值，允许重复选择
}

function handleDrop(e) {
  isDragover.value = false;
  const files = e.dataTransfer.files;
  if (files && files.length > 0) {
    prepareDocumentConfig(files[0]);
  }
}

function prepareDocumentConfig(file) {
  if (!file) return;

  // 简单的格式校验
  const allowedTypes = ['pdf', 'txt', 'md', 'doc', 'docx', 'jpg', 'jpeg', 'png', 'gif', 'webp'];
  const fileExt = file.name.split('.').pop().toLowerCase();
  if (!allowedTypes.includes(fileExt)) {
    showToast(`不支持的文件格式: ${fileExt}`, 'error');
    return;
  }

  // 重置并暂存文件信息
  reset();
  form.value.rawFile = file; // 暂存 File 对象
  form.value.fileName = file.name;
  form.value.fileType = fileExt;

  // 打开配置窗口
  open.value = true;
  title.value = "配置文档信息";
}

function getList() {
  loading.value = true
  listDocument(queryParams.value).then(response => {
    documentList.value = response.rows
    total.value = response.total
    loading.value = false
    nextTick(() => {
      document.querySelectorAll('.doc-card').forEach((card, i) => {
        setTimeout(() => card.classList.add('visible'), i * 50)
      })
    })
  })
}

function cancel() {
  open.value = false;
  reset()
}

function reset() {
  form.value = {
    id: null,
    knowledgeId: queryParams.value.knowledgeId || null,
    rawFile: null,
    fileName: null,
    filePath: null,
    fileType: null,
    status: null,
    chunkCount: 10,
    chunkSize: 500,
    createTime: null
  }
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  queryParams.value.pageSize = 10;
  getList()
}

/**
 * 提交表单：先上传附件，成功后用返回的 name/url 调用 addDocument 保存
 */
function submitForm() {
  // —— 新增上传流程：有暂存的 rawFile ——
  if (form.value.rawFile) {
    if (!form.value.chunkCount || form.value.chunkCount < 1) {
      showToast('请输入有效的切片数量', 'error')
      return
    }
    if (!form.value.chunkSize || form.value.chunkSize < 1) {
      showToast('请输入有效的分块大小', 'error')
      return
    }

    proxy.$modal.loading("正在上传文件...")

    const formData = new FormData()
    formData.append('file', form.value.rawFile)

    uploadFile(formData).then(uploadRes => {
      if (uploadRes.code === 200 && uploadRes.data) {
        const { name, url } = uploadRes.data

        const documentData = {
          knowledgeId: form.value.knowledgeId,
          fileName:    name,
          filePath:    url,
          fileType:    form.value.fileType,
          chunkCount:  form.value.chunkCount,
          chunkSize:   form.value.chunkSize,
          status:      0
        }

        // 4. 调用 addDocument 保存
        return addDocument(documentData)
      } else {
        throw new Error(uploadRes.msg || '文件上传失败')
      }
    }).then(() => {
      // addDocument 保存成功
      showToast("上传并保存成功", 'success')
      open.value = false
      getList()
    }).catch(error => {
      showToast(error.message || '操作失败', 'error')
    }).finally(() => {
      proxy.$modal.closeLoading()
    })

    return
  }

  if (form.value.id) {
    if (!form.value.filePath) {
      showToast('文件路径缺失', 'error')
      return
    }
    if (!form.value.chunkCount || form.value.chunkCount < 1) {
      showToast('请输入有效的切片数量', 'error')
      return
    }
    if (!form.value.chunkSize || form.value.chunkSize < 1) {
      showToast('请输入有效的分块大小', 'error')
      return
    }

    proxy.$modal.loading("正在保存文档...")
    const documentData = {
      id:          form.value.id,
      knowledgeId: form.value.knowledgeId,
      fileName:    form.value.fileName,
      filePath:    form.value.filePath,
      fileType:    form.value.fileType,
      chunkCount:  form.value.chunkCount,
      chunkSize:   form.value.chunkSize,
      status:      form.value.status
    }

    addDocument(documentData).then(() => {
      showToast("保存成功", 'success')
      open.value = false
      getList()
    }).catch(error => {
      showToast(error.message || '保存失败', 'error')
    }).finally(() => {
      proxy.$modal.closeLoading()
    })
    return
  }

  // 既没有新文件，也不是编辑：兜底提示
  showToast('请先选择文件', 'error')
}
// --- 修改的核心逻辑结束 ---

function handleChunks(row) {
  router.push({
    path: '/ai/rag/chunk/index',
    query: { documentId: row.id, documentName: row.fileName, knowledgeId: row.knowledgeId }
  })
}

function handleDownload(doc) {
  if (!doc.filePath) {
    showToast('文件路径不存在', 'error')
    return
  }
  proxy.$modal.loading("正在下载文件...")
  downloadDocument(doc.filePath).then(response => {
    const blob = new Blob([response.data])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = doc.fileName || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    showToast('下载成功', 'success')
  }).catch(error => {
    console.error('下载失败:', error)
    showToast('下载失败', 'error')
  }).finally(() => {
    proxy.$modal.closeLoading()
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除该文档？').then(() => delDocument(row.id))
  .then(() => {
    getList();
    showToast("删除成功", 'success')
  }).catch(() => {})
}

function showToast(message, type = 'success') {
  const container = document.getElementById('toastContainer')
  const toast = document.createElement('div')
  toast.className = 'toast'
  toast.innerHTML = `
    <div class="toast-icon ${type}">
      ${type === 'success' ? '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>' :
      '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/>' +
      '<line x1="9" y1="9" x2="15" y2="15"/></svg>'}
    </div>
    <span class="toast-message">${message}</span>
  `
  container.appendChild(toast)
  setTimeout(() => toast.classList.add('show'), 10)
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300)
  }, 3000)
}

onMounted(() => {
  const container = document.getElementById('particles')
  if (container) {
    for (let i = 0; i < 15; i++) {
      const particle = document.createElement('div')
      particle.className = 'particle'
      particle.style.left = Math.random() * 100 + '%'
      particle.style.animationDelay = Math.random() * 20 + 's'
      particle.style.animationDuration = (15 + Math.random() * 10) + 's'
      container.appendChild(particle)
    }
  }
  getList()
})
</script>