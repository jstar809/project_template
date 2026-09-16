# fullstack-template

Spring Boot(Java 21) + React(Vite) 모노레포 템플릿. 인증, 게시판 CMS, 관리자 페이지, 테스트, CI/CD가 미리 구현된 상태에서 새 프로젝트를 시작하기 위한 프로젝트다.

- 요구사항 문서: [docs/PRD.md](docs/PRD.md)
- 진행 현황판: [PROGRESS.md](PROGRESS.md)
- 결정 기록: [docs/decisions.md](docs/decisions.md)
- 제작 과정 튜토리얼: [docs/devlog/](docs/devlog/)

> 현재 진행 단계: **M0 완료** (스캐폴딩과 개발환경)

## 기술 스택

| 영역 | 스택 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4.1, Gradle(Groovy DSL), JPA + MyBatis, Flyway |
| DB | PostgreSQL 17 / MySQL 8.4 (설정 하나로 전환) |
| 프론트 | React 19, Vite 8, TypeScript 6, Tailwind 4, shadcn/ui |
| 인프라 | Docker Compose, GitHub Actions (M7~M8) |

## 사전 준비

| 도구 | 버전 |
|---|---|
| JDK | 21 |
| Node.js | 22 LTS (>= 22.12) |
| Docker Desktop | 최신 |

## 빠르게 실행하기

```powershell
# 1) 환경 변수 파일 준비
copy .env.example .env

# 2) DB 실행 (PostgreSQL)
.\scripts\db-up.ps1
#   MySQL로 하려면:  .\scripts\db-up.ps1 -Db mysql

# 3) 백엔드 실행 → http://localhost:8080
.\scripts\be-run.ps1
#   MySQL로 하려면:  .\scripts\be-run.ps1 -Db mysql

# 4) 프론트 실행 → http://localhost:5173
.\scripts\fe-run.ps1
```

macOS / Linux는 같은 기능을 `make`로 실행한다.

```bash
cp .env.example .env
make db-up DB=postgresql
make be-run
make fe-run
```

확인용 주소

| 주소 | 설명 |
|---|---|
| http://localhost:5173 | 프론트엔드 |
| http://localhost:8080/actuator/health | 백엔드 상태 |
| http://localhost:8080/swagger-ui.html | API 문서 |

### 포트가 이미 사용 중일 때
`.env`에서 포트를 바꾼다. 예를 들어 로컬에 MySQL(XAMPP 등)이 이미 3306을 쓰고 있으면 `MYSQL_PORT=3307`, `DB_PORT=3307`로 바꾼다. 백엔드 포트는 `SERVER_PORT`로 바꾼다.

## 자주 쓰는 명령

| 목적 | Windows | macOS/Linux |
|---|---|---|
| DB 실행 | `.\scripts\db-up.ps1 -Db postgresql` | `make db-up DB=postgresql` |
| DB 종료 | `.\scripts\db-down.ps1` | `make db-down` |
| 백엔드 실행 | `.\scripts\be-run.ps1` | `make be-run` |
| 백엔드 테스트 | `.\scripts\be-test.ps1 -All` | `make be-test-all` |
| 프론트 실행 | `.\scripts\fe-run.ps1` | `make fe-run` |
| 프론트 검사 | `.\scripts\fe-check.ps1` | `make fe-check` |
| 포맷 정리 | `cd backend && .\gradlew.bat spotlessApply` | `make lint` |

## 프로젝트 구조

```text
backend/    Spring Boot (Gradle Groovy DSL)
  src/main/java/com/example/template/
    global/    공통 설정, 보안, 에러, 로깅, 저장소
    domain/    도메인별 api / application / domain / infra
  src/main/resources/
    db/migration/postgresql, db/migration/mysql   Flyway (DB별로 같은 버전 번호)
    mapper/                                       MyBatis XML
frontend/   React + Vite
  src/app        라우터, 레이아웃, 프로바이더
  src/features   기능(도메인)별 화면과 API
  src/shared     공통 API 클라이언트, UI, 훅
  src/components/ui  shadcn/ui 생성 컴포넌트
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
M1 이후 각 계층이 갖춰지면 이 절차대로 추가한다.

1. `domain/{name}/domain`: Entity, Repository 인터페이스, ErrorCode enum
2. `db/migration/postgresql`과 `db/migration/mysql`에 **같은 버전 번호로** 마이그레이션 작성
3. `infra/jpa`: Repository 구현, `infra/mybatis` + `mapper/{name}/*.xml`: 조회 Mapper
4. `application`: Service(트랜잭션 경계), Command/Query DTO
5. `api`: Controller, Request/Response DTO
6. 테스트: 단위 + 통합(두 DB) + Controller
7. 프론트: `src/features/{name}/` 추가, 라우트 등록
8. `.\scripts\be-test.ps1 -All`과 `.\scripts\fe-check.ps1` 통과 확인
