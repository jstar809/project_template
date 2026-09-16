import { apiClient, type TokenResponse, type UserDto } from '@/shared/api/client'

export interface SignupPayload {
  email: string
  password: string
  nickname: string
}

export interface LoginPayload {
  email: string
  password: string
}

export const authApi = {
  signup: (payload: SignupPayload) =>
    apiClient.post<UserDto>('/auth/signup', payload).then((res) => res.data),

  login: (payload: LoginPayload) =>
    apiClient.post<TokenResponse>('/auth/login', payload).then((res) => res.data),

  logout: () => apiClient.post<void>('/auth/logout').then((res) => res.data),
}
