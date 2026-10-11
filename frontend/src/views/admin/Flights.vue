<template>
  <div>
    <el-button type="primary" @click="open()">新增航班</el-button>
    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="flightNo" label="航班号" />
      <el-table-column prop="airline" label="航司" />
      <el-table-column label="航线"><template #default="{ row }">{{ row.departureCity }}→{{ row.arrivalCity }}</template></el-table-column>
      <el-table-column prop="departureTime" label="起飞" />
      <el-table-column label="状态"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button text @click="open(row)">编辑</el-button>
          <el-button text type="danger" @click="setStatus(row, 'CANCELLED')">停飞</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="10" @current-change="load" />
    <el-dialog v-model="show" :title="form.id ? '编辑航班' : '新增航班'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="航班号"><el-input v-model="form.flightNo" /></el-form-item>
        <el-form-item label="航司"><el-input v-model="form.airline" /></el-form-item>
        <el-form-item label="出发城市"><el-input v-model="form.departureCity" /></el-form-item>
        <el-form-item label="到达城市"><el-input v-model="form.arrivalCity" /></el-form-item>
        <el-form-item label="出发机场"><el-input v-model="form.departureAirport" /></el-form-item>
        <el-form-item label="到达机场"><el-input v-model="form.arrivalAirport" /></el-form-item>
        <el-form-item label="起飞时间"><el-date-picker v-model="form.departureTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item label="到达时间"><el-date-picker v-model="form.arrivalTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item label="机型"><el-input v-model="form.aircraftType" /></el-form-item>
        <el-form-item label="经济舱价"><el-input v-model="form.economyPrice" /></el-form-item>
        <el-form-item label="经济舱座"><el-input v-model="form.economySeats" /></el-form-item>
        <el-form-item label="商务舱价"><el-input v-model="form.businessPrice" /></el-form-item>
        <el-form-item label="商务舱座"><el-input v-model="form.businessSeats" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../api/http'
const list = ref([])
const total = ref(0)
const page = ref(1)
const show = ref(false)
const form = reactive({})
function statusText(status) {
  return ({ SCHEDULED: '计划中', CANCELLED: '已停飞', DEPARTED: '已起飞', ARRIVED: '已到达' })[status] || status
}
function blankForm() {
  return {
    airline: '中国国际航空',
    aircraftType: 'A320',
    economyPrice: 680,
    economySeats: 120,
    businessPrice: 1880,
    businessSeats: 16
  }
}
function fillForm(next) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, next)
}
async function load() {
  const data = await http.get('/admin/flights', { params: { page: page.value, size: 10 } })
  list.value = data.records
  total.value = data.total
}
function open(row) {
  if (row) {
    fillForm({
      id: row.id,
      flightNo: row.flightNo,
      airline: row.airline,
      departureCity: row.departureCity,
      arrivalCity: row.arrivalCity,
      departureAirport: row.departureAirport,
      arrivalAirport: row.arrivalAirport,
      departureTime: row.departureTime,
      arrivalTime: row.arrivalTime,
      aircraftType: row.aircraftType,
      economyPrice: row.economyPrice,
      economySeats: row.economySeats ?? row.economyTotal,
      businessPrice: row.businessPrice,
      businessSeats: row.businessSeats ?? row.businessTotal
    })
  } else {
    fillForm(blankForm())
  }
  show.value = true
}
async function save() {
  const payload = {
    flightNo: form.flightNo,
    airline: form.airline,
    departureCity: form.departureCity,
    arrivalCity: form.arrivalCity,
    departureAirport: form.departureAirport,
    arrivalAirport: form.arrivalAirport,
    departureTime: form.departureTime,
    arrivalTime: form.arrivalTime,
    aircraftType: form.aircraftType,
    economyPrice: form.economyPrice,
    economySeats: form.economySeats,
    businessPrice: form.businessPrice,
    businessSeats: form.businessSeats
  }
  if (form.id) await http.put(`/admin/flights/${form.id}`, payload)
  else await http.post('/admin/flights', payload)
  show.value = false
  load()
}
async function setStatus(row, status) {
  await http.put(`/admin/flights/${row.id}/status`, { status })
  load()
}
onMounted(load)
</script>
