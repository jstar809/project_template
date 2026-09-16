import { Link } from 'react-router-dom'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useBoards } from '@/features/board/hooks'
import { EmptyState, ErrorState } from '@/shared/ui/States'

/** 홈: 현재 사용자가 읽을 수 있는 게시판 목록 */
export default function HomePage() {
  const boards = useBoards()

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">게시판</h1>

      {boards.isLoading && <Skeleton className="h-40 w-full" />}
      {boards.isError && <ErrorState error={boards.error} onRetry={() => void boards.refetch()} />}
      {boards.data?.length === 0 && <EmptyState message="공개된 게시판이 없습니다." />}

      <div className="grid gap-3 sm:grid-cols-2">
        {boards.data?.map((board) => (
          <Link key={board.id} to={`/boards/${board.slug}`} className="block">
            <Card className="h-full transition-colors hover:border-primary">
              <CardHeader>
                <CardTitle className="text-base">{board.name}</CardTitle>
                {board.description && <CardDescription>{board.description}</CardDescription>}
              </CardHeader>
              <CardContent className="text-xs text-muted-foreground">
                {board.commentEnabled ? '댓글 가능' : '댓글 없음'}
                {board.attachmentEnabled ? ' · 첨부 가능' : ''}
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}
