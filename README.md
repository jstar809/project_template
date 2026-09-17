# fullstack-template

Spring Boot(Java 21) + React(Vite) 모노레포 템플릿. 인증, 게시판 CMS, 관리자 페이지, 테스트, CI/CD가 미리 구현된 상태에서 새 프로젝트를 시작하기 위한 프로젝트다.

- **사용 가이드: [docs/usage.md](docs/usage.md)** ← 처음이라면 여기부터
- 배포 가이드: [docs/deployment.md](docs/deployment.md)
- 요구사항 문서: [docs/PRD.md](docs/PRD.md)
- 진행 현황판: [PROGRESS.md](PROGRESS.md)
- 결정 기록: [docs/decisions.md](docs/decisions.md)
- 제작 과정 튜토리얼: [docs/devlog/](docs/devlog/)

> 현재 진행 단계: **M7 완료** (품질 도구, Docker, CI). 다음은 M8(자동 배포).

## 들어 있는 기능

| 영역 | 내용 |
|---|---|
| 인증 | 회원가입, 로그인, JWT(Access 15분 + Refresh 14일), 토큰 회전·재사용 탐지, 로그인 시도 제한 |
| 게시판 CMS | 게시판 동적 생성(권한·댓글·첨부 설정), 글 작성·검색·공지 고정, 1단계 대댓글, 파일 첨부 |
| 에디터 | TipTap 리치 텍스트, 이미지 붙여넣기 즉시 업로드 |
| 관리자 | 대시보드, 게시판·게시글·댓글·사용자 관리, 일괄 숨김·삭제 |
| 보안 | HTML 정제(서버+클라이언트), 파일 매직 넘버 검증, 권한 기반 접근 제어 |
| 운영 | DB 2종 지원, 수평 확장(stateless + 분산 락), Docker, GitHub Actions CI |

## 기술 스택

| 영역 | 스택 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4.1, Gradle(Groovy DSL), JPA + MyBatis, Flyway |
| DB | PostgreSQL 17 / MySQL 8.4 (설정 하나로 전환) |
| 프론트 | React 19, Vite 8, TypeScript 6, Tailwind 4, shadcn/ui |
| 테스트 | JUnit 5 + Testcontainers + ArchUnit / Vitest + Testing Library + MSW / Playwright |
| 인프라 | Docker Compose, GitHub Actions |

## 사전 준비

| 도구 | 버전 |
|---|---|
| JDK | 21 |
| Node.js | 22 LTS (>= 22.12) |
| Docker Desktop | 최신 |

## 실행 방법

### 1) 개발용 (코드를 고치며 작업할 때)

```powershell
copy .env.example .env

.\scripts\db-up.ps1              # DB만 도커로 실행 (MySQL이면 -Db mysql)
.\scripts\be-run.ps1             # 백엔드 → http://localhost:8080
.\scripts\fe-run.ps1             # 프론트 → http://localhost:5173
```

macOS / Linux는 `make db-up DB=postgresql`, `make be-run`, `make fe-run`.

### 2) 전체 도커 실행 (배포와 같은 형태로 확인할 때)

```bash
cp .env.example .env
echo "ADMIN_EMAIL=admin@example.com" >> .env
echo "ADMIN_PASSWORD=admin1234" >> .env

docker compose --profile full --profile postgresql up -d --build
# → http://localhost:8081
```

MySQL로 돌리려면 `.env`에서 `DB_VENDOR=mysql`, `DB_INTERNAL_PORT=3306`으로 바꾸고 `--profile mysql`을 쓴다.

백엔드를 여러 대로 늘려볼 수도 있다.

```bash
docker compose --profile full --profile postgresql up -d --scale backend=2
```

> 인스턴스가 늘어도 로그인·업로드가 정상 동작하고, 스케줄러는 분산 락(ShedLock) 덕분에 한 인스턴스에서만 실행된다.

### 확인용 주소

| 주소 | 설명 |
|---|---|
| http://localhost:5173 | 프론트엔드 (개발 모드) |
| http://localhost:8081 | 전체 도커 실행 |
| http://localhost:8080/actuator/health | 백엔드 상태 |
| http://localhost:8080/swagger-ui.html | API 문서 |

### 포트가 이미 사용 중일 때
`.env`에서 포트를 바꾼다. 예를 들어 로컬에 MySQL(XAMPP 등)이 3306을 쓰고 있으면 `MYSQL_PORT=3307`. 백엔드는 `SERVER_PORT`, 도커 전체 실행은 `FRONTEND_PORT`로 바꾼다.

