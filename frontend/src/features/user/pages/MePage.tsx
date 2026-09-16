import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import type { ApiError } from '@/shared/api/errors'
import { useAuthStore } from '@/shared/store/authStore'
import { userApi, userKeys } from '../api'

const nicknameSchema = z.object({
  nickname: z.string().min(2, '2자 이상').max(20, '20자 이하'),
})

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, '현재 비밀번호를 입력해 주세요.'),
    newPassword: z
      .string()
      .min(8, '비밀번호는 8자 이상이어야 합니다.')
      .max(64)
      .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, '비밀번호는 영문과 숫자를 모두 포함해야 합니다.'),
    newPasswordConfirm: z.string(),
  })
  .refine((values) => values.newPassword === values.newPasswordConfirm, {
    path: ['newPasswordConfirm'],
    message: '비밀번호가 일치하지 않습니다.',
  })

type NicknameForm = z.infer<typeof nicknameSchema>
type PasswordForm = z.infer<typeof passwordSchema>

export default function MePage() {
  const queryClient = useQueryClient()
  const clearSession = useAuthStore((state) => state.clearSession)

  const { data: me, isLoading } = useQuery({
    queryKey: userKeys.me(),
    queryFn: userApi.me,
  })

  const nicknameForm = useForm<NicknameForm>({
    resolver: zodResolver(nicknameSchema),
    values: { nickname: me?.nickname ?? '' },
  })

  const passwordForm = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { currentPassword: '', newPassword: '', newPasswordConfirm: '' },
  })

  const updateNickname = useMutation({
    mutationFn: userApi.updateNickname,
    onSuccess: async (user) => {
      toast.success('닉네임을 변경했습니다.')
      queryClient.setQueryData(userKeys.me(), user)
      useAuthStore.setState((state) => ({ user: state.user ? { ...state.user, ...user } : user }))
    },
    onError: (error: ApiError) => toast.error(error.message),
  })

  const changePassword = useMutation({
    mutationFn: userApi.changePassword,
    onSuccess: () => {
      // 서버가 기존 refresh token을 모두 폐기하므로 다시 로그인해야 한다.
      toast.success('비밀번호를 변경했습니다. 다시 로그인해 주세요.')
      passwordForm.reset()
      clearSession()
    },
    onError: (error: ApiError) => {
      const messages = error.toFieldMessages()
      if (Object.keys(messages).length > 0) {
        Object.entries(messages).forEach(([field, message]) =>
          passwordForm.setError(field as keyof PasswordForm, { message }),
        )
        return
      }
      toast.error(error.message)
    },
  })

  if (isLoading || !me) {
    return <Skeleton className="h-64 w-full" />
  }

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>내 정보</CardTitle>
          <CardDescription>{me.email}</CardDescription>
        </CardHeader>
        <CardContent>
          <form
            className="space-y-4"
            onSubmit={nicknameForm.handleSubmit((values) => updateNickname.mutate(values))}
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="nickname">닉네임</Label>
              <Input id="nickname" {...nicknameForm.register('nickname')} />
              {nicknameForm.formState.errors.nickname && (
                <p className="text-sm text-destructive">
                  {nicknameForm.formState.errors.nickname.message}
                </p>
              )}
            </div>
            <Button type="submit" disabled={updateNickname.isPending}>
              닉네임 변경
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>비밀번호 변경</CardTitle>
          <CardDescription>변경하면 모든 기기에서 다시 로그인해야 합니다.</CardDescription>
        </CardHeader>
        <CardContent>
          <form
            className="space-y-4"
            onSubmit={passwordForm.handleSubmit((values) =>
              changePassword.mutate({
                currentPassword: values.currentPassword,
                newPassword: values.newPassword,
              }),
            )}
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="currentPassword">현재 비밀번호</Label>
              <Input
                id="currentPassword"
                type="password"
                autoComplete="current-password"
                {...passwordForm.register('currentPassword')}
              />
              {passwordForm.formState.errors.currentPassword && (
                <p className="text-sm text-destructive">
                  {passwordForm.formState.errors.currentPassword.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="newPassword">새 비밀번호</Label>
              <Input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                {...passwordForm.register('newPassword')}
              />
              {passwordForm.formState.errors.newPassword && (
                <p className="text-sm text-destructive">
                  {passwordForm.formState.errors.newPassword.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="newPasswordConfirm">새 비밀번호 확인</Label>
              <Input
                id="newPasswordConfirm"
                type="password"
                autoComplete="new-password"
                {...passwordForm.register('newPasswordConfirm')}
              />
              {passwordForm.formState.errors.newPasswordConfirm && (
                <p className="text-sm text-destructive">
                  {passwordForm.formState.errors.newPasswordConfirm.message}
                </p>
              )}
            </div>

            <Button type="submit" disabled={changePassword.isPending}>
              비밀번호 변경
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
