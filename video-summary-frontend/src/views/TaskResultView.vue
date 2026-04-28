<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const task = ref<any>(null)
const results = ref<Record<string, string>>({})
const loading = ref(true)
const activeTab = ref('summary')
const regenerating = ref<string>('')

let pollTimer: any = null

const tabLabels: Record<string, string> = {
  summary: '总结',
  article: '文章',
  card: '学习卡片',
  xiaohongshu: '小红书文案',
}

function formatDuration(seconds: number): string {
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}:${String(sec).padStart(2, '0')}`
}

function copyText(text: string) {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

async function fetchTask() {
  try {
    const id = route.params.id
    const res = await axios.get(`/api/video/${id}`)
    if (res.data.code === 0) {
      task.value = res.data.data
    }
  } catch (e) {
    console.error('Failed to fetch task', e)
  } finally {
    loading.value = false
  }
}

async function fetchResults() {
  try {
    const id = route.params.id
    const res = await axios.get(`/api/video/${id}/results`)
    if (res.data.code === 0 && res.data.data) {
      for (const r of res.data.data) {
        if (r.content) {
          results.value[r.outputType] = r.content
        }
      }
    }
  } catch (e) {
    console.error('Failed to fetch results', e)
  }
}

async function regenerateStep(outputType: string) {
  const id = route.params.id
  regenerating.value = outputType
  try {
    await axios.post(`/api/video/${id}/regenerate/${outputType}`)
    await fetchResults()
    ElMessage.success('重新生成完成')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '重新生成失败')
  } finally {
    regenerating.value = ''
  }
}

function exportMarkdown() {
  const id = route.params.id
  window.open(`/api/video/${id}/export`, '_blank')
}

onMounted(() => {
  fetchTask()
  fetchResults()
  // Poll every 3s if task is still processing
  pollTimer = setInterval(() => {
    if (task.value && !['completed', 'failed', 'cancelled', 'partially_completed'].includes(task.value.status)) {
      fetchTask()
      fetchResults()
    }
  }, 3000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="task-result-view">
    <div v-if="loading" style="text-align: center; padding: 80px">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <p style="margin-top: 12px; color: #999">加载中...</p>
    </div>

    <div v-else-if="task" class="task-card">
      <div class="back-row">
        <el-button text @click="router.push('/history')">
          <el-icon><ArrowLeft /></el-icon> 返回历史
        </el-button>
      </div>

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

      <!-- AI Pipeline Results Tabs -->
      <div v-if="Object.keys(results).length > 0" class="results-section">
        <div class="results-header">
          <span class="results-title">生成结果</span>
          <el-button size="small" @click="exportMarkdown">
            导出 Markdown
          </el-button>
        </div>
        <el-tabs v-model="activeTab">
          <el-tab-pane
            v-for="(label, key) in tabLabels"
            :key="key"
            :label="label"
            :name="key"
          >
            <div v-if="results[key]" class="tab-content">
              <div class="result-text">{{ results[key] }}</div>
              <div class="result-actions">
                <el-button type="primary" size="small" @click="copyText(results[key])">
                  复制
                </el-button>
                <el-button
                  size="small"
                  :loading="regenerating === key"
                  @click="regenerateStep(key)"
                >
                  重新生成
                </el-button>
              </div>
            </div>
            <div v-else class="tab-empty">该内容类型暂未生成</div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- Fallback: subtitle text -->
      <div v-else-if="task.subtitleText" class="subtitle-section">
        <h4>字幕内容</h4>
        <el-input
          type="textarea"
          :model-value="task.subtitleText"
          readonly
          :rows="15"
          resize="vertical"
        />
        <el-button type="primary" style="margin-top: 8px" @click="copyText(task.subtitleText)">
          复制字幕
        </el-button>
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
.back-row {
  margin-bottom: 16px;
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
.results-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.results-title {
  font-size: 16px;
  font-weight: 600;
}
.result-text {
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: 14px;
  max-height: 400px;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}
.result-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
.tab-empty {
  color: #999;
  text-align: center;
  padding: 40px 0;
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