## 테스트

| 목적 | Windows | macOS/Linux |
|---|---|---|
| 백엔드 테스트(기본 DB) | `.\scripts\be-test.ps1` | `make be-test` |
| 백엔드 테스트(두 DB 모두) | `.\scripts\be-test.ps1 -All` | `make be-test-all` |
| 프론트 검사(lint+타입+빌드) | `.\scripts\fe-check.ps1` | `make fe-check` |
| 프론트 단위·통합 테스트 | `cd frontend && npm run test` | 동일 |
| E2E 스모크 테스트 | `cd frontend && npm run e2e` | 동일 |

- 백엔드 테스트는 Testcontainers로 실제 DB를 띄우므로 Docker가 실행 중이어야 한다.
- E2E는 **전체 도커 실행이 떠 있는 상태**에서 돌린다(기본 대상 http://localhost:8081, `E2E_BASE_URL`로 변경 가능).

## CI

`.github/workflows/ci.yml`이 PR과 main push에서 자동 실행된다.

| Job | 내용 |
|---|---|
| backend | PostgreSQL·MySQL **매트릭스**로 `spotlessCheck test build` |
| frontend | lint, 포맷 검사, 타입 검사, 단위 테스트, 빌드, 취약점 점검 |
| docker-build | 두 이미지 빌드 확인 |
| e2e | (main 한정) compose로 전체 실행 후 Playwright 스모크 테스트 |

경로 필터가 있어 바뀐 영역만 실행된다. Dependabot이 주간으로 의존성 PR을 만든다.

## 프로젝트 구조

```text
backend/    Spring Boot (Gradle Groovy DSL)
  src/main/java/com/example/template/
    global/    공통 설정, 보안, 에러, 로깅, 저장소
    domain/    도메인별 api / application / domain / infra
  src/main/resources/
    db/migration/postgresql, db/migration/mysql   Flyway (DB별 같은 버전 번호)
    mapper/                                       MyBatis XML
frontend/   React + Vite
  src/app        라우터, 레이아웃, 프로바이더
  src/features   기능(도메인)별 화면과 API
  src/shared     공통 API 클라이언트, UI, 스토어
  e2e/           Playwright 스모크 테스트
docs/       PRD, 결정 기록, 개발 로그
scripts/    Windows용 실행 스크립트
```

## 데이터 접근 규칙 (중요)

| 상황 | 사용 |
|---|---|
| 생성·수정·삭제, ID 단건 조회 | **JPA** |
| 목록, 검색, 페이징, 통계, 관리자 조회 | **MyBatis** |

자세한 기준은 [PRD 8.2](docs/PRD.md#82-jpa와-mybatis-역할-분담-가벼운-cqrs)를 참고한다.

## 새 도메인 추가 절차

1. `domain/{name}/domain`: Entity, Repository 인터페이스, ErrorCode enum
2. `db/migration/postgresql`과 `db/migration/mysql`에 **같은 버전 번호로** 마이그레이션 작성
3. `infra/jpa`: Repository 구현, `infra/mybatis` + `mapper/{name}/*.xml`: 조회 Mapper
4. `application`: Service(트랜잭션 경계), Command/Query DTO
5. `api`: Controller, Request/Response DTO
6. 테스트: 단위 + 통합(두 DB) + Controller
7. 프론트: `src/features/{name}/` 추가, 라우트 등록
8. `.\scripts\be-test.ps1 -All`과 `.\scripts\fe-check.ps1` 통과 확인

아키텍처 규칙(계층 의존 방향, Entity 노출 금지, 생성자 주입 등)은 ArchUnit 테스트가 자동으로 검사한다.

## 프로젝트 이름 바꾸기

1. `backend/settings.gradle`의 `rootProject.name`
2. 패키지 `com.example.template` → 원하는 패키지 (IDE의 rename refactoring 사용)
3. `backend/build.gradle`의 `group`
4. `docker-compose.yml`의 `name: template`
5. `frontend/package.json`의 `name`, `index.html`의 `<title>`

## 브랜치 보호 설정(권장)

GitHub 레포 Settings → Branches → main 규칙 추가:
- Require a pull request before merging
- Require status checks: `backend (postgresql)`, `backend (mysql)`, `frontend`
