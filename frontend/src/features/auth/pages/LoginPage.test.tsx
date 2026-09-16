import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import LoginPage from './LoginPage'
import { setAccessToken } from '@/shared/api/client'
import { useAuthStore } from '@/shared/store/authStore'
import { renderWithProviders } from '@/test/renderWithProviders'

describe('로그인 화면', () => {
  beforeEach(() => {
    setAccessToken(null)
    useAuthStore.setState({ user: null, status: 'anonymous' })
  })

  it('이메일 형식이 아니면 제출되지 않고 오류를 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />)

    await user.type(screen.getByLabelText('이메일'), 'not-an-email')
    await user.type(screen.getByLabelText('비밀번호'), 'password123')
    await user.click(screen.getByRole('button', { name: '로그인' }))

    expect(await screen.findByText('이메일 형식이 올바르지 않습니다.')).toBeInTheDocument()
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('로그인에 성공하면 스토어에 사용자 정보가 채워진다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />)

    await user.type(screen.getByLabelText('이메일'), 'user@example.com')
    await user.type(screen.getByLabelText('비밀번호'), 'password123')
    await user.click(screen.getByRole('button', { name: '로그인' }))

    await waitFor(() => {
      expect(useAuthStore.getState().status).toBe('authenticated')
    })
    expect(useAuthStore.getState().user?.nickname).toBe('테스터')
  })

  it('비밀번호가 틀리면 로그인되지 않는다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />)

    await user.type(screen.getByLabelText('이메일'), 'user@example.com')
    await user.type(screen.getByLabelText('비밀번호'), 'wrongpassword')
    await user.click(screen.getByRole('button', { name: '로그인' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '로그인' })).toBeEnabled()
    })
    expect(useAuthStore.getState().status).toBe('anonymous')
  })
})
