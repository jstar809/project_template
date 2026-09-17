# 사용 가이드

이 템플릿을 **처음 쓰는 사람**을 위한 문서다. 설치부터 기능 사용, 새 기능 추가, 설정 변경까지 순서대로 다룬다.

## 문서 지도

| 문서 | 언제 보나 |
|---|---|
| [README.md](../README.md) | 프로젝트가 뭔지, 어떻게 실행하는지 한눈에 |
| **usage.md (이 문서)** | 실제로 써보면서 궁금할 때 |
| [deployment.md](deployment.md) | 서버에 올릴 때 |
| [PRD.md](PRD.md) | "왜 이렇게 만들었는지" 요구사항 원본 |
| [decisions.md](decisions.md) | "왜 이 기술/방식을 골랐는지" |
| [devlog/](devlog/) | 만드는 과정을 따라가며 배우고 싶을 때 |
| [conventions.md](conventions.md) | 코드를 쓰기 전 규칙 확인 |

---

# 1부. 시작하기

## 1-1. 처음 실행 (5분)

**필요한 것**: JDK 21, Node.js 22 이상, Docker Desktop(실행 중)

```powershell
# 1) 환경 파일 준비
copy .env.example .env

# 2) .env 를 열어 관리자 계정을 채운다 (최초 1회)
#    ADMIN_EMAIL=admin@example.com
#    ADMIN_PASSWORD=admin1234

# 3) DB 실행
.\scripts\db-up.ps1

# 4) 백엔드 실행 (새 터미널)
.\scripts\be-run.ps1

# 5) 프론트 실행 (새 터미널)
.\scripts\fe-run.ps1
```

http://localhost:5173 을 연다. macOS/Linux는 `make db-up DB=postgresql`, `make be-run`, `make fe-run`.

## 1-2. 잘 떴는지 확인

| 확인 | 주소 | 정상 결과 |
|---|---|---|
| 백엔드 | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| API 문서 | http://localhost:8080/swagger-ui.html | Swagger 화면 |
| 프론트 | http://localhost:5173 | 게시판 목록 화면 |

백엔드 로그에 이 줄이 보이면 관리자 계정이 만들어진 것이다.

```text
AdminAccountInitializer - 초기 관리자 계정을 생성했다: admin@example.com
```

## 1-3. 처음 할 일 3가지

1. **관리자로 로그인** — `.env`에 적은 이메일/비밀번호
2. **게시판 만들기** — 우측 상단 메뉴 → 관리자 → 게시판 관리 → 새 게시판
3. **글 써보기** — 헤더의 게시판 클릭 → 글쓰기

> 기본으로 `공지사항`, `자유게시판` 두 개가 이미 들어 있다.

---

# 2부. 기능 사용법

## 2-1. 일반 사용자

### 회원가입과 로그인
- 비밀번호는 **8자 이상, 영문과 숫자 포함**이어야 한다.
- 로그인 상태는 새로고침해도 유지된다. 브라우저를 닫아도 14일간 유지된다.
- 로그인을 5분 안에 10번 실패하면 잠시 막힌다(기본값).

### 글쓰기
| 하고 싶은 것 | 방법 |
|---|---|
| 서식(굵게, 목록 등) | 에디터 위쪽 버튼 |
| 이미지 넣기 | **붙여넣기(Ctrl+V)** 또는 끌어다 놓기 → 바로 업로드됨 |
| 파일 첨부 | 아래 "파일 추가" 버튼 |
| 공지로 올리기 | 관리자만 보이는 체크박스 |

> 글을 저장하지 않고 나가면, 올렸던 이미지는 24시간 뒤 서버가 자동으로 정리한다.

### 글 찾기
목록 위 검색창에서 **제목 / 내용 / 제목+내용 / 작성자** 중에 골라 검색한다. 검색어와 페이지 번호가 주소에 남으므로, 그 주소를 복사해 공유하면 같은 화면이 열린다.

### 댓글
- 로그인해야 쓸 수 있다.
- 댓글에 **답글(대댓글)** 을 달 수 있다. 답글에는 다시 답글을 달 수 없다.
- 내가 쓴 댓글만 수정·삭제할 수 있다.

## 2-2. 관리자

우측 상단 사용자 메뉴 → **관리자**

