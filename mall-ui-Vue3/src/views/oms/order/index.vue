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
      <el-form-item label="订单号" prop="orderSn">
        <el-input
          v-model="queryParams.orderSn"
          placeholder="请输入订单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="使用的优惠券" prop="couponId">
        <el-input
          v-model="queryParams.couponId"
          placeholder="请输入使用的优惠券"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户名" prop="memberUsername">
        <el-input
          v-model="queryParams.memberUsername"
          placeholder="请输入用户名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单总额" prop="totalAmount">
        <el-input
          v-model="queryParams.totalAmount"
          placeholder="请输入订单总额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="应付总额" prop="payAmount">
        <el-input
          v-model="queryParams.payAmount"
          placeholder="请输入应付总额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="运费金额" prop="freightAmount">
        <el-input
          v-model="queryParams.freightAmount"
          placeholder="请输入运费金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="促销优化金额" prop="promotionAmount">
        <el-input
          v-model="queryParams.promotionAmount"
          placeholder="请输入促销优化金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="积分抵扣金额" prop="integrationAmount">
        <el-input
          v-model="queryParams.integrationAmount"
          placeholder="请输入积分抵扣金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="优惠券抵扣金额" prop="couponAmount">
        <el-input
          v-model="queryParams.couponAmount"
          placeholder="请输入优惠券抵扣金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="后台调整订单使用的折扣金额" prop="discountAmount">
        <el-input
          v-model="queryParams.discountAmount"
          placeholder="请输入后台调整订单使用的折扣金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="物流公司(配送方式)" prop="deliveryCompany">
        <el-input
          v-model="queryParams.deliveryCompany"
          placeholder="请输入物流公司(配送方式)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="物流单号" prop="deliverySn">
        <el-input
          v-model="queryParams.deliverySn"
          placeholder="请输入物流单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="自动确认时间" prop="autoConfirmDay">
        <el-input
          v-model="queryParams.autoConfirmDay"
          placeholder="请输入自动确认时间"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="可以获得的积分" prop="integration">
        <el-input
          v-model="queryParams.integration"
          placeholder="请输入可以获得的积分"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="可以获得的成长值" prop="growth">
        <el-input
          v-model="queryParams.growth"
          placeholder="请输入可以获得的成长值"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="发票抬头" prop="billHeader">
        <el-input
          v-model="queryParams.billHeader"
          placeholder="请输入发票抬头"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收票人电话" prop="billReceiverPhone">
        <el-input
          v-model="queryParams.billReceiverPhone"
          placeholder="请输入收票人电话"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收票人邮箱" prop="billReceiverEmail">
        <el-input
          v-model="queryParams.billReceiverEmail"
          placeholder="请输入收票人邮箱"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收货人姓名" prop="receiverName">
        <el-input
          v-model="queryParams.receiverName"
          placeholder="请输入收货人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收货人电话" prop="receiverPhone">
        <el-input
          v-model="queryParams.receiverPhone"
          placeholder="请输入收货人电话"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收货人邮编" prop="receiverPostCode">
        <el-input
          v-model="queryParams.receiverPostCode"
          placeholder="请输入收货人邮编"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="省份/直辖市" prop="receiverProvince">
        <el-input
          v-model="queryParams.receiverProvince"
          placeholder="请输入省份/直辖市"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="城市" prop="receiverCity">
        <el-input
          v-model="queryParams.receiverCity"
          placeholder="请输入城市"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="区" prop="receiverRegion">
        <el-input
          v-model="queryParams.receiverRegion"
          placeholder="请输入区"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="详细地址" prop="receiverDetailAddress">
        <el-input
          v-model="queryParams.receiverDetailAddress"
          placeholder="请输入详细地址"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="下单时使用的积分" prop="useIntegration">
        <el-input
          v-model="queryParams.useIntegration"
          placeholder="请输入下单时使用的积分"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付时间" prop="paymentTime">
        <el-date-picker clearable
          v-model="queryParams.paymentTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择支付时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="发货时间" prop="deliveryTime">
        <el-date-picker clearable
          v-model="queryParams.deliveryTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择发货时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="确认收货时间" prop="receiveTime">
        <el-date-picker clearable
          v-model="queryParams.receiveTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择确认收货时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="评价时间" prop="commentTime">
        <el-date-picker clearable
          v-model="queryParams.commentTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择评价时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="修改时间" prop="modifyTime">
        <el-date-picker clearable
          v-model="queryParams.modifyTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择修改时间">
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
          v-hasPermi="['oms:order:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['oms:order:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['oms:order:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['oms:order:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="member_id" align="center" prop="memberId" />
      <el-table-column label="订单号" align="center" prop="orderSn" />
      <el-table-column label="使用的优惠券" align="center" prop="couponId" />
      <el-table-column label="用户名" align="center" prop="memberUsername" />
      <el-table-column label="订单总额" align="center" prop="totalAmount" />
      <el-table-column label="应付总额" align="center" prop="payAmount" />
      <el-table-column label="运费金额" align="center" prop="freightAmount" />
      <el-table-column label="促销优化金额" align="center" prop="promotionAmount" />
      <el-table-column label="积分抵扣金额" align="center" prop="integrationAmount" />
      <el-table-column label="优惠券抵扣金额" align="center" prop="couponAmount" />
      <el-table-column label="后台调整订单使用的折扣金额" align="center" prop="discountAmount" />
      <el-table-column label="支付方式【1->支付宝；2->微信；3->银联； 4->货到付款；】" align="center" prop="payType" />
      <el-table-column label="订单来源[0->PC订单；1->app订单]" align="center" prop="sourceType" />
      <el-table-column label="订单状态【0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单】" align="center" prop="status" />
      <el-table-column label="物流公司(配送方式)" align="center" prop="deliveryCompany" />
      <el-table-column label="物流单号" align="center" prop="deliverySn" />
      <el-table-column label="自动确认时间" align="center" prop="autoConfirmDay" />
      <el-table-column label="可以获得的积分" align="center" prop="integration" />
      <el-table-column label="可以获得的成长值" align="center" prop="growth" />
      <el-table-column label="发票类型[0->不开发票；1->电子发票；2->纸质发票]" align="center" prop="billType" />
      <el-table-column label="发票抬头" align="center" prop="billHeader" />
      <el-table-column label="发票内容" align="center" prop="billContent" />
      <el-table-column label="收票人电话" align="center" prop="billReceiverPhone" />
      <el-table-column label="收票人邮箱" align="center" prop="billReceiverEmail" />
      <el-table-column label="收货人姓名" align="center" prop="receiverName" />
      <el-table-column label="收货人电话" align="center" prop="receiverPhone" />
      <el-table-column label="收货人邮编" align="center" prop="receiverPostCode" />
      <el-table-column label="省份/直辖市" align="center" prop="receiverProvince" />
      <el-table-column label="城市" align="center" prop="receiverCity" />
      <el-table-column label="区" align="center" prop="receiverRegion" />
      <el-table-column label="详细地址" align="center" prop="receiverDetailAddress" />
      <el-table-column label="订单备注" align="center" prop="note" />
      <el-table-column label="确认收货状态[0->未确认；1->已确认]" align="center" prop="confirmStatus" />
      <el-table-column label="删除状态【0->未删除；1->已删除】" align="center" prop="deleteStatus" />
      <el-table-column label="下单时使用的积分" align="center" prop="useIntegration" />
      <el-table-column label="支付时间" align="center" prop="paymentTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.paymentTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发货时间" align="center" prop="deliveryTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.deliveryTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="确认收货时间" align="center" prop="receiveTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.receiveTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="评价时间" align="center" prop="commentTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.commentTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="修改时间" align="center" prop="modifyTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.modifyTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['oms:order:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['oms:order:remove']">删除</el-button>
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

    <!-- 添加或修改订单对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="orderRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="member_id" prop="memberId">
              <el-input v-model="form.memberId" placeholder="请输入member_id" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单号" prop="orderSn">
              <el-input v-model="form.orderSn" placeholder="请输入订单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="使用的优惠券" prop="couponId">
              <el-input v-model="form.couponId" placeholder="请输入使用的优惠券" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="用户名" prop="memberUsername">
              <el-input v-model="form.memberUsername" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单总额" prop="totalAmount">
              <el-input v-model="form.totalAmount" placeholder="请输入订单总额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="应付总额" prop="payAmount">
              <el-input v-model="form.payAmount" placeholder="请输入应付总额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="运费金额" prop="freightAmount">
              <el-input v-model="form.freightAmount" placeholder="请输入运费金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="促销优化金额" prop="promotionAmount">
              <el-input v-model="form.promotionAmount" placeholder="请输入促销优化金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="积分抵扣金额" prop="integrationAmount">
              <el-input v-model="form.integrationAmount" placeholder="请输入积分抵扣金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="优惠券抵扣金额" prop="couponAmount">
              <el-input v-model="form.couponAmount" placeholder="请输入优惠券抵扣金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="后台调整订单使用的折扣金额" prop="discountAmount">
              <el-input v-model="form.discountAmount" placeholder="请输入后台调整订单使用的折扣金额" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="物流公司(配送方式)" prop="deliveryCompany">
              <el-input v-model="form.deliveryCompany" placeholder="请输入物流公司(配送方式)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="物流单号" prop="deliverySn">
              <el-input v-model="form.deliverySn" placeholder="请输入物流单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="自动确认时间" prop="autoConfirmDay">
              <el-input v-model="form.autoConfirmDay" placeholder="请输入自动确认时间" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可以获得的积分" prop="integration">
              <el-input v-model="form.integration" placeholder="请输入可以获得的积分" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可以获得的成长值" prop="growth">
              <el-input v-model="form.growth" placeholder="请输入可以获得的成长值" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发票抬头" prop="billHeader">
              <el-input v-model="form.billHeader" placeholder="请输入发票抬头" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发票内容">
              <editor v-model="form.billContent" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收票人电话" prop="billReceiverPhone">
              <el-input v-model="form.billReceiverPhone" placeholder="请输入收票人电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收票人邮箱" prop="billReceiverEmail">
              <el-input v-model="form.billReceiverEmail" placeholder="请输入收票人邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货人姓名" prop="receiverName">
              <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货人电话" prop="receiverPhone">
              <el-input v-model="form.receiverPhone" placeholder="请输入收货人电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货人邮编" prop="receiverPostCode">
              <el-input v-model="form.receiverPostCode" placeholder="请输入收货人邮编" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="省份/直辖市" prop="receiverProvince">
              <el-input v-model="form.receiverProvince" placeholder="请输入省份/直辖市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="城市" prop="receiverCity">
              <el-input v-model="form.receiverCity" placeholder="请输入城市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="区" prop="receiverRegion">
              <el-input v-model="form.receiverRegion" placeholder="请输入区" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" prop="receiverDetailAddress">
              <el-input v-model="form.receiverDetailAddress" placeholder="请输入详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单备注" prop="note">
              <el-input v-model="form.note" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="下单时使用的积分" prop="useIntegration">
              <el-input v-model="form.useIntegration" placeholder="请输入下单时使用的积分" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="支付时间" prop="paymentTime">
              <el-date-picker clearable
                v-model="form.paymentTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择支付时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发货时间" prop="deliveryTime">
              <el-date-picker clearable
                v-model="form.deliveryTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择发货时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="确认收货时间" prop="receiveTime">
              <el-date-picker clearable
                v-model="form.receiveTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择确认收货时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评价时间" prop="commentTime">
              <el-date-picker clearable
                v-model="form.commentTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择评价时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="修改时间" prop="modifyTime">
              <el-date-picker clearable
                v-model="form.modifyTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择修改时间">
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

<script setup name="Order">
import { listOrder, getOrder, delOrder, addOrder, updateOrder } from "@/api/oms/order"

const { proxy } = getCurrentInstance()

const orderList = ref([])
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
    orderSn: null,
    couponId: null,
    memberUsername: null,
    totalAmount: null,
    payAmount: null,
    freightAmount: null,
    promotionAmount: null,
    integrationAmount: null,
    couponAmount: null,
    discountAmount: null,
    payType: null,
    sourceType: null,
    status: null,
    deliveryCompany: null,
    deliverySn: null,
    autoConfirmDay: null,
    integration: null,
    growth: null,
    billType: null,
    billHeader: null,
    billContent: null,
    billReceiverPhone: null,
    billReceiverEmail: null,
    receiverName: null,
    receiverPhone: null,
    receiverPostCode: null,
    receiverProvince: null,
    receiverCity: null,
    receiverRegion: null,
    receiverDetailAddress: null,
    note: null,
    confirmStatus: null,
    deleteStatus: null,
    useIntegration: null,
    paymentTime: null,
    deliveryTime: null,
    receiveTime: null,
    commentTime: null,
    modifyTime: null
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询订单列表 */
function getList() {
  loading.value = true
  listOrder(queryParams.value).then(response => {
    orderList.value = response.rows
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
    orderSn: null,
    couponId: null,
    createTime: null,
    memberUsername: null,
    totalAmount: null,
    payAmount: null,
    freightAmount: null,
    promotionAmount: null,
    integrationAmount: null,
    couponAmount: null,
    discountAmount: null,
    payType: null,
    sourceType: null,
    status: null,
    deliveryCompany: null,
    deliverySn: null,
    autoConfirmDay: null,
    integration: null,
    growth: null,
    billType: null,
    billHeader: null,
    billContent: null,
    billReceiverPhone: null,
    billReceiverEmail: null,
    receiverName: null,
    receiverPhone: null,
    receiverPostCode: null,
    receiverProvince: null,
    receiverCity: null,
    receiverRegion: null,
    receiverDetailAddress: null,
    note: null,
    confirmStatus: null,
    deleteStatus: null,
    useIntegration: null,
    paymentTime: null,
    deliveryTime: null,
    receiveTime: null,
    commentTime: null,
    modifyTime: null
  }
  proxy.resetForm("orderRef")
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
  title.value = "添加订单"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getOrder(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改订单"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["orderRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateOrder(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addOrder(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除订单编号为"' + _ids + '"的数据项？').then(function() {
    return delOrder(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('oms/order/export', {
    ...queryParams.value
  }, `order_${new Date().getTime()}.xlsx`)
}

getList()
</script>
