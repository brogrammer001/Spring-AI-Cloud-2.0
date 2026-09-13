<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="规则名" prop="ruleName">
        <el-input
          v-model="queryParams.ruleName"
          placeholder="请输入规则名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="适用关键词，逗号分隔，为空则全局生效" prop="appliesTo">
        <el-input
          v-model="queryParams.appliesTo"
          placeholder="请输入适用关键词，逗号分隔，为空则全局生效"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否启用" prop="enabled">
        <el-input
          v-model="queryParams.enabled"
          placeholder="请输入是否启用"
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
          v-hasPermi="['aichat:rule:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['aichat:rule:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['aichat:rule:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['aichat:rule:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ruleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="规则ID" align="center" prop="id" />
      <el-table-column label="规则名" align="center" prop="ruleName" />
      <el-table-column label=""有效用户"= status=0 AND del_flag=0；"本月"= 本月1日至今" align="center" prop="ruleContent" />
      <el-table-column label="适用关键词，逗号分隔，为空则全局生效" align="center" prop="appliesTo" />
      <el-table-column label="是否启用" align="center" prop="enabled" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['aichat:rule:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aichat:rule:remove']">删除</el-button>
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

    <!-- 添加或修改NL2SQL业务规则对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="ruleRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="规则名" prop="ruleName">
              <el-input v-model="form.ruleName" placeholder="请输入规则名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label=""有效用户"= status=0 AND del_flag=0；"本月"= 本月1日至今">
              <editor v-model="form.ruleContent" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="适用关键词，逗号分隔，为空则全局生效" prop="appliesTo">
              <el-input v-model="form.appliesTo" placeholder="请输入适用关键词，逗号分隔，为空则全局生效" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否启用" prop="enabled">
              <el-input v-model="form.enabled" placeholder="请输入是否启用" />
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

<script setup name="Rule">
import { listRule, getRule, delRule, addRule, updateRule } from "@/api/aichat/rule"

const { proxy } = getCurrentInstance()

const ruleList = ref([])
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
    ruleName: undefined,
    ruleContent: undefined,
    appliesTo: undefined,
    enabled: undefined,
  },
  rules: {
    ruleName: [
      { required: true, message: "规则名不能为空", trigger: "blur" }
    ],
    ruleContent: [
      { required: true, message: ""有效用户"= status=0 AND del_flag=0；"本月"= 本月1日至今不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询NL2SQL业务规则列表 */
function getList() {
  loading.value = true
  listRule(queryParams.value).then(response => {
    ruleList.value = response.rows
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
    ruleName: null,
    ruleContent: null,
    appliesTo: null,
    enabled: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm("ruleRef")
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
  title.value = "添加NL2SQL业务规则"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getRule(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改NL2SQL业务规则"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["ruleRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateRule(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addRule(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除NL2SQL业务规则编号为"' + _ids + '"的数据项？').then(function() {
    return delRule(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('aichat/rule/export', {
    ...queryParams.value
  }, `rule_${new Date().getTime()}.xlsx`)
}

getList()
</script>
