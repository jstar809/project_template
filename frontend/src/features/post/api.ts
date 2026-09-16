import { apiClient } from '@/shared/api/client'

export interface PostListItem {
  id: number
  title: string
  authorNickname: string
  notice: boolean
  viewCount: number
  commentCount: number
  hasAttachment: boolean
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

export interface PostListResponse {
  notices: PostListItem[]
  page: PageResponse<PostListItem>
}

export interface Attachment {
  id: number
  url: string
  originalName: string
  contentType: string
  size: number
}

export interface PostDetail {
  id: number
  board: { slug: string; name: string }
  title: string
  content: string
  author: { id: number; nickname: string }
  notice: boolean
  viewCount: number
  commentCount: number
  attachments: Attachment[]
  canEdit: boolean
  canDelete: boolean
  createdAt: string
  updatedAt: string
}

export type SearchType = 'title' | 'content' | 'title_content' | 'author'

export interface PostListParams {
  page?: number
  size?: number
  searchType?: SearchType
  keyword?: string
}

export interface PostUpsertPayload {
  title: string
  content: string
  notice: boolean
  fileIds: number[]
  inlineFileIds: number[]
}

export const postApi = {
  list: (slug: string, params: PostListParams) =>
    apiClient.get<PostListResponse>(`/boards/${slug}/posts`, { params }).then((res) => res.data),

  detail: (id: number) => apiClient.get<PostDetail>(`/posts/${id}`).then((res) => res.data),

  create: (slug: string, payload: PostUpsertPayload) =>
    apiClient.post<PostDetail>(`/boards/${slug}/posts`, payload).then((res) => res.data),

  update: (id: number, payload: PostUpsertPayload) =>
    apiClient.put<PostDetail>(`/posts/${id}`, payload).then((res) => res.data),

  remove: (id: number) => apiClient.delete<void>(`/posts/${id}`).then((res) => res.data),
}

export const postKeys = {
  all: ['posts'] as const,
  list: (slug: string, params: PostListParams) => [...postKeys.all, 'list', slug, params] as const,
  detail: (id: number) => [...postKeys.all, 'detail', id] as const,
}
