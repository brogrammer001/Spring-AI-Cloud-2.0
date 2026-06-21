<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="属性名" prop="attrName">
        <el-input
          v-model="queryParams.attrName"
          placeholder="请输入属性名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="属性图标" prop="icon">
        <el-input
          v-model="queryParams.icon"
          placeholder="请输入属性图标"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="可选值列表[用逗号分隔]" prop="valueSelect">
        <el-input
          v-model="queryParams.valueSelect"
          placeholder="请输入可选值列表[用逗号分隔]"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="启用状态[0 - 禁用，1 - 启用]" prop="enable">
        <el-input
          v-model="queryParams.enable"
          placeholder="请输入启用状态[0 - 禁用，1 - 启用]"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属分类" prop="catelogId">
        <el-input
          v-model="queryParams.catelogId"
          placeholder="请输入所属分类"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="快速展示【是否展示在介绍上；0-否 1-是】，在sku中仍然可以调整" prop="showDesc">
        <el-input
          v-model="queryParams.showDesc"
          placeholder="请输入快速展示【是否展示在介绍上；0-否 1-是】，在sku中仍然可以调整"
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
          v-hasPermi="['pms:attr:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['pms:attr:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['pms:attr:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['pms:attr:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="attrList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="属性id" align="center" prop="attrId" />
      <el-table-column label="属性名" align="center" prop="attrName" />
      <el-table-column label="是否需要检索[0-不需要，1-需要]" align="center" prop="searchType" />
      <el-table-column label="值类型[0-为单个值，1-可以选择多个值]" align="center" prop="valueType" />
      <el-table-column label="属性图标" align="center" prop="icon" />
      <el-table-column label="可选值列表[用逗号分隔]" align="center" prop="valueSelect" />
      <el-table-column label="属性类型[0-销售属性，1-基本属性，2-既是销售属性又是基本属性]" align="center" prop="attrType" />
      <el-table-column label="启用状态[0 - 禁用，1 - 启用]" align="center" prop="enable" />
      <el-table-column label="所属分类" align="center" prop="catelogId" />
      <el-table-column label="快速展示【是否展示在介绍上；0-否 1-是】，在sku中仍然可以调整" align="center" prop="showDesc" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['pms:attr:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['pms:attr:remove']">删除</el-button>
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

    <!-- 添加或修改商品属性对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="attrRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="属性名" prop="attrName">
              <el-input v-model="form.attrName" placeholder="请输入属性名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="属性图标" prop="icon">
              <el-input v-model="form.icon" placeholder="请输入属性图标" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可选值列表[用逗号分隔]" prop="valueSelect">
              <el-input v-model="form.valueSelect" placeholder="请输入可选值列表[用逗号分隔]" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="启用状态[0 - 禁用，1 - 启用]" prop="enable">
              <el-input v-model="form.enable" placeholder="请输入启用状态[0 - 禁用，1 - 启用]" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属分类" prop="catelogId">
              <el-input v-model="form.catelogId" placeholder="请输入所属分类" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="快速展示【是否展示在介绍上；0-否 1-是】，在sku中仍然可以调整" prop="showDesc">
              <el-input v-model="form.showDesc" placeholder="请输入快速展示【是否展示在介绍上；0-否 1-是】，在sku中仍然可以调整" />
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

<script setup name="Attr">
import { listAttr, getAttr, delAttr, addAttr, updateAttr } from "@/api/pms/attr"

const { proxy } = getCurrentInstance()

const attrList = ref([])
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
    attrName: null,
    searchType: null,
    valueType: null,
    icon: null,
    valueSelect: null,
    attrType: null,
    enable: null,
    catelogId: null,
    showDesc: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询商品属性列表 */
function getList() {
  loading.value = true
  listAttr(queryParams.value).then(response => {
    attrList.value = response.rows
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
    attrId: null,
    attrName: null,
    searchType: null,
    valueType: null,
    icon: null,
    valueSelect: null,
    attrType: null,
    enable: null,
    catelogId: null,
    showDesc: null
  }
  proxy.resetForm("attrRef")
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
  ids.value = selection.map(item => item.attrId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加商品属性"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _attrId = row.attrId || ids.value
  getAttr(_attrId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改商品属性"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["attrRef"].validate(valid => {
    if (valid) {
      if (form.value.attrId != null) {
        updateAttr(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAttr(form.value).then(() => {
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
  const _attrIds = row.attrId || ids.value
  proxy.$modal.confirm('是否确认删除商品属性编号为"' + _attrIds + '"的数据项？').then(function() {
    return delAttr(_attrIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('pms/attr/export', {
    ...queryParams.value
  }, `attr_${new Date().getTime()}.xlsx`)
}

getList()
</script>
