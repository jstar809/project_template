# 진행 현황판

> - **기준 문서**: [docs/PRD.md](docs/PRD.md)
> - **상태 표기**: ⬜ 대기 · 🔄 진행 중 · 🙋 사용자 확인 필요 · ✅ 완료 · ⚠️ 이슈 있음

## 한눈에 보기

| 단계 | 내용 | 상태 | 사용자 확인 | 커밋 | 완료일 |
|---|---|---|---|---|---|
| 준비 | 환경 점검, 레포 초기화 | ✅ | ✅ | 첫 커밋 | 2026-09-15 |
| M0 | 레포 스캐폴딩과 개발환경 | ✅ | ⏭️ 보류 | `8861f88` | 2026-09-16 |
| M1 | 백엔드 공통 인프라 (JPA+MyBatis, DB 2종, ArchUnit) | ✅ | ⏭️ 보류 | `59467e0` | 2026-09-16 |
| M2 | 인증과 사용자 (JWT) | 🙋 | ⬜ | M2 커밋 | 2026-09-16 |
| M3 | 프론트 기반과 인증 화면 | ⬜ | ⬜ | | |
| M4 | CMS 백엔드 (게시판·글·댓글·파일) | ⬜ | ⬜ | | |
| M5 | CMS 사용자 화면과 에디터 | ⬜ | ⬜ | | |
| M6 | 관리자 페이지 | ⬜ | ⬜ | | |
| M7 | 품질 도구, Docker, CI | ⬜ | ⬜ | | |
| M8 | CD(자동 배포)와 운영 문서 | ⬜ | ⬜ | | |

## 진행 방법 (단계마다 반복)

1. **시작**: 사용자가 "M0 시작"처럼 말한다.
2. **작업**: Claude가 아래 순서로 진행한다.
   1. 작업 체크리스트를 하나씩 체크하며 구현한다.
   2. 완료 조건을 직접 검증한다.
   3. devlog를 작성한다.
   4. 커밋한다.
   5. 상태를 🙋로 바꾼다.
3. **확인**: 사용자가 그 단계의 **"내가 확인할 것"** 을 따라 해본다.
4. **완료 또는 수정**
   - 문제가 없으면 "M0 확인 완료"라고 말한다. 상태가 ✅로 바뀌고 다음 단계로 넘어간다.
   - 문제가 있으면 맨 아래 **이슈 기록**에 적고 수정한 뒤 다시 확인한다.

> 모든 명령은 Windows PowerShell 기준이고, 프로젝트 폴더(`D:\이준선 문서\개인문서\firstproject`)에서 실행한다.

---

## 준비 단계

### 개발 환경 점검 결과 (2026-09-15)
| 도구 | PRD 기준 | 설치된 버전 | 결과 |
|---|---|---|---|
| Java | JDK 21 | 21.0.11 (Oracle) | ✅ |
| Node.js | 22 LTS로 변경 (결정 D-002) | 22.22.3 | ✅ |
| npm | Node 번들 | 10.9.8 | ✅ |
| Docker | Docker + Compose v2 | 29.5.2 (실행 중) | ✅ |
| Git | – | 설치됨, 사용자 설정 완료 | ✅ |

### 작업
- [x] 프로젝트 폴더 생성과 `git init` (사용자)
- [x] PRD 작성
- [x] PRD를 `docs/PRD.md`로 이동
- [x] 기본 브랜치 이름을 `master`에서 `main`으로 변경 (PRD의 CD가 `main` 브랜치 기준이기 때문)
- [x] 진행 현황판(`PROGRESS.md`) 생성
- [x] 첫 커밋 (`docs: PRD와 진행 현황판 추가`)

### 결정 필요 (🙋)
- [x] **Node 버전**: 선택지 B(22 LTS 유지)로 결정. `engines.node >= 22.12`, `.nvmrc = 22`로 고정했고 M7의 Docker/CI도 22로 맞춘다. (decisions.md D-002)
- [ ] (M8 전까지만 정하면 됨) 배포 서버와 도메인, staging/production 분리 방식, GHCR 이미지 공개 여부. PRD 21.2 참고.

