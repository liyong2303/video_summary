<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getMe, logout, type UserInfo } from './api/auth'

const router = useRouter()
const route = useRoute()

const currentNav = computed(() => {
  if (route.path === '/history') return 'history'
  if (route.path === '/settings') return 'settings'
  if (route.path === '/custom-prompts') return 'custom-prompts'
  if (route.path === '/templates') return 'templates'
  if (route.path === '/quick-actions') return 'quick-actions'
  return 'home'
})

const userInfo = ref<UserInfo | null>(null)

onMounted(async () => {
  const cached = localStorage.getItem('userInfo')
  if (cached) {
    try { userInfo.value = JSON.parse(cached) } catch {}
  }
  // 刷新最新数据（如果已登录）
  if (localStorage.getItem('token')) {
    try {
      const info = await getMe()
      userInfo.value = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    } catch {}
  }
})

const quotaText = computed(() => {
  if (!userInfo.value) return ''
  const { todayUsed, dailyLimit } = userInfo.value
  if (dailyLimit === -1) return '无限次'
  return `今日剩余 ${Math.max(0, dailyLimit - todayUsed)} 次`
})
</script>

<template>
  <el-container>
    <el-header class="app-header">
      <div class="header-left">
        <div class="logo" @click="router.push('/')">VideoSum</div>
        <span class="subtitle">B站视频内容再生产</span>
      </div>
      <div class="nav-links">
        <span
          class="nav-link"
          :class="{ active: currentNav === 'home' }"
          @click="router.push('/')"
        >
          生成内容
        </span>
        <span
          class="nav-link"
          :class="{ active: currentNav === 'history' }"
          @click="router.push('/history')"
        >
          历史记录
        </span>
        <span
          class="nav-link"
          :class="{ active: currentNav === 'settings' }"
          @click="router.push('/settings')"
        >
          设置
        </span>
        <span
          class="nav-link"
          :class="{ active: currentNav === 'custom-prompts' }"
          @click="router.push('/custom-prompts')"
        >
          自定义Prompt
        </span>
        <span
          class="nav-link"
          :class="{ active: currentNav === 'templates' }"
          @click="router.push('/templates')"
        >
          模板
        </span>
        <span
          class="nav-link"
          :class="{ active: currentNav === 'quick-actions' }"
          @click="router.push('/quick-actions')"
        >
          快捷操作
        </span>
        <span class="quota-badge" v-if="userInfo && userInfo.dailyLimit !== -1">{{ quotaText }}</span>
        <span class="nav-link" @click="logout">退出</span>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<style>
body {
  margin: 0;
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
  background: #f8f9fb;
}
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e8ecf1;
  padding: 0 24px;
  height: 60px;
}
.header-left {
  display: flex;
  align-items: center;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #00a1d6, #6c5ce7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  cursor: pointer;
}
.subtitle {
  color: #999;
  font-size: 13px;
  margin-left: 10px;
}
.nav-links {
  display: flex;
  gap: 24px;
  align-items: center;
}
.nav-link {
  font-size: 14px;
  color: #666;
  cursor: pointer;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
}
.nav-link:hover {
  color: #00a1d6;
}
.nav-link.active {
  color: #00a1d6;
  border-bottom-color: #00a1d6;
  font-weight: 500;
}
.quota-badge {
  font-size: 12px;
  color: #999;
  background: #f5f7fa;
  padding: 2px 10px;
  border-radius: 10px;
}
</style>
