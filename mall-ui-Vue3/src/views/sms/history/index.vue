<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="优惠券id" prop="couponId">
        <el-input
          v-model="queryParams.couponId"
          placeholder="请输入优惠券id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员id" prop="memberId">
        <el-input
          v-model="queryParams.memberId"
          placeholder="请输入会员id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员名字" prop="memberNickName">
        <el-input
          v-model="queryParams.memberNickName"
          placeholder="请输入会员名字"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="使用时间" prop="useTime">
        <el-date-picker clearable
          v-model="queryParams.useTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择使用时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="订单id" prop="orderId">
        <el-input
          v-model="queryParams.orderId"
          placeholder="请输入订单id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单号" prop="orderSn">
        <el-input
          v-model="queryParams.orderSn"
          placeholder="请输入订单号"
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
          v-hasPermi="['sms:history:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['sms:history:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['sms:history:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['sms:history:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="historyList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="优惠券id" align="center" prop="couponId" />
      <el-table-column label="会员id" align="center" prop="memberId" />
      <el-table-column label="会员名字" align="center" prop="memberNickName" />
      <el-table-column label="获取方式[0->后台赠送；1->主动领取]" align="center" prop="getType" />
      <el-table-column label="使用状态[0->未使用；1->已使用；2->已过期]" align="center" prop="useType" />
      <el-table-column label="使用时间" align="center" prop="useTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.useTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="订单id" align="center" prop="orderId" />
      <el-table-column label="订单号" align="center" prop="orderSn" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sms:history:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sms:history:remove']">删除</el-button>
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

    <!-- 添加或修改优惠券领取历史记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="historyRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="优惠券id" prop="couponId">
              <el-input v-model="form.couponId" placeholder="请输入优惠券id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会员id" prop="memberId">
              <el-input v-model="form.memberId" placeholder="请输入会员id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会员名字" prop="memberNickName">
              <el-input v-model="form.memberNickName" placeholder="请输入会员名字" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="使用时间" prop="useTime">
              <el-date-picker clearable
                v-model="form.useTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择使用时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单id" prop="orderId">
              <el-input v-model="form.orderId" placeholder="请输入订单id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单号" prop="orderSn">
              <el-input v-model="form.orderSn" placeholder="请输入订单号" />
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

<script setup name="History">
import { listHistory, getHistory, delHistory, addHistory, updateHistory } from "@/api/sms/history"

const { proxy } = getCurrentInstance()

const historyList = ref([])
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
    couponId: null,
    memberId: null,
    memberNickName: null,
    getType: null,
    useType: null,
    useTime: null,
    orderId: null,
    orderSn: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询优惠券领取历史记录列表 */
function getList() {
  loading.value = true
  listHistory(queryParams.value).then(response => {
    historyList.value = response.rows
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
    couponId: null,
    memberId: null,
    memberNickName: null,
    getType: null,
    createTime: null,
    useType: null,
    useTime: null,
    orderId: null,
    orderSn: null
  }
  proxy.resetForm("historyRef")
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
  title.value = "添加优惠券领取历史记录"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getHistory(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改优惠券领取历史记录"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["historyRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateHistory(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addHistory(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除优惠券领取历史记录编号为"' + _ids + '"的数据项？').then(function() {
    return delHistory(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('sms/history/export', {
    ...queryParams.value
  }, `history_${new Date().getTime()}.xlsx`)
}

getList()
</script>