### 내가 확인할 것 (🙋)
- [x] 이 파일(`PROGRESS.md`)이 보기 편한지
- [x] `git log --oneline`에 첫 커밋이 보이는지 (c24faa4)

---

## M0. 레포 스캐폴딩과 개발환경

### 작업
- [x] 0-1. 라이브러리 최신 안정 버전 확인 → PRD 6장 표와 `docs/decisions.md`에 기록
- [x] 0-2. 루트 파일 작성: `.gitignore`, `.gitattributes`, `.editorconfig`, `README.md` 초안, `CLAUDE.md`, `docs/conventions.md`
- [x] 0-3. backend 생성: Spring Boot, **Gradle Groovy DSL**, Web/Security/Validation/JPA/Actuator/Flyway, PostgreSQL·MySQL 드라이버, MyBatis
- [x] 0-4. frontend 생성: Vite + React + TypeScript, Tailwind v4, shadcn/ui, ESLint/Prettier
- [x] 0-5. `docker-compose.yml` (`postgresql` / `mysql` 프로필), `.env.example`
- [x] 0-6. 편의 스크립트: `Makefile`, `scripts/*.ps1` (DB 실행, 백엔드 실행, 프론트 실행)
- [x] 0-7. `docs/devlog/00-index.md`, `docs/devlog/M0-scaffolding.md` 작성
- [x] 0-8. 커밋

### 완료 조건 (Claude가 검증)
- [x] `*.kts` 파일이 하나도 없다. (`git ls-files | grep -c "\.kts$"` → 0)
- [x] PostgreSQL 17.11과 MySQL 8.4 컨테이너가 각각 healthy 상태로 기동한다.
- [x] 백엔드가 **두 DB 모두에서** 기동하고 `/actuator/health`가 `UP`이다. (`{"status":"UP"}`)
- [x] Swagger UI가 200으로 응답한다.
- [x] 프론트 `npm run lint / typecheck / build`가 모두 통과한다. (빌드 702ms)

### 내가 확인할 것 (🙋)
```powershell
# 1) DB 띄우기
docker compose --profile postgresql up -d
docker compose ps                      # postgres가 running(healthy)인지

# 2) 백엔드 실행 (새 터미널)
cd backend
.\gradlew.bat bootRun                  # "Started TemplateApplication" 로그 확인

# 3) 프론트 실행 (새 터미널)
cd frontend
npm install
npm run dev
```
- [ ] 브라우저에서 http://localhost:8080/actuator/health 에 `{"status":"UP"}`가 보인다.
- [ ] 브라우저에서 http://localhost:5173 화면이 뜬다.
- [ ] `docs/devlog/M0-scaffolding.md`를 읽고 내용이 이해된다.
- [ ] 확인이 끝나면 `docker compose --profile postgresql down`으로 정리한다.

### 메모
- 2026-09-16: 사용자 시간 부족으로 **수동 확인은 보류**하고 M1로 진행. 완료 조건은 Claude가 자동 검증 완료(두 DB 기동 + health UP + 프론트 빌드). 나중에 확인 후 문제가 있으면 이슈 기록에 적는다.

---

## M1. 백엔드 공통 인프라

