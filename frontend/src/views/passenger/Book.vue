<template>
  <div class="page-wrap" v-if="flight">
    <el-card>
      <h3>填写乘机人信息</h3>
      <p>{{ flight.airline }} {{ flight.flightNo }}　{{ flight.departureCity }} → {{ flight.arrivalCity }}</p>
      <el-radio-group v-model="cabinType" style="margin:12px 0">
        <el-radio-button value="ECONOMY">经济舱 ¥{{ flight.economyPrice }}</el-radio-button>
        <el-radio-button value="BUSINESS">商务舱 ¥{{ flight.businessPrice }}</el-radio-button>
      </el-radio-group>
      <div v-for="(p, i) in passengers" :key="i" class="p-row">
        <el-input v-model="p.name" placeholder="姓名" />
        <el-input v-model="p.idCard" placeholder="身份证号" />
        <el-input v-model="p.phone" placeholder="手机号" />
        <el-button v-if="passengers.length > 1" text type="danger" @click="passengers.splice(i,1)">删除</el-button>
      </div>
      <el-button text @click="passengers.push({name:'',idCard:'',phone:''})">添加乘机人</el-button>
      <div class="total">合计：¥{{ total }}</div>
      <el-button type="primary" :loading="loading" @click="submit">提交订单并模拟支付</el-button>
    </el-card>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
const route = useRoute()
const router = useRouter()
const flight = ref(null)
const cabinType = ref('ECONOMY')
const passengers = ref([{ name: '', idCard: '', phone: '' }])
const loading = ref(false)
const total = computed(() => {
  if (!flight.value) return 0
  const price = cabinType.value === 'BUSINESS' ? flight.value.businessPrice : flight.value.economyPrice
  return (Number(price) * passengers.value.length).toFixed(2)
})
onMounted(async () => { flight.value = await http.get('/flights/' + route.params.id) })
async function submit() {
  for (const passenger of passengers.value) {
    if (!passenger.name?.trim()) {
      ElMessage.error('请填写乘机人姓名')
      return
    }
    if (!/^[0-9]{17}[0-9Xx]$/.test(passenger.idCard || '')) {
      ElMessage.error('身份证号必须为18位有效格式')
      return
    }
  }
  loading.value = true
  try {
    const order = await http.post('/orders', { flightId: Number(route.params.id), cabinType: cabinType.value, passengers: passengers.value })
    await http.post(`/orders/${order.order.id}/pay`)
    ElMessage.success('支付成功，已出票')
    router.push('/orders/' + order.order.id)
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.p-row { display:flex; gap:8px; margin-bottom:8px; }
.total { margin: 12px 0; font-size: 20px; font-weight: 700; color:#1a56db; }
</style>
