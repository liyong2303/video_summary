<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const props = defineProps<{
  visible: boolean
  taskId: number
  outputType: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'rollback': []
}>()

const histories = ref<any[]>([])
const loading = ref(false)

async function fetchHistories() {
  loading.value = true
  try {
    const res = await axios.get(`/api/video/${props.taskId}/result/${props.outputType}/history`)
    if (res.data.code === 0) {
      histories.value = res.data.data
    }
  } catch (e) {
    console.error('Failed to fetch histories', e)
  } finally {
    loading.value = false
  }
}

async function rollback(version: number) {
  try {
    await axios.post(`/api/video/${props.taskId}/result/${props.outputType}/rollback/${version}`)
    ElMessage.success('回滚成功')
    emit('rollback')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '回滚失败')
  }
}

watch(() => props.visible, (val) => {
  if (val) fetchHistories()
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="编辑历史"
    width="600px"
  >
    <el-timeline v-loading="loading">
      <el-timeline-item
        v-for="h in histories"
        :key="h.id"
        :timestamp="new Date(h.createdAt).toLocaleString()"
      >
        <div class="history-item">
          <div class="history-header">
            <span class="version-badge">v{{ h.version }}</span>
            <el-button
              v-if="h.version !== histories[0]?.version"
              type="primary"
              size="small"
              link
              @click="rollback(h.version)"
            >
              回滚到此版本
            </el-button>
          </div>
          <div class="history-content">{{ h.content }}</div>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="!loading && histories.length === 0" description="暂无历史记录" />
  </el-dialog>
</template>

<style scoped>
.version-badge {
  background: #409eff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
}
.history-content {
  margin-top: 8px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  max-height: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
