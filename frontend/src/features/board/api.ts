import { apiClient } from '@/shared/api/client'

export interface BoardSummary {
  id: number
  slug: string
  name: string
  description: string | null
  commentEnabled: boolean
  attachmentEnabled: boolean
}

export interface BoardDetail extends BoardSummary {
  canWrite: boolean
}

export const boardApi = {
  list: () => apiClient.get<BoardSummary[]>('/boards').then((res) => res.data),
  detail: (slug: string) => apiClient.get<BoardDetail>(`/boards/${slug}`).then((res) => res.data),
}

export const boardKeys = {
  all: ['boards'] as const,
  list: () => [...boardKeys.all, 'list'] as const,
  detail: (slug: string) => [...boardKeys.all, 'detail', slug] as const,
}
