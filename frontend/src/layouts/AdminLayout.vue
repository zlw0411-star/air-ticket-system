<template>
  <el-container style="min-height:100vh">
    <el-aside width="220px" class="aside">
      <div class="logo">管理后台</div>
      <el-menu router :default-active="$route.path" background-color="#0f2a5a" text-color="#fff" active-text-color="#93c5fd">
        <el-menu-item index="/admin">数据概览</el-menu-item>
        <el-menu-item index="/admin/users">用户管理</el-menu-item>
        <el-menu-item index="/admin/flights">航班管理</el-menu-item>
        <el-menu-item index="/admin/orders">订单管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="top">
        <span>航空订票管理系统</span>
        <el-button text @click="$router.push('/')">返回前台</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.aside { background: #0f2a5a; color: #fff; }
.logo { padding: 20px; font-weight: 700; }
.top { display:flex; justify-content:space-between; align-items:center; background:#fff; border-bottom:1px solid #e5e7eb; }
</style>
