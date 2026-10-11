<template>
  <div>
    <el-header class="nav">
      <div class="brand" @click="$router.push('/')">航空订票</div>
      <el-menu mode="horizontal" :ellipsis="false" router :default-active="$route.path">
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/flights">航班查询</el-menu-item>
        <el-menu-item index="/orders">我的订单</el-menu-item>
        <el-menu-item index="/profile">个人中心</el-menu-item>
      </el-menu>
      <div class="right">
        <template v-if="auth.isLogin">
          <el-button v-if="auth.isAdmin" text @click="$router.push('/admin')">管理后台</el-button>
          <span>{{ auth.user?.realName || auth.user?.username }}</span>
          <el-button text @click="logout">退出</el-button>
        </template>
        <template v-else>
          <el-button text @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>
    <router-view />
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
const auth = useAuthStore()
const router = useRouter()
function logout() {
  auth.logout()
  router.push('/')
}
</script>

<style scoped>
.nav {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  height: 64px;
}
.brand { font-weight: 800; color: #1a56db; cursor: pointer; min-width: 96px; }
.el-menu { flex: 1; border-bottom: none; }
.right { display: flex; align-items: center; gap: 8px; }
</style>
