<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="秒杀订单超时关闭时间(分)" prop="flashOrderOvertime">
        <el-input
          v-model="queryParams.flashOrderOvertime"
          placeholder="请输入秒杀订单超时关闭时间(分)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="正常订单超时时间(分)" prop="normalOrderOvertime">
        <el-input
          v-model="queryParams.normalOrderOvertime"
          placeholder="请输入正常订单超时时间(分)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="发货后自动确认收货时间" prop="confirmOvertime">
        <el-input
          v-model="queryParams.confirmOvertime"
          placeholder="请输入发货后自动确认收货时间"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="自动完成交易时间，不能申请退货" prop="finishOvertime">
        <el-input
          v-model="queryParams.finishOvertime"
          placeholder="请输入自动完成交易时间，不能申请退货"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单完成后自动好评时间" prop="commentOvertime">
        <el-input
          v-model="queryParams.commentOvertime"
          placeholder="请输入订单完成后自动好评时间"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员等级【0-不限会员等级，全部通用；其他-对应的其他会员等级】" prop="memberLevel">
        <el-input
          v-model="queryParams.memberLevel"
          placeholder="请输入会员等级【0-不限会员等级，全部通用；其他-对应的其他会员等级】"
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
          v-hasPermi="['oms:setting:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['oms:setting:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['oms:setting:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['oms:setting:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="settingList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="秒杀订单超时关闭时间(分)" align="center" prop="flashOrderOvertime" />
      <el-table-column label="正常订单超时时间(分)" align="center" prop="normalOrderOvertime" />
      <el-table-column label="发货后自动确认收货时间" align="center" prop="confirmOvertime" />
      <el-table-column label="自动完成交易时间，不能申请退货" align="center" prop="finishOvertime" />
      <el-table-column label="订单完成后自动好评时间" align="center" prop="commentOvertime" />
      <el-table-column label="会员等级【0-不限会员等级，全部通用；其他-对应的其他会员等级】" align="center" prop="memberLevel" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['oms:setting:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['oms:setting:remove']">删除</el-button>
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

    <!-- 添加或修改订单配置信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="settingRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="秒杀订单超时关闭时间(分)" prop="flashOrderOvertime">
              <el-input v-model="form.flashOrderOvertime" placeholder="请输入秒杀订单超时关闭时间(分)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正常订单超时时间(分)" prop="normalOrderOvertime">
              <el-input v-model="form.normalOrderOvertime" placeholder="请输入正常订单超时时间(分)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发货后自动确认收货时间" prop="confirmOvertime">
              <el-input v-model="form.confirmOvertime" placeholder="请输入发货后自动确认收货时间" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="自动完成交易时间，不能申请退货" prop="finishOvertime">
              <el-input v-model="form.finishOvertime" placeholder="请输入自动完成交易时间，不能申请退货" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单完成后自动好评时间" prop="commentOvertime">
              <el-input v-model="form.commentOvertime" placeholder="请输入订单完成后自动好评时间" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会员等级【0-不限会员等级，全部通用；其他-对应的其他会员等级】" prop="memberLevel">
              <el-input v-model="form.memberLevel" placeholder="请输入会员等级【0-不限会员等级，全部通用；其他-对应的其他会员等级】" />
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

<script setup name="Setting">
import { listSetting, getSetting, delSetting, addSetting, updateSetting } from "@/api/oms/setting"

const { proxy } = getCurrentInstance()

const settingList = ref([])
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
    flashOrderOvertime: null,
    normalOrderOvertime: null,
    confirmOvertime: null,
    finishOvertime: null,
    commentOvertime: null,
    memberLevel: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询订单配置信息列表 */
function getList() {
  loading.value = true
  listSetting(queryParams.value).then(response => {
    settingList.value = response.rows
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
    flashOrderOvertime: null,
    normalOrderOvertime: null,
    confirmOvertime: null,
    finishOvertime: null,
    commentOvertime: null,
    memberLevel: null
  }
  proxy.resetForm("settingRef")
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
  title.value = "添加订单配置信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getSetting(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改订单配置信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["settingRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateSetting(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addSetting(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除订单配置信息编号为"' + _ids + '"的数据项？').then(function() {
    return delSetting(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('oms/setting/export', {
    ...queryParams.value
  }, `setting_${new Date().getTime()}.xlsx`)
}

getList()
</script>
