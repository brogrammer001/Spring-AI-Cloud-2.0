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
      <el-form-item label="满多少" prop="fullPrice">
        <el-input
          v-model="queryParams.fullPrice"
          placeholder="请输入满多少"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="减多少" prop="reducePrice">
        <el-input
          v-model="queryParams.reducePrice"
          placeholder="请输入减多少"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否参与其他优惠" prop="addOther">
        <el-input
          v-model="queryParams.addOther"
          placeholder="请输入是否参与其他优惠"
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
          v-hasPermi="['sms:reduction:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['sms:reduction:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['sms:reduction:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['sms:reduction:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="reductionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="spu_id" align="center" prop="skuId" />
      <el-table-column label="满多少" align="center" prop="fullPrice" />
      <el-table-column label="减多少" align="center" prop="reducePrice" />
      <el-table-column label="是否参与其他优惠" align="center" prop="addOther" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sms:reduction:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sms:reduction:remove']">删除</el-button>
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

    <!-- 添加或修改商品满减信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="reductionRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="spu_id" prop="skuId">
              <el-input v-model="form.skuId" placeholder="请输入spu_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="满多少" prop="fullPrice">
              <el-input v-model="form.fullPrice" placeholder="请输入满多少" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="减多少" prop="reducePrice">
              <el-input v-model="form.reducePrice" placeholder="请输入减多少" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否参与其他优惠" prop="addOther">
              <el-input v-model="form.addOther" placeholder="请输入是否参与其他优惠" />
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

<script setup name="Reduction">
import { listReduction, getReduction, delReduction, addReduction, updateReduction } from "@/api/sms/reduction"

const { proxy } = getCurrentInstance()

const reductionList = ref([])
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
    fullPrice: null,
    reducePrice: null,
    addOther: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询商品满减信息列表 */
function getList() {
  loading.value = true
  listReduction(queryParams.value).then(response => {
    reductionList.value = response.rows
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
    fullPrice: null,
    reducePrice: null,
    addOther: null
  }
  proxy.resetForm("reductionRef")
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
  title.value = "添加商品满减信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getReduction(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改商品满减信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["reductionRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateReduction(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addReduction(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除商品满减信息编号为"' + _ids + '"的数据项？').then(function() {
    return delReduction(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('sms/reduction/export', {
    ...queryParams.value
  }, `reduction_${new Date().getTime()}.xlsx`)
}

getList()
</script>
