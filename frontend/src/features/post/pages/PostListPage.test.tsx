import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import PostListPage from './PostListPage'
import { useAuthStore } from '@/shared/store/authStore'
import { cmsHandlers } from '@/test/msw/cmsHandlers'
import { server } from '@/test/msw/server'
import { renderWithProviders } from '@/test/renderWithProviders'

function renderList(route = '/boards/free') {
  return renderWithProviders(
    <Routes>
      <Route path="/boards/:slug" element={<PostListPage />} />
      <Route path="/boards/:slug/write" element={<p>글쓰기 화면</p>} />
      <Route path="/boards/:slug/posts/:id" element={<p>상세 화면</p>} />
    </Routes>,
    { route },
  )
}

describe('게시글 목록 화면', () => {
  beforeEach(() => {
    server.use(...cmsHandlers)
    useAuthStore.setState({ user: null, status: 'anonymous' })
  })

  it('게시판 이름과 글 목록을 보여준다', async () => {
    renderList()

    expect(await screen.findByRole('heading', { name: '자유게시판' })).toBeInTheDocument()
    expect(await screen.findByText('첫 번째 글')).toBeInTheDocument()
  })

  it('공지는 목록 위쪽에 따로 표시된다', async () => {
    renderList()

    const notice = await screen.findByText('공지사항입니다')
    const normal = await screen.findByText('첫 번째 글')

    // DOM 순서상 공지가 먼저 온다
    expect(notice.compareDocumentPosition(normal) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('첨부와 댓글 수 표시가 나타난다', async () => {
    renderList()

    expect(await screen.findByLabelText('첨부 있음')).toBeInTheDocument()
    expect(screen.getByText('[2]')).toBeInTheDocument()
  })

  it('검색하면 결과만 남는다', async () => {
    const user = userEvent.setup()
    renderList()

    await screen.findByText('첫 번째 글')

    await user.type(screen.getByLabelText('검색어'), '없는단어')
    await user.click(screen.getByRole('button', { name: '검색' }))

    await waitFor(() => {
      expect(screen.getByText('검색 결과가 없습니다.')).toBeInTheDocument()
    })
    expect(screen.queryByText('공지사항입니다')).not.toBeInTheDocument()
  })

  it('글쓰기 버튼은 쓰기 권한이 있을 때만 보인다', async () => {
    renderList()
    expect(await screen.findByRole('link', { name: '글쓰기' })).toBeInTheDocument()
  })
})
