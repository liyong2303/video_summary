<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'

const router = useRouter()
const route = useRoute()

const currentNav = computed(() => {
  if (route.path === '/history') return 'history'
  return 'home'
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
</style>
