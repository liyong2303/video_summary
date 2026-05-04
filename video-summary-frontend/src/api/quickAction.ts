import request from './request'

export interface QuickAction {
  id: number
  name: string
  steps: Array<{ action: string; params: any }>
  applyScope: string
  createdAt?: string
  updatedAt?: string
}

export interface QuickActionRequest {
  name: string
  steps: Array<{ action: string; params: any }>
  applyScope: string
}

export function getQuickActions() {
  return request<QuickAction[]>({
    url: '/api/quick-actions',
    method: 'get'
  })
}

export function getQuickAction(id: number) {
  return request<QuickAction>({
    url: `/api/quick-actions/${id}`,
    method: 'get'
  })
}

export function createQuickAction(data: QuickActionRequest) {
  return request<QuickAction>({
    url: '/api/quick-actions',
    method: 'post',
    data
  })
}

export function updateQuickAction(id: number, data: QuickActionRequest) {
  return request<QuickAction>({
    url: `/api/quick-actions/${id}`,
    method: 'put',
    data
  })
}

export function deleteQuickAction(id: number) {
  return request<void>({
    url: `/api/quick-actions/${id}`,
    method: 'delete'
  })
}
