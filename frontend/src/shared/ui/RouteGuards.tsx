import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/shared/store/authStore'
import { FullPageSpinner } from './FullPageSpinner'

/**
 * 로그인이 필요한 라우트를 감싼다.
 * 세션 복원이 끝나기 전에 판단하면 새로고침할 때마다 로그인 화면이 깜빡이므로, 복원 중에는 로딩을 보여준다.
 */
export function RequireAuth() {
  const status = useAuthStore((state) => state.status)
  const location = useLocation()

  if (status === 'idle' || status === 'loading') {
    return <FullPageSpinner />
  }
  if (status !== 'authenticated') {
    // 로그인 후 원래 가려던 곳으로 돌려보내기 위해 경로를 넘긴다.
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />
  }
  return <Outlet />
}

/** 특정 역할만 접근할 수 있는 라우트를 감싼다. */
export function RequireRole({ role }: { role: 'ADMIN' | 'USER' }) {
  const { status, user } = useAuthStore()
  const location = useLocation()

  if (status === 'idle' || status === 'loading') {
    return <FullPageSpinner />
  }
  if (status !== 'authenticated') {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />
  }
  if (user?.role !== role) {
    return <Navigate to="/403" replace />
  }
  return <Outlet />
}

/** 로그인한 사용자는 볼 필요가 없는 화면(로그인, 회원가입)을 감싼다. */
export function GuestOnly() {
  const status = useAuthStore((state) => state.status)

  if (status === 'idle' || status === 'loading') {
    return <FullPageSpinner />
  }
  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
