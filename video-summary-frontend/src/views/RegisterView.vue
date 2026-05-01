<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, getMe } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = ref({ username: '', password: '', confirm: '' })
const loading = ref(false)

async function handleRegister() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.value.password !== form.value.confirm) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    const token = await register(form.value.username, form.value.password)
    localStorage.setItem('token', token)
    const userInfo = await getMe()
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    ElMessage.success('注册成功')
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header><span class="auth-title">注册 VideoSum</span></template>
      <el-form @submit.prevent="handleRegister">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名（2-20字符）" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码（6位以上）"
            size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.confirm" type="password" placeholder="确认密码"
            size="large" show-password @keyup.enter="handleRegister" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading"
          @click="handleRegister">注册</el-button>
      </el-form>
      <div class="auth-footer">
        已有账号？<el-link type="primary" @click="$router.push('/login')">登录</el-link>
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