### 대시보드
전체 사용자·글·댓글 수, 최근 7일 가입자와 게시글 추이, 최근 글 5개.

### 게시판 관리

새 게시판을 만들 때 정하는 값들이다.

| 항목 | 설명 |
|---|---|
| 주소(slug) | URL에 쓰이는 이름. 영문 소문자·숫자·하이픈만. 예: `notice` → `/boards/notice` |
| 읽기 권한 | **누구나** / 회원 / 관리자 |
| 쓰기 권한 | 회원 / 관리자 |
| 댓글 허용 | 끄면 댓글을 쓸 수 없다 |
| 첨부 허용 | 끄면 파일을 붙일 수 없다 |
| 사용 | 끄면 목록에서 사라진다(글은 남는다) |

**권한 조합 예시**

| 만들고 싶은 게시판 | 읽기 | 쓰기 |
|---|---|---|
| 공지사항 | 누구나 | 관리자 |
| 자유게시판 | 누구나 | 회원 |
| 회원 전용 게시판 | 회원 | 회원 |
| 내부 메모 | 관리자 | 관리자 |

순서는 위/아래 버튼으로 바꾼다. 바꾸면 사용자 화면 헤더 메뉴 순서도 같이 바뀐다.

> **게시판 삭제**: 글이 하나라도 있으면 삭제되지 않는다. 이때는 **"사용" 체크를 꺼서 감추는 것**이 맞다. 실수로 글 전체를 날리는 것을 막기 위한 장치다.

### 게시글·댓글 관리

체크박스로 여러 개를 골라 한 번에 처리한다.

| 버튼 | 결과 | 되돌리기 |
|---|---|---|
| 숨기기 | 사용자 화면에서 사라짐 | **복구** 버튼으로 되돌림 |
| 복구 | 다시 보임 | |
| 삭제 | 목록에서 사라짐 | 화면에서는 불가 (DB에는 남아 있음) |

**판단 기준**: 신고를 받았거나 확인이 필요한 글은 **숨기기**, 명백한 스팸은 삭제.

숨긴 댓글은 사라지지 않고 "관리자에 의해 숨겨진 댓글입니다."로 바뀐다. 거기 달린 답글이 붕 뜨지 않게 하기 위해서다.

### 사용자 관리

| 동작 | 효과 |
|---|---|
| 관리자로 / 회원으로 | 역할 변경 |
| 정지 | 로그인과 이용이 막힌다 |
| 정지 해제 | 다시 이용 가능 |

- **본인 계정은 바꿀 수 없다.** 관리자가 0명이 되는 사고를 막기 위해서다.
- 역할을 바꾸거나 정지하면 그 사용자는 **다시 로그인해야 한다.** 다만 이미 발급된 접속 토큰 때문에 최대 15분간은 이전 권한이 남을 수 있다.

---

# 3부. 개발자 가이드

## 3-1. 새 기능(도메인) 추가하기

`FAQ` 기능을 추가한다고 가정한 순서다.

### 1단계: DB 테이블

**두 DB 폴더에 같은 번호로** 파일을 만든다. 하나만 만들면 다른 DB에서 실행이 실패한다.

```text
backend/src/main/resources/db/migration/postgresql/V4__faq.sql
backend/src/main/resources/db/migration/mysql/V4__faq.sql
```

이미 적용된 마이그레이션 파일은 **절대 수정하지 않는다.** 바꾸려면 새 번호로 추가한다.

### 2단계: 도메인 코드

```text
backend/src/main/java/com/example/template/domain/faq/
  domain/        Faq(엔티티), FaqRepository(인터페이스), FaqErrorCode
  infra/jpa/     FaqJpaRepository, FaqRepositoryAdapter
  infra/mybatis/ FaqQueryMapper        ← 목록·검색이 필요하면
  application/   FaqService
  api/           FaqController, 요청·응답 DTO(record)
```

기존 도메인(`board`, `post`)을 그대로 복사해 이름만 바꾸는 게 가장 빠르다.

### 3단계: JPA와 MyBatis 중 무엇을 쓸까

