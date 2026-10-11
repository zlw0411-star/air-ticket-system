<template>
  <div class="page-wrap">
    <el-form :inline="true" :model="query" class="search-card" style="margin-bottom:16px">
      <el-form-item label="出发地"><el-input v-model="query.departureCity" /></el-form-item>
      <el-form-item label="目的地"><el-input v-model="query.arrivalCity" /></el-form-item>
      <el-form-item label="日期"><el-date-picker v-model="query.date" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item><el-button type="primary" @click="load">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="list" v-loading="loading">
      <el-table-column prop="flightNo" label="航班号" width="110" />
      <el-table-column prop="airline" label="航司" width="140" />
      <el-table-column label="航线">
        <template #default="{ row }">{{ row.departureCity }} → {{ row.arrivalCity }}</template>
      </el-table-column>
      <el-table-column label="起飞 / 到达">
        <template #default="{ row }">{{ row.departureTime }}<br>{{ row.arrivalTime }}</template>
      </el-table-column>
      <el-table-column label="经济舱">
        <template #default="{ row }">¥{{ row.economyPrice }} / 余 {{ row.economyAvailable }}</template>
      </el-table-column>
      <el-table-column label="商务舱">
        <template #default="{ row }">¥{{ row.businessPrice }} / 余 {{ row.businessAvailable }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push('/flights/' + row.id)">详情</el-button>
          <el-button type="primary" @click="$router.push('/book/' + row.id)">购票</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:16px" v-model:current-page="page" :page-size="10" :total="total" @current-change="load" />
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'
const route = useRoute()
const query = reactive({ departureCity: route.query.departureCity || '', arrivalCity: route.query.arrivalCity || '', date: route.query.date || '' })
const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    const data = await http.get('/flights', { params: { ...query, page: page.value, size: 10 } })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
