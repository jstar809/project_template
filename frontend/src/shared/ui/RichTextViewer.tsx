import DOMPurify from 'dompurify'
import { useMemo } from 'react'

/**
 * 게시글 본문을 보여준다.
 *
 * 서버가 이미 정제(sanitize)한 HTML이지만, 브라우저에서 한 번 더 DOMPurify를 통과시킨다.
 * 방어를 두 겹으로 두면 한쪽이 뚫려도 막힌다(PRD NFR-SEC-01).
 */
export function RichTextViewer({ html }: { html: string }) {
  const safeHtml = useMemo(() => DOMPurify.sanitize(html), [html])

  return (
    <div
      className="prose-content text-sm leading-relaxed break-words"
      dangerouslySetInnerHTML={{ __html: safeHtml }}
    />
  )
}
