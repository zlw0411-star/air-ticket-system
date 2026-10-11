<template>
  <div class="page-wrap">
    <h2>我的订单</h2>
    <el-table :data="list">
      <el-table-column prop="order.orderNo" label="订单号" />
      <el-table-column label="状态"><template #default="{ row }">{{ statusText(row.order.status) }}</template></el-table-column>
      <el-table-column prop="order.totalAmount" label="金额" />
      <el-table-column prop="order.createdAt" label="下单时间" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push('/orders/' + row.order.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api/http'
const list = ref([])
function statusText(status) {
  return ({ PENDING_PAYMENT: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退票' })[status] || status
}
onMounted(async () => {
  const data = await http.get('/orders', { params: { page: 1, size: 50 } })
  list.value = data.records
})
</script>
