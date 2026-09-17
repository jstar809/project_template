import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { adminApi, adminKeys } from '../api'
import { AdminCell, AdminRow, AdminTable } from '../components/AdminTable'
import { postKeys } from '@/features/post/api'
import type { ApiError } from '@/shared/api/errors'
import { formatDateTime } from '@/shared/lib/format'
import { Pagination } from '@/shared/ui/Pagination'
import { EmptyState, ErrorState } from '@/shared/ui/States'

export default function AdminPostsPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [selected, setSelected] = useState<number[]>([])

  const params = { page, status: status || undefined, keyword: keyword || undefined }
  const posts = useQuery({
    queryKey: adminKeys.posts(params),
    queryFn: () => adminApi.posts(params),
  })

  const refresh = async () => {
    setSelected([])
    await queryClient.invalidateQueries({ queryKey: adminKeys.all })
    await queryClient.invalidateQueries({ queryKey: postKeys.all })
  }

  const changeStatus = useMutation({
    mutationFn: ({ ids, next }: { ids: number[]; next: 'PUBLISHED' | 'HIDDEN' }) =>
      adminApi.changePostStatus(ids, next),
    onSuccess: async () => {
      toast.success('상태를 변경했습니다.')
      await refresh()
    },
    onError: (error: ApiError) => toast.error(error.message),
  })

  const remove = useMutation({
    mutationFn: (ids: number[]) => adminApi.deletePosts(ids),
    onSuccess: async () => {
      toast.success('삭제했습니다.')
      await refresh()
    },
    onError: (error: ApiError) => toast.error(error.message),
  })

  const toggle = (id: number) =>
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id],
    )

  const toggleAll = () => {
    const ids = posts.data?.content.map((post) => post.id) ?? []
    setSelected((prev) => (prev.length === ids.length ? [] : ids))
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">게시글 관리</h1>

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
          aria-label="상태 필터"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPage(0)
          }}
        >
          <option value="">전체 상태</option>
          <option value="PUBLISHED">공개</option>
          <option value="HIDDEN">숨김</option>
        </select>
        <Input
          className="max-w-60"
          aria-label="검색어"
          placeholder="제목 또는 내용"
          value={keywordInput}
          onChange={(event) => setKeywordInput(event.target.value)}
        />
        <Button type="submit" variant="outline">
          검색
        </Button>
      </form>

      {selected.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 rounded-md border bg-muted/30 p-2 text-sm">
          <span>{selected.length}건 선택</span>
          <Button
            size="sm"
            variant="outline"
            onClick={() => changeStatus.mutate({ ids: selected, next: 'HIDDEN' })}
          >
            숨기기
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => changeStatus.mutate({ ids: selected, next: 'PUBLISHED' })}
          >
            복구
          </Button>
          <Button size="sm" variant="destructive" onClick={() => remove.mutate(selected)}>
            삭제
          </Button>
        </div>
      )}

      {posts.isLoading && <Skeleton className="h-64 w-full" />}
      {posts.isError && <ErrorState error={posts.error} onRetry={() => void posts.refetch()} />}

      {posts.data && posts.data.content.length === 0 && <EmptyState message="게시글이 없습니다." />}

      {posts.data && posts.data.content.length > 0 && (
        <>
          <AdminTable
            headers={[
              <input
                key="all"
                type="checkbox"
                aria-label="전체 선택"
                checked={selected.length === posts.data.content.length}
                onChange={toggleAll}
              />,
              '제목',
              '게시판',
              '작성자',
              '상태',
              '조회/댓글',
              '작성일',
            ]}
          >
            {posts.data.content.map((post) => (
              <AdminRow key={post.id}>
                <AdminCell>
                  <input
                    type="checkbox"
                    aria-label={`${post.title} 선택`}
                    checked={selected.includes(post.id)}
                    onChange={() => toggle(post.id)}
                  />
                </AdminCell>
                <AdminCell>
                  <Link
                    to={`/boards/${post.boardSlug}/posts/${post.id}`}
                    className="hover:underline"
                    title={post.title}
                  >
                    {post.title}
                  </Link>
                </AdminCell>
                <AdminCell className="text-muted-foreground">{post.boardName}</AdminCell>
                <AdminCell className="text-muted-foreground">{post.authorNickname}</AdminCell>
                <AdminCell>
                  {post.deleted ? (
                    <Badge variant="outline">삭제됨</Badge>
                  ) : post.status === 'HIDDEN' ? (
                    <Badge variant="destructive">숨김</Badge>
                  ) : (
                    <Badge variant="secondary">공개</Badge>
                  )}
                </AdminCell>
                <AdminCell className="text-muted-foreground">
                  {post.viewCount} / {post.commentCount}
                </AdminCell>
                <AdminCell className="text-xs text-muted-foreground">
                  {formatDateTime(post.createdAt)}
                </AdminCell>
              </AdminRow>
            ))}
          </AdminTable>

          <Pagination page={page} totalPages={posts.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
