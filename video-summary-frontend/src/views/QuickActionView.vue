<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getQuickActions, createQuickAction, updateQuickAction, deleteQuickAction } from '../api/quickAction'

interface QuickAction {
  id: number
  name: string
  steps: Array<{ action: string; params: any }>
  applyScope: string
  createdAt?: string
  updatedAt?: string
}

const quickActions = ref<QuickAction[]>([])
const dialogVisible = ref(false)
const editingQuickAction = ref<QuickAction | null>(null)

const formRef = ref<FormInstance>()

const quickActionForm = ref({
  name: '',
  steps: [] as Array<{ action: string; params: any }>,
  applyScope: 'single'
})

const quickActionRules: FormRules = {
  name: [{ required: true, message: '请输入操作名称', trigger: 'blur' }]
}

const actionOptions = [
  { label: '复制内容', value: 'copy' },
  { label: '导出Markdown', value: 'export' },
  { label: '重新生成', value: 'regenerate' }
]

onMounted(async () => {
  await loadQuickActions()
})

async function loadQuickActions() {
  try {
    quickActions.value = await getQuickActions()
  } catch (error: any) {
    ElMessage.error(error.message || '加载快捷操作失败')
  }
}

function openCreateDialog() {
  editingQuickAction.value = null
  quickActionForm.value = {
    name: '',
    steps: [{ action: 'copy', params: {} }],
    applyScope: 'single'
  }
  dialogVisible.value = true
}

function openEditDialog(action: QuickAction) {
  editingQuickAction.value = action
  quickActionForm.value = {
    name: action.name,
    steps: JSON.parse(JSON.stringify(action.steps)),
    applyScope: action.applyScope
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (editingQuickAction.value) {
          await updateQuickAction(editingQuickAction.value.id, quickActionForm.value)
          ElMessage.success('更新成功')
        } else {
          await createQuickAction(quickActionForm.value)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        await loadQuickActions()
      } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
      }
    }
  })
}

async function handleDelete(action: QuickAction) {
  try {
    await deleteQuickAction(action.id)
    ElMessage.success('删除成功')
    await loadQuickActions()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

function addStep() {
  quickActionForm.value.steps.push({ action: 'copy', params: {} })
}

function removeStep(index: number) {
  quickActionForm.value.steps.splice(index, 1)
}
</script>

<template>
  <div class="quick-action-view">
    <div class="page-header">
      <h1>快捷操作</h1>
      <el-button type="primary" @click="openCreateDialog">创建快捷操作</el-button>
    </div>

    <div class="action-list">
      <div v-if="quickActions.length === 0" class="empty-state">
        <p>暂无快捷操作</p>
      </div>
      <div v-else class="grid">
        <div v-for="action in quickActions" :key="action.id" class="action-card">
          <div class="action-header">
            <span class="action-name">{{ action.name }}</span>
            <div class="action-actions">
              <el-button link @click="openEditDialog(action)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(action)">删除</el-button>
            </div>
          </div>
          <div class="action-meta">
            <el-tag size="small" type="info">{{ action.applyScope === 'single' ? '单个任务' : '批量任务' }}</el-tag>
            <span class="step-count">{{ action.steps.length }} 个步骤</span>
          </div>
          <div class="action-steps">
            <div v-for="(step, idx) in action.steps" :key="idx" class="step-item">
              <span class="step-index">{{ idx + 1 }}.</span>
              <span class="step-action">{{ step.action }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingQuickAction ? '编辑快捷操作' : '创建快捷操作'"
      width="600px"
    >
      <el-form ref="formRef" :model="quickActionForm" :rules="quickActionRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="quickActionForm.name" placeholder="操作名称" />
        </el-form-item>
        <el-form-item label="应用范围">
          <el-radio-group v-model="quickActionForm.applyScope">
            <el-radio value="single">单个任务</el-radio>
            <el-radio value="batch">批量任务</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="操作步骤">
          <div class="steps-container">
            <div
              v-for="(step, index) in quickActionForm.steps"
              :key="index"
              class="step-row"
            >
              <span class="step-label">{{ index + 1 }}.</span>
              <el-select v-model="step.action" class="step-select">
                <el-option
                  v-for="opt in actionOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-button
                link
                type="danger"
                size="small"
                @click="removeStep(index)"
                :disabled="quickActionForm.steps.length <= 1"
              >
                删除
              </el-button>
            </div>
            <el-button link @click="addStep">+ 添加步骤</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.quick-action-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.action-list {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8ecf1;
  padding: 24px;
  min-height: 400px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
  color: #999;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.action-card {
  border: 1px solid #e8ecf1;
  border-radius: 8px;
  padding: 16px;
  transition: box-shadow 0.2s;
}

.action-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.action-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.action-name {
  font-weight: 500;
  font-size: 15px;
}

.action-actions {
  display: flex;
  gap: 8px;
}

.action-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: #666;
}

.step-count {
  color: #999;
}

.action-steps {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.step-index {
  font-weight: 500;
  color: #999;
}

.step-action {
  flex: 1;
}

.steps-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.step-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-label {
  width: 24px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.step-select {
  flex: 1;
}
</style>
