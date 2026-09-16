import { Link, NavLink, Outlet } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { UserMenu } from '@/features/auth/components/UserMenu'

/** M6에서 메뉴가 채워진다. 지금은 뼈대만 둔다. */
const MENUS = [
  { to: '/admin', label: '대시보드', end: true },
  { to: '/admin/boards', label: '게시판 관리' },
  { to: '/admin/posts', label: '게시글 관리' },
  { to: '/admin/users', label: '사용자 관리' },
]

export function AdminLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="border-b">
        <div className="flex h-14 items-center justify-between gap-4 px-4">
          <div className="flex items-center gap-3">
            <Link to="/admin" className="font-semibold">
              관리자
            </Link>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/">사용자 화면으로</Link>
            </Button>
          </div>
          <UserMenu />
        </div>
      </header>

      <div className="flex flex-1 flex-col md:flex-row">
        <nav className="border-b p-2 md:w-56 md:border-r md:border-b-0 md:p-4">
          <ul className="flex gap-1 overflow-x-auto md:flex-col">
            {MENUS.map((menu) => (
              <li key={menu.to}>
                <NavLink
                  to={menu.to}
                  end={menu.end}
                  className={({ isActive }) =>
                    cn(
                      'block rounded-md px-3 py-2 text-sm whitespace-nowrap transition-colors',
                      isActive
                        ? 'bg-accent font-medium text-accent-foreground'
                        : 'text-muted-foreground hover:bg-accent/50',
                    )
                  }
                >
                  {menu.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <main className="flex-1 p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
