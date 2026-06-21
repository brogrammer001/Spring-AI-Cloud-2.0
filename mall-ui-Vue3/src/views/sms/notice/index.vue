<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="member_id" prop="memberId">
        <el-input
          v-model="queryParams.memberId"
          placeholder="请输入member_id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="sku_id" prop="skuId">
        <el-input
          v-model="queryParams.skuId"
          placeholder="请输入sku_id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="活动场次id" prop="sessionId">
        <el-input
          v-model="queryParams.sessionId"
          placeholder="请输入活动场次id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订阅时间" prop="subcribeTime">
        <el-date-picker clearable
          v-model="queryParams.subcribeTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择订阅时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="发送时间" prop="sendTime">
        <el-date-picker clearable
          v-model="queryParams.sendTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择发送时间">
        </el-date-picker>
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
          v-hasPermi="['sms:notice:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['sms:notice:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['sms:notice:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['sms:notice:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="member_id" align="center" prop="memberId" />
      <el-table-column label="sku_id" align="center" prop="skuId" />
      <el-table-column label="活动场次id" align="center" prop="sessionId" />
      <el-table-column label="订阅时间" align="center" prop="subcribeTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.subcribeTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发送时间" align="center" prop="sendTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.sendTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="通知方式[0-短信，1-邮件]" align="center" prop="noticeType" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sms:notice:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sms:notice:remove']">删除</el-button>
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

    <!-- 添加或修改秒杀商品通知订阅对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="noticeRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="member_id" prop="memberId">
              <el-input v-model="form.memberId" placeholder="请输入member_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="sku_id" prop="skuId">
              <el-input v-model="form.skuId" placeholder="请输入sku_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动场次id" prop="sessionId">
              <el-input v-model="form.sessionId" placeholder="请输入活动场次id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订阅时间" prop="subcribeTime">
              <el-date-picker clearable
                v-model="form.subcribeTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择订阅时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发送时间" prop="sendTime">
              <el-date-picker clearable
                v-model="form.sendTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择发送时间">
              </el-date-picker>
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

<script setup name="Notice">
import { listNotice, getNotice, delNotice, addNotice, updateNotice } from "@/api/sms/notice"

const { proxy } = getCurrentInstance()

const noticeList = ref([])
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
    skuId: null,
    sessionId: null,
    subcribeTime: null,
    sendTime: null,
    noticeType: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询秒杀商品通知订阅列表 */
function getList() {
  loading.value = true
  listNotice(queryParams.value).then(response => {
    noticeList.value = response.rows
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
    skuId: null,
    sessionId: null,
    subcribeTime: null,
    sendTime: null,
    noticeType: null
  }
  proxy.resetForm("noticeRef")
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
  title.value = "添加秒杀商品通知订阅"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getNotice(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改秒杀商品通知订阅"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["noticeRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateNotice(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addNotice(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除秒杀商品通知订阅编号为"' + _ids + '"的数据项？').then(function() {
    return delNotice(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('sms/notice/export', {
    ...queryParams.value
  }, `notice_${new Date().getTime()}.xlsx`)
}

getList()
</script>
