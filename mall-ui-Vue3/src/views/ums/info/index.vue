<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="会员id" prop="memberId">
        <el-input
          v-model="queryParams.memberId"
          placeholder="请输入会员id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="累计消费金额" prop="consumeAmount">
        <el-input
          v-model="queryParams.consumeAmount"
          placeholder="请输入累计消费金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="累计优惠金额" prop="couponAmount">
        <el-input
          v-model="queryParams.couponAmount"
          placeholder="请输入累计优惠金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单数量" prop="orderCount">
        <el-input
          v-model="queryParams.orderCount"
          placeholder="请输入订单数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="优惠券数量" prop="couponCount">
        <el-input
          v-model="queryParams.couponCount"
          placeholder="请输入优惠券数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评价数" prop="commentCount">
        <el-input
          v-model="queryParams.commentCount"
          placeholder="请输入评价数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退货数量" prop="returnOrderCount">
        <el-input
          v-model="queryParams.returnOrderCount"
          placeholder="请输入退货数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="登录次数" prop="loginCount">
        <el-input
          v-model="queryParams.loginCount"
          placeholder="请输入登录次数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="关注数量" prop="attendCount">
        <el-input
          v-model="queryParams.attendCount"
          placeholder="请输入关注数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="粉丝数量" prop="fansCount">
        <el-input
          v-model="queryParams.fansCount"
          placeholder="请输入粉丝数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收藏的商品数量" prop="collectProductCount">
        <el-input
          v-model="queryParams.collectProductCount"
          placeholder="请输入收藏的商品数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收藏的专题活动数量" prop="collectSubjectCount">
        <el-input
          v-model="queryParams.collectSubjectCount"
          placeholder="请输入收藏的专题活动数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收藏的评论数量" prop="collectCommentCount">
        <el-input
          v-model="queryParams.collectCommentCount"
          placeholder="请输入收藏的评论数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邀请的朋友数量" prop="inviteFriendCount">
        <el-input
          v-model="queryParams.inviteFriendCount"
          placeholder="请输入邀请的朋友数量"
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
          v-hasPermi="['ums:info:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['ums:info:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['ums:info:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['ums:info:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="infoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="会员id" align="center" prop="memberId" />
      <el-table-column label="累计消费金额" align="center" prop="consumeAmount" />
      <el-table-column label="累计优惠金额" align="center" prop="couponAmount" />
      <el-table-column label="订单数量" align="center" prop="orderCount" />
      <el-table-column label="优惠券数量" align="center" prop="couponCount" />
      <el-table-column label="评价数" align="center" prop="commentCount" />
      <el-table-column label="退货数量" align="center" prop="returnOrderCount" />
      <el-table-column label="登录次数" align="center" prop="loginCount" />
      <el-table-column label="关注数量" align="center" prop="attendCount" />
      <el-table-column label="粉丝数量" align="center" prop="fansCount" />
      <el-table-column label="收藏的商品数量" align="center" prop="collectProductCount" />
      <el-table-column label="收藏的专题活动数量" align="center" prop="collectSubjectCount" />
      <el-table-column label="收藏的评论数量" align="center" prop="collectCommentCount" />
      <el-table-column label="邀请的朋友数量" align="center" prop="inviteFriendCount" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ums:info:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ums:info:remove']">删除</el-button>
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

    <!-- 添加或修改会员统计信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="infoRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="会员id" prop="memberId">
              <el-input v-model="form.memberId" placeholder="请输入会员id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="累计消费金额" prop="consumeAmount">
              <el-input v-model="form.consumeAmount" placeholder="请输入累计消费金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="累计优惠金额" prop="couponAmount">
              <el-input v-model="form.couponAmount" placeholder="请输入累计优惠金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单数量" prop="orderCount">
              <el-input v-model="form.orderCount" placeholder="请输入订单数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="优惠券数量" prop="couponCount">
              <el-input v-model="form.couponCount" placeholder="请输入优惠券数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评价数" prop="commentCount">
              <el-input v-model="form.commentCount" placeholder="请输入评价数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退货数量" prop="returnOrderCount">
              <el-input v-model="form.returnOrderCount" placeholder="请输入退货数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="登录次数" prop="loginCount">
              <el-input v-model="form.loginCount" placeholder="请输入登录次数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="关注数量" prop="attendCount">
              <el-input v-model="form.attendCount" placeholder="请输入关注数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="粉丝数量" prop="fansCount">
              <el-input v-model="form.fansCount" placeholder="请输入粉丝数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收藏的商品数量" prop="collectProductCount">
              <el-input v-model="form.collectProductCount" placeholder="请输入收藏的商品数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收藏的专题活动数量" prop="collectSubjectCount">
              <el-input v-model="form.collectSubjectCount" placeholder="请输入收藏的专题活动数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收藏的评论数量" prop="collectCommentCount">
              <el-input v-model="form.collectCommentCount" placeholder="请输入收藏的评论数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邀请的朋友数量" prop="inviteFriendCount">
              <el-input v-model="form.inviteFriendCount" placeholder="请输入邀请的朋友数量" />
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
import { listInfo, getInfo, delInfo, addInfo, updateInfo } from "@/api/ums/info"

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
    memberId: null,
    consumeAmount: null,
    couponAmount: null,
    orderCount: null,
    couponCount: null,
    commentCount: null,
    returnOrderCount: null,
    loginCount: null,
    attendCount: null,
    fansCount: null,
    collectProductCount: null,
    collectSubjectCount: null,
    collectCommentCount: null,
    inviteFriendCount: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询会员统计信息列表 */
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
    id: null,
    memberId: null,
    consumeAmount: null,
    couponAmount: null,
    orderCount: null,
    couponCount: null,
    commentCount: null,
    returnOrderCount: null,
    loginCount: null,
    attendCount: null,
    fansCount: null,
    collectProductCount: null,
    collectSubjectCount: null,
    collectCommentCount: null,
    inviteFriendCount: null
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
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加会员统计信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getInfo(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改会员统计信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["infoRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
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
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除会员统计信息编号为"' + _ids + '"的数据项？').then(function() {
    return delInfo(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('ums/info/export', {
    ...queryParams.value
  }, `info_${new Date().getTime()}.xlsx`)
}

getList()
</script>
