import axios from 'axios'

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
  return axios.get<{ code: number; data: QuickAction[] }>('/api/quick-actions').then(res => res.data.data)
}

export function getQuickAction(id: number) {
  return axios.get<{ code: number; data: QuickAction }>(`/api/quick-actions/${id}`).then(res => res.data.data)
}

export function createQuickAction(data: QuickActionRequest) {
  return axios.post<{ code: number; data: QuickAction }>('/api/quick-actions', data).then(res => res.data.data)
}

export function updateQuickAction(id: number, data: QuickActionRequest) {
  return axios.put<{ code: number; data: QuickAction }>(`/api/quick-actions/${id}`, data).then(res => res.data.data)
}

export function deleteQuickAction(id: number) {
  return axios.delete<{ code: number; data: void }>(`/api/quick-actions/${id}`).then(res => res.data.data)
}
