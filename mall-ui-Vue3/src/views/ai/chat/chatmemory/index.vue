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
      <el-form-item label="${comment}" prop="sequenceId">
        <el-input
          v-model="queryParams.sequenceId"
          placeholder="请输入${comment}"
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
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['aichat:chatmemory:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['aichat:chatmemory:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['aichat:chatmemory:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['aichat:chatmemory:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="chatmemoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="会话ID" align="center" prop="conversationId" />
      <el-table-column label="消息内容" align="center" prop="content" />
      <el-table-column label="消息类型：USER / ASSISTANT" align="center" prop="type" />
      <el-table-column label="时间戳" align="center" prop="timestamp" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.timestamp, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="${comment}" align="center" prop="sequenceId" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['aichat:chatmemory:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aichat:chatmemory:remove']">删除</el-button>
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

    <!-- 添加或修改【请填写功能名称】对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="chatmemoryRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="会话ID" prop="conversationId">
              <el-input v-model="form.conversationId" placeholder="请输入会话ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="消息内容">
              <editor v-model="form.content" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="时间戳" prop="timestamp">
              <el-date-picker clearable
                v-model="form.timestamp"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择时间戳">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="sequenceId">
              <el-input v-model="form.sequenceId" placeholder="请输入${comment}" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="chatmemory">
import {addChatmemory, delChatmemory, getChatmemory, listChatmemory, updateChatmemory} from "@/api/ai/chatmemory"

const { proxy } = getCurrentInstance()

const chatmemoryList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    conversationId: undefined,
    content: undefined,
    type: undefined,
    timestamp: undefined,
    sequenceId: undefined
  },
  rules: {
    conversationId: [
      { required: true, message: "会话ID不能为空", trigger: "blur" }
    ],
    content: [
      { required: true, message: "消息内容不能为空", trigger: "blur" }
    ],
    type: [
      { required: true, message: "消息类型：USER / ASSISTANT不能为空", trigger: "change" }
    ],
    timestamp: [
      { required: true, message: "时间戳不能为空", trigger: "blur" }
    ],
    sequenceId: [
      { required: true, message: "$comment不能为空", trigger: "blur" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询【请填写功能名称】列表 */
function getList() {
  loading.value = true
  listChatmemory(queryParams.value).then(response => {
    chatmemoryList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    conversationId: null,
    content: null,
    type: null,
    timestamp: null,
    sequenceId: null
  }
  proxy.resetForm("chatmemoryRef")
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

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加【请填写功能名称】"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _conversationId = row.conversationId || ids.value
  getChatmemory(_conversationId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改【请填写功能名称】"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["chatmemoryRef"].validate(valid => {
    if (valid) {
      if (form.value.conversationId != null) {
        updateChatmemory(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addChatmemory(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _conversationIds = row.conversationId || ids.value
  proxy.$modal.confirm('是否确认删除【请填写功能名称】编号为"' + _conversationIds + '"的数据项？').then(function() {
    return delChatmemory(_conversationIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('aichat/chatmemory/export', {
    ...queryParams.value
  }, `chatmemory_${new Date().getTime()}.xlsx`)
}

getList()
</script>
