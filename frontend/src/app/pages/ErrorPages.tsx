import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'

function ErrorScreen({
  code,
  title,
  description,
}: {
  code: string
  title: string
  description: string
}) {
  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center gap-3 text-center">
      <p className="text-4xl font-semibold">{code}</p>
      <h1 className="text-lg font-medium">{title}</h1>
      <p className="text-sm text-muted-foreground">{description}</p>
      <Button asChild className="mt-2">
        <Link to="/">홈으로</Link>
      </Button>
    </div>
  )
}

export function NotFoundPage() {
  return (
    <ErrorScreen
      code="404"
      title="페이지를 찾을 수 없습니다"
      description="주소를 다시 확인해 주세요."
    />
  )
}

export function ForbiddenPage() {
  return (
    <ErrorScreen code="403" title="접근 권한이 없습니다" description="관리자에게 문의해 주세요." />
  )
}
