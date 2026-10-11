<template>
  <div>
    <div class="hero">
      <div class="page-wrap">
        <h1>查找您的下一趟航班</h1>
        <p>覆盖国内主要航线，支持在线购票、改签与退票</p>
        <div class="search-card">
          <el-form :inline="true" :model="query">
            <el-form-item label="出发地"><el-select v-model="query.departureCity" filterable allow-create default-first-option placeholder="城市" style="width:140px"><el-option v-for="c in cities" :key="c" :label="c" :value="c" /></el-select></el-form-item>
            <el-form-item label="目的地"><el-select v-model="query.arrivalCity" filterable allow-create default-first-option placeholder="城市" style="width:140px"><el-option v-for="c in cities" :key="'a'+c" :label="c" :value="c" /></el-select></el-form-item>
            <el-form-item label="日期"><el-date-picker v-model="query.date" value-format="YYYY-MM-DD" /></el-form-item>
            <el-form-item><el-button type="primary" @click="search">查询航班</el-button></el-form-item>
          </el-form>
        </div>
      </div>
    </div>
    <div class="page-wrap">
      <h3>热门航线</h3>
      <el-row :gutter="16">
        <el-col :span="8" v-for="item in hot" :key="item"><el-card>{{ item }}</el-card></el-col>
      </el-row>
    </div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../api/http'
const router = useRouter()
const cities = ref([])
const query = reactive({ departureCity: '北京', arrivalCity: '上海', date: new Date().toISOString().slice(0,10) })
const hot = ['北京 → 上海', '广州 → 北京', '上海 → 成都', '深圳 → 上海', '成都 → 北京', '杭州 → 深圳']
onMounted(async () => { cities.value = await http.get('/flights/cities') })
function search() {
  router.push({ path: '/flights', query: { ...query } })
}
</script>
