<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="order_id" prop="orderId">
        <el-input
          v-model="queryParams.orderId"
          placeholder="请输入order_id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="order_sn" prop="orderSn">
        <el-input
          v-model="queryParams.orderSn"
          placeholder="请输入order_sn"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收货人" prop="consignee">
        <el-input
          v-model="queryParams.consignee"
          placeholder="请输入收货人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收货人电话" prop="consigneeTel">
        <el-input
          v-model="queryParams.consigneeTel"
          placeholder="请输入收货人电话"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单备注" prop="orderComment">
        <el-input
          v-model="queryParams.orderComment"
          placeholder="请输入订单备注"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="付款方式【 1:在线付款 2:货到付款】" prop="paymentWay">
        <el-input
          v-model="queryParams.paymentWay"
          placeholder="请输入付款方式【 1:在线付款 2:货到付款】"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单描述" prop="orderBody">
        <el-input
          v-model="queryParams.orderBody"
          placeholder="请输入订单描述"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="物流单号" prop="trackingNo">
        <el-input
          v-model="queryParams.trackingNo"
          placeholder="请输入物流单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="仓库id" prop="wareId">
        <el-input
          v-model="queryParams.wareId"
          placeholder="请输入仓库id"
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
          v-hasPermi="['wms:task:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['wms:task:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['wms:task:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['wms:task:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="order_id" align="center" prop="orderId" />
      <el-table-column label="order_sn" align="center" prop="orderSn" />
      <el-table-column label="收货人" align="center" prop="consignee" />
      <el-table-column label="收货人电话" align="center" prop="consigneeTel" />
      <el-table-column label="配送地址" align="center" prop="deliveryAddress" />
      <el-table-column label="订单备注" align="center" prop="orderComment" />
      <el-table-column label="付款方式【 1:在线付款 2:货到付款】" align="center" prop="paymentWay" />
      <el-table-column label="任务状态" align="center" prop="taskStatus" />
      <el-table-column label="订单描述" align="center" prop="orderBody" />
      <el-table-column label="物流单号" align="center" prop="trackingNo" />
      <el-table-column label="仓库id" align="center" prop="wareId" />
      <el-table-column label="工作单备注" align="center" prop="taskComment" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['wms:task:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['wms:task:remove']">删除</el-button>
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

    <!-- 添加或修改库存工作单对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="taskRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="order_id" prop="orderId">
              <el-input v-model="form.orderId" placeholder="请输入order_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="order_sn" prop="orderSn">
              <el-input v-model="form.orderSn" placeholder="请输入order_sn" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货人" prop="consignee">
              <el-input v-model="form.consignee" placeholder="请输入收货人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货人电话" prop="consigneeTel">
              <el-input v-model="form.consigneeTel" placeholder="请输入收货人电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="配送地址" prop="deliveryAddress">
              <el-input v-model="form.deliveryAddress" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单备注" prop="orderComment">
              <el-input v-model="form.orderComment" placeholder="请输入订单备注" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="付款方式【 1:在线付款 2:货到付款】" prop="paymentWay">
              <el-input v-model="form.paymentWay" placeholder="请输入付款方式【 1:在线付款 2:货到付款】" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单描述" prop="orderBody">
              <el-input v-model="form.orderBody" placeholder="请输入订单描述" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="物流单号" prop="trackingNo">
              <el-input v-model="form.trackingNo" placeholder="请输入物流单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="仓库id" prop="wareId">
              <el-input v-model="form.wareId" placeholder="请输入仓库id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="工作单备注" prop="taskComment">
              <el-input v-model="form.taskComment" type="textarea" placeholder="请输入内容" />
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

<script setup name="Task">
import { listTask, getTask, delTask, addTask, updateTask } from "@/api/wms/task"

const { proxy } = getCurrentInstance()

const taskList = ref([])
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
    orderId: null,
    orderSn: null,
    consignee: null,
    consigneeTel: null,
    deliveryAddress: null,
    orderComment: null,
    paymentWay: null,
    taskStatus: null,
    orderBody: null,
    trackingNo: null,
    wareId: null,
    taskComment: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询库存工作单列表 */
function getList() {
  loading.value = true
  listTask(queryParams.value).then(response => {
    taskList.value = response.rows
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
    orderId: null,
    orderSn: null,
    consignee: null,
    consigneeTel: null,
    deliveryAddress: null,
    orderComment: null,
    paymentWay: null,
    taskStatus: null,
    orderBody: null,
    trackingNo: null,
    createTime: null,
    wareId: null,
    taskComment: null
  }
  proxy.resetForm("taskRef")
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
  title.value = "添加库存工作单"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getTask(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改库存工作单"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["taskRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateTask(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTask(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除库存工作单编号为"' + _ids + '"的数据项？').then(function() {
    return delTask(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('wms/task/export', {
    ...queryParams.value
  }, `task_${new Date().getTime()}.xlsx`)
}

getList()
</script>
