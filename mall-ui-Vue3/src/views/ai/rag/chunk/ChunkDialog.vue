<template>
  <!-- 切片管理弹窗 -->
  <el-dialog
    :title="dialogTitle"
    v-model="visible"
    width="900px"
    append-to-body
    destroy-on-close
  >
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="内容查询" prop="content">
        <el-input
          v-model="queryParams.content"
          placeholder="请输入切片内容"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="chunkList">
      <el-table-column label="切片文本内容" align="center" prop="content" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 切片详情弹窗 -->
  <el-dialog :title="detailTitle" v-model="detailOpen" width="600px" append-to-body>
    <el-form ref="chunkRef" :model="form" label-width="100px">
      <el-form-item label="创建时间">
        <el-input v-model="form.createTime" readonly />
      </el-form-item>
      <el-form-item label="切片内容">
        <el-input v-model="form.content" type="textarea" :rows="10" readonly />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="detailOpen = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="ChunkDialog">
import { listChunk, getChunk } from "@/api/ai/chatrag/chunk"

const visible = ref(false)
const loading = ref(false)
const chunkList = ref([])
const total = ref(0)
const dialogTitle = ref("切片管理")

const detailOpen = ref(false)
const detailTitle = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    documentId: undefined,
    knowledgeId: undefined,
    content: undefined,
  }
})

const { queryParams, form } = toRefs(data)

/**
 * 打开切片管理弹窗
 * @param {Object} row 文档行数据，需包含 id、knowledgeId、fileName
 */
function open(row) {
  reset()
  queryParams.value.documentId = row.id
  queryParams.value.knowledgeId = row.knowledgeId
  queryParams.value.content = undefined
  queryParams.value.pageNum = 1
  dialogTitle.value = row.fileName ? `切片管理 - ${row.fileName}` : "切片管理"
  visible.value = true
  getList()
}

function getList() {
  loading.value = true
  listChunk(queryParams.value).then(response => {
    chunkList.value = response.rows
    total.value = response.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value.content = undefined
  handleQuery()
}

function reset() {
  form.value = {
    id: null,
    documentId: null,
    knowledgeId: null,
    content: null,
    createTime: null
  }
}

function handleView(row) {
  reset()
  getChunk(row.id).then(response => {
    form.value = response.data
    detailTitle.value = "切片详情"
    detailOpen.value = true
  })
}

defineExpose({ open })
</script>
