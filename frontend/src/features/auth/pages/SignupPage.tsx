import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useSignup } from '../hooks'

/** 검증 규칙은 백엔드 Bean Validation과 같게 맞춘다(PRD FE-06). */
const signupSchema = z
  .object({
    email: z.email('이메일 형식이 올바르지 않습니다.').max(255),
    password: z
      .string()
      .min(8, '비밀번호는 8자 이상이어야 합니다.')
      .max(64, '비밀번호는 64자 이하여야 합니다.')
      .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, '비밀번호는 영문과 숫자를 모두 포함해야 합니다.'),
    passwordConfirm: z.string(),
    nickname: z
      .string()
      .min(2, '닉네임은 2자 이상이어야 합니다.')
      .max(20, '닉네임은 20자 이하여야 합니다.'),
  })
  .refine((values) => values.password === values.passwordConfirm, {
    path: ['passwordConfirm'],
    message: '비밀번호가 일치하지 않습니다.',
  })

type SignupForm = z.infer<typeof signupSchema>

export default function SignupPage() {
  const signup = useSignup()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<SignupForm>({
    resolver: zodResolver(signupSchema),
    defaultValues: { email: '', password: '', passwordConfirm: '', nickname: '' },
  })

  useEffect(() => {
    if (!signup.error) return
    Object.entries(signup.error.toFieldMessages()).forEach(([field, message]) => {
      setError(field as keyof SignupForm, { message })
    })
  }, [signup.error, setError])

  return (
    <div className="mx-auto flex max-w-sm flex-col justify-center py-10">
      <Card>
        <CardHeader>
          <CardTitle>회원가입</CardTitle>
          <CardDescription>이메일과 비밀번호로 가입합니다.</CardDescription>
        </CardHeader>
        <CardContent>
          <form
            className="space-y-4"
            onSubmit={handleSubmit((values) =>
              signup.mutate({
                email: values.email,
                password: values.password,
                nickname: values.nickname,
              }),
            )}
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="email">이메일</Label>
              <Input id="email" type="email" autoComplete="email" {...register('email')} />
              {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="nickname">닉네임</Label>
              <Input id="nickname" autoComplete="nickname" {...register('nickname')} />
              {errors.nickname && (
                <p className="text-sm text-destructive">{errors.nickname.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">비밀번호</Label>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                {...register('password')}
              />
              {errors.password && (
                <p className="text-sm text-destructive">{errors.password.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="passwordConfirm">비밀번호 확인</Label>
              <Input
                id="passwordConfirm"
                type="password"
                autoComplete="new-password"
                {...register('passwordConfirm')}
              />
              {errors.passwordConfirm && (
                <p className="text-sm text-destructive">{errors.passwordConfirm.message}</p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={isSubmitting || signup.isPending}>
              {signup.isPending ? '가입 중…' : '회원가입'}
            </Button>
          </form>

          <p className="mt-4 text-center text-sm text-muted-foreground">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="underline underline-offset-4">
              로그인
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
