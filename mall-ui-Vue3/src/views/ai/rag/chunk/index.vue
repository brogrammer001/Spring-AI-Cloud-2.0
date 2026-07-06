<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px" style="margin-top: 20px;">
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
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
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
          <el-button @click="cancel">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Chunk">
import { listChunk, getChunk } from "@/api/ai/chatrag/chunk"

const { proxy } = getCurrentInstance()
const route = useRoute()

const chunkList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref("")

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

if (route.query.documentId) {
  data.queryParams.documentId = route.query.documentId
}
if (route.query.knowledgeId) {
  data.queryParams.knowledgeId = route.query.knowledgeId
}

const { queryParams, form } = toRefs(data)

function getList() {
  loading.value = true
  listChunk(queryParams.value).then(response => {
    chunkList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
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

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleView(row) {
  reset()
  getChunk(row.id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "切片详情"
  })
}

getList()
</script>