| 하려는 일 | 쓸 것 |
|---|---|
| 저장, 수정, 삭제 | JPA |
| ID로 한 건 조회 | JPA |
| 목록, 검색, 페이징 | **MyBatis** |
| 통계, 여러 테이블 조인 | **MyBatis** |
| 조회수 +1 같은 증감 | JPA `@Modifying` |

### 4단계: 규칙 (어기면 테스트가 실패한다)

```text
의존 방향:  api → application → domain ← infra
- Controller는 Entity를 주고받지 않는다 (DTO를 쓴다)
- @Transactional은 application 계층에만
- 다른 도메인은 그 도메인의 Service를 통해 부른다 (Repository 직접 접근 금지)
- 필드 주입(@Autowired 필드) 금지, 생성자 주입만
```

`./gradlew test` 를 돌리면 ArchUnit이 자동으로 검사한다.

### 5단계: 에러 코드 추가

```java
public enum FaqErrorCode implements ErrorCode {
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "FAQ_NOT_FOUND", "FAQ를 찾을 수 없습니다.");
    // ...
}

// 서비스에서
throw new BusinessException(FaqErrorCode.FAQ_NOT_FOUND);
```

프론트는 이 `code` 값으로 분기한다. 한 번 정한 코드 문자열은 바꾸지 않는다.

### 6단계: 프론트

```text
frontend/src/features/faq/
  api.ts       API 호출 + queryKey
  pages/       화면
  components/  이 기능 전용 컴포넌트
```

라우트는 `src/app/router.tsx`에 추가한다.

```tsx
const FaqPage = lazy(() => import('@/features/faq/pages/FaqPage'))
// ...
<Route path="faq" element={<FaqPage />} />
```

### 7단계: 테스트

| 무엇을 | 어디에 |
|---|---|
| 도메인 규칙, 권한 판정 | `backend/src/test/.../domain/` 단위 테스트 |
| API 동작 | `IntegrationTestSupport`를 상속한 통합 테스트 |
| 화면 | `frontend/src/features/faq/pages/*.test.tsx` (MSW로 API 흉내) |

```powershell
.\scripts\be-test.ps1 -All     # 두 DB 모두 통과해야 한다
.\scripts\fe-check.ps1
```

## 3-2. 자주 하는 수정

| 하고 싶은 것 | 고칠 곳 |
|---|---|
| 로고·사이트 이름 | `frontend/src/app/layouts/MainLayout.tsx`, `index.html`의 `<title>` |
| 색상·디자인 | `frontend/src/index.css` 의 CSS 변수 |
| 관리자 메뉴 추가 | `frontend/src/app/layouts/AdminLayout.tsx` 의 `MENUS` |
| 허용 파일 형식 | `backend/.../file/application/FileValidator.java` 의 `ALLOWED` |
| 본문 허용 태그 | `backend/.../global/html/HtmlSanitizer.java` |
| 비밀번호 규칙 | 백엔드 `SignupRequest` + 프론트 `SignupPage`의 zod 스키마 (**둘 다**) |

## 3-3. 프로젝트 이름 바꾸기

1. `backend/settings.gradle` → `rootProject.name`
2. 패키지 `com.example.template` → IDE의 rename refactoring
3. `backend/build.gradle` → `group`
4. `docker-compose.yml` → `name: template`
5. `frontend/package.json` → `name`, `index.html` → `<title>`

---

# 4부. 설정 바꾸기

`.env` 파일을 고치고 서버를 다시 시작하면 적용된다.

## 자주 바꾸는 값

| 항목 | 변수 | 기본값 | 설명 |
|---|---|---|---|
| DB 종류 | `DB_VENDOR` | `postgresql` | `mysql`로 바꾸면 MySQL 사용 |
| 백엔드 포트 | `SERVER_PORT` | 8080 | 충돌 시 변경 |
| 파일 최대 크기 | `FILE_MAX_SIZE` | 10MB | |
| 로그인 시도 제한 | `LOGIN_RATE_LIMIT` | 10 | 5분 기준 |
| 로그인 유지 기간 | `JWT_REFRESH_TTL` | P14D | 14일 |
| 접속 토큰 수명 | `JWT_ACCESS_TTL` | PT15M | 15분 |
| API 문서 공개 | `SWAGGER_ENABLED` | true | 운영에서는 false |
| 관리자 계정 | `ADMIN_EMAIL` / `ADMIN_PASSWORD` | 없음 | 최초 1회 생성용 |

