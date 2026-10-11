<template>
  <div>
    <el-select v-model="status" clearable placeholder="订单状态" @change="load" style="width:180px;margin-bottom:12px">
      <el-option label="待支付" value="PENDING_PAYMENT" />
      <el-option label="已支付" value="PAID" />
      <el-option label="已取消" value="CANCELLED" />
      <el-option label="已退票" value="REFUNDED" />
    </el-select>
    <el-table :data="list">
      <el-table-column prop="order.orderNo" label="订单号" />
      <el-table-column label="状态"><template #default="{ row }">{{ statusText(row.order.status) }}</template></el-table-column>
      <el-table-column prop="order.totalAmount" label="金额" />
      <el-table-column prop="order.createdAt" label="时间" />
      <el-table-column label="机票数"><template #default="{ row }">{{ row.tickets.length }}</template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="10" @current-change="load" />
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api/http'
const list = ref([])
const total = ref(0)
const page = ref(1)
const status = ref('')
function statusText(value) {
  return ({ PENDING_PAYMENT: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退票' })[value] || value
}
async function load() {
  const data = await http.get('/admin/orders', { params: { status: status.value, page: page.value, size: 10 } })
  list.value = data.records
  total.value = data.total
}
onMounted(load)
</script>
