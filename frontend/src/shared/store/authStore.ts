import { create } from 'zustand'
import {
  refreshSession,
  setAccessToken,
  setSessionHandlers,
  type TokenResponse,
  type UserDto,
} from '@/shared/api/client'

export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous'

interface AuthState {
  user: UserDto | null
  status: AuthStatus
  /** 로그인 성공/재발급 결과를 반영한다. */
  setSession: (response: TokenResponse) => void
  /** 로그아웃하거나 세션이 끊겼을 때 */
  clearSession: () => void
  /** 앱 시작 시 쿠키로 세션 복원을 시도한다. */
  restore: () => Promise<void>
  isAdmin: () => boolean
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  status: 'idle',

  setSession: (response) => {
    setAccessToken(response.accessToken)
    set({ user: response.user, status: 'authenticated' })
  },

  clearSession: () => {
    setAccessToken(null)
    set({ user: null, status: 'anonymous' })
  },

  restore: async () => {
    set({ status: 'loading' })
    try {
      await refreshSession()
      // 성공 시 setSessionHandlers의 onRefreshed가 상태를 채운다.
    } catch {
      set({ user: null, status: 'anonymous' })
    }
  },

  isAdmin: () => get().user?.role === 'ADMIN',
}))

/**
 * API 클라이언트가 토큰을 재발급하거나 세션이 끊겼을 때 스토어를 갱신하도록 연결한다.
 * (클라이언트 → 스토어 단방향. 스토어가 클라이언트를 import 하는 구조라 순환이 생기지 않는다.)
 */
setSessionHandlers({
  onRefreshed: (response) => {
    useAuthStore.setState({ user: response.user, status: 'authenticated' })
  },
  onExpired: () => {
    useAuthStore.setState({ user: null, status: 'anonymous' })
  },
})
