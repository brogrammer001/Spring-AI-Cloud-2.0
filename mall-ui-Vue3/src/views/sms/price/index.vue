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
      <el-form-item label="会员等级id" prop="memberLevelId">
        <el-input
          v-model="queryParams.memberLevelId"
          placeholder="请输入会员等级id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员等级名" prop="memberLevelName">
        <el-input
          v-model="queryParams.memberLevelName"
          placeholder="请输入会员等级名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员对应价格" prop="memberPrice">
        <el-input
          v-model="queryParams.memberPrice"
          placeholder="请输入会员对应价格"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="可否叠加其他优惠[0-不可叠加优惠，1-可叠加]" prop="addOther">
        <el-input
          v-model="queryParams.addOther"
          placeholder="请输入可否叠加其他优惠[0-不可叠加优惠，1-可叠加]"
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
          v-hasPermi="['sms:price:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['sms:price:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['sms:price:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['sms:price:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="priceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="sku_id" align="center" prop="skuId" />
      <el-table-column label="会员等级id" align="center" prop="memberLevelId" />
      <el-table-column label="会员等级名" align="center" prop="memberLevelName" />
      <el-table-column label="会员对应价格" align="center" prop="memberPrice" />
      <el-table-column label="可否叠加其他优惠[0-不可叠加优惠，1-可叠加]" align="center" prop="addOther" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sms:price:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sms:price:remove']">删除</el-button>
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

    <!-- 添加或修改商品会员价格对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="priceRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="sku_id" prop="skuId">
              <el-input v-model="form.skuId" placeholder="请输入sku_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会员等级id" prop="memberLevelId">
              <el-input v-model="form.memberLevelId" placeholder="请输入会员等级id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会员等级名" prop="memberLevelName">
              <el-input v-model="form.memberLevelName" placeholder="请输入会员等级名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会员对应价格" prop="memberPrice">
              <el-input v-model="form.memberPrice" placeholder="请输入会员对应价格" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可否叠加其他优惠[0-不可叠加优惠，1-可叠加]" prop="addOther">
              <el-input v-model="form.addOther" placeholder="请输入可否叠加其他优惠[0-不可叠加优惠，1-可叠加]" />
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

<script setup name="Price">
import { listPrice, getPrice, delPrice, addPrice, updatePrice } from "@/api/sms/price"

const { proxy } = getCurrentInstance()

const priceList = ref([])
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
    memberLevelId: null,
    memberLevelName: null,
    memberPrice: null,
    addOther: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询商品会员价格列表 */
function getList() {
  loading.value = true
  listPrice(queryParams.value).then(response => {
    priceList.value = response.rows
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
    memberLevelId: null,
    memberLevelName: null,
    memberPrice: null,
    addOther: null
  }
  proxy.resetForm("priceRef")
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
  title.value = "添加商品会员价格"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getPrice(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改商品会员价格"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["priceRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updatePrice(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPrice(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除商品会员价格编号为"' + _ids + '"的数据项？').then(function() {
    return delPrice(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('sms/price/export', {
    ...queryParams.value
  }, `price_${new Date().getTime()}.xlsx`)
}

getList()
</script>
