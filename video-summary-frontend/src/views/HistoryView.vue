<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

interface HistoryItem {
  taskId: number
  bvid: string
  videoTitle: string
  videoDuration: number
  coverUrl: string
  status: string
  outputTypes: string[]
  createdAt: string
  completedAt: string | null
}

interface HistoryPage {
  items: HistoryItem[]
  total: number
  page: number
  pageSize: number
}

const history = ref<HistoryPage | null>(null)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 10

const outputTypeLabels: Record<string, string> = {
  summary: '总结',
  article: '文章',
  card: '学习卡片',
  xiaohongshu: '小红书',
}

function formatDuration(seconds: number): string {
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}:${String(sec).padStart(2, '0')}`
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

function statusType(status: string): string {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'partially_completed') return 'warning'
  return 'info'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    completed: '完成',
    failed: '失败',
    partially_completed: '部分完成',
    processing: '处理中',
    pending: '等待中',
    cancelled: '已取消',
  }
  return map[status] || status
}

async function fetchHistory() {
  loading.value = true
  try {
    const res = await axios.get('/api/video/history', {
      params: { page: currentPage.value, pageSize },
    })
    if (res.data.code === 0) {
      history.value = res.data.data
    }
  } catch (e) {
    console.error('Failed to fetch history', e)
  } finally {
    loading.value = false
  }
}

function goToResult(taskId: number) {
  router.push({ name: 'task', params: { id: taskId } })
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchHistory()
}

onMounted(() => {
  fetchHistory()
})
</script>

<template>
  <div class="history-view">
    <h2 class="page-title">历史记录</h2>

    <div v-if="loading" style="text-align: center; padding: 60px">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <p style="margin-top: 12px; color: #999">加载中...</p>
    </div>

    <div v-else-if="history && history.items.length === 0" class="empty-state">
      <p>暂无历史记录</p>
      <el-button type="primary" @click="router.push('/')">去生成内容</el-button>
    </div>

    <div v-else-if="history" class="history-list">
      <div
        v-for="item in history.items"
        :key="item.taskId"
        class="history-item"
        @click="goToResult(item.taskId)"
      >
        <el-image
          v-if="item.coverUrl"
          :src="item.coverUrl"
          fit="cover"
          class="cover"
        />
        <div v-else class="cover cover-placeholder">
          <span>无封面</span>
        </div>

        <div class="info">
          <h3 class="title">{{ item.videoTitle || item.bvid }}</h3>
          <div class="meta">
            <span>{{ item.bvid }}</span>
            <span v-if="item.videoDuration"> · {{ formatDuration(item.videoDuration) }}</span>
            <span v-if="item.createdAt"> · {{ formatTime(item.createdAt) }}</span>
          </div>
          <div class="tags">
            <el-tag
              v-for="type in item.outputTypes"
              :key="type"
              size="small"
              type="info"
              class="output-tag"
            >
              {{ outputTypeLabels[type] || type }}
            </el-tag>
          </div>
        </div>

        <div class="status-col">
          <el-tag :type="statusType(item.status)" size="small">
            {{ statusLabel(item.status) }}
          </el-tag>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>
      </div>

      <div v-if="history.total > pageSize" class="pagination">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="history.total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.history-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 24px;
}
.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #999;
}
.empty-state p {
  margin-bottom: 16px;
}
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.history-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.history-item:hover {
  border-color: #00a1d6;
  box-shadow: 0 2px 8px rgba(0, 161, 214, 0.1);
}
.cover {
  width: 120px;
  height: 68px;
  border-radius: 6px;
  flex-shrink: 0;
  overflow: hidden;
}
.cover-placeholder {
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 12px;
}
.info {
  flex: 1;
  min-width: 0;
}
.info .title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.info .meta {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}
.tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.output-tag {
  font-size: 12px;
}
.status-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.arrow-icon {
  color: #ccc;
  font-size: 14px;
}
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
