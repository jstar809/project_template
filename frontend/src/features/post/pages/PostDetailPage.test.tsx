import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import PostDetailPage from './PostDetailPage'
import { useAuthStore } from '@/shared/store/authStore'
import { cmsHandlers, postDetail } from '@/test/msw/cmsHandlers'
import { BASE, testUser } from '@/test/msw/handlers'
import { server } from '@/test/msw/server'
import { renderWithProviders } from '@/test/renderWithProviders'

function renderDetail() {
  return renderWithProviders(
    <Routes>
      <Route path="/boards/:slug/posts/:id" element={<PostDetailPage />} />
    </Routes>,
    { route: '/boards/free/posts/11' },
  )
}

describe('게시글 상세 화면', () => {
  beforeEach(() => {
    server.use(...cmsHandlers)
    useAuthStore.setState({ user: testUser, status: 'authenticated' })
  })

  it('제목, 작성자, 본문, 첨부를 보여준다', async () => {
    renderDetail()

    expect(await screen.findByRole('heading', { name: '첫 번째 글' })).toBeInTheDocument()
    expect(screen.getByText('테스터')).toBeInTheDocument()
    expect(screen.getByText('본문 내용입니다')).toBeInTheDocument()
    expect(screen.getByText('자료.pdf')).toBeInTheDocument()
  })

  it('본문의 위험한 태그는 렌더링되지 않는다', async () => {
    server.use(
      http.get(`${BASE}/posts/11`, () =>
        HttpResponse.json({
          ...postDetail,
          content: '<p>안전한 내용</p><script>window.__xss = true</script>',
        }),
      ),
    )

    renderDetail()

    expect(await screen.findByText('안전한 내용')).toBeInTheDocument()
    expect(document.querySelector('script')).toBeNull()
  })

  it('댓글과 대댓글을 트리로 보여준다', async () => {
    renderDetail()

    expect(await screen.findByText('첫 댓글입니다')).toBeInTheDocument()
    expect(screen.getByText('답글입니다')).toBeInTheDocument()
    expect(screen.getByText('다른사람')).toBeInTheDocument()
  })

  it('권한이 있으면 수정/삭제 버튼이 보인다', async () => {
    renderDetail()

    expect(await screen.findByRole('link', { name: '수정' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '삭제' })).toBeInTheDocument()
  })

  it('권한이 없으면 수정/삭제 버튼이 보이지 않는다', async () => {
    server.use(
      http.get(`${BASE}/posts/11`, () =>
        HttpResponse.json({ ...postDetail, canEdit: false, canDelete: false }),
      ),
    )

    renderDetail()

    await screen.findByRole('heading', { name: '첫 번째 글' })
    expect(screen.queryByRole('link', { name: '수정' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument()
  })
})
