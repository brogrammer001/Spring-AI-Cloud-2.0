<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="会员等级id" prop="levelId">
        <el-input
          v-model="queryParams.levelId"
          placeholder="请输入会员等级id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="queryParams.password"
          placeholder="请输入密码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input
          v-model="queryParams.nickname"
          placeholder="请输入昵称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号码" prop="mobile">
        <el-input
          v-model="queryParams.mobile"
          placeholder="请输入手机号码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="queryParams.email"
          placeholder="请输入邮箱"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="性别" prop="gender">
        <el-input
          v-model="queryParams.gender"
          placeholder="请输入性别"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生日" prop="birth">
        <el-date-picker clearable
          v-model="queryParams.birth"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择生日">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="职业" prop="job">
        <el-input
          v-model="queryParams.job"
          placeholder="请输入职业"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="个性签名" prop="sign">
        <el-input
          v-model="queryParams.sign"
          placeholder="请输入个性签名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="积分" prop="integration">
        <el-input
          v-model="queryParams.integration"
          placeholder="请输入积分"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="成长值" prop="growth">
        <el-input
          v-model="queryParams.growth"
          placeholder="请输入成长值"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="社交用户的唯一id" prop="socialUid">
        <el-input
          v-model="queryParams.socialUid"
          placeholder="请输入社交用户的唯一id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="访问令牌" prop="accessToken">
        <el-input
          v-model="queryParams.accessToken"
          placeholder="请输入访问令牌"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="访问令牌的时间" prop="expiresIn">
        <el-input
          v-model="queryParams.expiresIn"
          placeholder="请输入访问令牌的时间"
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
          v-hasPermi="['ums:member:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['ums:member:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['ums:member:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['ums:member:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="memberList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="会员等级id" align="center" prop="levelId" />
      <el-table-column label="用户名" align="center" prop="username" />
      <el-table-column label="密码" align="center" prop="password" />
      <el-table-column label="昵称" align="center" prop="nickname" />
      <el-table-column label="手机号码" align="center" prop="mobile" />
      <el-table-column label="邮箱" align="center" prop="email" />
      <el-table-column label="头像" align="center" prop="header" />
      <el-table-column label="性别" align="center" prop="gender" />
      <el-table-column label="生日" align="center" prop="birth" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.birth, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="所在城市" align="center" prop="city" />
      <el-table-column label="职业" align="center" prop="job" />
      <el-table-column label="个性签名" align="center" prop="sign" />
      <el-table-column label="用户来源" align="center" prop="sourceType" />
      <el-table-column label="积分" align="center" prop="integration" />
      <el-table-column label="成长值" align="center" prop="growth" />
      <el-table-column label="启用状态" align="center" prop="status" />
      <el-table-column label="社交用户的唯一id" align="center" prop="socialUid" />
      <el-table-column label="访问令牌" align="center" prop="accessToken" />
      <el-table-column label="访问令牌的时间" align="center" prop="expiresIn" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ums:member:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ums:member:remove']">删除</el-button>
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

    <!-- 添加或修改会员对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="memberRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="会员等级id" prop="levelId">
              <el-input v-model="form.levelId" placeholder="请输入会员等级id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" placeholder="请输入密码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="手机号码" prop="mobile">
              <el-input v-model="form.mobile" placeholder="请输入手机号码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="头像" prop="header">
              <el-input v-model="form.header" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="性别" prop="gender">
              <el-input v-model="form.gender" placeholder="请输入性别" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="生日" prop="birth">
              <el-date-picker clearable
                v-model="form.birth"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择生日">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所在城市" prop="city">
              <el-input v-model="form.city" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="职业" prop="job">
              <el-input v-model="form.job" placeholder="请输入职业" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="个性签名" prop="sign">
              <el-input v-model="form.sign" placeholder="请输入个性签名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="积分" prop="integration">
              <el-input v-model="form.integration" placeholder="请输入积分" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="成长值" prop="growth">
              <el-input v-model="form.growth" placeholder="请输入成长值" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="社交用户的唯一id" prop="socialUid">
              <el-input v-model="form.socialUid" placeholder="请输入社交用户的唯一id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="访问令牌" prop="accessToken">
              <el-input v-model="form.accessToken" placeholder="请输入访问令牌" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="访问令牌的时间" prop="expiresIn">
              <el-input v-model="form.expiresIn" placeholder="请输入访问令牌的时间" />
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

<script setup name="Member">
import { listMember, getMember, delMember, addMember, updateMember } from "@/api/ums/member"

const { proxy } = getCurrentInstance()

const memberList = ref([])
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
    levelId: null,
    username: null,
    password: null,
    nickname: null,
    mobile: null,
    email: null,
    header: null,
    gender: null,
    birth: null,
    city: null,
    job: null,
    sign: null,
    sourceType: null,
    integration: null,
    growth: null,
    status: null,
    socialUid: null,
    accessToken: null,
    expiresIn: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询会员列表 */
function getList() {
  loading.value = true
  listMember(queryParams.value).then(response => {
    memberList.value = response.rows
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
    levelId: null,
    username: null,
    password: null,
    nickname: null,
    mobile: null,
    email: null,
    header: null,
    gender: null,
    birth: null,
    city: null,
    job: null,
    sign: null,
    sourceType: null,
    integration: null,
    growth: null,
    status: null,
    createTime: null,
    socialUid: null,
    accessToken: null,
    expiresIn: null
  }
  proxy.resetForm("memberRef")
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
  title.value = "添加会员"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getMember(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改会员"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["memberRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateMember(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addMember(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除会员编号为"' + _ids + '"的数据项？').then(function() {
    return delMember(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('ums/member/export', {
    ...queryParams.value
  }, `member_${new Date().getTime()}.xlsx`)
}

getList()
</script>
