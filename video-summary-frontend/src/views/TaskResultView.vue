<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import HistoryDialog from '@/components/HistoryDialog.vue'
import { getQuickActions, type QuickAction } from '@/api/quickAction'

const route = useRoute()
const router = useRouter()
const task = ref<any>(null)
const results = ref<Record<string, string>>({})
const loading = ref(true)
const activeTab = ref('summary')
const regenerating = ref<string>('')
const editing = ref<Record<string, boolean>>({})
const unsavedChanges = ref<Record<string, boolean>>({})
const historyDialogVisible = ref(false)
const currentOutputType = ref('')
const quickActions = ref<QuickAction[]>([])

let pollTimer: any = null

// Filter quick actions for single task scope
const singleTaskActions = computed(() =>
  quickActions.value.filter(a => a.applyScope === 'single')
)

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

async function saveEdit(outputType: string) {
  try {
    const id = route.params.id
    await axios.put(`/api/video/${id}/result/${outputType}`, {
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
  fetchResults()
}

async function loadQuickActions() {
  try {
    quickActions.value = await getQuickActions()
  } catch {
    // Ignore error
  }
}

async function executeQuickAction(action: QuickAction) {
  try {
    for (const step of action.steps) {
      switch (step.action) {
        case 'copy':
          if (activeTab.value && results.value[activeTab.value]) {
            navigator.clipboard.writeText(results.value[activeTab.value])
            ElMessage.success('已复制')
          }
          break
        case 'export':
          exportMarkdown()
          break
        case 'regenerate':
          if (activeTab.value) {
            await regenerateStep(activeTab.value)
          }
          break
      }
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '执行快捷操作失败')
  }
}

onMounted(async () => {
  await loadQuickActions()
  fetchTask()
  fetchResults()
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
          <div class="header-actions">
            <el-dropdown v-if="singleTaskActions.length > 0" split-button type="primary" size="small" @click="exportMarkdown">
              导出 Markdown
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="action in singleTaskActions"
                    :key="action.id"
                    @click="executeQuickAction(action)"
                  >
                    {{ action.name }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-else size="small" @click="exportMarkdown">
              导出 Markdown
            </el-button>
          </div>
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
                  @click="editing[key] = false; fetchResults()"
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

    <HistoryDialog
      v-model:visible="historyDialogVisible"
      :task-id="Number(task?.id)"
      :output-type="currentOutputType"
      @rollback="handleRollback"
    />
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
.header-actions {
  display: flex;
  gap: 8px;
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

/* 移动端适配 */
@media (max-width: 768px) {
  .task-result-view {
    padding: 20px 12px;
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
