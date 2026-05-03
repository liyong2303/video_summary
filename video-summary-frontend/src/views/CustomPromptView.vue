<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()

interface CustomPrompt {
  id?: number
  name: string
  outputType: string
  systemPrompt: string
  userPrompt: string
  isDefault: boolean
  createdAt?: string
}

const loading = ref(false)
const prompts = ref<CustomPrompt[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingPrompt = ref<CustomPrompt>({
  name: '',
  outputType: 'summary',
  systemPrompt: '',
  userPrompt: '',
  isDefault: false,
})

const formRef = ref<FormInstance>()
const saving = ref(false)

const formRules: FormRules = {
  name: [{ required: true, message: '请输入Prompt名称', trigger: 'blur' }],
  outputType: [{ required: true, message: '请选择输出类型', trigger: 'change' }],
  systemPrompt: [{ required: true, message: '请输入系统Prompt', trigger: 'blur' }],
  userPrompt: [{ required: true, message: '请输入用户Prompt', trigger: 'blur' }],
}

const outputTypeOptions = [
  { label: '总结', value: 'summary' },
  { label: '文章', value: 'article' },
  { label: '学习卡片', value: 'card' },
  { label: '小红书文案', value: 'xiaohongshu' },
]

async function loadPrompts() {
  loading.value = true
  try {
    const res = await axios.get('/api/custom-prompt')
    if (res.data.code === 0 && res.data.data) {
      prompts.value = res.data.data
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载自定义Prompt失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  dialogMode.value = 'create'
  editingPrompt.value = {
    name: '',
    outputType: 'summary',
    systemPrompt: '',
    userPrompt: '',
    isDefault: false,
  }
  dialogVisible.value = true
}

function openEditDialog(prompt: CustomPrompt) {
  dialogMode.value = 'edit'
  editingPrompt.value = { ...prompt }
  dialogVisible.value = true
}

async function savePrompt() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      if (dialogMode.value === 'create') {
        await axios.post('/api/custom-prompt', editingPrompt.value)
        ElMessage.success('创建成功')
      } else {
        await axios.put(`/api/custom-prompt/${editingPrompt.value.id}`, editingPrompt.value)
        ElMessage.success('更新成功')
      }
      dialogVisible.value = false
      await loadPrompts()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

async function deletePrompt(prompt: CustomPrompt) {
  try {
    await ElMessageBox.confirm('确定要删除这个自定义Prompt吗？', '确认删除', {
      type: 'warning',
    })
    await axios.delete(`/api/custom-prompt/${prompt.id}`)
    ElMessage.success('删除成功')
    await loadPrompts()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

async function setAsDefault(prompt: CustomPrompt) {
  try {
    await axios.post(`/api/custom-prompt/${prompt.id}/set-default`, null, {
      params: { outputType: prompt.outputType }
    })
    ElMessage.success('已设为默认')
    await loadPrompts()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '设置失败')
  }
}

function getOutputTypeLabel(type: string): string {
  const option = outputTypeOptions.find(opt => opt.value === type)
  return option ? option.label : type
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadPrompts()
})
</script>

<template>
  <div class="custom-prompt-view">
    <div class="prompt-container">
      <div class="prompt-header">
        <h2>自定义 Prompt 管理</h2>
        <el-button @click="goBack" text>返回</el-button>
      </div>

      <el-card shadow="never">
        <div class="actions-bar">
          <el-button type="primary" @click="openCreateDialog">
            创建新 Prompt
          </el-button>
        </div>

        <div v-loading="loading" class="prompt-list">
          <el-empty v-if="!loading && prompts.length === 0" description="暂无自定义Prompt" />
          <el-card
            v-for="prompt in prompts"
            :key="prompt.id"
            class="prompt-card"
            shadow="never"
          >
            <div class="prompt-header-row">
              <div class="prompt-title">
                <h3>{{ prompt.name }}</h3>
                <el-tag v-if="prompt.isDefault" type="success" size="small">默认</el-tag>
              </div>
              <div class="prompt-actions">
                <el-button
                  v-if="!prompt.isDefault"
                  type="primary"
                  link
                  size="small"
                  @click="setAsDefault(prompt)"
                >
                  设为默认
                </el-button>
                <el-button type="primary" link size="small" @click="openEditDialog(prompt)">
                  编辑
                </el-button>
                <el-button type="danger" link size="small" @click="deletePrompt(prompt)">
                  删除
                </el-button>
              </div>
            </div>
            <div class="prompt-meta">
              <el-tag size="small">{{ getOutputTypeLabel(prompt.outputType) }}</el-tag>
              <span class="prompt-date">{{ prompt.createdAt }}</span>
            </div>
            <div class="prompt-content">
              <div class="prompt-section">
                <strong>系统 Prompt：</strong>
                <p>{{ prompt.systemPrompt }}</p>
              </div>
              <div class="prompt-section">
                <strong>用户 Prompt：</strong>
                <p>{{ prompt.userPrompt }}</p>
              </div>
            </div>
          </el-card>
        </div>
      </el-card>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '创建自定义 Prompt' : '编辑自定义 Prompt'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="editingPrompt"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="editingPrompt.name" placeholder="给这个Prompt起个名字" />
        </el-form-item>
        <el-form-item label="输出类型" prop="outputType">
          <el-select v-model="editingPrompt.outputType" placeholder="选择输出类型">
            <el-option
              v-for="option in outputTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="系统 Prompt" prop="systemPrompt">
          <el-input
            v-model="editingPrompt.systemPrompt"
            type="textarea"
            :rows="4"
            placeholder="AI的角色设定和指导原则"
          />
        </el-form-item>
        <el-form-item label="用户 Prompt" prop="userPrompt">
          <el-input
            v-model="editingPrompt.userPrompt"
            type="textarea"
            :rows="4"
            placeholder="具体的任务描述和输出要求"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePrompt">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.custom-prompt-view {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
}
.prompt-container {
  background: #fff;
}
.prompt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.prompt-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}
.actions-bar {
  margin-bottom: 20px;
}
.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.prompt-card {
  border: 1px solid #e8ecf1;
}
.prompt-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}
.prompt-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.prompt-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.prompt-actions {
  display: flex;
  gap: 8px;
}
.prompt-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.prompt-date {
  font-size: 12px;
  color: #999;
}
.prompt-content {
  padding: 12px;
  background: #f8f9fb;
  border-radius: 6px;
}
.prompt-section {
  margin-bottom: 12px;
}
.prompt-section:last-child {
  margin-bottom: 0;
}
.prompt-section strong {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}
.prompt-section p {
  margin: 0;
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .custom-prompt-view {
    padding: 20px 12px;
  }
  .prompt-header-row {
    flex-direction: column;
    gap: 12px;
  }
  .prompt-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
