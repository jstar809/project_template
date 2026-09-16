import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useEffect, useState, type ReactNode } from 'react'
import { Toaster } from '@/components/ui/sonner'
import { ApiError } from '@/shared/api/errors'
import { useAuthStore } from '@/shared/store/authStore'
import { useThemeStore } from '@/shared/store/themeStore'

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: (failureCount, error) => {
          // 인증/권한/없음 오류는 다시 시도해도 결과가 같다.
          if (error instanceof ApiError && [400, 401, 403, 404].includes(error.status)) {
            return false
          }
          return failureCount < 2
        },
      },
    },
  })
}

/**
 * 앱 전역 프로바이더.
 * 첫 렌더 전에 테마를 적용하고, 쿠키로 로그인 세션 복원을 시도한다.
 */
export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createQueryClient)
  const restore = useAuthStore((state) => state.restore)
  const initTheme = useThemeStore((state) => state.init)

  useEffect(() => {
    initTheme()
    void restore()
  }, [initTheme, restore])

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <Toaster position="top-center" richColors />
    </QueryClientProvider>
  )
}
