<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="等级名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入等级名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="等级需要的成长值" prop="growthPoint">
        <el-input
          v-model="queryParams.growthPoint"
          placeholder="请输入等级需要的成长值"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="免运费标准" prop="freeFreightPoint">
        <el-input
          v-model="queryParams.freeFreightPoint"
          placeholder="请输入免运费标准"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="每次评价获取的成长值" prop="commentGrowthPoint">
        <el-input
          v-model="queryParams.commentGrowthPoint"
          placeholder="请输入每次评价获取的成长值"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否有免邮特权" prop="priviledgeFreeFreight">
        <el-input
          v-model="queryParams.priviledgeFreeFreight"
          placeholder="请输入是否有免邮特权"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否有会员价格特权" prop="priviledgeMemberPrice">
        <el-input
          v-model="queryParams.priviledgeMemberPrice"
          placeholder="请输入是否有会员价格特权"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否有生日特权" prop="priviledgeBirthday">
        <el-input
          v-model="queryParams.priviledgeBirthday"
          placeholder="请输入是否有生日特权"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="备注" prop="note">
        <el-input
          v-model="queryParams.note"
          placeholder="请输入备注"
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
          v-hasPermi="['ums:level:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['ums:level:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['ums:level:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['ums:level:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="levelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="等级名称" align="center" prop="name" />
      <el-table-column label="等级需要的成长值" align="center" prop="growthPoint" />
      <el-table-column label="是否为默认等级[0->不是；1->是]" align="center" prop="defaultStatus" />
      <el-table-column label="免运费标准" align="center" prop="freeFreightPoint" />
      <el-table-column label="每次评价获取的成长值" align="center" prop="commentGrowthPoint" />
      <el-table-column label="是否有免邮特权" align="center" prop="priviledgeFreeFreight" />
      <el-table-column label="是否有会员价格特权" align="center" prop="priviledgeMemberPrice" />
      <el-table-column label="是否有生日特权" align="center" prop="priviledgeBirthday" />
      <el-table-column label="备注" align="center" prop="note" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ums:level:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ums:level:remove']">删除</el-button>
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

    <!-- 添加或修改会员等级对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="levelRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="等级名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入等级名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="等级需要的成长值" prop="growthPoint">
              <el-input v-model="form.growthPoint" placeholder="请输入等级需要的成长值" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="免运费标准" prop="freeFreightPoint">
              <el-input v-model="form.freeFreightPoint" placeholder="请输入免运费标准" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="每次评价获取的成长值" prop="commentGrowthPoint">
              <el-input v-model="form.commentGrowthPoint" placeholder="请输入每次评价获取的成长值" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否有免邮特权" prop="priviledgeFreeFreight">
              <el-input v-model="form.priviledgeFreeFreight" placeholder="请输入是否有免邮特权" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否有会员价格特权" prop="priviledgeMemberPrice">
              <el-input v-model="form.priviledgeMemberPrice" placeholder="请输入是否有会员价格特权" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否有生日特权" prop="priviledgeBirthday">
              <el-input v-model="form.priviledgeBirthday" placeholder="请输入是否有生日特权" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="note">
              <el-input v-model="form.note" placeholder="请输入备注" />
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

<script setup name="Level">
import { listLevel, getLevel, delLevel, addLevel, updateLevel } from "@/api/ums/level"

const { proxy } = getCurrentInstance()

const levelList = ref([])
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
    name: null,
    growthPoint: null,
    defaultStatus: null,
    freeFreightPoint: null,
    commentGrowthPoint: null,
    priviledgeFreeFreight: null,
    priviledgeMemberPrice: null,
    priviledgeBirthday: null,
    note: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询会员等级列表 */
function getList() {
  loading.value = true
  listLevel(queryParams.value).then(response => {
    levelList.value = response.rows
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
    name: null,
    growthPoint: null,
    defaultStatus: null,
    freeFreightPoint: null,
    commentGrowthPoint: null,
    priviledgeFreeFreight: null,
    priviledgeMemberPrice: null,
    priviledgeBirthday: null,
    note: null
  }
  proxy.resetForm("levelRef")
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
  title.value = "添加会员等级"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getLevel(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改会员等级"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["levelRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateLevel(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addLevel(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除会员等级编号为"' + _ids + '"的数据项？').then(function() {
    return delLevel(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('ums/level/export', {
    ...queryParams.value
  }, `level_${new Date().getTime()}.xlsx`)
}

getList()
</script>
