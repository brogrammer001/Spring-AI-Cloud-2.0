<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="优惠卷名字" prop="couponName">
        <el-input
          v-model="queryParams.couponName"
          placeholder="请输入优惠卷名字"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="数量" prop="num">
        <el-input
          v-model="queryParams.num"
          placeholder="请输入数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="金额" prop="amount">
        <el-input
          v-model="queryParams.amount"
          placeholder="请输入金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="每人限领张数" prop="perLimit">
        <el-input
          v-model="queryParams.perLimit"
          placeholder="请输入每人限领张数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="使用门槛" prop="minPoint">
        <el-input
          v-model="queryParams.minPoint"
          placeholder="请输入使用门槛"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker clearable
          v-model="queryParams.startTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker clearable
          v-model="queryParams.endTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择结束时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="备注" prop="note">
        <el-input
          v-model="queryParams.note"
          placeholder="请输入备注"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="发行数量" prop="publishCount">
        <el-input
          v-model="queryParams.publishCount"
          placeholder="请输入发行数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="已使用数量" prop="useCount">
        <el-input
          v-model="queryParams.useCount"
          placeholder="请输入已使用数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="领取数量" prop="receiveCount">
        <el-input
          v-model="queryParams.receiveCount"
          placeholder="请输入领取数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="可以领取的开始日期" prop="enableStartTime">
        <el-date-picker clearable
          v-model="queryParams.enableStartTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择可以领取的开始日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="可以领取的结束日期" prop="enableEndTime">
        <el-date-picker clearable
          v-model="queryParams.enableEndTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择可以领取的结束日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="优惠码" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入优惠码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="可以领取的会员等级[0->不限等级，其他-对应等级]" prop="memberLevel">
        <el-input
          v-model="queryParams.memberLevel"
          placeholder="请输入可以领取的会员等级[0->不限等级，其他-对应等级]"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="发布状态[0-未发布，1-已发布]" prop="publish">
        <el-input
          v-model="queryParams.publish"
          placeholder="请输入发布状态[0-未发布，1-已发布]"
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
          v-hasPermi="['sms:coupon:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['sms:coupon:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['sms:coupon:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['sms:coupon:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="couponList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="优惠卷类型[0->全场赠券；1->会员赠券；2->购物赠券；3->注册赠券]" align="center" prop="couponType" />
      <el-table-column label="优惠券图片" align="center" prop="couponImg" />
      <el-table-column label="优惠卷名字" align="center" prop="couponName" />
      <el-table-column label="数量" align="center" prop="num" />
      <el-table-column label="金额" align="center" prop="amount" />
      <el-table-column label="每人限领张数" align="center" prop="perLimit" />
      <el-table-column label="使用门槛" align="center" prop="minPoint" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="使用类型[0->全场通用；1->指定分类；2->指定商品]" align="center" prop="useType" />
      <el-table-column label="备注" align="center" prop="note" />
      <el-table-column label="发行数量" align="center" prop="publishCount" />
      <el-table-column label="已使用数量" align="center" prop="useCount" />
      <el-table-column label="领取数量" align="center" prop="receiveCount" />
      <el-table-column label="可以领取的开始日期" align="center" prop="enableStartTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.enableStartTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="可以领取的结束日期" align="center" prop="enableEndTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.enableEndTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="优惠码" align="center" prop="code" />
      <el-table-column label="可以领取的会员等级[0->不限等级，其他-对应等级]" align="center" prop="memberLevel" />
      <el-table-column label="发布状态[0-未发布，1-已发布]" align="center" prop="publish" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sms:coupon:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sms:coupon:remove']">删除</el-button>
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

    <!-- 添加或修改优惠券信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="couponRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="优惠券图片" prop="couponImg">
              <el-input v-model="form.couponImg" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="优惠卷名字" prop="couponName">
              <el-input v-model="form.couponName" placeholder="请输入优惠卷名字" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="数量" prop="num">
              <el-input v-model="form.num" placeholder="请输入数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="金额" prop="amount">
              <el-input v-model="form.amount" placeholder="请输入金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="每人限领张数" prop="perLimit">
              <el-input v-model="form.perLimit" placeholder="请输入每人限领张数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="使用门槛" prop="minPoint">
              <el-input v-model="form.minPoint" placeholder="请输入使用门槛" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker clearable
                v-model="form.startTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择开始时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker clearable
                v-model="form.endTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择结束时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="note">
              <el-input v-model="form.note" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发行数量" prop="publishCount">
              <el-input v-model="form.publishCount" placeholder="请输入发行数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="已使用数量" prop="useCount">
              <el-input v-model="form.useCount" placeholder="请输入已使用数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="领取数量" prop="receiveCount">
              <el-input v-model="form.receiveCount" placeholder="请输入领取数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可以领取的开始日期" prop="enableStartTime">
              <el-date-picker clearable
                v-model="form.enableStartTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择可以领取的开始日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可以领取的结束日期" prop="enableEndTime">
              <el-date-picker clearable
                v-model="form.enableEndTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择可以领取的结束日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="优惠码" prop="code">
              <el-input v-model="form.code" placeholder="请输入优惠码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可以领取的会员等级[0->不限等级，其他-对应等级]" prop="memberLevel">
              <el-input v-model="form.memberLevel" placeholder="请输入可以领取的会员等级[0->不限等级，其他-对应等级]" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发布状态[0-未发布，1-已发布]" prop="publish">
              <el-input v-model="form.publish" placeholder="请输入发布状态[0-未发布，1-已发布]" />
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

<script setup name="Coupon">
import { listCoupon, getCoupon, delCoupon, addCoupon, updateCoupon } from "@/api/sms/coupon"

const { proxy } = getCurrentInstance()

const couponList = ref([])
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
    couponType: null,
    couponImg: null,
    couponName: null,
    num: null,
    amount: null,
    perLimit: null,
    minPoint: null,
    startTime: null,
    endTime: null,
    useType: null,
    note: null,
    publishCount: null,
    useCount: null,
    receiveCount: null,
    enableStartTime: null,
    enableEndTime: null,
    code: null,
    memberLevel: null,
    publish: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询优惠券信息列表 */
function getList() {
  loading.value = true
  listCoupon(queryParams.value).then(response => {
    couponList.value = response.rows
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
    couponType: null,
    couponImg: null,
    couponName: null,
    num: null,
    amount: null,
    perLimit: null,
    minPoint: null,
    startTime: null,
    endTime: null,
    useType: null,
    note: null,
    publishCount: null,
    useCount: null,
    receiveCount: null,
    enableStartTime: null,
    enableEndTime: null,
    code: null,
    memberLevel: null,
    publish: null
  }
  proxy.resetForm("couponRef")
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
  title.value = "添加优惠券信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getCoupon(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改优惠券信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["couponRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateCoupon(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addCoupon(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除优惠券信息编号为"' + _ids + '"的数据项？').then(function() {
    return delCoupon(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('sms/coupon/export', {
    ...queryParams.value
  }, `coupon_${new Date().getTime()}.xlsx`)
}

getList()
</script>
