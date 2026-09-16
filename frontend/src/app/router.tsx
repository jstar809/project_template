import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { AdminLayout } from './layouts/AdminLayout'
import { MainLayout } from './layouts/MainLayout'
import { ForbiddenPage, NotFoundPage } from './pages/ErrorPages'
import { FullPageSpinner } from '@/shared/ui/FullPageSpinner'
import { GuestOnly, RequireAuth, RequireRole } from '@/shared/ui/RouteGuards'

// 라우트 단위 코드 분할. /admin 이하는 일반 사용자가 내려받지 않는다(PRD FE-05).
const HomePage = lazy(() => import('./pages/HomePage'))
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage'))
const SignupPage = lazy(() => import('@/features/auth/pages/SignupPage'))
const MePage = lazy(() => import('@/features/user/pages/MePage'))
const AdminDashboard = lazy(() => import('./pages/AdminDashboardPlaceholder'))

export function AppRouter() {
  return (
    <Suspense fallback={<FullPageSpinner />}>
      <Routes>
        {/* 사용자 화면 */}
        <Route element={<MainLayout />}>
          <Route index element={<HomePage />} />

          {/* 로그인한 사용자는 접근할 필요가 없는 화면 */}
          <Route element={<GuestOnly />}>
            <Route path="login" element={<LoginPage />} />
            <Route path="signup" element={<SignupPage />} />
          </Route>

          {/* 로그인이 필요한 화면 */}
          <Route element={<RequireAuth />}>
            <Route path="me" element={<MePage />} />
          </Route>

          <Route path="403" element={<ForbiddenPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>

        {/* 관리자 화면 */}
        <Route element={<RequireRole role="ADMIN" />}>
          <Route path="admin" element={<AdminLayout />}>
            <Route index element={<AdminDashboard />} />
          </Route>
        </Route>
      </Routes>
    </Suspense>
  )
}
