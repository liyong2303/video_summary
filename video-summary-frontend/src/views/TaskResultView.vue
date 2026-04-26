<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const task = ref<any>(null)
const loading = ref(true)
let pollTimer: any = null

function formatDuration(seconds: number): string {
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}:${String(sec).padStart(2, '0')}`
}

async function fetchTask() {
  try {
    const id = route.params.id
    const res = await axios.get(`/api/video/${id}`)
    if (res.data.code === 0) {
      task.value = res.data.data
      // Stop polling if task is in terminal state
      if (['completed', 'failed', 'cancelled', 'partially_completed'].includes(task.value.status)) {
        if (pollTimer) {
          clearInterval(pollTimer)
          pollTimer = null
        }
      }
    }
  } catch (e) {
    console.error('Failed to fetch task', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchTask()
  // Poll every 2s if task is pending/processing
  pollTimer = setInterval(() => {
    if (task.value && !['completed', 'failed', 'cancelled', 'partially_completed'].includes(task.value.status)) {
      fetchTask()
    }
  }, 2000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="task-result-view">
    <div v-if="loading" style="text-align: center; padding: 80px">
      <p>加载中...</p>
    </div>

    <div v-else-if="task" class="task-card">
      <div class="video-info">
        <el-image
          v-if="task.coverUrl"
          :src="task.coverUrl"
          fit="cover"
          class="cover"
        />
        <div class="meta">
          <h3>{{ task.videoTitle }}</h3>
          <p>时长 {{ formatDuration(task.videoDuration || 0) }} · {{ task.bvid }}</p>
          <el-tag :type="task.status === 'completed' ? 'success' : task.status === 'failed' ? 'danger' : 'warning'">
            {{ task.status }}
          </el-tag>
        </div>
      </div>

      <div v-if="task.subtitleText" class="subtitle-section">
        <h4>字幕内容</h4>
        <el-input
          type="textarea"
          :model-value="task.subtitleText"
          readonly
          :rows="15"
          resize="vertical"
        />
      </div>

      <el-alert
        v-if="task.errorMessage"
        :title="task.errorMessage"
        type="error"
        show-icon
        :closable="false"
      />
    </div>
  </div>
</template>

<style scoped>
.task-result-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}
.task-card {
  text-align: left;
}
.video-info {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.cover {
  width: 160px;
  height: 90px;
  border-radius: 6px;
  flex-shrink: 0;
}
.meta h3 {
  font-size: 16px;
  margin-bottom: 4px;
}
.meta p {
  font-size: 13px;
  color: #888;
  margin-bottom: 8px;
}
.subtitle-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.subtitle-section h4 {
  margin-bottom: 8px;
}
</style>
