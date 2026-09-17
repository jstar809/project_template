import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { adminApi, adminKeys, type DailyCount } from '../api'
import { formatDateTime } from '@/shared/lib/format'
import { EmptyState, ErrorState } from '@/shared/ui/States'

export default function AdminDashboardPage() {
  const dashboard = useQuery({
    queryKey: adminKeys.dashboard(),
    queryFn: adminApi.dashboard,
  })

  if (dashboard.isLoading) return <Skeleton className="h-96 w-full" />
  if (dashboard.isError)
    return <ErrorState error={dashboard.error} onRetry={() => void dashboard.refetch()} />
  if (!dashboard.data) return null

  const { totals, dailySignups, dailyPosts, recentPosts } = dashboard.data

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">대시보드</h1>

      <div className="grid gap-3 sm:grid-cols-3">
        <StatCard label="사용자" value={totals.users} />
        <StatCard label="게시글" value={totals.posts} />
        <StatCard label="댓글" value={totals.comments} />
      </div>

      <div className="grid gap-3 lg:grid-cols-2">
        <TrendCard title="최근 7일 가입자" data={dailySignups} />
        <TrendCard title="최근 7일 게시글" data={dailyPosts} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">최근 게시글</CardTitle>
        </CardHeader>
        <CardContent>
          {recentPosts.length === 0 ? (
            <EmptyState message="아직 글이 없습니다." />
          ) : (
            <ul className="divide-y">
              {recentPosts.map((post) => (
                <li key={post.id} className="flex flex-wrap items-center gap-2 py-2 text-sm">
                  <Link to={`/boards/${post.boardName}`} className="text-muted-foreground">
                    {post.boardName}
                  </Link>
                  <span className="flex-1 truncate">{post.title}</span>
                  <span className="text-muted-foreground">{post.authorNickname}</span>
                  <span className="text-xs text-muted-foreground">
                    {formatDateTime(post.createdAt)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardDescription>{label}</CardDescription>
        <CardTitle className="text-2xl">{value.toLocaleString('ko-KR')}</CardTitle>
      </CardHeader>
    </Card>
  )
}

/**
 * 간단한 막대 그래프.
 *
 * 차트 라이브러리를 넣으면 번들이 커지므로, 이 정도 표현은 div 높이로 처리한다.
 */
function TrendCard({ title, data }: { title: string; data: DailyCount[] }) {
  const max = Math.max(1, ...data.map((point) => point.count))

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {data.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">데이터가 없습니다.</p>
        ) : (
          <div className="flex h-32 items-end gap-2">
            {data.map((point) => (
              <div key={point.date} className="flex flex-1 flex-col items-center gap-1">
                <span className="text-xs text-muted-foreground">{point.count}</span>
                <div
                  className="w-full rounded-t bg-primary"
                  style={{ height: `${Math.max(4, (point.count / max) * 100)}%` }}
                  role="img"
                  aria-label={`${point.date}: ${point.count}건`}
                />
                <span className="text-[10px] text-muted-foreground">{point.date.slice(5)}</span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
