import axios from 'axios'

export interface Template {
  id: number
  name: string
  style: string
  length: string
  outputTypes: string[]
  customPromptIds: number[]
  categoryId: number | null
  categoryName?: string
  createdAt?: string
  updatedAt?: string
}

export interface TemplateRequest {
  name: string
  categoryId?: number | null
  style: string
  length: string
  outputTypes: string[]
  customPromptIds?: number[]
}

export interface TemplateCategory {
  id: number
  name: string
  sortOrder: number
  userId: number
  createdAt?: string
  updatedAt?: string
}

export function getTemplates() {
  return axios.get<{ code: number; data: Template[] }>('/api/templates').then(res => res.data.data)
}

export function getTemplate(id: number) {
  return axios.get<{ code: number; data: Template }>(`/api/templates/${id}`).then(res => res.data.data)
}

export function createTemplate(data: TemplateRequest) {
  return axios.post<{ code: number; data: Template }>('/api/templates', data).then(res => res.data.data)
}

export function updateTemplate(id: number, data: TemplateRequest) {
  return axios.put<{ code: number; data: Template }>(`/api/templates/${id}`, data).then(res => res.data.data)
}

export function deleteTemplate(id: number) {
  return axios.delete<{ code: number; data: void }>(`/api/templates/${id}`).then(res => res.data.data)
}

export function getTemplateCategories() {
  return axios.get<{ code: number; data: TemplateCategory[] }>('/api/template-categories').then(res => res.data.data)
}

export function createCategory(data: { name: string; sortOrder?: number }) {
  return axios.post<{ code: number; data: TemplateCategory }>('/api/template-categories', data).then(res => res.data.data)
}

export function updateCategory(id: number, data: { name: string; sortOrder?: number }) {
  return axios.put<{ code: number; data: void }>(`/api/template-categories/${id}`, { id, ...data }).then(res => res.data.data)
}

export function deleteCategory(id: number) {
  return axios.delete<{ code: number; data: void }>(`/api/template-categories/${id}`).then(res => res.data.data)
}