> `P14D`, `PT15M`은 ISO-8601 기간 표기다. `P`는 기간, `T` 뒤는 시간 단위. 예: `PT30M`(30분), `P7D`(7일).

## DB를 MySQL로 바꾸기

```powershell
# .env 수정
#   DB_VENDOR=mysql
#   DB_PORT=3306          (도커 포트 충돌 시 MYSQL_PORT도 함께)
#   DB_INTERNAL_PORT=3306

.\scripts\db-up.ps1 -Db mysql
.\scripts\be-run.ps1 -Db mysql
```

두 DB는 **스키마가 서로 공유되지 않는다.** 데이터는 각자 따로 쌓인다.

---

# 5부. 문제가 생겼을 때

## 실행이 안 될 때

| 증상 | 원인과 해결 |
|---|---|
| `Port 8080 was already in use` | 다른 프로그램이 쓰는 중. `.env`의 `SERVER_PORT`를 바꾸거나 그 프로그램을 종료 |
| `Bind for 0.0.0.0:5432 failed` | 로컬 PostgreSQL이 이미 실행 중. `.env`의 `POSTGRES_PORT`를 5433 등으로 변경 |
| `UnknownHostException` / DB 접속 실패 | DB 컨테이너가 떠 있는지 확인 (`docker compose ps`) |
| 백엔드 테스트가 실패 | Docker Desktop이 실행 중인지 확인 (Testcontainers가 필요) |
| `app.jwt.secret 은 32바이트 이상` | `.env`의 `JWT_SECRET`을 더 길게 |

## 동작이 이상할 때

| 증상 | 확인할 것 |
|---|---|
| 로그인이 계속 실패(429) | 시도 제한에 걸림. 5분 기다리거나 `LOGIN_RATE_LIMIT`를 올린다 |
| 새로고침하면 로그아웃됨 | 브라우저가 쿠키를 차단하는지, 주소가 `CORS_ALLOWED_ORIGINS`에 있는지 |
| 브라우저에서만 403 | `CORS_ALLOWED_ORIGINS`에 접속 중인 주소를 추가 |
| 파일 업로드 실패 | 크기(`FILE_MAX_SIZE`), 확장자(이미지/pdf/문서만), 게시판의 "첨부 허용" |
| 게시판이 목록에 안 보임 | "사용" 체크, 읽기 권한, 로그인 여부 |
| 글이 안 보임 | 관리자 화면에서 "숨김" 상태인지 확인 |

## 로그 보기

```powershell
# 개발 모드: 백엔드를 실행한 터미널에 바로 출력된다

# 도커 실행 시
docker compose logs -f backend
docker compose logs -f frontend
```

에러 응답에는 `traceId`가 들어 있다. 그 값으로 로그를 검색하면 해당 요청만 골라볼 수 있다.

```json
{ "status": 404, "code": "POST_NOT_FOUND", "traceId": "8cd76f6da7624091" }
```

## 완전히 초기화하기

```powershell
docker compose --profile full --profile postgresql --profile mysql down -v
```

`-v`는 **DB 데이터와 업로드 파일까지 삭제**한다. 개발 중 데이터가 꼬였을 때만 쓴다.

---

# 부록. 명령어 모음

| 목적 | Windows | macOS/Linux |
|---|---|---|
| DB 실행 | `.\scripts\db-up.ps1` | `make db-up DB=postgresql` |
| DB 종료 | `.\scripts\db-down.ps1` | `make db-down` |
| 백엔드 실행 | `.\scripts\be-run.ps1` | `make be-run` |
| 프론트 실행 | `.\scripts\fe-run.ps1` | `make fe-run` |
| 백엔드 테스트(두 DB) | `.\scripts\be-test.ps1 -All` | `make be-test-all` |
| 프론트 검사 | `.\scripts\fe-check.ps1` | `make fe-check` |
| 코드 포맷 정리 | `cd backend; .\gradlew.bat spotlessApply` | `make lint` |
| 전체 도커 실행 | `docker compose --profile full --profile postgresql up -d --build` | 동일 |
| E2E 테스트 | `cd frontend; npm run e2e` | 동일 |