### 작업
- [x] 1-1. 패키지 구조: `global/*`, 도메인 계층(`api/application/domain/infra`)
- [x] 1-2. DB 전환: `DB_VENDOR` → `application-db-postgresql.yml` / `application-db-mysql.yml`
- [x] 1-3. Flyway 마이그레이션을 DB 2종 폴더에 작성: `V1__init.sql`, `V2__seed.sql`, `V3__shedlock.sql`
- [x] 1-4. JPA 설정: `validate`, UTC, `open-in-view=false`, Auditing, `BaseTimeEntity`/`BaseEntity`
- [x] 1-5. MyBatis 설정: `DatabaseIdProvider`, camelCase 매핑, mapper 경로, 트랜잭션 공유
- [x] 1-6. 에러 처리: `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`(ProblemDetail), `PageResponse`
- [x] 1-7. traceId 필터, 로깅(local 텍스트 / prod JSON), CORS, Swagger, Actuator
- [x] 1-8. ShedLock, Caffeine 캐시, `StorageService`·`RateLimiter` 인터페이스와 기본 구현
- [x] 1-9. Testcontainers 테스트 베이스(`-PdbVendor`), MyBatis 샘플 쿼리 테스트
- [x] 1-10. ArchUnit 아키텍처 테스트 (ARCH-01~06)
- [x] 1-11. devlog `M1-backend-infra.md` 작성, 커밋

### 완료 조건 (Claude가 검증)
- [x] 두 DB 각각에서 마이그레이션과 JPA `validate`가 통과한다. (PostgreSQL 17 / MySQL 8.4, 각 11개 테스트 통과)
- [x] MyBatis 샘플 테스트가 두 DB에서 통과한다. (`databaseId` 분기 동작 확인)
- [x] 없는 경로로 요청하면 ProblemDetail 형식에 `code`, `traceId`가 포함된 404가 반환된다.
- [x] ArchUnit이 규칙 위반을 잡아낸다. 일부러 `@Autowired` 필드 주입 샘플을 넣어 ARCH-06 실패를 확인한 뒤 삭제했다.

### 내가 확인할 것 (🙋)
```powershell
cd backend
.\gradlew.bat test -PdbVendor=postgresql
.\gradlew.bat test -PdbVendor=mysql
```
- [ ] 두 명령 모두 `BUILD SUCCESSFUL`로 끝난다.
- [ ] 테스트 리포트 `backend\build\reports\tests\test\index.html`을 열어 실패가 0건이다.
- [ ] 백엔드를 실행한 상태에서 http://localhost:8080/swagger-ui.html 이 열린다.
- [ ] http://localhost:8080/api/v1/없는경로 로 접속하면 `code`, `traceId`가 포함된 JSON 에러가 보인다.
- [ ] devlog를 읽고 **JPA와 MyBatis를 언제 쓰는지** 설명할 수 있다.

### 메모
- 2026-09-16: 사용자 시간 부족으로 수동 확인 보류. 자동 테스트(두 DB × 11건)로 검증 완료.
- Spring Boot 4에서 `@AutoConfigureMockMvc` 패키지 경로가 바뀌었고, 없는 경로 요청 시 `NoHandlerFoundException`이 던져진다(devlog 5장 참고).

---

## M2. 인증과 사용자

### 작업
- [x] 2-1. `users`, `refresh_tokens` 엔티티와 Repository
- [x] 2-2. SecurityConfig: stateless, JWT 필터, 401/403 응답을 ProblemDetail로
- [x] 2-3. 회원가입, 로그인, 로그아웃 API
- [x] 2-4. Refresh Token rotation과 재사용 탐지
- [x] 2-5. 내 정보 조회·수정, 비밀번호 변경
- [x] 2-6. 초기 관리자 계정 생성(환경변수)
- [x] 2-7. 로그인 rate limit
- [x] 2-8. 만료 토큰 정리 스케줄러(ShedLock)
- [x] 2-9. 테스트 작성(두 DB), devlog `M2-auth.md`, 커밋

### 완료 조건 (Claude가 검증)
- [x] 폐기된 refresh token을 재사용하면 해당 사용자의 모든 토큰이 폐기된다. (별도 트랜잭션 처리 — 롤백 버그 수정)
- [x] 정지된 사용자의 로그인과 재발급이 거부된다.
- [x] 로그인을 11회 연속 실패하면 429가 반환된다.
- [x] 두 DB에서 테스트가 모두 통과한다. (각 26건)
- [x] 실제 서버 기동으로 관리자 자동 생성, 로그인, 재발급, 재사용 차단을 curl로 확인했다.

