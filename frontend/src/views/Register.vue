<template>
  <div class="auth">
    <el-card class="box">
      <h2>注册乘客账号</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-button type="primary" style="width:100%" :loading="loading" @click="submit">注册并登录</el-button>
      </el-form>
      <p>已有账号？<router-link to="/login">去登录</router-link></p>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const form = reactive({ username:'', password:'', realName:'', idCard:'', phone:'', email:'' })
async function submit() {
  loading.value = true
  try {
    await auth.register(form)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.auth { min-height:100vh; display:flex; align-items:center; justify-content:center; background:linear-gradient(135deg,#0f2a5a,#1a56db); }
.box { width:420px; }
</style>
