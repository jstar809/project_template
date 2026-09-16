import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuthStore } from '@/shared/store/authStore'

/** M5에서 게시판 목록과 최근 글로 교체한다. */
export default function HomePage() {
  const { status, user } = useAuthStore()

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Fullstack Template</CardTitle>
          <CardDescription>Spring Boot + React 모노레포 템플릿</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {status === 'authenticated' && user ? (
            <p className="text-sm">
              <span className="font-medium">{user.nickname}</span>님으로 로그인되어 있습니다.
            </p>
          ) : (
            <p className="text-sm text-muted-foreground">
              로그인하면 글쓰기와 댓글 기능을 사용할 수 있습니다.
            </p>
          )}

          <div className="flex flex-wrap gap-2">
            <Button asChild variant="outline">
              <Link to="/me">내 정보</Link>
            </Button>
            <Button asChild variant="outline">
              <a href="/swagger-ui.html" target="_blank" rel="noreferrer">
                API 문서
              </a>
            </Button>
          </div>

          <p className="text-sm text-muted-foreground">
            다음 단계: M4 게시판 백엔드, M5 게시판 화면
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
