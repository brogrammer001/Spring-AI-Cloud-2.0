<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px" style="margin-top: 20px;">
      <el-form-item label="知识库名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入知识库名称"
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
          v-hasPermi="['aichat:base:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single || hasActiveSelected"
          @click="handleUpdate"
          v-hasPermi="['aichat:base:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple || hasActiveSelected"
          @click="handleDelete"
          v-hasPermi="['aichat:base:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['aichat:base:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="baseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="知识库名称" align="center" prop="name" />
      <el-table-column label="知识库描述" align="center" prop="description" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
            v-hasPermi="['aichat:base:edit']"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="FolderOpen" @click="handleDocuments(scope.row)" v-hasPermi="['aichat:document:list']">文档管理</el-button>
          <el-tooltip :disabled="scope.row.status !== '0'" content="启用状态不允许修改" placement="top">
            <el-button link type="primary" icon="Edit" :disabled="scope.row.status === '0'" @click="handleUpdate(scope.row)" v-hasPermi="['aichat:base:edit']">修改</el-button>
          </el-tooltip>
          <el-tooltip :disabled="scope.row.status !== '0'" content="启用状态不允许删除" placement="top">
            <el-button link type="primary" icon="Delete" :disabled="scope.row.status === '0'" @click="handleDelete(scope.row)" v-hasPermi="['aichat:base:remove']">删除</el-button>
          </el-tooltip>
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

    <!-- 添加或修改知识库对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="baseRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="知识库名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入知识库名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="知识库描述" prop="description">
              <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <!-- <el-col :span="24">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                    v-for="dict in sys_normal_disable"
                    :key="dict.value"
                    :value="dict.value"
                >{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col> -->
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

<script setup name="Base">
import { listBase, getBase, delBase, addBase, updateBase } from "@/api/ai/chatrag/base"

const { proxy } = getCurrentInstance()
const router = useRouter()

const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

const baseList = ref([])
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
    name: undefined,
    description: undefined,
    status: undefined,
  },
  rules: {
    name: [
      { required: true, message: "知识库名称不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

const hasActiveSelected = computed(() => {
  return ids.value.some(id => {
    const item = baseList.value.find(item => item.id === id)
    return item && item.status === '0'
  })
})

/** 查询知识库列表 */
function getList() {
  loading.value = true
  listBase(queryParams.value).then(response => {
    baseList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    name: null,
    description: null,
    status: '0',
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("baseRef")
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

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加知识库"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  if (row && row.status === '0') {
    proxy.$modal.msgError("启用状态的知识库不允许修改")
    return
  }
  reset()
  const _id = row.id || ids.value
  getBase(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改知识库"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["baseRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateBase(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addBase(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 文档管理按钮操作 */
function handleDocuments(row) {
  router.push({ path: '/ai/rag/document/index', query: { knowledgeId: row.id, knowledgeName: row.name } })
}

/** 删除按钮操作 */
function handleDelete(row) {
  if (row) {
    if (row.status === '0') {
      proxy.$modal.msgError("启用状态的知识库不允许删除")
      return
    }
  } else {
    const activeItems = ids.value.filter(id => {
      const item = baseList.value.find(item => item.id === id)
      return item && item.status === '0'
    })
    if (activeItems.length > 0) {
      proxy.$modal.msgError("选中的知识库中包含启用状态的，不允许删除")
      return
    }
  }
  const _ids = row.id || ids.value
  const names = row ? row.name : ids.value.map(id => baseList.value.find(item => item.id === id)?.name).filter(Boolean).join('、')
  proxy.$modal.confirm('是否确认删除知识库"' + (names || _ids) + '"？').then(function() {
    return delBase(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 状态修改 */
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用"
  proxy.$modal.confirm('确认要"' + text + '"知识库"' + row.name + '"吗?').then(function () {
    return updateBase(row)
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功")
  }).catch(function () {
    row.status = row.status === "0" ? "1" : "0"
  })
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('aichat/base/export', {
    ...queryParams.value
  }, `base_${new Date().getTime()}.xlsx`)
}

getList()
</script>
