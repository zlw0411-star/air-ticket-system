<template>
  <div class="page-wrap" v-if="flight">
    <el-card>
      <h2>{{ flight.airline }} {{ flight.flightNo }}</h2>
      <p>{{ flight.departureCity }} {{ flight.departureAirport }} → {{ flight.arrivalCity }} {{ flight.arrivalAirport }}</p>
      <p>起飞 {{ flight.departureTime }}　到达 {{ flight.arrivalTime }}　机型 {{ flight.aircraftType }}</p>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="经济舱价格">¥{{ flight.economyPrice }}</el-descriptions-item>
        <el-descriptions-item label="经济舱余票">{{ flight.economyAvailable }} / {{ flight.economyTotal }}</el-descriptions-item>
        <el-descriptions-item label="商务舱价格">¥{{ flight.businessPrice }}</el-descriptions-item>
        <el-descriptions-item label="商务舱余票">{{ flight.businessAvailable }} / {{ flight.businessTotal }}</el-descriptions-item>
      </el-descriptions>
      <el-button type="primary" style="margin-top:16px" @click="$router.push('/book/' + flight.id)">立即购票</el-button>
    </el-card>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'
const route = useRoute()
const flight = ref(null)
onMounted(async () => { flight.value = await http.get('/flights/' + route.params.id) })
</script>
