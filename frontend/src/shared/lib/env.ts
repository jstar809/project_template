import { z } from 'zod'

/**
 * 환경변수는 여기서 한 번만 읽고 검증한다.
 * 값이 잘못되면 앱이 뜨는 시점에 바로 알 수 있다.
 */
const envSchema = z.object({
  VITE_API_BASE_URL: z.string().default('/api/v1'),
})

const parsed = envSchema.safeParse(import.meta.env)

if (!parsed.success) {
  console.error('환경변수 설정이 올바르지 않습니다.', parsed.error.flatten().fieldErrors)
  throw new Error('환경변수 설정 오류')
}

export const env = {
  apiBaseUrl: parsed.data.VITE_API_BASE_URL,
}
