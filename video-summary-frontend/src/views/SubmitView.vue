<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import HistoryDialog from '@/components/HistoryDialog.vue'

const url = ref('')
const loading = ref(false)
const result = ref<any>(null)
const taskResult = ref<any>(null)
const results = ref<Record<string, string>>({})
const streamingStep = ref('')
const loadingMessage = ref('')
const activeTab = ref('summary')
const error = ref('')
const isStreaming = ref(false)
const editing = ref<Record<string, boolean>>({})
const unsavedChanges = ref<Record<string, boolean>>({})
const historyDialogVisible = ref(false)
const currentOutputType = ref('')

const stepMessages: Record<string, string> = {
  extract: '正在提取B站字幕...',
  summary: '正在生成总结...',
  article: '正在生成文章...',
  card: '正在生成学习卡片...',
  xiaohongshu: '正在生成小红书文案...',
  parallel: '正在并行生成文章、学习卡片、小红书文案...',
}

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

const regenerating = ref<string>('')

async function regenerateStep(outputType: string) {
  if (!result.value) return
  regenerating.value = outputType
  try {
    await axios.post(`/api/video/${result.value.taskId}/regenerate/${outputType}`)
    await fetchResults(result.value.taskId)
    ElMessage.success('重新生成完成')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '重新生成失败')
  } finally {
    regenerating.value = ''
  }
}

function exportMarkdown() {
  if (!result.value) return
  window.open(`/api/video/${result.value.taskId}/export`, '_blank')
}

async function saveEdit(outputType: string) {
  if (!result.value) return
  try {
    await axios.put(`/api/video/${result.value.taskId}/result/${outputType}`, {
      content: results.value[outputType]
    })
    unsavedChanges.value[outputType] = false
    editing.value[outputType] = false
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

// 检测内容变化
watch(() => results.value, (newVal, oldVal) => {
  for (const key in newVal) {
    if (oldVal[key] !== newVal[key]) {
      unsavedChanges.value[key] = true
    }
  }
}, { deep: true })

function showHistory(outputType: string) {
  currentOutputType.value = outputType
  historyDialogVisible.value = true
}

function handleRollback() {
  if (result.value) fetchResults(result.value.taskId)
}

async function submit() {
  if (!url.value.trim()) return
  loading.value = true
  error.value = ''
  result.value = null
  taskResult.value = null
  results.value = {}
  streamingStep.value = ''
  isStreaming.value = false

  try {
    // Step 1: Submit and create task
    const res = await axios.post('/api/video/submit', { url: url.value.trim() })
    if (res.data.code !== 0) {
      error.value = res.data.message
      return
    }
    result.value = res.data.data
    const taskId = result.value.taskId

    // Step 2: Connect SSE stream for real-time results
    if (!result.value.isExisting) {
      isStreaming.value = true
      await connectSSE(taskId)
    } else {
      // Existing task, fetch results
      await fetchResults(taskId)
      const taskRes = await axios.get(`/api/video/${taskId}`)
      if (taskRes.data.code === 0) {
        taskResult.value = taskRes.data.data
      }
    }
  } catch (e: any) {
    if (e.response?.data?.message) {
      error.value = e.response.data.message
    } else {
      error.value = '请求失败，请稍后重试'
    }
  } finally {
    loading.value = false
    isStreaming.value = false
  }
}

function connectSSE(taskId: number): Promise<void> {
  return new Promise((resolve) => {
    const eventSource = new EventSource(`/api/video/${taskId}/stream`)

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)

        switch (data.type) {
          case 'step_start':
            streamingStep.value = data.step
            if (data.message) {
              loadingMessage.value = data.message
            } else {
              loadingMessage.value = stepMessages[data.step] || '处理中...'
            }
            // Initialize empty content for streaming step
            if (data.step !== 'extract') {
              results.value[data.step] = ''
            }
            // Fetch task info on first step
            if (!taskResult.value) {
              fetchTaskInfo(taskId)
            }
            break

          case 'chunk':
            if (data.step && data.step !== 'extract') {
              results.value[data.step] = (results.value[data.step] || '') + data.content
            }
            break

          case 'step_complete':
            if (data.message) {
              loadingMessage.value = data.message
            }
            // 等待1秒后清空提示
            setTimeout(() => {
              streamingStep.value = ''
              loadingMessage.value = ''
            }, 1000)
            // After a step completes, fetch full result from server
            fetchResults(taskId)
            break

          case 'pipeline_complete':
            isStreaming.value = false
            eventSource.close()
            fetchTaskInfo(taskId)
            fetchResults(taskId)
            resolve()
            break

          case 'error':
            error.value = data.message || '处理出错'
            isStreaming.value = false
            eventSource.close()
            fetchTaskInfo(taskId)
            resolve()
            break
        }
      } catch (e) {
        console.error('Failed to parse SSE data', e)
      }
    }

    eventSource.onerror = () => {
      eventSource.close()
      // On error, try to fetch results normally
      fetchTaskInfo(taskId)
      fetchResults(taskId)
      isStreaming.value = false
      resolve()
    }
  })
}

