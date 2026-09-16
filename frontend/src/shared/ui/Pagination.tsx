import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface PaginationProps {
  /** 0부터 시작하는 현재 페이지 */
  page: number
  totalPages: number
  onChange: (page: number) => void
}

const WINDOW = 5

/** 현재 페이지 주변 번호만 보여주는 간단한 페이지네이션. */
export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null

  const start = Math.max(0, Math.min(page - Math.floor(WINDOW / 2), totalPages - WINDOW))
  const pages = Array.from({ length: Math.min(WINDOW, totalPages) }, (_, index) => start + index)

  return (
    <nav className="flex items-center justify-center gap-1" aria-label="페이지 이동">
      <Button
        variant="ghost"
        size="icon"
        aria-label="이전 페이지"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        <ChevronLeft className="size-4" />
      </Button>

      {pages.map((target) => (
        <Button
          key={target}
          variant={target === page ? 'default' : 'ghost'}
          size="icon"
          aria-label={`${target + 1}페이지`}
          aria-current={target === page ? 'page' : undefined}
          onClick={() => onChange(target)}
        >
          {target + 1}
        </Button>
      ))}

      <Button
        variant="ghost"
        size="icon"
        aria-label="다음 페이지"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        <ChevronRight className="size-4" />
      </Button>
    </nav>
  )
}
