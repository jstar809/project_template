import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

/** M6에서 실제 대시보드로 교체한다. 지금은 관리자 라우트 보호가 동작하는지 확인하는 용도다. */
export default function AdminDashboardPlaceholder() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>관리자 대시보드</CardTitle>
        <CardDescription>M6에서 통계와 관리 화면이 들어옵니다.</CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">
          이 화면은 ADMIN 역할을 가진 사용자만 볼 수 있습니다.
        </p>
      </CardContent>
    </Card>
  )
}
