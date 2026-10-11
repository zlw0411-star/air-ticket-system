<template>
  <div>
    <div class="stat-grid">
      <div class="stat-card"><div class="label">累计订单</div><div class="value">{{ stats.orderCount || 0 }}</div></div>
      <div class="stat-card"><div class="label">累计营收</div><div class="value">¥{{ stats.revenue || 0 }}</div></div>
      <div class="stat-card"><div class="label">今日订单</div><div class="value">{{ stats.todayOrderCount || 0 }}</div></div>
      <div class="stat-card"><div class="label">今日营收</div><div class="value">¥{{ stats.todayRevenue || 0 }}</div></div>
    </div>
    <el-card style="margin-top:16px">
      <h3>热门航线</h3>
      <el-table :data="stats.hotRoutes || []">
        <el-table-column prop="flightNo" label="航班号" />
        <el-table-column prop="route" label="航线" />
        <el-table-column prop="count" label="出票数" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api/http'
const stats = ref({})
onMounted(async () => { stats.value = await http.get('/admin/statistics') })
</script>
