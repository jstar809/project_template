import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuthStore } from '@/shared/store/authStore'
import { useLogout } from '../hooks'

/** 헤더 오른쪽: 로그인 전에는 로그인/가입 버튼, 로그인 후에는 사용자 메뉴. */
export function UserMenu() {
  const { status, user } = useAuthStore()
  const logout = useLogout()

  if (status === 'idle' || status === 'loading') {
    return <Skeleton className="h-9 w-24" />
  }

  if (status !== 'authenticated' || !user) {
    return (
      <div className="flex items-center gap-2">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/login">로그인</Link>
        </Button>
        <Button size="sm" asChild>
          <Link to="/signup">회원가입</Link>
        </Button>
      </div>
    )
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="sm">
          {user.nickname}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuLabel className="font-normal text-muted-foreground">
          {user.email}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to="/me">내 정보</Link>
        </DropdownMenuItem>
        {user.role === 'ADMIN' && (
          <DropdownMenuItem asChild>
            <Link to="/admin">관리자</Link>
          </DropdownMenuItem>
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={() => logout.mutate()} disabled={logout.isPending}>
          로그아웃
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
