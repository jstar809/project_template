# M3. 프론트엔드 기반과 인증 화면

> 작성일: 2026-09-16 · 소요 시간: 약 1.5시간 · 관련 요구사항: PRD 8.6(FE-01~07), 13장(라우트와 화면)

## 1. 이번 단계의 목표

M2에서 만든 인증 API를 실제로 쓰는 화면을 만든다. 이 단계가 끝나면 다음이 가능해진다.

- 회원가입하고 로그인한다.
- 새로고침해도 로그인이 유지된다.
- Access Token이 만료되면 사용자가 모르는 사이에 자동으로 재발급된다.
- 로그인하지 않으면 보호된 화면에 들어갈 수 없고, 일반 사용자는 관리자 화면에 들어갈 수 없다.
- 다크 모드를 켤 수 있다.

## 2. 배경 개념

### 2.1 SPA와 클라이언트 라우팅
- **전통적인 방식**: 링크를 누르면 브라우저가 서버에 새 HTML을 요청하고 화면 전체를 다시 그린다.
- **SPA(Single Page Application)**: HTML은 한 번만 받고, 이후에는 **자바스크립트가 주소만 바꿔치기**하며 필요한 부분만 다시 그린다. 화면 전환이 빠르고 깜빡이지 않는다.

```text
[전통]  /login 클릭 → 서버에 요청 → 새 HTML 전체 수신 → 전체 렌더
[SPA ]  /login 클릭 → 주소만 변경(history API) → React가 로그인 컴포넌트만 렌더
```

주의할 점: 사용자가 `/me`를 주소창에 직접 입력하면 브라우저는 서버에 `/me`를 요청한다. 서버에 그런 경로가 없으므로 **무조건 index.html을 돌려줘야** 한다(SPA fallback). 개발 중에는 Vite가, 배포 시에는 nginx가 이 역할을 한다(M7).

### 2.2 서버 상태와 클라이언트 상태
React에서 상태를 한 덩어리로 다루면 금방 복잡해진다. 두 종류를 나누면 훨씬 단순해진다.

| | 서버 상태 | 클라이언트 상태 |
|---|---|---|
| 예 | 게시글 목록, 내 정보 | 다크 모드, 모달 열림, 로그인 여부 |
| 특징 | 내 것이 아니다. 서버가 주인이고 언제든 낡을 수 있다 | 브라우저 안에서만 산다 |
| 도구 | **TanStack Query** | **Zustand** |

TanStack Query는 "캐시 + 자동 갱신 + 로딩/에러 상태"를 대신 관리해준다. 그래서 서버 데이터를 Zustand에 복사하면 **두 곳이 서로 달라지는 문제**가 생긴다. 복사하지 않는 것이 규칙이다(PRD FE-03).

### 2.3 Access Token을 어디에 둘 것인가
| 저장 위치 | 장점 | 문제 |
|---|---|---|
| localStorage | 새로고침해도 유지 | **XSS로 훔칠 수 있다.** 악성 스크립트가 `localStorage.getItem('token')` 한 줄이면 끝 |
| 메모리(JS 변수) | 스크립트가 훔치기 어렵다 | 새로고침하면 사라진다 |

이 프로젝트는 **메모리**를 택했다. 새로고침 문제는 앱이 시작될 때 `/auth/refresh`를 한 번 호출해 해결한다. Refresh Token은 httpOnly 쿠키에 있어서 브라우저가 알아서 보내주기 때문에 가능하다.

```text
새로고침 → 메모리의 Access Token 사라짐
        → 앱 시작 시 POST /auth/refresh (쿠키 자동 전송)
        → 새 Access Token 수신 → 로그인 상태 복원
```

### 2.4 axios 인터셉터와 single-flight
- **인터셉터**: 모든 요청/응답이 반드시 거쳐가는 공통 통로. "요청에 토큰 붙이기", "401이면 재발급하기"를 한 곳에 쓸 수 있다.
- **single-flight(단일 비행)**: 같은 작업이 동시에 여러 번 일어나지 않게 **하나만 실행하고 나머지는 그 결과를 기다리게** 하는 기법.

왜 필요한가? 토큰이 만료되는 순간 화면에서 보통 여러 요청이 동시에 나간다.

