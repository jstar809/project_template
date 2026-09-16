import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

/**
 * M0 임시 화면. M3에서 라우터와 레이아웃, 인증 화면으로 교체한다.
 */
function App() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background p-6">
      <Card className="w-full max-w-lg">
        <CardHeader>
          <CardTitle>풀스택 템플릿</CardTitle>
          <CardDescription>
            M0 스캐폴딩 완료. Spring Boot + React(Vite) 모노레포가 준비되었습니다.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <ul className="list-inside list-disc text-sm text-muted-foreground">
            <li>백엔드: http://localhost:8080/actuator/health</li>
            <li>API 문서: http://localhost:8080/swagger-ui.html</li>
            <li>다음 단계: M1 백엔드 공통 인프라</li>
          </ul>
          <Button asChild>
            <a href="http://localhost:8080/actuator/health" target="_blank" rel="noreferrer">
              백엔드 상태 확인
            </a>
          </Button>
        </CardContent>
      </Card>
    </main>
  )
}

export default App
