<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="专题名字" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入专题名字"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="专题标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入专题标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="专题副标题" prop="subTitle">
        <el-input
          v-model="queryParams.subTitle"
          placeholder="请输入专题副标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input
          v-model="queryParams.sort"
          placeholder="请输入排序"
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
          v-hasPermi="['sms:subject:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['sms:subject:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['sms:subject:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['sms:subject:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="subjectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="专题名字" align="center" prop="name" />
      <el-table-column label="专题标题" align="center" prop="title" />
      <el-table-column label="专题副标题" align="center" prop="subTitle" />
      <el-table-column label="显示状态" align="center" prop="status" />
      <el-table-column label="详情连接" align="center" prop="url" />
      <el-table-column label="排序" align="center" prop="sort" />
      <el-table-column label="专题图片地址" align="center" prop="img" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sms:subject:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sms:subject:remove']">删除</el-button>
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

    <!-- 添加或修改首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="subjectRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="专题名字" prop="name">
              <el-input v-model="form.name" placeholder="请输入专题名字" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="专题标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入专题标题" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="专题副标题" prop="subTitle">
              <el-input v-model="form.subTitle" placeholder="请输入专题副标题" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详情连接" prop="url">
              <el-input v-model="form.url" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排序" prop="sort">
              <el-input v-model="form.sort" placeholder="请输入排序" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="专题图片地址" prop="img">
              <el-input v-model="form.img" type="textarea" placeholder="请输入内容" />
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

<script setup name="Subject">
import { listSubject, getSubject, delSubject, addSubject, updateSubject } from "@/api/sms/subject"

const { proxy } = getCurrentInstance()

const subjectList = ref([])
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
    title: null,
    subTitle: null,
    status: null,
    url: null,
    sort: null,
    img: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】列表 */
function getList() {
  loading.value = true
  listSubject(queryParams.value).then(response => {
    subjectList.value = response.rows
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
    title: null,
    subTitle: null,
    status: null,
    url: null,
    sort: null,
    img: null
  }
  proxy.resetForm("subjectRef")
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
  title.value = "添加首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getSubject(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["subjectRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateSubject(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addSubject(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除首页专题【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】编号为"' + _ids + '"的数据项？').then(function() {
    return delSubject(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('sms/subject/export', {
    ...queryParams.value
  }, `subject_${new Date().getTime()}.xlsx`)
}

getList()
</script>
