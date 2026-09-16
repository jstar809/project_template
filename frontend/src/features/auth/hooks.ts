import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { authApi, type LoginPayload, type SignupPayload } from './api'
import { ApiError } from '@/shared/api/errors'
import { useAuthStore } from '@/shared/store/authStore'

/** 로그인. 성공하면 스토어에 세션을 채우고 원래 가려던 경로로 이동한다. */
export function useLogin(redirectTo: string) {
  const navigate = useNavigate()
  const setSession = useAuthStore((state) => state.setSession)

  return useMutation({
    mutationFn: (payload: LoginPayload) => authApi.login(payload),
    onSuccess: (data) => {
      setSession(data)
      toast.success(`${data.user.nickname}님, 환영합니다.`)
      navigate(redirectTo, { replace: true })
    },
    onError: (error: ApiError) => {
      // 필드 오류는 폼에서 표시하고, 그 외에는 토스트로 알린다.
      if (error.fieldErrors.length === 0) {
        toast.error(error.message)
      }
    },
  })
}

/** 회원가입. 성공하면 로그인 화면으로 보낸다. */
export function useSignup() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (payload: SignupPayload) => authApi.signup(payload),
    onSuccess: () => {
      toast.success('가입이 완료되었습니다. 로그인해 주세요.')
      navigate('/login', { replace: true })
    },
    onError: (error: ApiError) => {
      if (error.fieldErrors.length === 0) {
        toast.error(error.message)
      }
    },
  })
}

/** 로그아웃. 서버 호출이 실패해도 클라이언트 세션은 지운다. */
export function useLogout() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const clearSession = useAuthStore((state) => state.clearSession)

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSettled: () => {
      clearSession()
      queryClient.clear()
      navigate('/', { replace: true })
    },
  })
}
