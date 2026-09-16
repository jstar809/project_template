import { Link, NavLink, Outlet } from 'react-router-dom'
import { Moon, Sun } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { UserMenu } from '@/features/auth/components/UserMenu'
import { useBoards } from '@/features/board/hooks'
import { cn } from '@/lib/utils'
import { useThemeStore } from '@/shared/store/themeStore'

/** 일반 사용자 화면의 공통 뼈대: 헤더 + 게시판 메뉴 + 본문 + 푸터 */
export function MainLayout() {
  const { theme, toggle } = useThemeStore()
  const boards = useBoards()

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="border-b">
        <div className="mx-auto flex h-14 w-full max-w-5xl items-center justify-between gap-4 px-4">
          <Link to="/" className="font-semibold whitespace-nowrap">
            Fullstack Template
          </Link>

          <nav className="hidden flex-1 gap-1 overflow-x-auto sm:flex" aria-label="게시판 메뉴">
            {boards.data?.map((board) => (
              <NavLink
                key={board.id}
                to={`/boards/${board.slug}`}
                className={({ isActive }) =>
                  cn(
                    'rounded-md px-3 py-1.5 text-sm whitespace-nowrap transition-colors',
                    isActive
                      ? 'bg-accent font-medium text-accent-foreground'
                      : 'text-muted-foreground hover:bg-accent/50',
                  )
                }
              >
                {board.name}
              </NavLink>
            ))}
          </nav>

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

        {/* 모바일: 게시판 메뉴를 아래 줄에 가로 스크롤로 보여준다 */}
        <nav
          className="flex gap-1 overflow-x-auto border-t px-4 py-1.5 sm:hidden"
          aria-label="게시판 메뉴(모바일)"
        >
          {boards.data?.map((board) => (
            <NavLink
              key={board.id}
              to={`/boards/${board.slug}`}
              className={({ isActive }) =>
                cn(
                  'rounded-md px-2.5 py-1 text-sm whitespace-nowrap',
                  isActive ? 'bg-accent font-medium' : 'text-muted-foreground',
                )
              }
            >
              {board.name}
            </NavLink>
          ))}
        </nav>
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
