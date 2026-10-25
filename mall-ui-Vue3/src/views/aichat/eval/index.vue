<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="最小行数" prop="expectedMinRows">
        <el-input
          v-model="queryParams.expectedMinRows"
          placeholder="请输入期望最小结果行数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="启用" prop="enabled">
        <el-input
          v-model="queryParams.enabled"
          placeholder="1启用 / 0停用"
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
          v-hasPermi="['aichat:eval:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['aichat:eval:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['aichat:eval:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['aichat:eval:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="evalList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="评测问题" align="center" prop="question" min-width="240" show-overflow-tooltip />
      <el-table-column label="期望意图" align="center" prop="expectedType" width="110" />
      <el-table-column label="SQL应包含" align="center" prop="expectedSqlContains" min-width="180" show-overflow-tooltip />
      <el-table-column label="SQL不应包含" align="center" prop="expectedSqlNotContains" min-width="180" show-overflow-tooltip />
      <el-table-column label="最小行数" align="center" prop="expectedMinRows" width="100" />
      <el-table-column label="启用" align="center" prop="enabled" width="90" />
      <el-table-column label="场景说明" align="center" prop="remark" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['aichat:eval:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aichat:eval:remove']">删除</el-button>
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

    <!-- 添加或修改NL2SQL黄金评测集对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="evalRef" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="评测问题" prop="question">
              <el-input v-model="form.question" type="textarea" placeholder="请输入评测问题" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="SQL应包含" prop="expectedSqlContains">
              <el-input v-model="form.expectedSqlContains" type="textarea" placeholder="请输入SQL应包含的片段" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="SQL不应包含" prop="expectedSqlNotContains">
              <el-input v-model="form.expectedSqlNotContains" type="textarea" placeholder="请输入SQL不应包含的片段" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="最小行数" prop="expectedMinRows">
              <el-input v-model="form.expectedMinRows" placeholder="请输入期望最小结果行数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="启用" prop="enabled">
              <el-input v-model="form.enabled" placeholder="1启用 / 0停用" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="场景说明" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入用例覆盖场景说明" />
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

<script setup name="Eval">
import { listEval, getEval, delEval, addEval, updateEval } from "@/api/aichat/eval"

const { proxy } = getCurrentInstance()

const evalList = ref([])
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
    question: undefined,
    expectedType: undefined,
    expectedSqlContains: undefined,
    expectedSqlNotContains: undefined,
    expectedMinRows: undefined,
    enabled: undefined,
  },
  rules: {
    question: [
      { required: true, message: "评测问题不能为空", trigger: "blur" }
    ],
    expectedType: [
      { required: true, message: "期望意图不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询NL2SQL黄金评测集列表 */
function getList() {
  loading.value = true
  listEval(queryParams.value).then(response => {
    evalList.value = response.rows
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
    question: null,
    expectedType: null,
    expectedSqlContains: null,
    expectedSqlNotContains: null,
    expectedMinRows: null,
    enabled: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("evalRef")
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
  title.value = "添加NL2SQL黄金评测集"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getEval(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改NL2SQL黄金评测集"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["evalRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateEval(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addEval(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除NL2SQL黄金评测集编号为"' + _ids + '"的数据项？').then(function() {
    return delEval(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('aichat/eval/export', {
    ...queryParams.value
  }, `eval_${new Date().getTime()}.xlsx`)
}

getList()
</script>