async function fetchTaskInfo(taskId: number) {
  try {
    const res = await axios.get(`/api/video/${taskId}`)
    if (res.data.code === 0) {
      taskResult.value = res.data.data
    }
  } catch {}
}

async function fetchResults(taskId: number) {
  try {
    const res = await axios.get(`/api/video/${taskId}/results`)
    if (res.data.code === 0 && res.data.data) {
      for (const r of res.data.data) {
        if (r.content) {
          results.value[r.outputType] = r.content
        }
      }
    }
  } catch {}
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
          :disabled="loading"
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

      <!-- Streaming indicator -->
      <div v-if="isStreaming" class="streaming-indicator">
        <el-icon class="is-loading"><svg viewBox="0 0 1024 1024" width="16" height="16"><path d="M512 64a32 32 0 0 1 32 32v192a32 32 0 0 1-64 0V96a32 32 0 0 1 32-32z" fill="currentColor"/><path d="M512 736a32 32 0 0 1 32 32v192a32 32 0 1 1-64 0V768a32 32 0 0 1 32-32z" fill="currentColor"/></svg></el-icon>
        <span>{{ loadingMessage || '处理中...' }}</span>
      </div>

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
                <el-input
                  v-model="results[key]"
                  type="textarea"
                  :rows="15"
                  placeholder="点击编辑按钮修改内容..."
                  :readonly="!editing[key]"
                  @input="unsavedChanges[key] = true"
                />
                <div class="result-actions">
                  <el-button
                    v-if="!editing[key]"
                    type="primary"
                    size="small"
                    @click="editing[key] = true"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-if="editing[key] && unsavedChanges[key]"
                    type="success"
                    size="small"
                    @click="saveEdit(key)"
                  >
                    保存
                  </el-button>
                  <el-button
                    v-if="editing[key]"
                    size="small"
                    @click="editing[key] = false; fetchResults(result.value.taskId)"
                  >
                    取消
                  </el-button>
                  <el-button
                    v-if="!editing[key]"
                    type="default"
                    size="small"
                    @click="copyText(results[key])"
                  >
                    复制
                  </el-button>
                  <el-button
                    size="small"
                    :loading="regenerating === key"
                    @click="regenerateStep(key)"
                  >
                    重新生成
                  </el-button>
                  <el-button
                    size="small"
                    @click="showHistory(key)"
                  >
                    历史版本
                  </el-button>
                </div>
              </div>
              <div v-else-if="isStreaming && streamingStep === key" class="tab-streaming">
                正在生成中...
              </div>
              <div v-else class="tab-empty">该内容类型暂未生成</div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- Fallback: subtitle text -->
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

      <HistoryDialog
        v-model:visible="historyDialogVisible"
        :task-id="result?.taskId"
        :output-type="currentOutputType"
        @rollback="handleRollback"
      />
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
.streaming-indicator {
  margin-top: 16px;
  color: #00a1d6;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
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
.tab-streaming {
  color: #00a1d6;
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

/* 移动端适配 */
@media (max-width: 768px) {
  .submit-view {
    padding: 20px 12px;
  }

  .input-row {
    flex-direction: column;
  }

  .video-info {
    flex-direction: column;
  }

  .cover {
    width: 100%;
    height: auto;
    aspect-ratio: 16/9;
  }

  .result-actions {
    flex-direction: column;
  }

  .result-actions .el-button {
    width: 100%;
  }
}
</style>
