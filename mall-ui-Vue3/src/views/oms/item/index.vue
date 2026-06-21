<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="order_id" prop="orderId">
        <el-input
          v-model="queryParams.orderId"
          placeholder="请输入order_id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="order_sn" prop="orderSn">
        <el-input
          v-model="queryParams.orderSn"
          placeholder="请输入order_sn"
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
      <el-form-item label="spu_name" prop="spuName">
        <el-input
          v-model="queryParams.spuName"
          placeholder="请输入spu_name"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="品牌" prop="spuBrand">
        <el-input
          v-model="queryParams.spuBrand"
          placeholder="请输入品牌"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品分类id" prop="categoryId">
        <el-input
          v-model="queryParams.categoryId"
          placeholder="请输入商品分类id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品sku编号" prop="skuId">
        <el-input
          v-model="queryParams.skuId"
          placeholder="请输入商品sku编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品sku名字" prop="skuName">
        <el-input
          v-model="queryParams.skuName"
          placeholder="请输入商品sku名字"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品sku价格" prop="skuPrice">
        <el-input
          v-model="queryParams.skuPrice"
          placeholder="请输入商品sku价格"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品购买的数量" prop="skuQuantity">
        <el-input
          v-model="queryParams.skuQuantity"
          placeholder="请输入商品购买的数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品促销分解金额" prop="promotionAmount">
        <el-input
          v-model="queryParams.promotionAmount"
          placeholder="请输入商品促销分解金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="优惠券优惠分解金额" prop="couponAmount">
        <el-input
          v-model="queryParams.couponAmount"
          placeholder="请输入优惠券优惠分解金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="积分优惠分解金额" prop="integrationAmount">
        <el-input
          v-model="queryParams.integrationAmount"
          placeholder="请输入积分优惠分解金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="该商品经过优惠后的分解金额" prop="realAmount">
        <el-input
          v-model="queryParams.realAmount"
          placeholder="请输入该商品经过优惠后的分解金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="赠送积分" prop="giftIntegration">
        <el-input
          v-model="queryParams.giftIntegration"
          placeholder="请输入赠送积分"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="赠送成长值" prop="giftGrowth">
        <el-input
          v-model="queryParams.giftGrowth"
          placeholder="请输入赠送成长值"
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
          v-hasPermi="['oms:item:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['oms:item:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['oms:item:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['oms:item:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="itemList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="order_id" align="center" prop="orderId" />
      <el-table-column label="order_sn" align="center" prop="orderSn" />
      <el-table-column label="spu_id" align="center" prop="spuId" />
      <el-table-column label="spu_name" align="center" prop="spuName" />
      <el-table-column label="spu_pic" align="center" prop="spuPic" />
      <el-table-column label="品牌" align="center" prop="spuBrand" />
      <el-table-column label="商品分类id" align="center" prop="categoryId" />
      <el-table-column label="商品sku编号" align="center" prop="skuId" />
      <el-table-column label="商品sku名字" align="center" prop="skuName" />
      <el-table-column label="商品sku图片" align="center" prop="skuPic" />
      <el-table-column label="商品sku价格" align="center" prop="skuPrice" />
      <el-table-column label="商品购买的数量" align="center" prop="skuQuantity" />
      <el-table-column label="商品销售属性组合" align="center" prop="skuAttrsVals" />
      <el-table-column label="商品促销分解金额" align="center" prop="promotionAmount" />
      <el-table-column label="优惠券优惠分解金额" align="center" prop="couponAmount" />
      <el-table-column label="积分优惠分解金额" align="center" prop="integrationAmount" />
      <el-table-column label="该商品经过优惠后的分解金额" align="center" prop="realAmount" />
      <el-table-column label="赠送积分" align="center" prop="giftIntegration" />
      <el-table-column label="赠送成长值" align="center" prop="giftGrowth" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['oms:item:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['oms:item:remove']">删除</el-button>
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

    <!-- 添加或修改订单项信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="itemRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="order_id" prop="orderId">
              <el-input v-model="form.orderId" placeholder="请输入order_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="order_sn" prop="orderSn">
              <el-input v-model="form.orderSn" placeholder="请输入order_sn" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="spu_id" prop="spuId">
              <el-input v-model="form.spuId" placeholder="请输入spu_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="spu_name" prop="spuName">
              <el-input v-model="form.spuName" placeholder="请输入spu_name" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="spu_pic" prop="spuPic">
              <el-input v-model="form.spuPic" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="品牌" prop="spuBrand">
              <el-input v-model="form.spuBrand" placeholder="请输入品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品分类id" prop="categoryId">
              <el-input v-model="form.categoryId" placeholder="请输入商品分类id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品sku编号" prop="skuId">
              <el-input v-model="form.skuId" placeholder="请输入商品sku编号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品sku名字" prop="skuName">
              <el-input v-model="form.skuName" placeholder="请输入商品sku名字" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品sku图片" prop="skuPic">
              <el-input v-model="form.skuPic" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品sku价格" prop="skuPrice">
              <el-input v-model="form.skuPrice" placeholder="请输入商品sku价格" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品购买的数量" prop="skuQuantity">
              <el-input v-model="form.skuQuantity" placeholder="请输入商品购买的数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品销售属性组合" prop="skuAttrsVals">
              <el-input v-model="form.skuAttrsVals" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品促销分解金额" prop="promotionAmount">
              <el-input v-model="form.promotionAmount" placeholder="请输入商品促销分解金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="优惠券优惠分解金额" prop="couponAmount">
              <el-input v-model="form.couponAmount" placeholder="请输入优惠券优惠分解金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="积分优惠分解金额" prop="integrationAmount">
              <el-input v-model="form.integrationAmount" placeholder="请输入积分优惠分解金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="该商品经过优惠后的分解金额" prop="realAmount">
              <el-input v-model="form.realAmount" placeholder="请输入该商品经过优惠后的分解金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="赠送积分" prop="giftIntegration">
              <el-input v-model="form.giftIntegration" placeholder="请输入赠送积分" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="赠送成长值" prop="giftGrowth">
              <el-input v-model="form.giftGrowth" placeholder="请输入赠送成长值" />
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

<script setup name="Item">
import { listItem, getItem, delItem, addItem, updateItem } from "@/api/oms/item"

const { proxy } = getCurrentInstance()

const itemList = ref([])
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
    orderId: null,
    orderSn: null,
    spuId: null,
    spuName: null,
    spuPic: null,
    spuBrand: null,
    categoryId: null,
    skuId: null,
    skuName: null,
    skuPic: null,
    skuPrice: null,
    skuQuantity: null,
    skuAttrsVals: null,
    promotionAmount: null,
    couponAmount: null,
    integrationAmount: null,
    realAmount: null,
    giftIntegration: null,
    giftGrowth: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询订单项信息列表 */
function getList() {
  loading.value = true
  listItem(queryParams.value).then(response => {
    itemList.value = response.rows
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
    orderId: null,
    orderSn: null,
    spuId: null,
    spuName: null,
    spuPic: null,
    spuBrand: null,
    categoryId: null,
    skuId: null,
    skuName: null,
    skuPic: null,
    skuPrice: null,
    skuQuantity: null,
    skuAttrsVals: null,
    promotionAmount: null,
    couponAmount: null,
    integrationAmount: null,
    realAmount: null,
    giftIntegration: null,
    giftGrowth: null
  }
  proxy.resetForm("itemRef")
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
  title.value = "添加订单项信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getItem(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改订单项信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["itemRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateItem(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addItem(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除订单项信息编号为"' + _ids + '"的数据项？').then(function() {
    return delItem(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('oms/item/export', {
    ...queryParams.value
  }, `item_${new Date().getTime()}.xlsx`)
}

getList()
</script>
