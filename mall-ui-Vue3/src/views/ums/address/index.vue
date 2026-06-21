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
      <el-form-item label="收货人姓名" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入收货人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="电话" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入电话"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邮政编码" prop="postCode">
        <el-input
          v-model="queryParams.postCode"
          placeholder="请输入邮政编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="省份/直辖市" prop="province">
        <el-input
          v-model="queryParams.province"
          placeholder="请输入省份/直辖市"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="城市" prop="city">
        <el-input
          v-model="queryParams.city"
          placeholder="请输入城市"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="区" prop="region">
        <el-input
          v-model="queryParams.region"
          placeholder="请输入区"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="详细地址(街道)" prop="detailAddress">
        <el-input
          v-model="queryParams.detailAddress"
          placeholder="请输入详细地址(街道)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="省市区代码" prop="areacode">
        <el-input
          v-model="queryParams.areacode"
          placeholder="请输入省市区代码"
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
          v-hasPermi="['ums:address:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['ums:address:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['ums:address:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['ums:address:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="addressList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="member_id" align="center" prop="memberId" />
      <el-table-column label="收货人姓名" align="center" prop="name" />
      <el-table-column label="电话" align="center" prop="phone" />
      <el-table-column label="邮政编码" align="center" prop="postCode" />
      <el-table-column label="省份/直辖市" align="center" prop="province" />
      <el-table-column label="城市" align="center" prop="city" />
      <el-table-column label="区" align="center" prop="region" />
      <el-table-column label="详细地址(街道)" align="center" prop="detailAddress" />
      <el-table-column label="省市区代码" align="center" prop="areacode" />
      <el-table-column label="是否默认" align="center" prop="defaultStatus" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ums:address:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ums:address:remove']">删除</el-button>
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

    <!-- 添加或修改会员收货地址对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="addressRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="member_id" prop="memberId">
              <el-input v-model="form.memberId" placeholder="请输入member_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货人姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入收货人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮政编码" prop="postCode">
              <el-input v-model="form.postCode" placeholder="请输入邮政编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="省份/直辖市" prop="province">
              <el-input v-model="form.province" placeholder="请输入省份/直辖市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="城市" prop="city">
              <el-input v-model="form.city" placeholder="请输入城市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="区" prop="region">
              <el-input v-model="form.region" placeholder="请输入区" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址(街道)" prop="detailAddress">
              <el-input v-model="form.detailAddress" placeholder="请输入详细地址(街道)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="省市区代码" prop="areacode">
              <el-input v-model="form.areacode" placeholder="请输入省市区代码" />
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

<script setup name="Address">
import { listAddress, getAddress, delAddress, addAddress, updateAddress } from "@/api/ums/address"

const { proxy } = getCurrentInstance()

const addressList = ref([])
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
    name: null,
    phone: null,
    postCode: null,
    province: null,
    city: null,
    region: null,
    detailAddress: null,
    areacode: null,
    defaultStatus: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询会员收货地址列表 */
function getList() {
  loading.value = true
  listAddress(queryParams.value).then(response => {
    addressList.value = response.rows
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
    name: null,
    phone: null,
    postCode: null,
    province: null,
    city: null,
    region: null,
    detailAddress: null,
    areacode: null,
    defaultStatus: null
  }
  proxy.resetForm("addressRef")
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
  title.value = "添加会员收货地址"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getAddress(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改会员收货地址"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["addressRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAddress(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAddress(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除会员收货地址编号为"' + _ids + '"的数据项？').then(function() {
    return delAddress(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('ums/address/export', {
    ...queryParams.value
  }, `address_${new Date().getTime()}.xlsx`)
}

getList()
</script>
