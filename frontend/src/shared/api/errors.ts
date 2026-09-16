import { AxiosError } from 'axios'

/** 백엔드가 내려주는 RFC 9457 ProblemDetail 형식 */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  code?: string
  traceId?: string
  errors?: FieldError[]
}

export interface FieldError {
  field: string
  reason: string
}

/**
 * 화면에서 다루기 쉬운 형태로 정규화한 에러.
 * 네트워크 오류든 서버 오류든 이 타입 하나로 처리한다.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly traceId?: string
  readonly fieldErrors: FieldError[]

  constructor(params: {
    message: string
    status: number
    code: string
    traceId?: string
    fieldErrors?: FieldError[]
  }) {
    super(params.message)
    this.name = 'ApiError'
    this.status = params.status
    this.code = params.code
    this.traceId = params.traceId
    this.fieldErrors = params.fieldErrors ?? []
  }

  /** 필드 단위 오류를 { 필드명: 메시지 } 로 바꾼다. 폼에 매핑할 때 쓴다. */
  toFieldMessages(): Record<string, string> {
    return Object.fromEntries(this.fieldErrors.map((error) => [error.field, error.reason]))
  }
}

/** axios 에러를 ApiError로 변환한다. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error

  if (error instanceof AxiosError) {
    const problem = error.response?.data as ProblemDetail | undefined
    if (problem && typeof problem.status === 'number') {
      return new ApiError({
        message: problem.detail ?? problem.title ?? '요청을 처리하지 못했습니다.',
        status: problem.status,
        code: problem.code ?? 'UNKNOWN',
        traceId: problem.traceId,
        fieldErrors: problem.errors,
      })
    }
    // 서버에 닿지 못한 경우(네트워크 끊김, CORS 등)
    return new ApiError({
      message: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.',
      status: 0,
      code: 'NETWORK_ERROR',
    })
  }

  return new ApiError({
    message: error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.',
    status: 0,
    code: 'UNKNOWN',
  })
}
