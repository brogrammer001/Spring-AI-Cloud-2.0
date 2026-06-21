<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="spu_id" prop="skuId">
        <el-input
          v-model="queryParams.skuId"
          placeholder="请输入spu_id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="满几件" prop="fullCount">
        <el-input
          v-model="queryParams.fullCount"
          placeholder="请输入满几件"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="打几折" prop="discount">
        <el-input
          v-model="queryParams.discount"
          placeholder="请输入打几折"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="折后价" prop="price">
        <el-input
          v-model="queryParams.price"
          placeholder="请输入折后价"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否叠加其他优惠[0-不可叠加，1-可叠加]" prop="addOther">
        <el-input
          v-model="queryParams.addOther"
          placeholder="请输入是否叠加其他优惠[0-不可叠加，1-可叠加]"
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
          v-hasPermi="['sms:ladder:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['sms:ladder:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['sms:ladder:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['sms:ladder:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ladderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="spu_id" align="center" prop="skuId" />
      <el-table-column label="满几件" align="center" prop="fullCount" />
      <el-table-column label="打几折" align="center" prop="discount" />
      <el-table-column label="折后价" align="center" prop="price" />
      <el-table-column label="是否叠加其他优惠[0-不可叠加，1-可叠加]" align="center" prop="addOther" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sms:ladder:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sms:ladder:remove']">删除</el-button>
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

    <!-- 添加或修改商品阶梯价格对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="ladderRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="spu_id" prop="skuId">
              <el-input v-model="form.skuId" placeholder="请输入spu_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="满几件" prop="fullCount">
              <el-input v-model="form.fullCount" placeholder="请输入满几件" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="打几折" prop="discount">
              <el-input v-model="form.discount" placeholder="请输入打几折" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="折后价" prop="price">
              <el-input v-model="form.price" placeholder="请输入折后价" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否叠加其他优惠[0-不可叠加，1-可叠加]" prop="addOther">
              <el-input v-model="form.addOther" placeholder="请输入是否叠加其他优惠[0-不可叠加，1-可叠加]" />
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

<script setup name="Ladder">
import { listLadder, getLadder, delLadder, addLadder, updateLadder } from "@/api/sms/ladder"

const { proxy } = getCurrentInstance()

const ladderList = ref([])
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
    fullCount: null,
    discount: null,
    price: null,
    addOther: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询商品阶梯价格列表 */
function getList() {
  loading.value = true
  listLadder(queryParams.value).then(response => {
    ladderList.value = response.rows
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
    fullCount: null,
    discount: null,
    price: null,
    addOther: null
  }
  proxy.resetForm("ladderRef")
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
  title.value = "添加商品阶梯价格"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getLadder(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改商品阶梯价格"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["ladderRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateLadder(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addLadder(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除商品阶梯价格编号为"' + _ids + '"的数据项？').then(function() {
    return delLadder(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('sms/ladder/export', {
    ...queryParams.value
  }, `ladder_${new Date().getTime()}.xlsx`)
}

getList()
</script>
