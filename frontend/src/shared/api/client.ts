import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { env } from '@/shared/lib/env'
import { toApiError } from './errors'

/** 서버가 내려주는 사용자 정보 */
export interface UserDto {
  id: number
  email: string
  nickname: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export interface TokenResponse {
  accessToken: string
  expiresIn: number
  user: UserDto
}

/**
 * Access Token은 메모리에만 둔다.
 * localStorage에 두면 XSS 스크립트가 훔쳐갈 수 있기 때문이다(PRD FR-AUTH-03).
 * 새로고침하면 사라지므로, 앱 시작 시 /auth/refresh 로 세션을 복원한다.
 */
let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}

/** 재발급 성공/실패를 authStore에 알리기 위한 콜백 */
interface SessionHandlers {
  onRefreshed: (response: TokenResponse) => void
  onExpired: () => void
}

let handlers: SessionHandlers = { onRefreshed: () => {}, onExpired: () => {} }

export function setSessionHandlers(next: SessionHandlers): void {
  handlers = next
}

/** 인터셉터가 없는 클라이언트. 재발급 호출이 다시 인터셉터를 타는 것을 막는다. */
const rawClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true, // refresh token 쿠키를 주고받는다
})

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
})

apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

/**
 * 재발급 요청은 동시에 하나만 보낸다(single-flight).
 *
 * 토큰이 만료된 순간 여러 요청이 동시에 401을 받는데, 각자 재발급을 호출하면
 * 서버의 rotation 때문에 뒤늦은 요청이 "폐기된 토큰"으로 취급되어 로그아웃된다.
 * 그래서 첫 요청이 만든 Promise를 나머지가 함께 기다린다.
 */
let refreshPromise: Promise<string> | null = null

export async function refreshSession(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = rawClient
      .post<TokenResponse>('/auth/refresh')
      .then((response) => {
        setAccessToken(response.data.accessToken)
        handlers.onRefreshed(response.data)
        return response.data.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

/** 재발급을 시도하지 않을 경로 (재발급 자체가 401이면 그냥 실패시킨다) */
const AUTH_PATHS = ['/auth/login', '/auth/signup', '/auth/refresh', '/auth/logout']

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!(error instanceof AxiosError) || !error.config) {
      return Promise.reject(toApiError(error))
    }

    const config = error.config as RetriableConfig
    const isAuthPath = AUTH_PATHS.some((path) => config.url?.startsWith(path))
    const shouldRetry = error.response?.status === 401 && !config._retried && !isAuthPath

    if (!shouldRetry) {
      return Promise.reject(toApiError(error))
    }

    config._retried = true
    try {
      const token = await refreshSession()
      config.headers.Authorization = `Bearer ${token}`
      return await apiClient.request(config)
    } catch {
      setAccessToken(null)
      handlers.onExpired()
      return Promise.reject(toApiError(error))
    }
  },
)
