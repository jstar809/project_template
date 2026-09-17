import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import AdminUsersPage from './AdminUsersPage'
import { useAuthStore } from '@/shared/store/authStore'
import { BASE, adminUser } from '@/test/msw/handlers'
import { server } from '@/test/msw/server'
import { renderWithProviders } from '@/test/renderWithProviders'

const users = [
  {
    id: 2,
    email: 'admin@example.com',
    nickname: '관리자',
    role: 'ADMIN',
    status: 'ACTIVE',
    postCount: 3,
    lastLoginAt: '2026-09-17T00:00:00Z',
    createdAt: '2026-09-01T00:00:00Z',
  },
  {
    id: 7,
    email: 'member@example.com',
    nickname: '일반회원',
    role: 'USER',
    status: 'ACTIVE',
    postCount: 1,
    lastLoginAt: null,
    createdAt: '2026-09-10T00:00:00Z',
  },
]

function page(content: typeof users) {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    hasNext: false,
  }
}

describe('관리자 사용자 관리 화면', () => {
  beforeEach(() => {
    // 로그인한 관리자는 id 2 (목록의 첫 번째 사용자와 동일)
    useAuthStore.setState({ user: { ...adminUser, id: 2 }, status: 'authenticated' })
    server.use(http.get(`${BASE}/admin/users`, () => HttpResponse.json(page(users))))
  })

  it('사용자 목록을 보여준다', async () => {
    renderWithProviders(<AdminUsersPage />)

    expect(await screen.findByText('member@example.com')).toBeInTheDocument()
    expect(screen.getByText('일반회원')).toBeInTheDocument()
  })

  it('본인 계정에는 역할/정지 버튼이 없다', async () => {
    renderWithProviders(<AdminUsersPage />)

    await screen.findByText('admin@example.com')
    expect(screen.getByText('본인')).toBeInTheDocument()
    // 다른 사용자에게만 버튼이 하나씩 있다
    expect(screen.getAllByRole('button', { name: '정지' })).toHaveLength(1)
  })

  it('다른 사용자의 역할을 바꾼다', async () => {
    const user = userEvent.setup()
    let requestedRole: string | null = null
    server.use(
      http.patch(`${BASE}/admin/users/7/role`, async ({ request }) => {
        const body = (await request.json()) as { role: string }
        requestedRole = body.role
        return new HttpResponse(null, { status: 204 })
      }),
    )

    renderWithProviders(<AdminUsersPage />)
    await screen.findByText('member@example.com')

    await user.click(screen.getByRole('button', { name: '관리자로' }))

    await waitFor(() => {
      expect(requestedRole).toBe('ADMIN')
    })
  })

  it('검색어를 입력하면 필터가 적용된다', async () => {
    const user = userEvent.setup()
    let requestedKeyword: string | null = null
    server.use(
      http.get(`${BASE}/admin/users`, ({ request }) => {
        requestedKeyword = new URL(request.url).searchParams.get('keyword')
        return HttpResponse.json(page(users))
      }),
    )

    renderWithProviders(<AdminUsersPage />)
    await screen.findByText('member@example.com')

    await user.type(screen.getByLabelText('검색어'), 'member')
    await user.click(screen.getByRole('button', { name: '검색' }))

    await waitFor(() => {
      expect(requestedKeyword).toBe('member')
    })
  })
})
