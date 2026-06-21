<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="sku_id" prop="skuId">
        <el-input
          v-model="queryParams.skuId"
          placeholder="请输入sku_id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="attr_id" prop="attrId">
        <el-input
          v-model="queryParams.attrId"
          placeholder="请输入attr_id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="销售属性名" prop="attrName">
        <el-input
          v-model="queryParams.attrName"
          placeholder="请输入销售属性名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="销售属性值" prop="attrValue">
        <el-input
          v-model="queryParams.attrValue"
          placeholder="请输入销售属性值"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="顺序" prop="attrSort">
        <el-input
          v-model="queryParams.attrSort"
          placeholder="请输入顺序"
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
          v-hasPermi="['pms:value:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['pms:value:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['pms:value:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['pms:value:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="valueList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="sku_id" align="center" prop="skuId" />
      <el-table-column label="attr_id" align="center" prop="attrId" />
      <el-table-column label="销售属性名" align="center" prop="attrName" />
      <el-table-column label="销售属性值" align="center" prop="attrValue" />
      <el-table-column label="顺序" align="center" prop="attrSort" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['pms:value:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['pms:value:remove']">删除</el-button>
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

    <!-- 添加或修改sku销售属性&值对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="valueRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="sku_id" prop="skuId">
              <el-input v-model="form.skuId" placeholder="请输入sku_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="attr_id" prop="attrId">
              <el-input v-model="form.attrId" placeholder="请输入attr_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="销售属性名" prop="attrName">
              <el-input v-model="form.attrName" placeholder="请输入销售属性名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="销售属性值" prop="attrValue">
              <el-input v-model="form.attrValue" placeholder="请输入销售属性值" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="顺序" prop="attrSort">
              <el-input v-model="form.attrSort" placeholder="请输入顺序" />
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

<script setup name="Value">
import { listValue, getValue, delValue, addValue, updateValue } from "@/api/pms/value"

const { proxy } = getCurrentInstance()

const valueList = ref([])
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
    skuId: null,
    attrId: null,
    attrName: null,
    attrValue: null,
    attrSort: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询sku销售属性&值列表 */
function getList() {
  loading.value = true
  listValue(queryParams.value).then(response => {
    valueList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    skuId: null,
    attrId: null,
    attrName: null,
    attrValue: null,
    attrSort: null
  }
  proxy.resetForm("valueRef")
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

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加sku销售属性&值"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getValue(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改sku销售属性&值"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["valueRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateValue(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addValue(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除sku销售属性&值编号为"' + _ids + '"的数据项？').then(function() {
    return delValue(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('pms/value/export', {
    ...queryParams.value
  }, `value_${new Date().getTime()}.xlsx`)
}

getList()
</script>
