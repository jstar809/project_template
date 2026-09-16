import { Button } from '@/components/ui/button'
import { ApiError } from '@/shared/api/errors'

/** 목록이 비었을 때 */
export function EmptyState({ message = '표시할 내용이 없습니다.' }: { message?: string }) {
  return (
    <div className="flex min-h-40 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">
      {message}
    </div>
  )
}

/** 조회에 실패했을 때. 권한 문제는 다시 시도해도 소용없으므로 버튼을 숨긴다. */
export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const apiError = error instanceof ApiError ? error : null
  const message = apiError?.message ?? '문제가 발생했습니다.'
  const retryable = !apiError || ![401, 403, 404].includes(apiError.status)

  return (
    <div className="flex min-h-40 flex-col items-center justify-center gap-3 rounded-md border border-dashed">
      <p className="text-sm text-muted-foreground">{message}</p>
      {onRetry && retryable && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          다시 시도
        </Button>
      )}
    </div>
  )
}