### 내가 확인할 것 (🙋)
- [ ] Swagger UI에서 `POST /api/v1/auth/signup`으로 가입한다.
- [ ] `POST /api/v1/auth/login`으로 로그인해 `accessToken`을 받는다.
- [ ] Swagger 우측 상단 **Authorize**에 토큰을 넣고 `GET /api/v1/users/me`를 호출하면 내 정보가 나온다.
- [ ] `.env`의 관리자 이메일/비밀번호로 로그인하면 `role`이 `ADMIN`이다.
- [ ] 두 DB 테스트가 모두 `BUILD SUCCESSFUL`이다.
- [ ] devlog를 읽고 **Access Token과 Refresh Token의 차이**를 설명할 수 있다.

### 메모
- 2026-09-16: 사용자 시간 부족으로 수동 확인 보류. 자동 테스트(두 DB × 26건) + curl 실제 호출로 검증 완료.
- 중요한 버그 수정: 재사용 탐지 시 토큰 폐기가 예외 롤백으로 취소되던 문제 (devlog 5장 문제 3).
- Spring Boot 4는 Jackson 3(`tools.jackson`)을 쓴다. import 주의.
- ArchUnit ARCH-02/ARCH-04 규칙을 PRD 문구에 맞게 정교화했다.

---

## M3. 프론트 기반과 인증 화면

### 작업
- [ ] 3-1. 라우터, 프로바이더, MainLayout, AdminLayout 뼈대
- [ ] 3-2. axios 클라이언트, 에러 변환(`ApiError`), 401 재발급 인터셉터(single-flight)
- [ ] 3-3. `authStore`(Zustand), 앱 부팅 시 세션 복원
- [ ] 3-4. 라우트 가드: `RequireAuth`, `RequireRole`, `GuestOnly`
- [ ] 3-5. `/login`, `/signup`, `/me` 화면
- [ ] 3-6. 다크모드, 토스트, 404 페이지
- [ ] 3-7. Vitest + MSW 테스트, devlog `M3-frontend-base.md`, 커밋

### 완료 조건 (Claude가 검증)
- [ ] `npm run lint`, `typecheck`, `test`, `build`가 모두 통과한다.
- [ ] 여러 요청이 동시에 401을 받아도 재발급은 1번만 일어난다(테스트).

### 내가 확인할 것 (🙋)
- [ ] http://localhost:5173/signup 에서 가입한다.
- [ ] 로그인한 뒤 **새로고침해도 로그인이 유지**된다.
- [ ] 일반 사용자로 http://localhost:5173/admin 에 접근하면 막힌다.
- [ ] 관리자로 로그인하면 `/admin`에 들어가진다(아직 빈 화면이어도 괜찮다).
- [ ] 다크모드 토글이 동작한다.
- [ ] 로그아웃이 된다.

### 메모

---

## M4. CMS 백엔드

### 작업
- [ ] 4-1. 게시판: 엔티티, 권한 판정, 관리자 게시판 API, 사용자 게시판 목록(캐시)
- [ ] 4-2. 게시글 작성·수정·삭제(JPA), HTML sanitize, 조회수 증가
- [ ] 4-3. 게시글 목록·검색·공지 고정(MyBatis, DB 2종)
- [ ] 4-4. 댓글·대댓글(1단계): 쓰기는 JPA, 목록은 MyBatis
- [ ] 4-5. 파일 업로드·다운로드, 형식 검증(매직 넘버), `LocalStorageService`
- [ ] 4-6. 고아 파일 정리 스케줄러(ShedLock)
- [ ] 4-7. 테스트 작성(두 DB), devlog `M4-cms-backend.md`, 커밋

