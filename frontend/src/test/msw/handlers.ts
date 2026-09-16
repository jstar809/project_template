import { http, HttpResponse } from 'msw'
import type { UserDto } from '@/shared/api/client'

export const BASE = '*/api/v1'

export const testUser: UserDto = {
  id: 1,
  email: 'user@example.com',
  nickname: '테스터',
  role: 'USER',
  createdAt: '2026-09-16T00:00:00Z',
}

export const adminUser: UserDto = { ...testUser, id: 2, nickname: '관리자', role: 'ADMIN' }

/** 백엔드의 ProblemDetail 형식과 같은 모양으로 만든다. */
export function problem(
  status: number,
  code: string,
  detail: string,
  errors?: { field: string; reason: string }[],
) {
  return HttpResponse.json(
    { status, code, detail, title: 'Error', traceId: 'test-trace', errors },
    { status },
  )
}

export const handlers = [
  http.post(`${BASE}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string }
    if (body.password !== 'password123') {
      return problem(401, 'AUTH_INVALID_CREDENTIALS', '이메일 또는 비밀번호가 올바르지 않습니다.')
    }
    return HttpResponse.json({ accessToken: 'access-1', expiresIn: 900, user: testUser })
  }),

  http.post(`${BASE}/auth/signup`, () => HttpResponse.json(testUser, { status: 201 })),

  http.post(`${BASE}/auth/logout`, () => new HttpResponse(null, { status: 204 })),

  http.post(`${BASE}/auth/refresh`, () =>
    HttpResponse.json({ accessToken: 'access-refreshed', expiresIn: 900, user: testUser }),
  ),

  http.get(`${BASE}/users/me`, ({ request }) => {
    const auth = request.headers.get('Authorization')
    if (!auth) {
      return problem(401, 'COMMON_UNAUTHORIZED', '인증이 필요합니다.')
    }
    return HttpResponse.json(testUser)
  }),
]
