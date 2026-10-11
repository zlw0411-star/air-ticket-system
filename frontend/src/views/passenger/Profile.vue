<template>
  <div class="page-wrap">
    <el-card>
      <h3>个人中心</h3>
      <el-form :model="form" label-width="80px" style="max-width:420px">
        <el-form-item label="用户名"><el-input :model-value="auth.user?.username" disabled /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-button type="primary" @click="save">保存资料</el-button>
      </el-form>
      <h3 style="margin-top: 28px">修改密码</h3>
      <el-form :model="passwordForm" label-width="80px" style="max-width:420px">
        <el-form-item label="原密码"><el-input v-model="passwordForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item>
        <el-button type="primary" @click="changePassword">修改密码</el-button>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { useAuthStore } from '../../stores/auth'
const auth = useAuthStore()
const form = reactive({ realName: '', phone: '', email: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '' })
onMounted(async () => {
  const u = await http.get('/users/profile')
  form.realName = u.realName
  form.phone = u.phone
  form.email = u.email
})
async function save() {
  const u = await http.put('/users/profile', form)
  auth.user = u
  localStorage.setItem('user', JSON.stringify(u))
  ElMessage.success('已保存')
}
async function changePassword() {
  if (!passwordForm.oldPassword || passwordForm.newPassword.length < 6) {
    ElMessage.error('请输入原密码，新密码至少6位')
    return
  }
  await http.put('/users/password', passwordForm)
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  ElMessage.success('密码已修改')
}
</script>