### 완료 조건 (Claude가 검증)
- [ ] 게시판 권한 조합 테스트가 통과한다.
- [ ] 목록·검색 Mapper가 두 DB에서 통과하고, 공지 고정과 숨김/삭제 제외가 동작한다.
- [ ] `<script>` 같은 위험한 HTML이 제거된다.
- [ ] 확장자를 위장한 파일이 거부된다.

### 내가 확인할 것 (🙋)
- [ ] Swagger에서 관리자 토큰으로 게시판을 만든다.
- [ ] 일반 사용자 토큰으로 이미지를 업로드하고, 그 파일 id를 넣어 글을 쓴다.
- [ ] 목록 API에서 검색어로 방금 쓴 글이 조회된다.
- [ ] 댓글과 대댓글을 달고, 대댓글에 또 대댓글을 달면 400 에러가 나는지 본다.
- [ ] 두 DB 테스트가 모두 `BUILD SUCCESSFUL`이다.

### 메모

---

## M5. CMS 사용자 화면과 에디터

### 작업
- [ ] 5-1. 홈: 게시판 목록과 최근 글
- [ ] 5-2. 게시글 목록: 검색, 페이지네이션, 공지 상단
- [ ] 5-3. 게시글 상세: 본문, 첨부 다운로드, 댓글/대댓글
- [ ] 5-4. 글쓰기·수정: TipTap 에디터, 이미지 즉시 업로드, 첨부
- [ ] 5-5. 권한에 따른 버튼 노출, 모바일 레이아웃
- [ ] 5-6. 테스트, devlog `M5-cms-frontend.md`, 커밋

### 완료 조건 (Claude가 검증)
- [ ] 프론트 lint/typecheck/test/build가 통과한다.

### 내가 확인할 것 (🙋)
- [ ] 글쓰기 화면에 **이미지를 복사해 붙여넣으면** 바로 본문에 보인다.
- [ ] 첨부 파일을 올리고 저장하면, 상세 화면에서 이미지와 첨부가 보이고 다운로드된다.
- [ ] 다른 계정으로 댓글을 달고, 원래 계정으로 대댓글을 단다.
- [ ] 목록에서 검색과 페이지 이동이 된다.
- [ ] 브라우저 폭을 줄여(개발자도구 모바일 모드) 화면이 깨지지 않는다.

### 메모

---

## M6. 관리자 페이지

### 작업
- [ ] 6-1. 대시보드 API(MyBatis 집계)와 화면
- [ ] 6-2. 게시판 관리 화면: 생성·수정·삭제, 드래그 정렬
- [ ] 6-3. 게시글/댓글 관리: 필터, 일괄 숨김·삭제
- [ ] 6-4. 사용자 관리: 역할 변경, 정지(자기 자신은 금지)
- [ ] 6-5. 테스트(두 DB), devlog `M6-admin.md`, 커밋

### 완료 조건 (Claude가 검증)
- [ ] 관리자 자신의 역할 변경과 정지가 거부된다.
- [ ] 정지된 사용자는 재발급이 불가하다.
- [ ] 관리자 조회 Mapper가 두 DB에서 통과한다.

### 내가 확인할 것 (🙋)
- [ ] `/admin/boards`에서 새 게시판을 만들면 사용자 화면 메뉴에 나타난다.
- [ ] `/admin/posts`에서 글을 숨기면 사용자 화면에서 사라진다.
- [ ] `/admin/users`에서 사용자를 정지하면 그 사용자는 로그인할 수 없다.
- [ ] 대시보드 숫자가 실제 데이터와 맞는다.

### 메모

---

## M7. 품질 도구, Docker, CI

### 작업
- [ ] 7-1. Spotless 전체 적용, JaCoCo 리포트
- [ ] 7-2. Playwright 스모크 테스트
- [ ] 7-3. backend/frontend Dockerfile(멀티스테이지)
- [ ] 7-4. compose `full` 프로필(전체 실행, `--scale backend=2` 지원)
- [ ] 7-5. `.github/workflows/ci.yml` (DB 매트릭스 2종), Dependabot
- [ ] 7-6. README 완성: 실행법, 새 도메인 추가 절차
- [ ] 7-7. devlog `M7-quality-ci.md`, 커밋

