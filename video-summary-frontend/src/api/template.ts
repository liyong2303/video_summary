import request from './request'

export interface Template {
  id: number
  name: string
  style: string
  length: string
  outputTypes: string[]
  customPromptIds?: number[]
  categoryId?: number | null
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
  return request<Template[]>({
    url: '/api/templates',
    method: 'get'
  })
}

export function getTemplate(id: number) {
  return request<Template>({
    url: `/api/templates/${id}`,
    method: 'get'
  })
}

export function createTemplate(data: TemplateRequest) {
  return request<Template>({
    url: '/api/templates',
    method: 'post',
    data
  })
}

export function updateTemplate(id: number, data: TemplateRequest) {
  return request<Template>({
    url: `/api/templates/${id}`,
    method: 'put',
    data
  })
}

export function deleteTemplate(id: number) {
  return request<void>({
    url: `/api/templates/${id}`,
    method: 'delete'
  })
}

export function getTemplateCategories() {
  return request<TemplateCategory[]>({
    url: '/api/template-categories',
    method: 'get'
  })
}

export function createCategory(data: { name: string; sortOrder?: number }) {
  return request<TemplateCategory>({
    url: '/api/template-categories',
    method: 'post',
    data
  })
}

export function updateCategory(id: number, data: { name: string; sortOrder?: number }) {
  return request<void>({
    url: `/api/template-categories/${id}`,
    method: 'put',
    data: { id, ...data }
  })
}

export function deleteCategory(id: number) {
  return request<void>({
    url: `/api/template-categories/${id}`,
    method: 'delete'
  })
}
