<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="Agent标识" prop="agentId">
        <el-input
          v-model="queryParams.agentId"
          placeholder="请输入Agent标识"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会话ID" prop="conversationId">
        <el-input
          v-model="queryParams.conversationId"
          placeholder="请输入会话ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="重要性" prop="importance">
        <el-input
          v-model="queryParams.importance"
          placeholder="请输入重要性（0.00~1.00）"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="去重指纹" prop="hash">
        <el-input
          v-model="queryParams.hash"
          placeholder="请输入SHA256指纹"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="有效期至" prop="validUntil">
        <el-date-picker clearable
          v-model="queryParams.validUntil"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择有效期至">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="向量同步" prop="vectorSynced">
        <el-input
          v-model="queryParams.vectorSynced"
          placeholder="1已同步 / 0待补偿"
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
          v-hasPermi="['aichat:fact:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['aichat:fact:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['aichat:fact:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['aichat:fact:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Upload"
          :disabled="multiple"
          :loading="syncing"
          @click="handleSyncVector"
          v-hasPermi="['aichat:fact:edit']"
        >同步向量库</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="factList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户ID" align="center" prop="userId" width="130" show-overflow-tooltip />
      <el-table-column label="Agent标识" align="center" prop="agentId" width="130" show-overflow-tooltip />
      <el-table-column label="会话ID" align="center" prop="conversationId" width="150" show-overflow-tooltip />
      <el-table-column label="事实分类" align="center" prop="factType" width="100" />
      <el-table-column label="记忆内容" align="center" prop="content" min-width="240" show-overflow-tooltip />
      <el-table-column label="重要性" align="center" prop="importance" width="90" />
      <el-table-column label="去重指纹" align="center" prop="hash" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="90" />
      <el-table-column label="有效期至" align="center" prop="validUntil" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.validUntil, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="向量同步" align="center" prop="vectorSynced" width="100" />
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['aichat:fact:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aichat:fact:remove']">删除</el-button>
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

    <!-- 添加或修改AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="factRef" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="用户ID" prop="userId">
              <el-input v-model="form.userId" placeholder="请输入用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Agent标识" prop="agentId">
              <el-input v-model="form.agentId" placeholder="请输入Agent标识" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会话ID" prop="conversationId">
              <el-input v-model="form.conversationId" placeholder="请输入会话ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="记忆内容">
              <editor v-model="form.content" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="重要性" prop="importance">
              <el-input v-model="form.importance" placeholder="请输入重要性（0.00~1.00）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="去重指纹" prop="hash">
              <el-input v-model="form.hash" placeholder="请输入SHA256指纹" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="有效期至" prop="validUntil">
              <el-date-picker clearable
                v-model="form.validUntil"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择有效期至">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="向量同步" prop="vectorSynced">
              <el-input v-model="form.vectorSynced" placeholder="1已同步 / 0待补偿" />
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

<script setup name="Fact">
import { listFact, getFact, delFact, addFact, updateFact, syncFactVector } from "@/api/aichat/fact"

const { proxy } = getCurrentInstance()

const factList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const syncing = ref(false)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    agentId: undefined,
    conversationId: undefined,
    factType: undefined,
    content: undefined,
    importance: undefined,
    hash: undefined,
    status: undefined,
    validUntil: undefined,
    vectorSynced: undefined,
  },
  rules: {
    userId: [
      { required: true, message: "用户ID不能为空", trigger: "blur" }
    ],
    agentId: [
      { required: true, message: "Agent标识不能为空", trigger: "blur" }
    ],
    factType: [
      { required: true, message: "事实分类不能为空", trigger: "change" }
    ],
    content: [
      { required: true, message: "记忆内容不能为空", trigger: "blur" }
    ],
    importance: [
      { required: true, message: "重要性不能为空", trigger: "blur" }
    ],
    hash: [
      { required: true, message: "去重指纹不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "状态不能为空", trigger: "change" }
    ],
    vectorSynced: [
      { required: true, message: "向量同步标记不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)列表 */
function getList() {
  loading.value = true
  listFact(queryParams.value).then(response => {
    factList.value = response.rows
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
    id: null,
    userId: null,
    agentId: null,
    conversationId: null,
    factType: null,
    content: null,
    importance: null,
    hash: null,
    status: null,
    validUntil: null,
    vectorSynced: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("factRef")
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
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getFact(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["factRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateFact(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addFact(form.value).then(() => {
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
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)编号为"' + _ids + '"的数据项？').then(function() {
    return delFact(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('aichat/fact/export', {
    ...queryParams.value
  }, `fact_${new Date().getTime()}.xlsx`)
}

/** 同步选中数据到向量库 */
function handleSyncVector() {
  const _ids = ids.value
  if (!_ids.length) {
    proxy.$modal.msgWarning("请先选择需要同步的数据")
    return
  }
  proxy.$modal.confirm('是否确认将选中的 ' + _ids.length + ' 条长期记忆同步到向量库？').then(function() {
    syncing.value = true
    return syncFactVector(_ids)
  }).then((response) => {
    proxy.$modal.msgSuccess(response.msg || "向量同步完成")
    getList()
  }).catch(() => {}).finally(() => {
    syncing.value = false
  })
}

getList()
</script>