```text
[없을 때]
요청A 401 → 재발급 호출 → T2 발급
요청B 401 → 재발급 호출 → T3 발급 (T2는 rotation으로 폐기됨)
요청C 401 → 재발급 호출 → T2를 쓰려다 "폐기된 토큰" → 재사용 탐지 발동 → 강제 로그아웃 ❌

[single-flight]
요청A 401 → 재발급 호출 ─┐
요청B 401 → 기다림       ├→ T2 하나만 발급 → 세 요청 모두 T2로 재시도 ✅
요청C 401 → 기다림       ┘
```

M2에서 만든 **재사용 탐지**가 정상 사용자를 튕겨내지 않게 하려면 프론트에서 이 처리가 반드시 필요하다.

구현은 단순하다. 진행 중인 Promise를 변수에 담아두고, 이미 있으면 그걸 반환한다.

```ts
let refreshPromise: Promise<string> | null = null

export async function refreshSession(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = rawClient.post('/auth/refresh')
      .then(...)
      .finally(() => { refreshPromise = null })   // 끝나면 비워서 다음 만료 때 다시 동작
  }
  return refreshPromise
}
```

### 2.5 라우트 가드
"로그인해야 볼 수 있는 화면"을 컴포넌트마다 검사하면 빠뜨리기 쉽다. 라우트를 감싸는 컴포넌트 하나로 처리한다.

```tsx
<Route element={<RequireAuth />}>      {/* 이 안의 모든 라우트가 보호된다 */}
  <Route path="me" element={<MePage />} />
</Route>
```

여기서 중요한 것은 **세 번째 상태**다. 로그인/비로그인만 있으면, 새로고침 직후(아직 복원 중)에 "비로그인"으로 판단해 로그인 화면이 잠깐 번쩍인다. 그래서 상태를 네 가지로 둔다.

```text
idle → loading(복원 중, 로딩 표시) → authenticated 또는 anonymous
```

### 2.6 shadcn/ui
버튼, 입력창 같은 컴포넌트를 **라이브러리로 설치하는 게 아니라 코드를 복사해 넣는** 방식이다.

- 장점: `src/components/ui/button.tsx`가 내 파일이라 자유롭게 고칠 수 있다. 라이브러리 업데이트에 끌려다니지 않는다.
- 단점: 업스트림 개선이 자동으로 들어오지 않는다.

### 2.7 MSW로 테스트하기
MSW(Mock Service Worker)는 **네트워크 계층에서** 요청을 가로채 가짜 응답을 준다.

- `axios.get`을 흉내 내는 게 아니라 실제 HTTP 요청을 가로채므로, 인터셉터·헤더·재시도 같은 실제 동작을 그대로 검증할 수 있다.
- 덕분에 "401 받고 재발급 후 재시도" 같은 흐름을 **진짜와 똑같이** 테스트할 수 있다.

## 3. 따라하기

### 3.1 설치

```bash
npm install react-router-dom @tanstack/react-query zustand axios react-hook-form zod @hookform/resolvers
npm install -D vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event msw
npx shadcn@latest add input label dropdown-menu sonner skeleton separator -y
```

### 3.2 폴더 구조

```text
src/
  app/            router.tsx, providers.tsx, layouts/, pages/   ← 앱 전체 뼈대
  features/
    auth/         api.ts, hooks.ts, components/, pages/          ← 인증 기능
    user/         api.ts, pages/                                 ← 내 정보 기능
  shared/
    api/          client.ts(axios), errors.ts(ApiError)
    store/        authStore.ts, themeStore.ts
    ui/           RouteGuards.tsx, FullPageSpinner.tsx
    lib/          env.ts
  components/ui/  shadcn 생성 컴포넌트
  test/           setup.ts, msw/, renderWithProviders.tsx
```

기능(feature) 단위로 묶으면 "게시판 기능을 통째로 들어내기"가 쉬워진다. 기능끼리 서로의 내부 파일을 import 하지 않는 것이 규칙이다.

### 3.3 에러를 한 타입으로 정규화

```ts
export class ApiError extends Error {
  readonly status: number
  readonly code: string         // 백엔드의 code (예: AUTH_INVALID_CREDENTIALS)
  readonly fieldErrors: FieldError[]

  toFieldMessages(): Record<string, string> { ... }  // 폼에 바로 꽂을 수 있는 형태
}
```

네트워크 끊김도, 서버 400도 모두 `ApiError`로 변환한다. 화면에서는 `error.message`만 보여주면 되고, 필드 오류가 있으면 폼에 매핑한다.

```tsx
useEffect(() => {
  if (!login.error) return
  Object.entries(login.error.toFieldMessages()).forEach(([field, message]) => {
    setError(field as keyof LoginForm, { message })
  })
}, [login.error, setError])
```

