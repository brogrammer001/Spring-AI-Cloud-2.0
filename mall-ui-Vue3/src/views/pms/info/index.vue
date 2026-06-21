<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="spuId" prop="spuId">
        <el-input
          v-model="queryParams.spuId"
          placeholder="请输入spuId"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="sku名称" prop="skuName">
        <el-input
          v-model="queryParams.skuName"
          placeholder="请输入sku名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属分类id" prop="catalogId">
        <el-input
          v-model="queryParams.catalogId"
          placeholder="请输入所属分类id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="品牌id" prop="brandId">
        <el-input
          v-model="queryParams.brandId"
          placeholder="请输入品牌id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="默认图片" prop="skuDefaultImg">
        <el-input
          v-model="queryParams.skuDefaultImg"
          placeholder="请输入默认图片"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="标题" prop="skuTitle">
        <el-input
          v-model="queryParams.skuTitle"
          placeholder="请输入标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="价格" prop="price">
        <el-input
          v-model="queryParams.price"
          placeholder="请输入价格"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="销量" prop="saleCount">
        <el-input
          v-model="queryParams.saleCount"
          placeholder="请输入销量"
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
          v-hasPermi="['pms:info:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['pms:info:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['pms:info:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['pms:info:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="infoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="skuId" align="center" prop="skuId" />
      <el-table-column label="spuId" align="center" prop="spuId" />
      <el-table-column label="sku名称" align="center" prop="skuName" />
      <el-table-column label="sku介绍描述" align="center" prop="skuDesc" />
      <el-table-column label="所属分类id" align="center" prop="catalogId" />
      <el-table-column label="品牌id" align="center" prop="brandId" />
      <el-table-column label="默认图片" align="center" prop="skuDefaultImg" />
      <el-table-column label="标题" align="center" prop="skuTitle" />
      <el-table-column label="副标题" align="center" prop="skuSubtitle" />
      <el-table-column label="价格" align="center" prop="price" />
      <el-table-column label="销量" align="center" prop="saleCount" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['pms:info:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['pms:info:remove']">删除</el-button>
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

    <!-- 添加或修改sku信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="infoRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="spuId" prop="spuId">
              <el-input v-model="form.spuId" placeholder="请输入spuId" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="sku名称" prop="skuName">
              <el-input v-model="form.skuName" placeholder="请输入sku名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="sku介绍描述" prop="skuDesc">
              <el-input v-model="form.skuDesc" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属分类id" prop="catalogId">
              <el-input v-model="form.catalogId" placeholder="请输入所属分类id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="品牌id" prop="brandId">
              <el-input v-model="form.brandId" placeholder="请输入品牌id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="默认图片" prop="skuDefaultImg">
              <el-input v-model="form.skuDefaultImg" placeholder="请输入默认图片" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标题" prop="skuTitle">
              <el-input v-model="form.skuTitle" placeholder="请输入标题" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="副标题" prop="skuSubtitle">
              <el-input v-model="form.skuSubtitle" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="价格" prop="price">
              <el-input v-model="form.price" placeholder="请输入价格" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="销量" prop="saleCount">
              <el-input v-model="form.saleCount" placeholder="请输入销量" />
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

<script setup name="Info">
import { listInfo, getInfo, delInfo, addInfo, updateInfo } from "@/api/pms/info"

const { proxy } = getCurrentInstance()

const infoList = ref([])
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
    spuId: null,
    skuName: null,
    skuDesc: null,
    catalogId: null,
    brandId: null,
    skuDefaultImg: null,
    skuTitle: null,
    skuSubtitle: null,
    price: null,
    saleCount: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询sku信息列表 */
function getList() {
  loading.value = true
  listInfo(queryParams.value).then(response => {
    infoList.value = response.rows
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
    skuId: null,
    spuId: null,
    skuName: null,
    skuDesc: null,
    catalogId: null,
    brandId: null,
    skuDefaultImg: null,
    skuTitle: null,
    skuSubtitle: null,
    price: null,
    saleCount: null
  }
  proxy.resetForm("infoRef")
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
  ids.value = selection.map(item => item.skuId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加sku信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _skuId = row.skuId || ids.value
  getInfo(_skuId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改sku信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["infoRef"].validate(valid => {
    if (valid) {
      if (form.value.skuId != null) {
        updateInfo(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addInfo(form.value).then(() => {
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
  const _skuIds = row.skuId || ids.value
  proxy.$modal.confirm('是否确认删除sku信息编号为"' + _skuIds + '"的数据项？').then(function() {
    return delInfo(_skuIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('pms/info/export', {
    ...queryParams.value
  }, `info_${new Date().getTime()}.xlsx`)
}

getList()
</script>