### 사전 준비 (🙋 이 단계 전에 필요)
- [ ] GitHub에 레포를 만들고 원격 저장소로 연결한다. 비공개 레포를 권장한다.

### 완료 조건 (Claude가 검증)
- [ ] 전체 compose가 PostgreSQL과 MySQL 각각으로 동작한다.
- [ ] 백엔드 2개 인스턴스에서 스케줄러가 한 번만 실행된다.

### 내가 확인할 것 (🙋)
```powershell
docker compose --profile full --profile postgresql up -d --build
```
- [ ] http://localhost:8081 에서 전체 앱이 동작한다. 로그인, 글쓰기, 관리자 화면까지 확인한다.
- [ ] `.env`에서 `DB_VENDOR=mysql`로 바꾸고 `--profile mysql`로 다시 띄워도 똑같이 동작한다.
- [ ] GitHub에 PR을 올리면 Actions 탭에서 CI가 모두 초록색이다.
- [ ] README만 보고 실행할 수 있을 정도로 설명되어 있다.

### 메모

---

## M8. CD(자동 배포)와 운영 문서

### 작업
- [ ] 8-1. `.github/workflows/cd.yml`: GHCR 푸시 → staging 자동 배포 → production 승인 배포
- [ ] 8-2. `deploy/docker-compose.prod.yml`, `Caddyfile`(자동 HTTPS)
- [ ] 8-3. `deploy/deploy.sh`: 헬스체크 실패 시 자동 롤백
- [ ] 8-4. `deploy/server-setup.sh`, `deploy/backup.sh`, `.env.prod.example`
- [ ] 8-5. GitHub Secrets / Environments 설정 가이드, 롤백 절차 문서
- [ ] 8-6. devlog `M8-cd.md`, 커밋

### 사전 준비 (🙋 이 단계 전에 필요)
- [ ] 리눅스 서버 준비 (Ubuntu 24.04, 최소 2 vCPU / 4GB 권장)
- [ ] 도메인 준비, 서버 IP로 DNS 연결
- [ ] staging과 production을 서버 1대로 할지 2대로 할지 결정
- [ ] GitHub 레포 Settings에서 Environments(`staging`, `production`)와 Secrets 등록 (가이드는 이 단계에서 제공)

### 완료 조건 (Claude가 검증)
- [ ] 워크플로와 스크립트가 문법 검사(`actionlint`, `shellcheck`)를 통과한다.
- [ ] 서버가 없으면 실제 배포 검증은 🙋로 남긴다.

### 내가 확인할 것 (🙋)
- [ ] main에 머지하면 Actions에서 이미지 빌드 → staging 배포가 자동으로 진행된다.
- [ ] `https://staging.내도메인/api/actuator/health` 가 `UP`이다.
- [ ] production 배포는 승인 버튼을 눌러야 진행된다.
- [ ] Actions에서 **Run workflow**로 이전 태그를 넣으면 롤백된다.
- [ ] 서버에서 `backup.sh`를 실행하면 백업 파일이 생긴다.

### 메모

---

## 이슈 기록

| # | 날짜 | 단계 | 내용 | 상태 | 해결 방법 |
|---|---|---|---|---|---|
| | | | | | |

## 결정 기록 요약
자세한 내용은 `docs/decisions.md`(M0에서 생성)에 기록한다.

| 날짜 | 결정 | 이유 |
|---|---|---|
| 2026-09-15 | 기본 브랜치를 `main`으로 사용 | PRD의 CD 트리거가 `main` 기준 |
| 2026-09-15 | PRD 위치를 `docs/PRD.md`로 확정 | 문서를 `docs/` 아래에 모으기 위해 |
