import { apiClient } from '@/shared/api/client'

export interface Comment {
  id: number
  content: string
  authorId: number | null
  authorNickname: string | null
  deleted: boolean
  hidden: boolean
  canEdit: boolean
  canDelete: boolean
  createdAt: string
  updatedAt: string
  replies: Comment[]
}

export const commentApi = {
  list: (postId: number) =>
    apiClient.get<Comment[]>(`/posts/${postId}/comments`).then((res) => res.data),

  create: (postId: number, payload: { content: string; parentId?: number }) =>
    apiClient.post<void>(`/posts/${postId}/comments`, payload).then((res) => res.data),

  update: (id: number, content: string) =>
    apiClient.put<void>(`/comments/${id}`, { content }).then((res) => res.data),

  remove: (id: number) => apiClient.delete<void>(`/comments/${id}`).then((res) => res.data),
}

export const commentKeys = {
  all: ['comments'] as const,
  list: (postId: number) => [...commentKeys.all, 'list', postId] as const,
}
