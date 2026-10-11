<template>
  <div class="page-wrap" v-if="detail">
    <el-card>
      <h3>订单 {{ detail.order.orderNo }}</h3>
      <p>状态：{{ orderStatusText(detail.order.status) }}　金额：¥{{ detail.order.totalAmount }}　已付：¥{{ detail.order.paidAmount }}</p>
      <el-button v-if="detail.order.status === 'PENDING_PAYMENT'" type="primary" @click="pay">模拟支付</el-button>
      <el-button v-if="detail.order.status === 'PENDING_PAYMENT'" @click="cancel">取消订单</el-button>
      <el-table :data="detail.tickets" style="margin-top:16px">
        <el-table-column prop="ticket.ticketNo" label="票号" />
        <el-table-column prop="ticket.passengerName" label="乘机人" />
        <el-table-column label="航班">
          <template #default="{ row }">{{ row.flight.flightNo }} {{ row.flight.departureCity }}→{{ row.flight.arrivalCity }}</template>
        </el-table-column>
        <el-table-column label="舱位"><template #default="{ row }">{{ cabinText(row.ticket.cabinType) }}</template></el-table-column>
        <el-table-column prop="ticket.price" label="票价" />
        <el-table-column label="状态"><template #default="{ row }">{{ ticketStatusText(row.ticket.status) }}</template></el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="canChange(row.ticket.status)" text type="primary" @click="openReschedule(row)">改签</el-button>
            <el-button v-if="canChange(row.ticket.status)" text type="danger" @click="refund(row.ticket.id)">退票</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="show" title="选择改签航班">
      <el-form :inline="true">
        <el-form-item label="日期"><el-date-picker v-model="q.date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item><el-button @click="search">查询</el-button></el-form-item>
      </el-form>
      <el-radio-group v-model="rescheduleCabin" style="margin-bottom: 12px">
        <el-radio-button value="ECONOMY">经济舱</el-radio-button>
        <el-radio-button value="BUSINESS">商务舱</el-radio-button>
      </el-radio-group>
      <el-table :data="flights">
        <el-table-column prop="flightNo" label="航班号" />
        <el-table-column label="航线"><template #default="{ row }">{{ row.departureCity }} → {{ row.arrivalCity }}</template></el-table-column>
        <el-table-column prop="departureTime" label="起飞" />
        <el-table-column label="经济舱"><template #default="{ row }">¥{{ row.economyPrice }} / 余{{ row.economyAvailable }}</template></el-table-column>
        <el-table-column label="商务舱"><template #default="{ row }">¥{{ row.businessPrice }} / 余{{ row.businessAvailable }}</template></el-table-column>
        <el-table-column label="操作"><template #default="{ row }"><el-button text type="primary" @click="doReschedule(row.id)">改签到此航班</el-button></template></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'
const route = useRoute()
const detail = ref(null)
const show = ref(false)
const currentTicket = ref(null)
const rescheduleCabin = ref('ECONOMY')
const flights = ref([])
const q = reactive({ date: '' })
async function load() { detail.value = await http.get('/orders/' + route.params.id) }
onMounted(load)
function orderStatusText(s) { return ({ PENDING_PAYMENT: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退票' })[s] || s }
function ticketStatusText(s) { return ({ PENDING: '待出票', ISSUED: '已出票', RESCHEDULED: '已改签', REFUNDED: '已退票', CANCELLED: '已取消' })[s] || s }
function cabinText(s) { return s === 'BUSINESS' ? '商务舱' : '经济舱' }
function canChange(s) { return s === 'ISSUED' || s === 'RESCHEDULED' }
async function pay() { await http.post(`/orders/${detail.value.order.id}/pay`); ElMessage.success('支付成功'); load() }
async function cancel() { await http.post(`/orders/${detail.value.order.id}/cancel`); load() }
async function refund(id) {
  await ElMessageBox.confirm('确认退票？将按退票规则退款')
  await http.post(`/tickets/${id}/refund`)
  ElMessage.success('退票成功')
  load()
}
function openReschedule(row) {
  currentTicket.value = row
  rescheduleCabin.value = row.ticket.cabinType
  q.date = row.flight.departureTime?.slice(0,10)
  show.value = true
  search()
}
async function search() {
  const f = currentTicket.value.flight
  const data = await http.get('/flights', { params: { departureCity: f.departureCity, arrivalCity: f.arrivalCity, date: q.date, page: 1, size: 20 } })
  flights.value = data.records.filter(x => x.id !== f.id)
}
async function doReschedule(id) {
  await http.post(`/tickets/${currentTicket.value.ticket.id}/reschedule`, { targetFlightId: id, cabinType: rescheduleCabin.value })
  ElMessage.success('改签成功')
  show.value = false
  load()
}
</script>
