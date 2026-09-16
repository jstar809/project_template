import { apiClient, type UserDto } from '@/shared/api/client'

export interface UpdateNicknamePayload {
  nickname: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export const userApi = {
  me: () => apiClient.get<UserDto>('/users/me').then((res) => res.data),

  updateNickname: (payload: UpdateNicknamePayload) =>
    apiClient.patch<UserDto>('/users/me', payload).then((res) => res.data),

  changePassword: (payload: ChangePasswordPayload) =>
    apiClient.patch<void>('/users/me/password', payload).then((res) => res.data),
}

export const userKeys = {
  all: ['users'] as const,
  me: () => [...userKeys.all, 'me'] as const,
}
