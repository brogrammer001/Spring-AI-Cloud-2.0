<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="会话ID" prop="conversationId">
        <el-input
          v-model="queryParams.conversationId"
          placeholder="请输入会话ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="时间戳" prop="timestamp">
        <el-date-picker clearable
          v-model="queryParams.timestamp"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择时间戳">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="排序" prop="sequenceId">
        <el-input
          v-model="queryParams.sequenceId"
          placeholder="请输入排序"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['aichat:history:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['aichat:history:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="historyList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户名称" align="center" prop="userName" />
      <el-table-column label="消息内容" align="center" prop="content" width="400">
        <template #default="scope">
          <el-tooltip :content="scope.row.content" placement="top">
            <span class="truncate-text">{{ scope.row.content }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="消息类型" align="center" prop="type">
        <template #default="scope">
          <dict-tag :options="sys_ai_role" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column label="时间" align="center" prop="timestamp" width="180" />
      <el-table-column label="排序" align="center" prop="sequenceId" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
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

    <el-dialog title="消息详情" v-model="openDetail" width="500px" append-to-body>
      <el-form ref="detailRef" :model="detailForm" label-width="100px">
        <el-form-item label="用户名称">
          <el-input v-model="detailForm.userName" disabled />
        </el-form-item>
        <el-form-item label="消息内容">
          <el-input v-model="detailForm.content" type="textarea" :rows="5" disabled />
        </el-form-item>
        <el-form-item label="消息类型">
          <el-radio-group v-model="detailForm.type" disabled>
            <el-radio
                v-for="dict in sys_ai_role"
                :key="dict.value"
                :value="dict.value"
            >{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时间">
          <el-input v-model="detailForm.timestamp" disabled />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model="detailForm.sequenceId" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="openDetail = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="History">
import {delHistory, getHistory, listHistory} from "@/api/ai/aichat/history"

const { proxy } = getCurrentInstance()

const { sys_ai_role } = proxy.useDict("sys_ai_role")

const historyList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const openDetail = ref(false)
const detailForm = reactive({
  userName: '',
  content: '',
  type: '',
  timestamp: '',
  sequenceId: ''
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    conversationId: undefined,
    content: undefined,
    type: undefined,
    timestamp: undefined,
    sequenceId: undefined
  }
})

const { queryParams } = toRefs(data)

/** 查询【请填写功能名称】列表 */
function getList() {
  loading.value = true
  listHistory(queryParams.value).then(response => {
    historyList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.conversationId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _conversationIds = row.conversationId || ids.value
  proxy.$modal.confirm('是否确认删除编号为"' + _conversationIds + '"的数据项？').then(function() {
    return delHistory(_conversationIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('aichat/history/export', {
    ...queryParams.value
  }, `history_${new Date().getTime()}.xlsx`)
}

/** 详情按钮操作 */
function handleDetail(row) {
  const id = row.id || row.conversationId
  if (!id) {
    proxy.$modal.msgError('缺少记录ID')
    return
  }
  getHistory(id).then(response => {
    const data = response.data || response || {}
    detailForm.userName = data.userName || ''
    detailForm.content = data.content || ''
    detailForm.type = data.type || ''
    detailForm.timestamp = data.timestamp || ''
    detailForm.sequenceId = data.sequenceId || ''
    openDetail.value = true
  }).catch(error => {
    console.error('获取详情失败:', error)
    proxy.$modal.msgError('获取详情失败')
  })
}

getList()
</script>

<style scoped>
.truncate-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
