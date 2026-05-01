<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, getMe } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = ref({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const token = await login(form.value.username, form.value.password)
    localStorage.setItem('token', token)
    const userInfo = await getMe()
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header><span class="auth-title">登录 VideoSum</span></template>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码"
            size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading"
          @click="handleLogin">登录</el-button>
      </el-form>
      <div class="auth-footer">
        没有账号？<el-link type="primary" @click="$router.push('/register')">注册</el-link>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page { display:flex; justify-content:center; align-items:center; min-height:80vh; }
.auth-card { width:360px; }
.auth-title { font-size:18px; font-weight:600; }
.auth-footer { text-align:center; margin-top:16px; font-size:14px; color:#666; }
</style>
