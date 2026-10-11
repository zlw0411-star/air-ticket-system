<template>
  <div>
    <el-form :inline="true">
      <el-form-item><el-input v-model="keyword" placeholder="用户名/姓名/手机" /></el-form-item>
      <el-form-item><el-button type="primary" @click="load">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="list">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="phone" label="手机" />
      <el-table-column label="角色"><template #default="{ row }">{{ row.role === 'ADMIN' ? '管理员' : '乘客' }}</template></el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }">{{ row.enabled ? '启用' : '禁用' }}</template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button text @click="toggle(row)">{{ row.enabled ? '禁用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="10" @current-change="load" />
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api/http'
const keyword = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
async function load() {
  const data = await http.get('/admin/users', { params: { keyword: keyword.value, page: page.value, size: 10 } })
  list.value = data.records
  total.value = data.total
}
async function toggle(row) {
  await http.put(`/admin/users/${row.id}/status`, { enabled: !row.enabled })
  load()
}
onMounted(load)
</script>
