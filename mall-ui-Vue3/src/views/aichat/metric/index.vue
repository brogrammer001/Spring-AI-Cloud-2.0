<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="指标名，如复购率" prop="metricName">
        <el-input
          v-model="queryParams.metricName"
          placeholder="请输入指标名，如复购率"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="同义词，逗号分隔：回购率,重复购买率" prop="synonyms">
        <el-input
          v-model="queryParams.synonyms"
          placeholder="请输入同义词，逗号分隔：回购率,重复购买率"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="默认聚合方式" prop="aggDefault">
        <el-input
          v-model="queryParams.aggDefault"
          placeholder="请输入默认聚合方式"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单位：% / 元 / 人" prop="unit">
        <el-input
          v-model="queryParams.unit"
          placeholder="请输入单位：% / 元 / 人"
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
          v-hasPermi="['aichat:metric:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['aichat:metric:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['aichat:metric:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['aichat:metric:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="metricList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="指标ID" align="center" prop="id" />
      <el-table-column label="指标名，如复购率" align="center" prop="metricName" />
      <el-table-column label="同义词，逗号分隔：回购率,重复购买率" align="center" prop="synonyms" />
      <el-table-column label="SQL表达式模板：COUNT(DISTINCT CASE WHEN 下单次数>=2 THEN user_id END) / COUNT(DISTINCT user_id)" align="center" prop="metricExpr" />
      <el-table-column label="默认聚合方式" align="center" prop="aggDefault" />
      <el-table-column label="单位：% / 元 / 人" align="center" prop="unit" />
      <el-table-column label="是否启用" align="center" prop="enabled" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['aichat:metric:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aichat:metric:remove']">删除</el-button>
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

    <!-- 添加或修改NL2SQL指标定义对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="metricRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="指标名，如复购率" prop="metricName">
              <el-input v-model="form.metricName" placeholder="请输入指标名，如复购率" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="同义词，逗号分隔：回购率,重复购买率" prop="synonyms">
              <el-input v-model="form.synonyms" placeholder="请输入同义词，逗号分隔：回购率,重复购买率" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="SQL表达式模板：COUNT(DISTINCT CASE WHEN 下单次数>=2 THEN user_id END) / COUNT(DISTINCT user_id)" prop="metricExpr">
              <el-input v-model="form.metricExpr" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="默认聚合方式" prop="aggDefault">
              <el-input v-model="form.aggDefault" placeholder="请输入默认聚合方式" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="单位：% / 元 / 人" prop="unit">
              <el-input v-model="form.unit" placeholder="请输入单位：% / 元 / 人" />
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

<script setup name="Metric">
import { listMetric, getMetric, delMetric, addMetric, updateMetric } from "@/api/aichat/metric"

const { proxy } = getCurrentInstance()

const metricList = ref([])
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
    metricName: undefined,
    synonyms: undefined,
    metricExpr: undefined,
    aggDefault: undefined,
    unit: undefined,
    enabled: undefined,
  },
  rules: {
    metricName: [
      { required: true, message: "指标名，如复购率不能为空", trigger: "blur" }
    ],
    metricExpr: [
      { required: true, message: "SQL表达式模板：COUNT(DISTINCT CASE WHEN 下单次数>=2 THEN user_id END) / COUNT(DISTINCT user_id)不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询NL2SQL指标定义列表 */
function getList() {
  loading.value = true
  listMetric(queryParams.value).then(response => {
    metricList.value = response.rows
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
    metricName: null,
    synonyms: null,
    metricExpr: null,
    aggDefault: null,
    unit: null,
    enabled: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm("metricRef")
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
  title.value = "添加NL2SQL指标定义"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getMetric(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改NL2SQL指标定义"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["metricRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateMetric(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addMetric(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除NL2SQL指标定义编号为"' + _ids + '"的数据项？').then(function() {
    return delMetric(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('aichat/metric/export', {
    ...queryParams.value
  }, `metric_${new Date().getTime()}.xlsx`)
}

getList()
</script>
