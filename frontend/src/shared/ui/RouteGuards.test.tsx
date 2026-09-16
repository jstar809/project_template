import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { GuestOnly, RequireAuth, RequireRole } from './RouteGuards'
import { useAuthStore } from '@/shared/store/authStore'
import { adminUser, testUser } from '@/test/msw/handlers'
import { renderWithProviders } from '@/test/renderWithProviders'

function TestRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<p>로그인 화면</p>} />
      <Route path="/403" element={<p>권한 없음</p>} />
      <Route path="/" element={<p>홈</p>} />
      <Route element={<RequireAuth />}>
        <Route path="/me" element={<p>내 정보</p>} />
      </Route>
      <Route element={<RequireRole role="ADMIN" />}>
        <Route path="/admin" element={<p>관리자</p>} />
      </Route>
      <Route element={<GuestOnly />}>
        <Route path="/signup" element={<p>회원가입 화면</p>} />
      </Route>
    </Routes>
  )
}

describe('라우트 가드', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, status: 'anonymous' })
  })

  it('비로그인 상태로 보호된 화면에 가면 로그인으로 이동한다', () => {
    renderWithProviders(<TestRoutes />, { route: '/me' })
    expect(screen.getByText('로그인 화면')).toBeInTheDocument()
  })

  it('세션 복원 중에는 로딩을 보여준다', () => {
    useAuthStore.setState({ user: null, status: 'loading' })
    renderWithProviders(<TestRoutes />, { route: '/me' })
    expect(screen.getByRole('status')).toBeInTheDocument()
  })

  it('로그인 상태에서는 보호된 화면을 보여준다', () => {
    useAuthStore.setState({ user: testUser, status: 'authenticated' })
    renderWithProviders(<TestRoutes />, { route: '/me' })
    expect(screen.getByText('내 정보')).toBeInTheDocument()
  })

  it('일반 사용자가 관리자 화면에 가면 403으로 보낸다', () => {
    useAuthStore.setState({ user: testUser, status: 'authenticated' })
    renderWithProviders(<TestRoutes />, { route: '/admin' })
    expect(screen.getByText('권한 없음')).toBeInTheDocument()
  })

  it('관리자는 관리자 화면을 볼 수 있다', () => {
    useAuthStore.setState({ user: adminUser, status: 'authenticated' })
    renderWithProviders(<TestRoutes />, { route: '/admin' })
    expect(screen.getByText('관리자')).toBeInTheDocument()
  })

  it('로그인한 사용자가 회원가입 화면에 가면 홈으로 보낸다', () => {
    useAuthStore.setState({ user: testUser, status: 'authenticated' })
    renderWithProviders(<TestRoutes />, { route: '/signup' })
    expect(screen.getByText('홈')).toBeInTheDocument()
  })
})
