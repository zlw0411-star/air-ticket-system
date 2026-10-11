<template>
  <div class="auth">
    <el-card class="box">
      <h2>登录航空订票系统</h2>
      <el-form :model="form" @submit.prevent="submit">
        <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" show-password placeholder="密码" /></el-form-item>
        <el-button type="primary" style="width:100%" :loading="loading" @click="submit">登录</el-button>
      </el-form>
      <p>还没有账号？<router-link to="/register">立即注册</router-link></p>
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
const form = reactive({ username: 'user', password: 'User123' })
async function submit() {
  loading.value = true
  try {
    await auth.login(form)
    router.push(auth.isAdmin ? '/admin' : '/')
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.auth { min-height:100vh; display:flex; align-items:center; justify-content:center; background:linear-gradient(135deg,#0f2a5a,#1a56db); }
.box { width:380px; }
</style>
