import { Loader2 } from 'lucide-react'

/** 세션 복원이나 페이지 로딩 중에 보여주는 전체 화면 로딩. */
export function FullPageSpinner({ label = '불러오는 중' }: { label?: string }) {
  return (
    <div className="flex min-h-[50vh] items-center justify-center" role="status" aria-label={label}>
      <Loader2 className="size-6 animate-spin text-muted-foreground" />
      <span className="sr-only">{label}</span>
    </div>
  )
}
