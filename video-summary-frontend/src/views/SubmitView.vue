<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const url = ref('')
const loading = ref(false)
const result = ref<any>(null)
const taskResult = ref<any>(null)
const results = ref<Record<string, string>>({})
const activeTab = ref('summary')
const error = ref('')

function copyText(text: string) {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

function formatDuration(seconds: number): string {
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}:${String(sec).padStart(2, '0')}`
}

const tabLabels: Record<string, string> = {
  summary: '总结',
  article: '文章',
  card: '学习卡片',
  xiaohongshu: '小红书文案',
}

async function submit() {
  if (!url.value.trim()) return
  loading.value = true
  error.value = ''
  result.value = null
  taskResult.value = null
  results.value = {}

  try {
    const res = await axios.post('/api/video/submit', { url: url.value.trim() })
    if (res.data.code === 0) {
      result.value = res.data.data
      // Fetch full task result
      const taskRes = await axios.get(`/api/video/${result.value.taskId}`)
      if (taskRes.data.code === 0) {
        taskResult.value = taskRes.data.data
        // Fetch individual results
        await fetchResults(result.value.taskId)
      }
    } else {
      error.value = res.data.message
    }
  } catch (e: any) {
    if (e.response?.data?.message) {
      error.value = e.response.data.message
    } else {
      error.value = '请求失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

async function fetchResults(taskId: number) {
  try {
    const res = await axios.get(`/api/video/${taskId}/results`)
    if (res.data.code === 0 && res.data.data) {
      for (const r of res.data.data) {
        results.value[r.outputType] = r.content
      }
    }
  } catch {
    // Results endpoint may not exist yet, that's ok
  }
}
</script>

<template>
  <div class="submit-view">
    <div class="submit-card">
      <h2 class="title">粘贴B站视频链接，一键生成多种内容</h2>
      <p class="subtitle">总结、文章、学习卡片、小红书文案，30秒内全部搞定</p>

      <div class="input-row">
        <el-input
          v-model="url"
          placeholder="请输入BV号或B站视频链接..."
          size="large"
          @keyup.enter="submit"
        />
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          @click="submit"
        >
          生成内容
        </el-button>
      </div>

      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        style="margin-top: 16px"
      />

      <el-card v-if="taskResult" class="result-card" shadow="never">
        <div class="video-info">
          <el-image
            v-if="taskResult.coverUrl"
            :src="taskResult.coverUrl"
            fit="cover"
            class="cover"
          />
          <div class="meta">
            <h3>{{ taskResult.videoTitle }}</h3>
            <p>时长 {{ formatDuration(taskResult.videoDuration || 0) }} · {{ taskResult.bvid }}</p>
            <el-tag :type="taskResult.status === 'completed' ? 'success' : taskResult.status === 'failed' ? 'danger' : 'warning'">
              {{ taskResult.status }}
            </el-tag>
          </div>
        </div>

        <!-- AI Pipeline Results Tabs -->
        <div v-if="Object.keys(results).length > 0" class="results-section">
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
                </div>
              </div>
              <div v-else class="tab-empty">该内容类型暂未生成</div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- Fallback: subtitle text if no pipeline results -->
        <div v-else-if="taskResult.subtitleText" class="subtitle-section">
          <h4>字幕内容</h4>
          <el-input
            type="textarea"
            :model-value="taskResult.subtitleText"
            readonly
            :rows="15"
            resize="vertical"
          />
          <el-button type="primary" style="margin-top: 8px" @click="copyText(taskResult.subtitleText)">
            复制字幕
          </el-button>
        </div>

        <el-alert
          v-if="taskResult.errorMessage"
          :title="taskResult.errorMessage"
          type="error"
          show-icon
          :closable="false"
        />
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.submit-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}
.submit-card {
  text-align: center;
}
.title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}
.subtitle {
  color: #999;
  font-size: 14px;
  margin-bottom: 24px;
}
.input-row {
  display: flex;
  gap: 10px;
  justify-content: center;
}
.input-row .el-input {
  flex: 1;
}
.result-card {
  margin-top: 24px;
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
.results-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.result-text {
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: 14px;
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
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