### 3.4 인증 스토어와 클라이언트 연결
순환 참조를 피하려고 방향을 한쪽으로 고정했다.

```text
authStore  ──import──▶  client (토큰 보관, 재발급)
    ▲                        │
    └──── 콜백으로 통지 ◀─────┘   setSessionHandlers({ onRefreshed, onExpired })
```

client는 스토어를 모른다. 대신 "재발급됨 / 세션 끊김"을 콜백으로 알린다. 스토어가 시작할 때 그 콜백을 등록한다.

### 3.5 라우터와 코드 분할

```tsx
const MePage = lazy(() => import('@/features/user/pages/MePage'))
const AdminDashboard = lazy(() => import('./pages/AdminDashboardPlaceholder'))

<Routes>
  <Route element={<MainLayout />}>
    <Route index element={<HomePage />} />
    <Route element={<GuestOnly />}>          {/* 로그인했으면 홈으로 */}
      <Route path="login" element={<LoginPage />} />
      <Route path="signup" element={<SignupPage />} />
    </Route>
    <Route element={<RequireAuth />}>        {/* 로그인 필요 */}
      <Route path="me" element={<MePage />} />
    </Route>
    <Route path="*" element={<NotFoundPage />} />
  </Route>

  <Route element={<RequireRole role="ADMIN" />}>   {/* 관리자만 */}
    <Route path="admin" element={<AdminLayout />}>
      <Route index element={<AdminDashboard />} />
    </Route>
  </Route>
</Routes>
```

`lazy`로 나누면 일반 사용자는 관리자 화면 코드를 내려받지 않는다.

### 3.6 폼 검증은 백엔드와 같은 규칙으로

```ts
const signupSchema = z.object({
  password: z.string()
    .min(8, '비밀번호는 8자 이상이어야 합니다.')
    .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, '비밀번호는 영문과 숫자를 모두 포함해야 합니다.'),
  ...
})
```

프론트 검증은 **사용자 편의**를 위한 것이고, 진짜 방어는 백엔드가 한다. 그래도 규칙을 같게 맞춰야 "프론트는 통과했는데 서버가 거부"하는 혼란이 없다.

## 4. 결정과 이유

| 결정 | 고려한 대안 | 선택 이유 |
|---|---|---|
| Access Token을 메모리에 | localStorage | XSS로 토큰을 훔기 어렵게. 새로고침은 refresh로 복원한다. |
| 상태를 4단계(idle/loading/authenticated/anonymous) | boolean isLoggedIn | 복원 중을 구분하지 않으면 새로고침마다 로그인 화면이 깜빡인다. |
| client → store 단방향 + 콜백 | store를 client에서 직접 import | 순환 참조를 피하고, client를 테스트에서 단독으로 쓸 수 있다. |
| 폼은 React Hook Form + Zod | 직접 useState로 관리 | 검증 규칙을 선언적으로 쓰고, 백엔드 규칙과 나란히 비교할 수 있다. |
| MSW로 테스트 | axios mock | 인터셉터와 재시도까지 포함한 실제 흐름을 검증하려면 네트워크 레벨 목이 필요하다. |
| shadcn `form` 컴포넌트 미사용 | shadcn Form 래퍼 사용 | 래퍼 없이도 충분히 짧고, 의존성과 추상화가 하나 줄어든다. |

## 5. 겪은 문제와 해결

### 문제 1: MSW가 요청을 하나도 가로채지 못함
```text
[MSW] Error: intercepted a request without a matching request handler:
ApiError: 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.
```
- **원인**: 핸들러를 `http://localhost/api/v1/...`로 등록했는데, 테스트 환경(jsdom)의 기준 주소가 `http://localhost:3000`이었다. 포트가 달라 매칭되지 않았다.
- **해결**: 기준 주소에 의존하지 않도록 와일드카드 패턴으로 바꿨다.

```ts
export const BASE = '*/api/v1'    // http://localhost/api/v1 → */api/v1
```
- **배운 점**: 상대 경로(`/api/v1/...`)로 요청하는 코드를 테스트할 때는, 어떤 절대 주소로 바뀌는지 확인해야 한다. 와일드카드가 가장 안전하다.

### 문제 2: 새로고침하면 로그인 화면이 잠깐 보인다 (설계로 예방)
처음 `RequireAuth`를 이렇게 쓰려 했다.

```tsx
if (!user) return <Navigate to="/login" />   // ❌
```

