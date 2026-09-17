import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowDown, ArrowUp } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { adminApi, adminKeys, type AdminBoard, type BoardUpsertPayload } from '../api'
import { AdminCell, AdminRow, AdminTable } from '../components/AdminTable'
import { boardKeys } from '@/features/board/api'
import type { ApiError } from '@/shared/api/errors'
import { EmptyState, ErrorState } from '@/shared/ui/States'

const EMPTY_FORM: BoardUpsertPayload = {
  slug: '',
  name: '',
  description: '',
  readPermission: 'ANONYMOUS',
  writePermission: 'USER',
  commentEnabled: true,
  attachmentEnabled: true,
  active: true,
}

export default function AdminBoardsPage() {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<AdminBoard | null>(null)
  const [form, setForm] = useState<BoardUpsertPayload>(EMPTY_FORM)
  const [formOpen, setFormOpen] = useState(false)

  const boards = useQuery({ queryKey: adminKeys.boards(), queryFn: adminApi.boards })

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: adminKeys.boards() })
    // 사용자 화면 메뉴도 갱신한다.
    await queryClient.invalidateQueries({ queryKey: boardKeys.all })
  }

  const save = useMutation({
    mutationFn: (payload: BoardUpsertPayload) =>
      editing ? adminApi.updateBoard(editing.id, payload) : adminApi.createBoard(payload),
    onSuccess: async () => {
      toast.success(editing ? '게시판을 수정했습니다.' : '게시판을 만들었습니다.')
      closeForm()
      await refresh()
    },
    onError: (error: ApiError) => toast.error(error.message),
  })

  const remove = useMutation({
    mutationFn: (id: number) => adminApi.deleteBoard(id),
    onSuccess: async () => {
      toast.success('게시판을 삭제했습니다.')
      await refresh()
    },
    onError: (error: ApiError) => toast.error(error.message),
  })

  const reorder = useMutation({
    mutationFn: (boardIds: number[]) => adminApi.reorderBoards(boardIds),
    onSuccess: refresh,
    onError: (error: ApiError) => toast.error(error.message),
  })

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormOpen(true)
  }

  const openEdit = (board: AdminBoard) => {
    setEditing(board)
    setForm({
      slug: board.slug,
      name: board.name,
      description: board.description ?? '',
      readPermission: board.readPermission,
      writePermission: board.writePermission,
      commentEnabled: board.commentEnabled,
      attachmentEnabled: board.attachmentEnabled,
      active: board.active,
    })
    setFormOpen(true)
  }

  const closeForm = () => {
    setFormOpen(false)
    setEditing(null)
    setForm(EMPTY_FORM)
  }

  /** 위/아래 버튼으로 순서를 바꾼다. 드래그보다 단순하고 모바일에서도 쓸 수 있다. */
  const move = (index: number, direction: -1 | 1) => {
    if (!boards.data) return
    const ids = boards.data.map((board) => board.id)
    const target = index + direction
    if (target < 0 || target >= ids.length) return
    ;[ids[index], ids[target]] = [ids[target], ids[index]]
    reorder.mutate(ids)
  }

  if (boards.isLoading) return <Skeleton className="h-64 w-full" />
  if (boards.isError)
    return <ErrorState error={boards.error} onRetry={() => void boards.refetch()} />

  return (
    <div className="space-y-4">
      <header className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">게시판 관리</h1>
        <Button onClick={openCreate}>새 게시판</Button>
      </header>

      {formOpen && (
        <form
          className="space-y-3 rounded-md border p-4"
          onSubmit={(event) => {
            event.preventDefault()
            save.mutate(form)
          }}
        >
          <h2 className="font-medium">{editing ? `게시판 수정: ${editing.name}` : '새 게시판'}</h2>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1">
              <Label htmlFor="slug">주소(slug)</Label>
              <Input
                id="slug"
                value={form.slug}
                placeholder="free"
                onChange={(event) => setForm({ ...form, slug: event.target.value })}
              />
              <p className="text-xs text-muted-foreground">
                영문 소문자, 숫자, 하이픈만 사용합니다.
              </p>
            </div>

            <div className="space-y-1">
              <Label htmlFor="name">이름</Label>
              <Input
                id="name"
                value={form.name}
                placeholder="자유게시판"
                onChange={(event) => setForm({ ...form, name: event.target.value })}
              />
            </div>

            <div className="space-y-1 sm:col-span-2">
              <Label htmlFor="description">설명</Label>
              <Input
                id="description"
                value={form.description ?? ''}
                onChange={(event) => setForm({ ...form, description: event.target.value })}
              />
            </div>

            <div className="space-y-1">
              <Label htmlFor="readPermission">읽기 권한</Label>
              <select
                id="readPermission"
                className="h-9 w-full rounded-md border bg-background px-2 text-sm"
                value={form.readPermission}
                onChange={(event) =>
                  setForm({
                    ...form,
                    readPermission: event.target.value as AdminBoard['readPermission'],
                  })
                }
              >
                <option value="ANONYMOUS">누구나</option>
                <option value="USER">회원</option>
                <option value="ADMIN">관리자</option>
              </select>
            </div>

            <div className="space-y-1">
              <Label htmlFor="writePermission">쓰기 권한</Label>
              <select
                id="writePermission"
                className="h-9 w-full rounded-md border bg-background px-2 text-sm"
                value={form.writePermission}
                onChange={(event) =>
                  setForm({
                    ...form,
                    writePermission: event.target.value as AdminBoard['writePermission'],
                  })
                }
              >
                <option value="USER">회원</option>
                <option value="ADMIN">관리자</option>
              </select>
            </div>
          </div>

          <div className="flex flex-wrap gap-4 text-sm">
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={form.commentEnabled}
                onChange={(event) => setForm({ ...form, commentEnabled: event.target.checked })}
              />
              댓글 허용
            </label>
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={form.attachmentEnabled}
                onChange={(event) => setForm({ ...form, attachmentEnabled: event.target.checked })}
              />
              첨부 허용
            </label>
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={form.active}
                onChange={(event) => setForm({ ...form, active: event.target.checked })}
              />
              사용
            </label>
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={closeForm}>
              취소
            </Button>
            <Button type="submit" disabled={save.isPending}>
              저장
            </Button>
          </div>
        </form>
      )}

      {boards.data?.length === 0 ? (
        <EmptyState message="게시판이 없습니다. 새로 만들어 보세요." />
      ) : (
        <AdminTable headers={['순서', '이름', '주소', '권한(읽기/쓰기)', '글 수', '상태', '']}>
          {boards.data?.map((board, index) => (
            <AdminRow key={board.id}>
              <AdminCell>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="size-7"
                    aria-label={`${board.name} 위로`}
                    disabled={index === 0 || reorder.isPending}
                    onClick={() => move(index, -1)}
                  >
                    <ArrowUp className="size-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="size-7"
                    aria-label={`${board.name} 아래로`}
                    disabled={index === (boards.data?.length ?? 0) - 1 || reorder.isPending}
                    onClick={() => move(index, 1)}
                  >
                    <ArrowDown className="size-3.5" />
                  </Button>
                </div>
              </AdminCell>
              <AdminCell>{board.name}</AdminCell>
              <AdminCell className="text-muted-foreground">{board.slug}</AdminCell>
              <AdminCell className="text-muted-foreground">
                {board.readPermission} / {board.writePermission}
              </AdminCell>
              <AdminCell className="text-muted-foreground">{board.postCount}</AdminCell>
              <AdminCell>
                {board.active ? (
                  <Badge variant="secondary">사용</Badge>
                ) : (
                  <Badge variant="outline">중지</Badge>
                )}
              </AdminCell>
              <AdminCell>
                <div className="flex gap-1">
                  <Button variant="ghost" size="sm" onClick={() => openEdit(board)}>
                    수정
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={remove.isPending}
                    onClick={() => remove.mutate(board.id)}
                  >
                    삭제
                  </Button>
                </div>
              </AdminCell>
            </AdminRow>
          ))}
        </AdminTable>
      )}
    </div>
  )
}
