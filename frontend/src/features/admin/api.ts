import { apiClient } from '@/shared/api/client'
import type { PageResponse } from '@/features/post/api'

export interface Totals {
  users: number
  posts: number
  comments: number
}

export interface DailyCount {
  date: string
  count: number
}

export interface RecentPost {
  id: number
  title: string
  boardName: string
  authorNickname: string
  createdAt: string
}

export interface Dashboard {
  totals: Totals
  dailySignups: DailyCount[]
  dailyPosts: DailyCount[]
  recentPosts: RecentPost[]
}

export interface AdminBoard {
  id: number
  slug: string
  name: string
  description: string | null
  readPermission: 'ANONYMOUS' | 'USER' | 'ADMIN'
  writePermission: 'ANONYMOUS' | 'USER' | 'ADMIN'
  commentEnabled: boolean
  attachmentEnabled: boolean
  active: boolean
  sortOrder: number
  postCount: number
  createdAt: string
}

export type BoardUpsertPayload = Omit<AdminBoard, 'id' | 'sortOrder' | 'postCount' | 'createdAt'>

export interface AdminPost {
  id: number
  title: string
  boardSlug: string
  boardName: string
  authorNickname: string
  status: 'PUBLISHED' | 'HIDDEN'
  deleted: boolean
  viewCount: number
  commentCount: number
  createdAt: string
}

export interface AdminComment {
  id: number
  postId: number
  postTitle: string
  content: string
  authorNickname: string
  status: 'PUBLISHED' | 'HIDDEN'
  deleted: boolean
  createdAt: string
}

export interface AdminUser {
  id: number
  email: string
  nickname: string
  role: 'USER' | 'ADMIN'
  status: 'ACTIVE' | 'SUSPENDED'
  postCount: number
  lastLoginAt: string | null
  createdAt: string
}

export const adminApi = {
  dashboard: () => apiClient.get<Dashboard>('/admin/dashboard').then((res) => res.data),

  // 게시판
  boards: () => apiClient.get<AdminBoard[]>('/admin/boards').then((res) => res.data),
  createBoard: (payload: BoardUpsertPayload) =>
    apiClient.post<AdminBoard>('/admin/boards', payload).then((res) => res.data),
  updateBoard: (id: number, payload: BoardUpsertPayload) =>
    apiClient.put<AdminBoard>(`/admin/boards/${id}`, payload).then((res) => res.data),
  deleteBoard: (id: number) =>
    apiClient.delete<void>(`/admin/boards/${id}`).then((res) => res.data),
  reorderBoards: (boardIds: number[]) =>
    apiClient.put<void>('/admin/boards/order', { boardIds }).then((res) => res.data),

  // 게시글
  posts: (params: { boardId?: number; status?: string; keyword?: string; page?: number }) =>
    apiClient.get<PageResponse<AdminPost>>('/admin/posts', { params }).then((res) => res.data),
  changePostStatus: (ids: number[], status: 'PUBLISHED' | 'HIDDEN') =>
    apiClient.patch<void>('/admin/posts/status', { ids, status }).then((res) => res.data),
  deletePosts: (ids: number[]) =>
    apiClient.delete<void>('/admin/posts', { data: { ids } }).then((res) => res.data),

  // 댓글
  comments: (params: { status?: string; keyword?: string; page?: number }) =>
    apiClient
      .get<PageResponse<AdminComment>>('/admin/comments', { params })
      .then((res) => res.data),
  changeCommentStatus: (ids: number[], status: 'PUBLISHED' | 'HIDDEN') =>
    apiClient.patch<void>('/admin/comments/status', { ids, status }).then((res) => res.data),
  deleteComments: (ids: number[]) =>
    apiClient.delete<void>('/admin/comments', { data: { ids } }).then((res) => res.data),

  // 사용자
  users: (params: { keyword?: string; role?: string; status?: string; page?: number }) =>
    apiClient.get<PageResponse<AdminUser>>('/admin/users', { params }).then((res) => res.data),
  changeUserRole: (id: number, role: 'USER' | 'ADMIN') =>
    apiClient.patch<void>(`/admin/users/${id}/role`, { role }).then((res) => res.data),
  changeUserStatus: (id: number, status: 'ACTIVE' | 'SUSPENDED') =>
    apiClient.patch<void>(`/admin/users/${id}/status`, { status }).then((res) => res.data),
}

export const adminKeys = {
  all: ['admin'] as const,
  dashboard: () => [...adminKeys.all, 'dashboard'] as const,
  boards: () => [...adminKeys.all, 'boards'] as const,
  posts: (params: unknown) => [...adminKeys.all, 'posts', params] as const,
  comments: (params: unknown) => [...adminKeys.all, 'comments', params] as const,
  users: (params: unknown) => [...adminKeys.all, 'users', params] as const,
}
