<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="sku_id" prop="skuId">
        <el-input
          v-model="queryParams.skuId"
          placeholder="请输入sku_id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="spu_id" prop="spuId">
        <el-input
          v-model="queryParams.spuId"
          placeholder="请输入spu_id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品名字" prop="spuName">
        <el-input
          v-model="queryParams.spuName"
          placeholder="请输入商品名字"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员昵称" prop="memberNickName">
        <el-input
          v-model="queryParams.memberNickName"
          placeholder="请输入会员昵称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="星级" prop="star">
        <el-input
          v-model="queryParams.star"
          placeholder="请输入星级"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员ip" prop="memberIp">
        <el-input
          v-model="queryParams.memberIp"
          placeholder="请输入会员ip"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="购买时属性组合" prop="spuAttributes">
        <el-input
          v-model="queryParams.spuAttributes"
          placeholder="请输入购买时属性组合"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="点赞数" prop="likesCount">
        <el-input
          v-model="queryParams.likesCount"
          placeholder="请输入点赞数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="回复数" prop="replyCount">
        <el-input
          v-model="queryParams.replyCount"
          placeholder="请输入回复数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户头像" prop="memberIcon">
        <el-input
          v-model="queryParams.memberIcon"
          placeholder="请输入用户头像"
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
          v-hasPermi="['pms:comment:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['pms:comment:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['pms:comment:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['pms:comment:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="commentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="sku_id" align="center" prop="skuId" />
      <el-table-column label="spu_id" align="center" prop="spuId" />
      <el-table-column label="商品名字" align="center" prop="spuName" />
      <el-table-column label="会员昵称" align="center" prop="memberNickName" />
      <el-table-column label="星级" align="center" prop="star" />
      <el-table-column label="会员ip" align="center" prop="memberIp" />
      <el-table-column label="显示状态[0-不显示，1-显示]" align="center" prop="showStatus" />
      <el-table-column label="购买时属性组合" align="center" prop="spuAttributes" />
      <el-table-column label="点赞数" align="center" prop="likesCount" />
      <el-table-column label="回复数" align="center" prop="replyCount" />
      <el-table-column label="评论图片/视频[json数据；[{type:文件类型,url:资源路径}]]" align="center" prop="resources" />
      <el-table-column label="内容" align="center" prop="content" />
      <el-table-column label="用户头像" align="center" prop="memberIcon" />
      <el-table-column label="评论类型[0 - 对商品的直接评论，1 - 对评论的回复]" align="center" prop="commentType" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['pms:comment:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['pms:comment:remove']">删除</el-button>
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

    <!-- 添加或修改商品评价对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="commentRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="sku_id" prop="skuId">
              <el-input v-model="form.skuId" placeholder="请输入sku_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="spu_id" prop="spuId">
              <el-input v-model="form.spuId" placeholder="请输入spu_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品名字" prop="spuName">
              <el-input v-model="form.spuName" placeholder="请输入商品名字" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会员昵称" prop="memberNickName">
              <el-input v-model="form.memberNickName" placeholder="请输入会员昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="星级" prop="star">
              <el-input v-model="form.star" placeholder="请输入星级" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会员ip" prop="memberIp">
              <el-input v-model="form.memberIp" placeholder="请输入会员ip" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="购买时属性组合" prop="spuAttributes">
              <el-input v-model="form.spuAttributes" placeholder="请输入购买时属性组合" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="点赞数" prop="likesCount">
              <el-input v-model="form.likesCount" placeholder="请输入点赞数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="回复数" prop="replyCount">
              <el-input v-model="form.replyCount" placeholder="请输入回复数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评论图片/视频[json数据；[{type:文件类型,url:资源路径}]]" prop="resources">
              <el-input v-model="form.resources" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="内容">
              <editor v-model="form.content" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="用户头像" prop="memberIcon">
              <el-input v-model="form.memberIcon" placeholder="请输入用户头像" />
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

<script setup name="Comment">
import { listComment, getComment, delComment, addComment, updateComment } from "@/api/pms/comment"

const { proxy } = getCurrentInstance()

const commentList = ref([])
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
    skuId: null,
    spuId: null,
    spuName: null,
    memberNickName: null,
    star: null,
    memberIp: null,
    showStatus: null,
    spuAttributes: null,
    likesCount: null,
    replyCount: null,
    resources: null,
    content: null,
    memberIcon: null,
    commentType: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询商品评价列表 */
function getList() {
  loading.value = true
  listComment(queryParams.value).then(response => {
    commentList.value = response.rows
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
    skuId: null,
    spuId: null,
    spuName: null,
    memberNickName: null,
    star: null,
    memberIp: null,
    createTime: null,
    showStatus: null,
    spuAttributes: null,
    likesCount: null,
    replyCount: null,
    resources: null,
    content: null,
    memberIcon: null,
    commentType: null
  }
  proxy.resetForm("commentRef")
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
  title.value = "添加商品评价"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getComment(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改商品评价"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["commentRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateComment(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addComment(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除商品评价编号为"' + _ids + '"的数据项？').then(function() {
    return delComment(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('pms/comment/export', {
    ...queryParams.value
  }, `comment_${new Date().getTime()}.xlsx`)
}

getList()
</script>
