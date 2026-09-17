import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import AdminPostsPage from './AdminPostsPage'
import { useAuthStore } from '@/shared/store/authStore'
import { BASE, adminUser } from '@/test/msw/handlers'
import { server } from '@/test/msw/server'
import { renderWithProviders } from '@/test/renderWithProviders'

const posts = [
  {
    id: 11,
    title: '공개 글',
    boardSlug: 'free',
    boardName: '자유게시판',
    authorNickname: '테스터',
    status: 'PUBLISHED',
    deleted: false,
    viewCount: 5,
    commentCount: 2,
    createdAt: '2026-09-16T00:00:00Z',
  },
  {
    id: 12,
    title: '숨긴 글',
    boardSlug: 'free',
    boardName: '자유게시판',
    authorNickname: '테스터',
    status: 'HIDDEN',
    deleted: false,
    viewCount: 1,
    commentCount: 0,
    createdAt: '2026-09-16T01:00:00Z',
  },
]

describe('관리자 게시글 관리 화면', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: adminUser, status: 'authenticated' })
    server.use(
      http.get(`${BASE}/admin/posts`, () =>
        HttpResponse.json({
          content: posts,
          page: 0,
          size: 20,
          totalElements: posts.length,
          totalPages: 1,
          hasNext: false,
        }),
      ),
    )
  })

  it('공개/숨김 상태를 배지로 구분해 보여준다', async () => {
    renderWithProviders(<AdminPostsPage />)

    expect(await screen.findByText('공개 글')).toBeInTheDocument()
    expect(screen.getByText('숨긴 글')).toBeInTheDocument()

    // 필터 select에도 같은 단어가 있으므로 표 안으로 범위를 좁힌다.
    const table = within(screen.getByRole('table'))
    expect(table.getByText('공개')).toBeInTheDocument()
    expect(table.getByText('숨김')).toBeInTheDocument()
  })

  it('선택한 글을 숨김 처리한다', async () => {
    const user = userEvent.setup()
    let requested: { ids: number[]; status: string } | null = null
    server.use(
      http.patch(`${BASE}/admin/posts/status`, async ({ request }) => {
        requested = (await request.json()) as { ids: number[]; status: string }
        return new HttpResponse(null, { status: 204 })
      }),
    )

    renderWithProviders(<AdminPostsPage />)
    await screen.findByText('공개 글')

    await user.click(screen.getByLabelText('공개 글 선택'))
    await user.click(screen.getByRole('button', { name: '숨기기' }))

    await waitFor(() => {
      expect(requested).toEqual({ ids: [11], status: 'HIDDEN' })
    })
  })

  it('선택 전에는 일괄 처리 버튼이 보이지 않는다', async () => {
    renderWithProviders(<AdminPostsPage />)
    await screen.findByText('공개 글')

    expect(screen.queryByRole('button', { name: '숨기기' })).not.toBeInTheDocument()
  })
})