새로고침 직후에는 아직 `/auth/refresh`가 끝나지 않아 `user`가 없다. 그래서 로그인 화면으로 튕겼다가, 복원이 끝나면 다시 돌아오는 깜빡임이 생긴다. 상태에 `loading`을 두고 그동안 스피너를 보여주는 것으로 해결했다.

```tsx
if (status === 'idle' || status === 'loading') return <FullPageSpinner />
if (status !== 'authenticated') return <Navigate to={`/login?redirect=...`} replace />
```

### 문제 3: 빌드 경고 — 청크가 500KB를 넘는다
```text
(!) Some chunks are larger than 500 kB after minification.
dist/assets/index-BvsvUG1y.js   547.11 kB │ gzip: 173.01 kB
```
- **원인**: React, React Router, TanStack Query, Radix UI가 한 파일에 묶였다.
- **지금 판단**: gzip 기준 173KB로, 실제 전송량은 문제 될 수준이 아니다. 라우트 단위 코드 분할은 이미 적용했다.
- **후속**: M7에서 vendor 청크 분리를 검토한다. PRD의 권장 목표는 gzip 300KB 이하이므로 현재는 목표 안에 있다.

## 6. 확인 방법

### 자동 테스트

```bash
cd frontend
npm run lint && npm run typecheck && npm run test && npm run build
```

```text
 ✓ src/shared/api/client.test.ts (5 tests)
   ✓ 401을 받으면 재발급 후 원래 요청을 다시 보낸다
   ✓ 여러 요청이 동시에 401을 받아도 재발급은 한 번만 호출된다 (single-flight)
   ✓ 재발급도 실패하면 세션을 비우고 에러를 던진다
   ✓ 로그인 실패(401)는 재발급을 시도하지 않는다
   ✓ 서버 에러를 ApiError로 변환하고 필드 오류를 담는다
 ✓ src/shared/ui/RouteGuards.test.tsx (6 tests)
 ✓ src/features/auth/pages/LoginPage.test.tsx (3 tests)

 Test Files  3 passed (3)
      Tests  14 passed (14)
```

### 직접 실행해서 확인

```powershell
docker compose --profile postgresql up -d
cd backend;  $env:ADMIN_EMAIL="admin@example.com"; $env:ADMIN_PASSWORD="admin1234"; .\gradlew.bat bootRun
cd frontend; npm run dev
```

http://localhost:5173 에서

1. `/signup`에서 가입 → 로그인 화면으로 이동
2. 로그인 → 헤더에 닉네임 표시
3. **새로고침** → 로그인 유지 (개발자도구 Network 탭에 `auth/refresh` 호출이 보인다)
4. 일반 계정으로 `/admin` 접속 → `/403`으로 이동
5. 관리자(admin@example.com)로 로그인 → `/admin` 접근 가능
6. 헤더의 달/해 아이콘으로 다크 모드 전환
7. 로그아웃 → 홈으로 이동, 헤더가 로그인/회원가입 버튼으로 바뀜

프록시 동작은 프론트 주소로 API를 불러 확인할 수 있다.

```bash
curl -X POST http://localhost:5173/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"admin1234"}'
# {"accessToken":"eyJ...","expiresIn":900,"user":{...}}
```

## 7. 회고와 다음 단계

**잘된 점**
- single-flight 재발급을 테스트로 검증했다. M2의 재사용 탐지와 맞물리는 부분이라, 여기가 틀리면 사용자가 무작위로 로그아웃된다.
- 에러 처리를 `ApiError` 하나로 모아서, 앞으로 만들 화면들이 같은 방식으로 에러를 다룰 수 있게 됐다.
- 로딩 상태를 명시적으로 둬서 새로고침 깜빡임을 처음부터 없앴다.

**아쉬운 점 / 개선할 점**
- 브라우저에서 눈으로 하는 확인은 아직 못 했다(확장 연결 문제). 자동 테스트와 HTTP 호출로 대체했고, PROGRESS에 사용자 확인 항목으로 남겼다.
- 번들이 gzip 173KB다. 아직 여유가 있지만 에디터(TipTap)가 들어오는 M5 이후 다시 확인해야 한다.
- 접근성 린트(jsx-a11y)가 빠져 있어 수동으로 신경 써야 한다(M0 D-005).

**다음 단계 (M4)**
- 게시판, 게시글, 댓글, 파일 업로드 백엔드
- 목록·검색은 MyBatis, 쓰기는 JPA로 분담
- HTML sanitize와 파일 형식 검증
