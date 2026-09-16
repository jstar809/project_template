import { Link, Outlet } from 'react-router-dom'
import { Moon, Sun } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useThemeStore } from '@/shared/store/themeStore'
import { UserMenu } from '@/features/auth/components/UserMenu'

/** 일반 사용자 화면의 공통 뼈대: 헤더 + 본문 + 푸터 */
export function MainLayout() {
  const { theme, toggle } = useThemeStore()

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="border-b">
        <div className="mx-auto flex h-14 w-full max-w-5xl items-center justify-between gap-4 px-4">
          <Link to="/" className="font-semibold">
            Fullstack Template
          </Link>

          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={toggle}
              aria-label={theme === 'dark' ? '밝은 테마로 전환' : '어두운 테마로 전환'}
            >
              {theme === 'dark' ? <Sun className="size-4" /> : <Moon className="size-4" />}
            </Button>
            <UserMenu />
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8">
        <Outlet />
      </main>

      <footer className="border-t py-6 text-center text-sm text-muted-foreground">
        Fullstack Template · Spring Boot + React
      </footer>
    </div>
  )
}
