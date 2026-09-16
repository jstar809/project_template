import { useQuery } from '@tanstack/react-query'
import { Paperclip, Pin } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { useBoard } from '@/features/board/hooks'
import { postApi, postKeys, type PostListItem, type SearchType } from '../api'
import { formatListDate } from '@/shared/lib/format'
import { Pagination } from '@/shared/ui/Pagination'
import { EmptyState, ErrorState } from '@/shared/ui/States'

const SEARCH_TYPES: { value: SearchType; label: string }[] = [
  { value: 'title_content', label: '제목+내용' },
  { value: 'title', label: '제목' },
  { value: 'content', label: '내용' },
  { value: 'author', label: '작성자' },
]

export default function PostListPage() {
  const { slug = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()

  const page = Number(searchParams.get('page') ?? '0')
  const searchType = (searchParams.get('searchType') ?? 'title_content') as SearchType
  const keyword = searchParams.get('keyword') ?? ''
  const [keywordInput, setKeywordInput] = useState(keyword)

  const board = useBoard(slug)
  const params = { page, size: 20, searchType, keyword: keyword || undefined }
  const posts = useQuery({
    queryKey: postKeys.list(slug, params),
    queryFn: () => postApi.list(slug, params),
    enabled: Boolean(slug),
  })

  const updateParams = (next: Record<string, string>) => {
    const merged = new URLSearchParams(searchParams)
    Object.entries(next).forEach(([key, value]) => {
      if (value) merged.set(key, value)
      else merged.delete(key)
    })
    setSearchParams(merged)
  }

  if (board.isError) return <ErrorState error={board.error} />

  return (
    <div className="space-y-4">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">
            {board.data?.name ?? <Skeleton className="h-6 w-24" />}
          </h1>
          {board.data?.description && (
            <p className="text-sm text-muted-foreground">{board.data.description}</p>
          )}
        </div>
        {board.data?.canWrite && (
          <Button asChild>
            <Link to={`/boards/${slug}/write`}>글쓰기</Link>
          </Button>
        )}
      </header>

      <form
        className="flex flex-wrap gap-2"
        onSubmit={(event) => {
          event.preventDefault()
          updateParams({ keyword: keywordInput, searchType, page: '' })
        }}
      >
        <select
          className="h-9 rounded-md border bg-background px-2 text-sm"
          aria-label="검색 조건"
          value={searchType}
          onChange={(event) => updateParams({ searchType: event.target.value, page: '' })}
        >
          {SEARCH_TYPES.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </select>
        <Input
          className="max-w-60"
          placeholder="검색어"
          aria-label="검색어"
          value={keywordInput}
          onChange={(event) => setKeywordInput(event.target.value)}
        />
        <Button type="submit" variant="outline">
          검색
        </Button>
        {keyword && (
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              setKeywordInput('')
              updateParams({ keyword: '', page: '' })
            }}
          >
            초기화
          </Button>
        )}
      </form>

      {posts.isLoading && <Skeleton className="h-64 w-full" />}
      {posts.isError && <ErrorState error={posts.error} onRetry={() => void posts.refetch()} />}

      {posts.data && (
        <>
          <div className="overflow-x-auto rounded-md border">
            <table className="w-full min-w-[36rem] text-sm">
              <thead className="border-b bg-muted/40 text-muted-foreground">
                <tr>
                  <th className="px-3 py-2 text-left font-medium">제목</th>
                  <th className="w-28 px-3 py-2 text-left font-medium">작성자</th>
                  <th className="w-24 px-3 py-2 text-left font-medium">작성일</th>
                  <th className="w-16 px-3 py-2 text-right font-medium">조회</th>
                </tr>
              </thead>
              <tbody>
                {posts.data.notices.map((post) => (
                  <PostRow key={`notice-${post.id}`} post={post} slug={slug} highlight />
                ))}
                {posts.data.page.content.map((post) => (
                  <PostRow key={post.id} post={post} slug={slug} />
                ))}
              </tbody>
            </table>
          </div>

          {posts.data.page.totalElements === 0 && posts.data.notices.length === 0 && (
            <EmptyState message={keyword ? '검색 결과가 없습니다.' : '아직 글이 없습니다.'} />
          )}

          <Pagination
            page={page}
            totalPages={posts.data.page.totalPages}
            onChange={(next) => updateParams({ page: String(next) })}
          />
        </>
      )}
    </div>
  )
}

function PostRow({
  post,
  slug,
  highlight = false,
}: {
  post: PostListItem
  slug: string
  highlight?: boolean
}) {
  return (
    <tr className="border-b last:border-0 hover:bg-muted/30">
      <td className="px-3 py-2">
        <Link
          to={`/boards/${slug}/posts/${post.id}`}
          className="flex items-center gap-1.5 hover:underline"
        >
          {highlight && <Pin className="size-3.5 shrink-0 text-primary" aria-label="공지" />}
          <span className={highlight ? 'font-medium' : undefined}>{post.title}</span>
          {post.commentCount > 0 && (
            <span className="text-xs text-muted-foreground">[{post.commentCount}]</span>
          )}
          {post.hasAttachment && (
            <Paperclip className="size-3.5 shrink-0 text-muted-foreground" aria-label="첨부 있음" />
          )}
        </Link>
      </td>
      <td className="px-3 py-2 text-muted-foreground">{post.authorNickname}</td>
      <td className="px-3 py-2 text-muted-foreground">{formatListDate(post.createdAt)}</td>
      <td className="px-3 py-2 text-right text-muted-foreground">{post.viewCount}</td>
    </tr>
  )
}
