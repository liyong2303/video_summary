<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()

const style = ref('concise')
const length = ref('standard')
const outputTypes = ref<string[]>(['summary', 'article', 'card', 'xiaohongshu'])
const loading = ref(false)
const saving = ref(false)

const styleOptions = [
  { label: '学术严谨', value: 'academic', description: '专业、客观、有逻辑的语言风格' },
  { label: '轻松口语', value: 'casual', description: '口语化、生动有趣的表达方式' },
  { label: '干货简洁', value: 'concise', description: '最简洁的语言传递核心信息' },
]

const lengthOptions = [
  { label: '简短', value: 'short', description: '标准长度的50%左右' },
  { label: '标准', value: 'standard', description: '平衡详细度和简洁性' },
  { label: '详细', value: 'long', description: '标准长度的150%，添加更多细节' },
]

const outputTypeOptions = [
  { label: '总结', value: 'summary' },
  { label: '文章', value: 'article' },
  { label: '学习卡片', value: 'card' },
  { label: '小红书文案', value: 'xiaohongshu' },
]

async function loadPreferences() {
  loading.value = true
  try {
    const res = await axios.get('/api/preference')
    if (res.data.code === 0 && res.data.data) {
      const pref = res.data.data
      if (pref.style) style.value = pref.style
      if (pref.length) length.value = pref.length
      if (pref.outputTypes && Array.isArray(pref.outputTypes)) {
        outputTypes.value = pref.outputTypes
      }
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载设置失败')
  } finally {
    loading.value = false
  }
}

async function savePreferences() {
  saving.value = true
  try {
    await axios.post('/api/preference', {
      style: style.value,
      length: length.value,
      outputTypes: outputTypes.value,
    })
    ElMessage.success('设置已保存')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存设置失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadPreferences()
})
</script>

<template>
  <div class="settings-view">
    <div class="settings-container">
      <div class="settings-header">
        <h2>偏好设置</h2>
        <el-button @click="goBack" text>返回</el-button>
      </div>

      <el-card v-loading="loading" shadow="never">
        <el-form label-width="100px">
          <el-form-item label="生成风格">
            <el-radio-group v-model="style" direction="vertical">
              <el-radio
                v-for="option in styleOptions"
                :key="option.value"
                :value="option.value"
              >
                <div class="radio-option">
                  <span class="option-label">{{ option.label }}</span>
                  <span class="option-desc">{{ option.description }}</span>
                </div>
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="内容长度">
            <el-radio-group v-model="length" direction="vertical">
              <el-radio
                v-for="option in lengthOptions"
                :key="option.value"
                :value="option.value"
              >
                <div class="radio-option">
                  <span class="option-label">{{ option.label }}</span>
                  <span class="option-desc">{{ option.description }}</span>
                </div>
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="默认生成">
            <el-checkbox-group v-model="outputTypes">
              <el-checkbox
                v-for="type in outputTypeOptions"
                :key="type.value"
                :value="type.value"
              >
                {{ type.label }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="savePreferences">
              保存设置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.settings-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}
.settings-container {
  background: #fff;
}
.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.settings-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}
.radio-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 8px;
}
.option-label {
  font-weight: 500;
  color: #333;
}
.option-desc {
  font-size: 13px;
  color: #888;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .settings-view {
    padding: 20px 12px;
  }
  .el-form-item {
    margin-bottom: 20px;
  }
}
</style>
