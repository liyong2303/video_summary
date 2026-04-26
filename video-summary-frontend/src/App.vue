<script setup lang="ts">
</script>

<template>
  <el-container>
    <el-header class="app-header">
      <div class="logo">VideoSum</div>
      <span class="subtitle">B站视频内容再生产</span>
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
  background: #fff;
  border-bottom: 1px solid #e8ecf1;
  padding: 0 24px;
  height: 60px;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #00a1d6, #6c5ce7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.subtitle {
  color: #999;
  font-size: 13px;
  margin-left: 10px;
}
</style>
