<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getTemplates, createTemplate, updateTemplate, deleteTemplate, getTemplateCategories, createCategory, updateCategory, deleteCategory } from '../api/template'

interface Template {
  id: number
  name: string
  style: string
  length: string
  outputTypes: string[]
  customPromptIds: number[]
  categoryId: number | null
  categoryName?: string
}

interface TemplateCategory {
  id: number
  name: string
  sortOrder: number
}

const templates = ref<Template[]>([])
const categories = ref<TemplateCategory[]>([])
const dialogVisible = ref(false)
const editingTemplate = ref<Template | null>(null)
const categoryDialogVisible = ref(false)
const editingCategory = ref<TemplateCategory | null>(null)

const formRef = ref<FormInstance>()
const categoryFormRef = ref<FormInstance>()

const templateForm = ref({
  name: '',
  categoryId: null as number | null,
  style: 'concise',
  length: 'standard',
  outputTypes: ['summary', 'article'] as string[],
  customPromptIds: [] as number[]
})

const categoryForm = ref({
  name: ''
})

const templateRules: FormRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const categoryRules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

onMounted(async () => {
  await Promise.all([loadTemplates(), loadCategories()])
})

async function loadTemplates() {
  try {
    templates.value = await getTemplates()
  } catch (error: any) {
    ElMessage.error(error.message || '加载模板失败')
  }
}

async function loadCategories() {
  try {
    categories.value = await getTemplateCategories()
  } catch (error: any) {
    ElMessage.error(error.message || '加载分类失败')
  }
}

function openCreateDialog() {
  editingTemplate.value = null
  templateForm.value = {
    name: '',
    categoryId: null,
    style: 'concise',
    length: 'standard',
    outputTypes: ['summary', 'article'],
    customPromptIds: []
  }
  dialogVisible.value = true
}

function openEditDialog(template: Template) {
  editingTemplate.value = template
  templateForm.value = {
    name: template.name,
    categoryId: template.categoryId,
    style: template.style,
    length: template.length,
    outputTypes: template.outputTypes,
    customPromptIds: template.customPromptIds || []
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (editingTemplate.value) {
          await updateTemplate(editingTemplate.value.id, templateForm.value)
          ElMessage.success('更新成功')
        } else {
          await createTemplate(templateForm.value)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        await loadTemplates()
      } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
      }
    }
  })
}

async function handleDelete(template: Template) {
  try {
    await deleteTemplate(template.id)
    ElMessage.success('删除成功')
    await loadTemplates()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

function openCategoryCreateDialog() {
  editingCategory.value = null
  categoryForm.value = { name: '' }
  categoryDialogVisible.value = true
}

function openCategoryEditDialog(category: TemplateCategory) {
  editingCategory.value = category
  categoryForm.value = { name: category.name }
  categoryDialogVisible.value = true
}

async function handleCategorySave() {
  if (!categoryFormRef.value) return

  await categoryFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (editingCategory.value) {
          await updateCategory(editingCategory.value.id, categoryForm.value)
          ElMessage.success('更新成功')
        } else {
          await createCategory({ ...categoryForm.value, sortOrder: 0 })
          ElMessage.success('创建成功')
        }
        categoryDialogVisible.value = false
        await loadCategories()
      } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
      }
    }
  })
}

async function handleCategoryDelete(category: TemplateCategory) {
  try {
    await deleteCategory(category.id)
    ElMessage.success('删除成功')
    await loadCategories()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}
</script>

<template>
  <div class="template-view">
    <div class="page-header">
      <h1>模板管理</h1>
      <el-button type="primary" @click="openCreateDialog">创建模板</el-button>
    </div>

    <div class="content-layout">
      <div class="category-sidebar">
        <div class="sidebar-header">
          <span>分类</span>
          <el-button link @click="openCategoryCreateDialog">+ 新建</el-button>
        </div>
        <el-scrollbar class="category-list">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="category-item"
          >
            <span class="category-name">{{ cat.name }}</span>
            <el-button link size="small" @click="openCategoryEditDialog(cat)">编辑</el-button>
            <el-button link size="small" type="danger" @click="handleCategoryDelete(cat)">删除</el-button>
          </div>
        </el-scrollbar>
      </div>

      <div class="template-main">
        <div v-if="templates.length === 0" class="empty-state">
          <p>暂无模板</p>
        </div>
        <div v-else class="template-list">
          <div v-for="t in templates" :key="t.id" class="template-card">
            <div class="template-header">
              <span class="template-name">{{ t.name }}</span>
              <div class="template-actions">
                <el-button link @click="openEditDialog(t)">编辑</el-button>
                <el-button link type="danger" @click="handleDelete(t)">删除</el-button>
              </div>
            </div>
            <div class="template-meta">
              <el-tag v-if="t.categoryName" size="small">{{ t.categoryName }}</el-tag>
              <el-tag size="small" type="success">{{ t.style }}</el-tag>
              <el-tag size="small" type="info">{{ t.length }}</el-tag>
            </div>
            <div class="template-types">
              <span v-for="type in t.outputTypes" :key="type" class="type-badge">{{ type }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingTemplate ? '编辑模板' : '创建模板'"
      width="600px"
    >
      <el-form ref="formRef" :model="templateForm" :rules="templateRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="模板名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="templateForm.categoryId" placeholder="选择分类" clearable>
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="风格">
          <el-radio-group v-model="templateForm.style">
            <el-radio value="concise">简洁</el-radio>
            <el-radio value="detailed">详细</el-radio>
            <el-radio value="creative">创意</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="长度">
          <el-radio-group v-model="templateForm.length">
            <el-radio value="short">短</el-radio>
            <el-radio value="standard">标准</el-radio>
            <el-radio value="long">长</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="输出类型">
          <el-checkbox-group v-model="templateForm.outputTypes">
            <el-checkbox value="summary">总结</el-checkbox>
            <el-checkbox value="article">文章</el-checkbox>
            <el-checkbox value="card">学习卡片</el-checkbox>
            <el-checkbox value="xiaohongshu">小红书</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="categoryDialogVisible"
      :title="editingCategory ? '编辑分类' : '创建分类'"
      width="400px"
    >
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="分类名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCategorySave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.template-view {
  padding: 24px;
  max-width: 1400px;
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

.content-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
}

.category-sidebar {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8ecf1;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e8ecf1;
  font-weight: 500;
}

.category-list {
  height: calc(100vh - 200px);
}

.category-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f7fa;
  cursor: pointer;
}

.category-item:hover {
  background: #f8f9fb;
}

.category-name {
  flex: 1;
  font-size: 14px;
}

.template-main {
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

.template-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.template-card {
  border: 1px solid #e8ecf1;
  border-radius: 8px;
  padding: 16px;
  transition: box-shadow 0.2s;
}

.template-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.template-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.template-name {
  font-weight: 500;
  font-size: 15px;
}

.template-actions {
  display: flex;
  gap: 8px;
}

.template-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.template-types {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.type-badge {
  font-size: 12px;
  color: #666;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>
