# 작업 인수인계 (AI 에이전트용)

> 이 문서는 **다른 환경에서 이 프로젝트를 이어받는 AI 에이전트**를 위한 것이다.
> 사람이 읽어도 되지만, 대상은 에이전트다. 먼저 이 문서를 끝까지 읽고 시작한다.
>
> 최종 갱신: 2026-09-17 · 기준 커밋: `2945094` · 레포: `jstar809/project_template`

---

## 0. 30초 요약

- **무엇**: Spring Boot(Java 21) + React(Vite) 모노레포 **템플릿**. 인증, 게시판 CMS, 관리자 페이지, 테스트, CI/CD가 이미 구현되어 있다.
- **어디까지**: 계획(M0)부터 자동 배포 설정(M8)까지 **코드 작업은 전부 끝났다.**
- **남은 것**: 코드가 아니라 **검증과 운영 준비**다. (CI 실제 실행 확인, 브라우저 눈 확인, 서버 준비 후 배포 검증)
- **다음에 할 일**: 아래 [4. 남은 작업](#4-남은-작업) 의 P0부터.

---

## 1. 먼저 읽을 문서 (순서대로)

| 순서 | 문서 | 왜 |
|---|---|---|
| 1 | **이 문서(HANDOFF.md)** | 현재 상태와 남은 일 |
| 2 | [CLAUDE.md](CLAUDE.md) | **반드시 지켜야 할 작업 규칙** (Kotlin 금지 등) |
| 3 | [PROGRESS.md](PROGRESS.md) | 단계별 진행 상태와 체크리스트 |
| 4 | [docs/PRD.md](docs/PRD.md) | 요구사항 원본. 판단이 갈릴 때의 기준 |
| 5 | [docs/decisions.md](docs/decisions.md) | 기술 선택 이유 25건. **같은 고민을 반복하지 않도록** |
| 6 | [docs/usage.md](docs/usage.md) | 기능 사용법, 새 도메인 추가 절차 |
| 7 | [docs/deployment.md](docs/deployment.md) | 배포 절차 |
| 8 | [docs/devlog/](docs/devlog/) | M0~M8 제작 과정. 특정 단계의 맥락이 필요할 때만 |

> 전부 읽을 필요는 없다. 1~3을 읽고, 작업 중 판단이 필요할 때 4~5를 찾아본다.

---

## 2. 현재 상태

### 2-1. 완료된 단계

| 단계 | 내용 | 커밋 |
|---|---|---|
| M0 | 모노레포 스캐폴딩, 개발환경 | `61312e5` |
| M1 | 백엔드 공통 인프라 (JPA+MyBatis, DB 2종, ArchUnit, 에러 규약) | `ea3ab22` |
| M2 | JWT 인증, 사용자 도메인 | `2312f2e` |
| M3 | 프론트 기반, 인증 화면 | `b8856ff` |
| M4 | CMS 백엔드 (게시판·글·댓글·파일) | `4127e0a` |
| M5 | CMS 화면, TipTap 에디터 | `70a4c40` |
| M6 | 관리자 페이지 | `4512e09` |
| M7 | Docker, CI, E2E | `8fe386b` |
| M8 | CD(자동 배포), 운영 문서 | `1f5c624` |

### 2-2. 검증된 사실 (직접 실행해 확인한 것)

- 백엔드 테스트: **PostgreSQL / MySQL 양쪽에서 전체 통과** (통합·단위·ArchUnit)
- 프론트 테스트: **31건 통과**, lint·typecheck·build 통과
- E2E: **실제 브라우저(Chromium)에서 2건 통과** — 관리자 로그인 → 게시판 생성 → 글 작성 → 댓글
- Docker 전체 실행: `http://localhost:8081` 정상 동작
- 수평 확장(`--scale backend=2`): 요청 분산 6:4, 업로드 볼륨 공유, **ShedLock 락 1건**(스케줄러 중복 실행 없음)
- 배포 스크립트: shellcheck 경고 0 / 워크플로: actionlint 통과 / 운영 compose: 두 DB 프로필 모두 정상 해석

### 2-3. 아직 검증되지 않은 것 (중요)

| 항목 | 이유 |
|---|---|
| GitHub Actions CI 실제 실행 | push는 했으나 결과를 확인하지 못함 |
| 브라우저 화면 눈 확인 | 자동 테스트와 E2E로 대체함. UI 디테일은 미확인 |
| 실제 서버 배포, 자동 롤백, 백업 | 서버가 없어 미검증 |

---

## 3. 환경 재구성 (새 환경에서 처음 할 일)

### 3-1. 필요한 도구

| 도구 | 버전 | 확인 |
|---|---|---|
| JDK | 21 | `java -version` |
| Node.js | 22 LTS (>= 22.12) | `node -v` |
| Docker | 최신, **실행 중이어야 함** | `docker ps` |
| Git | - | `git --version` |

> Node 24가 아니라 **22**다. 이유는 decisions.md D-002. 임의로 올리지 말 것.

### 3-2. 클론과 실행

```bash
git clone https://github.com/jstar809/project_template.git
cd project_template

cp .env.example .env
# .env 에 아래 두 줄이 있는지 확인(없으면 추가). 최초 관리자 계정이 만들어진다.
#   ADMIN_EMAIL=admin@example.com
#   ADMIN_PASSWORD=admin1234

docker compose --profile postgresql up -d          # DB
cd backend && ./gradlew bootRun                    # 백엔드 (별도 터미널)
cd frontend && npm install && npm run dev          # 프론트 (별도 터미널)
```

### 3-3. 이어받은 직후 반드시 실행할 검증

**작업을 시작하기 전에** 현재 코드가 정상인지 먼저 확인한다. 여기서 실패하면 환경 문제이지 코드 문제가 아닐 가능성이 높다.

```bash
# 백엔드 (Docker 필요 — Testcontainers가 실제 DB를 띄운다)
cd backend
./gradlew test -PdbVendor=postgresql
./gradlew test -PdbVendor=mysql

# 프론트
cd ../frontend
npm ci
npm run lint && npm run typecheck && npm run test && npm run build
```

**기대 결과**: 백엔드 두 번 모두 `BUILD SUCCESSFUL`, 프론트 테스트 31건 통과.

### 3-4. 환경 차이로 자주 겪는 문제

이전 환경(Windows)에서 실제로 겪은 것들이다.

| 증상 | 원인 | 해결 |
|---|---|---|
| `Port 8080 already in use` | 다른 앱 점유 | `.env`의 `SERVER_PORT` 변경 |
| `Bind for 0.0.0.0:3306 failed` | 로컬 MySQL(XAMPP 등) | `.env`의 `MYSQL_PORT=3307` |
| 셸 스크립트 실행 실패 (`bad interpreter`) | CRLF 줄바꿈 | `deploy/*.sh`는 **LF로 저장**해야 한다 (`.gitattributes`에 규칙 있음) |
| 테스트가 Docker를 못 찾음 | Docker 미실행 | Docker Desktop 실행 |
| 브라우저에서만 403 | CORS | `.env`의 `CORS_ALLOWED_ORIGINS`에 접속 주소 추가 |

---

## 4. 남은 작업

우선순위 순이다. **P0부터 진행한다.**

### P0-1. GitHub Actions CI 실행 확인

- **상태**: 워크플로 파일은 올라가 있고 actionlint를 통과했다. 실제 실행 결과는 미확인.
- **할 일**
  1. GitHub 레포 → Actions 탭에서 최근 실행 확인
  2. 실패한 job이 있으면 로그를 읽고 수정 → 커밋 → push
  3. 특히 확인할 것: `backend (postgresql)`, `backend (mysql)` 두 매트릭스가 모두 통과하는지
- **예상 실패 지점**
  - Testcontainers가 CI 러너에서 DB를 띄우는 데 시간이 오래 걸릴 수 있다 → 타임아웃 조정
  - `npm run format:check` 가 줄바꿈 차이로 실패할 수 있다 → `.gitattributes` 확인
- **완료 조건**: main 브랜치 push 시 모든 job이 녹색.

### P0-2. 브랜치 보호 규칙 설정 (사람이 해야 함)

- GitHub Settings → Branches → main
  - Require a pull request before merging
  - Require status checks: `backend (postgresql)`, `backend (mysql)`, `frontend`
- **에이전트는 이 설정을 대신할 수 없다.** 사용자에게 안내만 한다.

### P1-1. 브라우저 화면 확인

- **상태**: 자동 테스트와 E2E는 통과하지만, UI를 눈으로 본 적이 없다.
- **할 일**: 전체 도커 실행 후 아래를 확인하고, 어긋나는 부분을 고친다.

```bash
docker compose --profile full --profile postgresql up -d --build
# http://localhost:8081
```

| 확인 항목 | 기대 |
|---|---|
| 회원가입 → 로그인 → **새로고침** | 로그인 유지, 화면 깜빡임 없음 |
| 글쓰기에서 **이미지 붙여넣기(Ctrl+V)** | 즉시 업로드되어 본문에 표시 |
| 첨부 파일 추가 → 저장 → 상세 | 다운로드 동작 |
| 댓글·대댓글 | 들여쓰기된 트리로 표시 |
| 일반 계정으로 `/admin` 접근 | 차단(403 화면) |
| 관리자 → 게시판 순서 변경 | 사용자 화면 헤더 메뉴 순서도 바뀜 |
| 다크 모드 토글 | 색상 전환, 새로고침해도 유지 |
| 모바일 폭(400px) | 레이아웃 깨짐 없음, 표는 가로 스크롤 |

- **완료 조건**: 위 항목 전부 정상. `PROGRESS.md`의 해당 체크박스를 채운다.

### P1-2. 서버 준비 후 배포 검증 (사용자 협조 필요)

- **선행 조건(사용자)**: Ubuntu 24.04 서버, 도메인, DNS A 레코드
- **할 일**: [docs/deployment.md](docs/deployment.md) 순서대로.
  1. `deploy/server-setup.sh` 실행
  2. GitHub Environments(`staging`, `production`) + Secrets 등록
  3. main push → staging 자동 배포 확인
  4. production 승인 배포 확인
  5. **자동 롤백 검증**: `.env.prod`의 `JWT_SECRET`을 일부러 짧게(32바이트 미만) 바꾸고 배포 → 헬스체크 실패 → 이전 태그로 자동 복구되는지 확인 → 원복
  6. `./backup.sh` 실행 → 백업 파일 생성 확인 → **복구도 한 번 해볼 것**
- **완료 조건**: 5번(자동 롤백)이 실제로 동작하는 것을 눈으로 확인. 이건 **일부러 실패시켜 봐야만** 믿을 수 있다.

### P2. 알려진 제약과 개선 후보

급하지 않다. 필요해지면 착수한다. 각 항목의 배경은 괄호 안 문서에 있다.

| # | 내용 | 배경 |
|---|---|---|
| 1 | `eslint-plugin-jsx-a11y` 추가 (ESLint 10 지원 시) — 현재 접근성 린트 없음 | decisions D-005 |
| 2 | TypeScript 7 전환 (typescript-eslint가 지원하면) | decisions D-003 |
| 3 | 목록 API 성능 측정 (PRD 목표: 1만 건에서 p95 200ms). 더미 데이터 생성 스크립트 필요 | devlog M4 회고 |
| 4 | 로그인 rate limit이 인스턴스별 인메모리 → 서버 N대면 한도 N배. Redis 구현으로 교체 | PRD SCALE-05 |
| 5 | 역할 변경·정지가 최대 15분 지연(Access Token 수명). 즉시 차단하려면 블랙리스트 필요 | decisions D-013 |
| 6 | 관리자 행위 감사 로그(누가 무엇을 숨겼는지) 없음 | devlog M6 회고 |
| 7 | 검색이 `LIKE '%키워드%'` → 데이터가 많아지면 느림. 전문 검색 도입 검토 | PRD DB-08 |
| 8 | 무중단 배포 아님(컨테이너 교체 중 수 초 끊김) | devlog M8 회고 |
| 9 | 모니터링·알림 없음 (Actuator prometheus 엔드포인트는 열 수 있음) | PRD 21.1 |
| 10 | 백엔드 이미지 경량화(distroless, jlink) | devlog M7 회고 |
| 11 | 관리자 게시글 관리에 게시판별 필터 UI 미구현(API는 있음) | devlog M6 회고 |

---

## 5. 작업 규칙 (반드시 지킬 것)

[CLAUDE.md](CLAUDE.md)의 요약이다. 위반하면 테스트나 리뷰에서 걸린다.

### 금지

- **Kotlin 사용 금지.** `*.kt`, `build.gradle.kts` 를 만들지 않는다. Gradle은 Groovy DSL, 코드는 Java. 문서 예시도 마찬가지. (사용자가 Kotlin을 읽지 못한다)
- **비밀값 커밋 금지.** `.env`, `.env.prod`, 키, 토큰. 예시 파일(`*.example`)만 커밋한다.
- 비목표 기능(소셜 로그인, 다국어, 결제, SSR, K8s)을 임의로 추가하지 않는다.
- 사용자의 다른 프로세스를 죽이지 않는다. 포트 충돌은 `.env`에서 해결한다.

### 필수

- **백엔드 변경 시 두 DB에서 모두 테스트**: `./gradlew test -PdbVendor=postgresql` **그리고** `-PdbVendor=mysql`
- **마이그레이션은 쌍으로**: `db/migration/postgresql/` 와 `db/migration/mysql/` 에 **같은 버전 번호**로 함께 작성. 이미 적용된 파일은 수정하지 않는다.
- **아키텍처 규칙**: 의존 방향 `api → application → domain ← infra`, Controller는 Entity를 주고받지 않음, `@Transactional`은 application 계층에만, 다른 도메인은 그 도메인의 Service를 통해서만, 생성자 주입만. → ArchUnit이 자동 검사한다.
- **데이터 접근 분담**: 쓰기·단건 조회는 JPA, 목록·검색·통계는 MyBatis.
- **PRD와 다른 결정을 했으면** `docs/decisions.md`에 기록한다 (배경 / 결정 / 이유).
- **의미 있는 작업 단위마다** `docs/devlog/`에 기록하고 `PROGRESS.md`를 갱신한다.
- 커밋 메시지는 Conventional Commits: `feat(m9): ...`, `fix: ...`, `docs: ...`

### 이 프로젝트의 문화

- **"작성했다"로 끝내지 않는다.** 실제로 실행해 확인하고, 확인할 수 없으면 "미검증"으로 명시한다.
- 실패한 테스트를 느슨하게 바꿔 통과시키지 않는다. 원인을 고친다.
- 린트 경고를 우회(`disable`)하기 전에, 그 경고가 가리키는 구조 문제를 먼저 본다.

---

## 6. 주요 파일 위치

```text
HANDOFF.md              ← 이 문서
CLAUDE.md               에이전트 작업 규칙
PROGRESS.md             단계별 진행 현황판
README.md               프로젝트 개요
docker-compose.yml      개발/전체 실행
.env.example            환경변수 목록

backend/
  build.gradle                         의존성·버전
  src/main/java/com/example/template/
    global/config/SecurityConfig.java  인가 규칙 (경로별 권한)
    global/error/                      에러 코드 체계
    global/security/                   JWT 발급·검증
    domain/{user,auth,board,post,comment,file,admin}/
  src/main/resources/
    application.yml                    공통 설정
    application-db-{postgresql,mysql}.yml
    db/migration/{postgresql,mysql}/   Flyway (쌍으로 관리)
    mapper/                            MyBatis XML
  src/test/java/.../architecture/ArchitectureTest.java   아키텍처 규칙
  src/test/java/.../support/IntegrationTestSupport.java  통합 테스트 베이스

frontend/
  src/app/router.tsx                   라우트 정의
  src/shared/api/client.ts             axios + 401 자동 재발급(single-flight)
  src/shared/store/authStore.ts        인증 상태
  src/features/{auth,user,board,post,comment,file,admin}/
  e2e/smoke.spec.ts                    Playwright

deploy/                                운영 배포 파일 (LF 유지 필수)
.github/workflows/{ci,cd}.yml
.github/actions/deploy/action.yml      배포 공통 액션
docs/                                  PRD, usage, deployment, decisions, devlog
```

---

## 7. 이어서 작업할 때의 흐름

```text
1. 이 문서 + CLAUDE.md + PROGRESS.md 읽기
2. 환경 재구성 후 [3-3]의 검증 명령을 돌려 현재 상태가 정상인지 확인
3. [4. 남은 작업]에서 항목 하나를 고름 (P0 우선)
4. 작업 → 테스트(백엔드는 두 DB) → devlog/PROGRESS 갱신 → 커밋 → push
5. 검증할 수 없는 항목은 "사용자 확인 필요"로 명시하고 넘어간다
```

새 기능을 추가하는 경우에는 [docs/usage.md 3부](docs/usage.md#3부-개발자-가이드)의 7단계 절차를 따른다.
