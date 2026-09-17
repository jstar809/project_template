import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { adminApi, adminKeys, type AdminUser } from '../api'
import { AdminCell, AdminRow, AdminTable } from '../components/AdminTable'
import type { ApiError } from '@/shared/api/errors'
import { formatDateTime } from '@/shared/lib/format'
import { useAuthStore } from '@/shared/store/authStore'
import { Pagination } from '@/shared/ui/Pagination'
import { EmptyState, ErrorState } from '@/shared/ui/States'

export default function AdminUsersPage() {
  const queryClient = useQueryClient()
  const myId = useAuthStore((state) => state.user?.id)
  const [page, setPage] = useState(0)
  const [role, setRole] = useState('')
  const [status, setStatus] = useState('')
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')

  const params = {
    page,
    role: role || undefined,
    status: status || undefined,
    keyword: keyword || undefined,
  }
  const users = useQuery({
    queryKey: adminKeys.users(params),
    queryFn: () => adminApi.users(params),
  })

  const refresh = () => queryClient.invalidateQueries({ queryKey: adminKeys.all })

  const changeRole = useMutation({
    mutationFn: ({ id, next }: { id: number; next: 'USER' | 'ADMIN' }) =>
      adminApi.changeUserRole(id, next),
    onSuccess: async () => {
      toast.success('역할을 변경했습니다. 해당 사용자는 다시 로그인해야 합니다.')
      await refresh()
    },
    onError: (error: ApiError) => toast.error(error.message),
  })

  const changeStatus = useMutation({
    mutationFn: ({ id, next }: { id: number; next: 'ACTIVE' | 'SUSPENDED' }) =>
      adminApi.changeUserStatus(id, next),
    onSuccess: async () => {
      toast.success('계정 상태를 변경했습니다.')
      await refresh()
    },
    onError: (error: ApiError) => toast.error(error.message),
  })

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">사용자 관리</h1>

      <form
        className="flex flex-wrap gap-2"
        onSubmit={(event) => {
          event.preventDefault()
          setKeyword(keywordInput)
          setPage(0)
        }}
      >
        <select
          className="h-9 rounded-md border bg-background px-2 text-sm"
          aria-label="역할 필터"
          value={role}
          onChange={(event) => {
            setRole(event.target.value)
            setPage(0)
          }}
        >
          <option value="">전체 역할</option>
          <option value="USER">회원</option>
          <option value="ADMIN">관리자</option>
        </select>
        <select
          className="h-9 rounded-md border bg-background px-2 text-sm"
          aria-label="상태 필터"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPage(0)
          }}
        >
          <option value="">전체 상태</option>
          <option value="ACTIVE">정상</option>
          <option value="SUSPENDED">정지</option>
        </select>
        <Input
          className="max-w-60"
          aria-label="검색어"
          placeholder="이메일 또는 닉네임"
          value={keywordInput}
          onChange={(event) => setKeywordInput(event.target.value)}
        />
        <Button type="submit" variant="outline">
          검색
        </Button>
      </form>

      {users.isLoading && <Skeleton className="h-64 w-full" />}
      {users.isError && <ErrorState error={users.error} onRetry={() => void users.refetch()} />}
      {users.data && users.data.content.length === 0 && <EmptyState message="사용자가 없습니다." />}

      {users.data && users.data.content.length > 0 && (
        <>
          <AdminTable headers={['이메일', '닉네임', '역할', '상태', '글 수', '마지막 로그인', '']}>
            {users.data.content.map((user) => (
              <UserRow
                key={user.id}
                user={user}
                isSelf={user.id === myId}
                onChangeRole={(next) => changeRole.mutate({ id: user.id, next })}
                onChangeStatus={(next) => changeStatus.mutate({ id: user.id, next })}
              />
            ))}
          </AdminTable>

          <Pagination page={page} totalPages={users.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

function UserRow({
  user,
  isSelf,
  onChangeRole,
  onChangeStatus,
}: {
  user: AdminUser
  isSelf: boolean
  onChangeRole: (next: 'USER' | 'ADMIN') => void
  onChangeStatus: (next: 'ACTIVE' | 'SUSPENDED') => void
}) {
  return (
    <AdminRow>
      <AdminCell>{user.email}</AdminCell>
      <AdminCell>{user.nickname}</AdminCell>
      <AdminCell>
        {user.role === 'ADMIN' ? <Badge>관리자</Badge> : <Badge variant="secondary">회원</Badge>}
      </AdminCell>
      <AdminCell>
        {user.status === 'SUSPENDED' ? (
          <Badge variant="destructive">정지</Badge>
        ) : (
          <Badge variant="outline">정상</Badge>
        )}
      </AdminCell>
      <AdminCell className="text-muted-foreground">{user.postCount}</AdminCell>
      <AdminCell className="text-xs text-muted-foreground">
        {user.lastLoginAt ? formatDateTime(user.lastLoginAt) : '-'}
      </AdminCell>
      <AdminCell>
        {/* 자기 자신은 서버도 막지만(ADMIN_SELF_MODIFICATION), 버튼부터 비활성화한다 */}
        {isSelf ? (
          <span className="text-xs text-muted-foreground">본인</span>
        ) : (
          <div className="flex gap-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onChangeRole(user.role === 'ADMIN' ? 'USER' : 'ADMIN')}
            >
              {user.role === 'ADMIN' ? '회원으로' : '관리자로'}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onChangeStatus(user.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED')}
            >
              {user.status === 'SUSPENDED' ? '정지 해제' : '정지'}
            </Button>
          </div>
        )}
      </AdminCell>
    </AdminRow>
  )
}
