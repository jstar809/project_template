import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { apiClient, setAccessToken } from './client'
import { ApiError } from './errors'
import { BASE, problem, testUser } from '@/test/msw/handlers'
import { server } from '@/test/msw/server'

describe('API 클라이언트', () => {
  beforeEach(() => {
    setAccessToken(null)
  })

  it('401을 받으면 재발급 후 원래 요청을 다시 보낸다', async () => {
    let meCalls = 0
    server.use(
      http.get(`${BASE}/users/me`, ({ request }) => {
        meCalls += 1
        // 첫 호출은 만료된 토큰, 재발급 이후에는 성공
        if (request.headers.get('Authorization') !== 'Bearer access-refreshed') {
          return problem(401, 'COMMON_UNAUTHORIZED', '인증이 필요합니다.')
        }
        return HttpResponse.json(testUser)
      }),
    )

    setAccessToken('expired-token')
    const response = await apiClient.get('/users/me')

    expect(response.data).toEqual(testUser)
    expect(meCalls).toBe(2) // 실패 1회 + 재시도 1회
  })

  it('여러 요청이 동시에 401을 받아도 재발급은 한 번만 호출된다 (single-flight)', async () => {
    let refreshCalls = 0
    server.use(
      http.post(`${BASE}/auth/refresh`, async () => {
        refreshCalls += 1
        // 동시성 확인을 위해 약간 지연시킨다.
        await new Promise((resolve) => setTimeout(resolve, 20))
        return HttpResponse.json({
          accessToken: 'access-refreshed',
          expiresIn: 900,
          user: testUser,
        })
      }),
      http.get(`${BASE}/users/me`, ({ request }) => {
        if (request.headers.get('Authorization') !== 'Bearer access-refreshed') {
          return problem(401, 'COMMON_UNAUTHORIZED', '인증이 필요합니다.')
        }
        return HttpResponse.json(testUser)
      }),
    )

    setAccessToken('expired-token')
    const results = await Promise.all([
      apiClient.get('/users/me'),
      apiClient.get('/users/me'),
      apiClient.get('/users/me'),
    ])

    expect(results).toHaveLength(3)
    expect(refreshCalls).toBe(1)
  })

  it('재발급도 실패하면 세션을 비우고 에러를 던진다', async () => {
    server.use(
      http.post(`${BASE}/auth/refresh`, () =>
        problem(401, 'AUTH_INVALID_REFRESH_TOKEN', '다시 로그인해 주세요.'),
      ),
      http.get(`${BASE}/users/me`, () => problem(401, 'COMMON_UNAUTHORIZED', '인증이 필요합니다.')),
    )

    setAccessToken('expired-token')

    await expect(apiClient.get('/users/me')).rejects.toBeInstanceOf(ApiError)
  })

  it('로그인 실패(401)는 재발급을 시도하지 않는다', async () => {
    let refreshCalls = 0
    server.use(
      http.post(`${BASE}/auth/refresh`, () => {
        refreshCalls += 1
        return HttpResponse.json({ accessToken: 'x', expiresIn: 900, user: testUser })
      }),
    )

    await expect(
      apiClient.post('/auth/login', { email: 'user@example.com', password: 'wrong' }),
    ).rejects.toMatchObject({ code: 'AUTH_INVALID_CREDENTIALS' })
    expect(refreshCalls).toBe(0)
  })

  it('서버 에러를 ApiError로 변환하고 필드 오류를 담는다', async () => {
    server.use(
      http.post(`${BASE}/auth/signup`, () =>
        problem(400, 'COMMON_INVALID_INPUT', '입력값이 올바르지 않습니다.', [
          { field: 'password', reason: '비밀번호는 영문과 숫자를 모두 포함해야 합니다.' },
        ]),
      ),
    )

    const error = await apiClient
      .post('/auth/signup', { email: 'a@b.com', password: 'short', nickname: '테스터' })
      .catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    const apiError = error as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.code).toBe('COMMON_INVALID_INPUT')
    expect(apiError.toFieldMessages()).toHaveProperty('password')
  })
})
