import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useLogin } from '../hooks'

const loginSchema = z.object({
  email: z.email('이메일 형식이 올바르지 않습니다.'),
  password: z.string().min(1, '비밀번호를 입력해 주세요.'),
})

type LoginForm = z.infer<typeof loginSchema>

export default function LoginPage() {
  const [searchParams] = useSearchParams()
  const redirectTo = searchParams.get('redirect') ?? '/'
  const login = useLogin(redirectTo)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  // 서버가 내려준 필드 오류를 폼에 반영한다.
  useEffect(() => {
    if (!login.error) return
    Object.entries(login.error.toFieldMessages()).forEach(([field, message]) => {
      setError(field as keyof LoginForm, { message })
    })
  }, [login.error, setError])

  return (
    <div className="mx-auto flex max-w-sm flex-col justify-center py-10">
      <Card>
        <CardHeader>
          <CardTitle>로그인</CardTitle>
          <CardDescription>계정 정보를 입력해 주세요.</CardDescription>
        </CardHeader>
        <CardContent>
          <form
            className="space-y-4"
            onSubmit={handleSubmit((values) => login.mutate(values))}
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="email">이메일</Label>
              <Input id="email" type="email" autoComplete="email" {...register('email')} />
              {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">비밀번호</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                {...register('password')}
              />
              {errors.password && (
                <p className="text-sm text-destructive">{errors.password.message}</p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={isSubmitting || login.isPending}>
              {login.isPending ? '로그인 중…' : '로그인'}
            </Button>
          </form>

          <p className="mt-4 text-center text-sm text-muted-foreground">
            계정이 없으신가요?{' '}
            <Link to="/signup" className="underline underline-offset-4">
              회원가입
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
