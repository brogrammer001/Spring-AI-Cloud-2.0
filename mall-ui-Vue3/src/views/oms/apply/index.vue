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
      <el-form-item label="退货商品id" prop="skuId">
        <el-input
          v-model="queryParams.skuId"
          placeholder="请输入退货商品id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单编号" prop="orderSn">
        <el-input
          v-model="queryParams.orderSn"
          placeholder="请输入订单编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员用户名" prop="memberUsername">
        <el-input
          v-model="queryParams.memberUsername"
          placeholder="请输入会员用户名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退款金额" prop="returnAmount">
        <el-input
          v-model="queryParams.returnAmount"
          placeholder="请输入退款金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退货人姓名" prop="returnName">
        <el-input
          v-model="queryParams.returnName"
          placeholder="请输入退货人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退货人电话" prop="returnPhone">
        <el-input
          v-model="queryParams.returnPhone"
          placeholder="请输入退货人电话"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="处理时间" prop="handleTime">
        <el-date-picker clearable
          v-model="queryParams.handleTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择处理时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="商品名称" prop="skuName">
        <el-input
          v-model="queryParams.skuName"
          placeholder="请输入商品名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品品牌" prop="skuBrand">
        <el-input
          v-model="queryParams.skuBrand"
          placeholder="请输入商品品牌"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退货数量" prop="skuCount">
        <el-input
          v-model="queryParams.skuCount"
          placeholder="请输入退货数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品单价" prop="skuPrice">
        <el-input
          v-model="queryParams.skuPrice"
          placeholder="请输入商品单价"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品实际支付单价" prop="skuRealPrice">
        <el-input
          v-model="queryParams.skuRealPrice"
          placeholder="请输入商品实际支付单价"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="原因" prop="reason">
        <el-input
          v-model="queryParams.reason"
          placeholder="请输入原因"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="处理人员" prop="handleMan">
        <el-input
          v-model="queryParams.handleMan"
          placeholder="请输入处理人员"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收货人" prop="receiveMan">
        <el-input
          v-model="queryParams.receiveMan"
          placeholder="请输入收货人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收货时间" prop="receiveTime">
        <el-date-picker clearable
          v-model="queryParams.receiveTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择收货时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="收货电话" prop="receivePhone">
        <el-input
          v-model="queryParams.receivePhone"
          placeholder="请输入收货电话"
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
          v-hasPermi="['oms:apply:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['oms:apply:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['oms:apply:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['oms:apply:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="applyList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="order_id" align="center" prop="orderId" />
      <el-table-column label="退货商品id" align="center" prop="skuId" />
      <el-table-column label="订单编号" align="center" prop="orderSn" />
      <el-table-column label="会员用户名" align="center" prop="memberUsername" />
      <el-table-column label="退款金额" align="center" prop="returnAmount" />
      <el-table-column label="退货人姓名" align="center" prop="returnName" />
      <el-table-column label="退货人电话" align="center" prop="returnPhone" />
      <el-table-column label="申请状态[0->待处理；1->退货中；2->已完成；3->已拒绝]" align="center" prop="status" />
      <el-table-column label="处理时间" align="center" prop="handleTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.handleTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="商品图片" align="center" prop="skuImg" />
      <el-table-column label="商品名称" align="center" prop="skuName" />
      <el-table-column label="商品品牌" align="center" prop="skuBrand" />
      <el-table-column label="商品销售属性(JSON)" align="center" prop="skuAttrsVals" />
      <el-table-column label="退货数量" align="center" prop="skuCount" />
      <el-table-column label="商品单价" align="center" prop="skuPrice" />
      <el-table-column label="商品实际支付单价" align="center" prop="skuRealPrice" />
      <el-table-column label="原因" align="center" prop="reason" />
      <el-table-column label="描述" align="center" prop="description述" />
      <el-table-column label="凭证图片，以逗号隔开" align="center" prop="descPics" />
      <el-table-column label="处理备注" align="center" prop="handleNote" />
      <el-table-column label="处理人员" align="center" prop="handleMan" />
      <el-table-column label="收货人" align="center" prop="receiveMan" />
      <el-table-column label="收货时间" align="center" prop="receiveTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.receiveTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收货备注" align="center" prop="receiveNote" />
      <el-table-column label="收货电话" align="center" prop="receivePhone" />
      <el-table-column label="公司收货地址" align="center" prop="companyAddress" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['oms:apply:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['oms:apply:remove']">删除</el-button>
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

    <!-- 添加或修改订单退货申请对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="applyRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="order_id" prop="orderId">
              <el-input v-model="form.orderId" placeholder="请输入order_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退货商品id" prop="skuId">
              <el-input v-model="form.skuId" placeholder="请输入退货商品id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单编号" prop="orderSn">
              <el-input v-model="form.orderSn" placeholder="请输入订单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会员用户名" prop="memberUsername">
              <el-input v-model="form.memberUsername" placeholder="请输入会员用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退款金额" prop="returnAmount">
              <el-input v-model="form.returnAmount" placeholder="请输入退款金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退货人姓名" prop="returnName">
              <el-input v-model="form.returnName" placeholder="请输入退货人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退货人电话" prop="returnPhone">
              <el-input v-model="form.returnPhone" placeholder="请输入退货人电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="处理时间" prop="handleTime">
              <el-date-picker clearable
                v-model="form.handleTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择处理时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品图片" prop="skuImg">
              <el-input v-model="form.skuImg" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品名称" prop="skuName">
              <el-input v-model="form.skuName" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品品牌" prop="skuBrand">
              <el-input v-model="form.skuBrand" placeholder="请输入商品品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品销售属性(JSON)" prop="skuAttrsVals">
              <el-input v-model="form.skuAttrsVals" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退货数量" prop="skuCount">
              <el-input v-model="form.skuCount" placeholder="请输入退货数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品单价" prop="skuPrice">
              <el-input v-model="form.skuPrice" placeholder="请输入商品单价" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品实际支付单价" prop="skuRealPrice">
              <el-input v-model="form.skuRealPrice" placeholder="请输入商品实际支付单价" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="原因" prop="reason">
              <el-input v-model="form.reason" placeholder="请输入原因" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述" prop="description述">
              <el-input v-model="form.description述" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="凭证图片，以逗号隔开" prop="descPics">
              <el-input v-model="form.descPics" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="处理备注" prop="handleNote">
              <el-input v-model="form.handleNote" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="处理人员" prop="handleMan">
              <el-input v-model="form.handleMan" placeholder="请输入处理人员" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货人" prop="receiveMan">
              <el-input v-model="form.receiveMan" placeholder="请输入收货人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货时间" prop="receiveTime">
              <el-date-picker clearable
                v-model="form.receiveTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择收货时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货备注" prop="receiveNote">
              <el-input v-model="form.receiveNote" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货电话" prop="receivePhone">
              <el-input v-model="form.receivePhone" placeholder="请输入收货电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="公司收货地址" prop="companyAddress">
              <el-input v-model="form.companyAddress" type="textarea" placeholder="请输入内容" />
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

<script setup name="Apply">
import { listApply, getApply, delApply, addApply, updateApply } from "@/api/oms/apply"

const { proxy } = getCurrentInstance()

const applyList = ref([])
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
    skuId: null,
    orderSn: null,
    memberUsername: null,
    returnAmount: null,
    returnName: null,
    returnPhone: null,
    status: null,
    handleTime: null,
    skuImg: null,
    skuName: null,
    skuBrand: null,
    skuAttrsVals: null,
    skuCount: null,
    skuPrice: null,
    skuRealPrice: null,
    reason: null,
    description述: null,
    descPics: null,
    handleNote: null,
    handleMan: null,
    receiveMan: null,
    receiveTime: null,
    receiveNote: null,
    receivePhone: null,
    companyAddress: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询订单退货申请列表 */
function getList() {
  loading.value = true
  listApply(queryParams.value).then(response => {
    applyList.value = response.rows
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
    skuId: null,
    orderSn: null,
    createTime: null,
    memberUsername: null,
    returnAmount: null,
    returnName: null,
    returnPhone: null,
    status: null,
    handleTime: null,
    skuImg: null,
    skuName: null,
    skuBrand: null,
    skuAttrsVals: null,
    skuCount: null,
    skuPrice: null,
    skuRealPrice: null,
    reason: null,
    description述: null,
    descPics: null,
    handleNote: null,
    handleMan: null,
    receiveMan: null,
    receiveTime: null,
    receiveNote: null,
    receivePhone: null,
    companyAddress: null
  }
  proxy.resetForm("applyRef")
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
  title.value = "添加订单退货申请"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getApply(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改订单退货申请"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["applyRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateApply(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addApply(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除订单退货申请编号为"' + _ids + '"的数据项？').then(function() {
    return delApply(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('oms/apply/export', {
    ...queryParams.value
  }, `apply_${new Date().getTime()}.xlsx`)
}

getList()
</script>
