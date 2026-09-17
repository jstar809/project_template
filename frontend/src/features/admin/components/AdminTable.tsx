import type { ReactNode } from 'react'

/** 관리자 화면 공통 표. 모바일에서는 가로 스크롤한다. */
export function AdminTable({ headers, children }: { headers: ReactNode[]; children: ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-md border">
      <table className="w-full min-w-[40rem] text-sm">
        <thead className="border-b bg-muted/40 text-muted-foreground">
          <tr>
            {headers.map((header, index) => (
              <th key={index} className="px-3 py-2 text-left font-medium whitespace-nowrap">
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}

export function AdminRow({ children }: { children: ReactNode }) {
  return <tr className="border-b last:border-0 hover:bg-muted/30">{children}</tr>
}

export function AdminCell({ children, className }: { children: ReactNode; className?: string }) {
  return <td className={`px-3 py-2 ${className ?? ''}`}>{children}</td>
}
