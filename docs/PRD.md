# PRD: 범용 풀스택 템플릿 (Spring Boot + React)

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.0 |
| 작성일 | 2026-09-15 |
| 상태 | 확정 대기 (Draft) |
| 대상 독자 | 템플릿을 구현할 AI 에이전트(Claude Code 등)와 개발자, 템플릿을 사용할 개발자 |
| 가칭 | `fullstack-template` (패키지 `com.example.template`, 이름 변경 가능) |

> **읽는 방법**
> - **Part A(기획)**: 무엇을 왜 만드는지 설명한다. 사람이 먼저 읽는다.
> - **Part B(구현 명세)**: 에이전트가 그대로 실행할 수 있게 구조, 규칙, 순서, 완료 조건을 적었다.
> - 문서에서 **"반드시(MUST)"** 는 꼭 지켜야 하는 요구사항이고, **"권장(SHOULD)"** 은 따르는 것이 좋은 사항이다.

---

## 목차

- **Part A. 기획**
  1. [개요](#1-개요)
  2. [목표와 비목표](#2-목표와-비목표)
  3. [사용자와 역할](#3-사용자와-역할)
  4. [핵심 사용자 시나리오](#4-핵심-사용자-시나리오)
  5. [성공 기준 요약](#5-성공-기준-요약)
- **Part B. 구현 명세**
  6. [기술 스택과 버전](#6-기술-스택과-버전)
  7. [레포지토리 구조](#7-레포지토리-구조)
  8. [아키텍처 원칙: 확장성과 유지보수성 (FR-ARCH)](#8-아키텍처-원칙-확장성과-유지보수성-fr-arch)
  9. [기능 요구사항](#9-기능-요구사항)
  10. [비기능 요구사항](#10-비기능-요구사항)
  11. [데이터 모델](#11-데이터-모델)
  12. [API 명세](#12-api-명세)
  13. [프론트엔드 라우트와 화면](#13-프론트엔드-라우트와-화면)
  14. [개발 환경과 실행](#14-개발-환경과-실행)
  15. [품질 도구 (FR-QUALITY)](#15-품질-도구-fr-quality)
  16. [CI/CD 파이프라인 (FR-CICD)](#16-cicd-파이프라인-fr-cicd)
  17. [개발 로그: 학습용 튜토리얼 (FR-DEVLOG)](#17-개발-로그-학습용-튜토리얼-fr-devlog)
  18. [구현 마일스톤과 완료 조건](#18-구현-마일스톤과-완료-조건)
  19. [최종 수용 기준 (E2E 체크리스트)](#19-최종-수용-기준-e2e-체크리스트)
  20. [에이전트 작업 규칙](#20-에이전트-작업-규칙)
  21. [향후 확장과 오픈 이슈](#21-향후-확장과-오픈-이슈)
  22. [부록](#22-부록)

---

# Part A. 기획

## 1. 개요

### 1.1 한 줄 요약
새 서비스를 시작할 때마다 반복하던 기본 설정을 미리 구현해 둔 **모노레포 템플릿**이다. `git clone` 한 번이면 인증, 게시판 CMS, 관리자 페이지, 테스트, CI/CD가 모두 동작하는 상태에서 시작할 수 있다.

### 1.2 배경
- 프로젝트마다 JWT 인증, DB 마이그레이션, 에러 처리, 로깅, CORS, Docker, CI를 매번 새로 만들고 있다. 이 과정에서 시간이 들고 설정 품질도 프로젝트마다 들쭉날쭉하다.
- 거의 모든 서비스에 공지사항, 자유게시판 같은 게시판과 이를 관리하는 관리자 화면이 필요하다.
- 회사나 고객 환경에 따라 DB가 PostgreSQL일 때도 MySQL일 때도 있다. 단순 CRUD는 JPA가 편하지만, 복잡한 조회는 SQL을 직접 쓰는 MyBatis가 유지보수에 유리하다.
- 템플릿 자체가 **"확장하기 쉽고 유지보수하기 좋은 구조의 기준점"** 이 되어야 한다. 만드는 과정도 기록해 두면 학습 자료로 쓸 수 있다.

### 1.3 결과물
1. 동작하는 모노레포 소스(backend, frontend, deploy, CI/CD 설정)
2. 실행과 확장 방법을 담은 문서(`README.md`, `CLAUDE.md`, `docs/decisions.md`)
3. 제작 과정을 튜토리얼 형태로 남긴 개발 로그(`docs/devlog/`)

## 2. 목표와 비목표

### 2.1 목표
| # | 목표 |
|---|---|
| G1 | `git clone` 후 `docker compose --profile full up` 한 번으로 인증과 CMS, 관리자 페이지가 동작한다. |
| G2 | 서버를 여러 대로 늘려도(수평 확장) 문제없이 동작하고, 계층 규칙이 자동으로 검증되어 유지보수하기 쉬운 코드 구조를 갖춘다. |
| G3 | JPA와 MyBatis를 역할을 나눠 함께 사용하고, 설정값 하나로 PostgreSQL과 MySQL 사이를 전환한다. |
| G4 | 새 도메인(기능)을 추가하는 절차가 문서화되어 있고, 기존 도메인을 복사하듯 쉽게 추가할 수 있다. |
| G5 | 백엔드/프론트엔드 테스트, 린트, 아키텍처 검증이 CI에서 자동으로 실행된다. |
| G6 | main 브랜치에 머지하면 staging 서버에 자동 배포되고, 승인하면 production에 배포된다. 실패하면 자동으로 롤백한다. |
| G7 | 제작 과정이 단계별 튜토리얼 로그로 남아, 읽으면서 따라 만들 수 있다. |

### 2.2 비목표 (이번 범위에서 하지 않는 것)
- 마이크로서비스(MSA) 분리, 메시지 브로커
- 소셜 로그인(OAuth2), 이메일 인증, 비밀번호 찾기 메일 발송
- 다국어(i18n), 결제, 알림(푸시, 이메일)
- SSR(Next.js 등). 프론트는 SPA로만 만든다.
- Kubernetes, AWS ECS 배포. 배포는 단일 서버 + Docker Compose로만 한다.
- **Kotlin 사용.** Gradle 빌드 스크립트를 포함해 Kotlin은 어떤 형태로도 쓰지 않는다.

> 비목표 중 일부는 [21. 향후 확장](#21-향후-확장과-오픈-이슈)에 후보로 적어 두었다.

## 3. 사용자와 역할

| 사용자 | 설명 | 주요 행동 |
|---|---|---|
| 템플릿 사용 개발자 | 이 템플릿으로 새 프로젝트를 시작하는 개발자 | clone, 이름 변경, 도메인 추가, 배포 설정 |
| 비로그인 방문자 (ANONYMOUS) | 로그인하지 않은 사용자 | 공개 게시판 읽기, 회원가입, 로그인 |
| 일반 사용자 (USER) | 가입한 사용자 | 글/댓글 작성·수정·삭제(본인 것), 파일 첨부, 내 정보 수정 |
| 관리자 (ADMIN) | 운영자 | 게시판 생성·설정, 모든 글/댓글 숨김·삭제, 사용자 역할 변경·정지, 대시보드 조회 |

## 4. 핵심 사용자 시나리오

1. **관리자 초기 설정**
   1. 서버가 처음 뜰 때 환경변수로 지정한 관리자 계정이 자동 생성된다.
   2. 관리자가 로그인해 `/admin/boards`에서 "자유게시판"(slug `free`, 읽기 ANONYMOUS, 쓰기 USER, 댓글 허용, 첨부 허용)을 만든다.
2. **사용자 글쓰기**
   1. 방문자가 `/signup`에서 가입하고 로그인한다.
   2. `/boards/free/write`에서 에디터에 이미지를 붙여넣는다. 이미지가 즉시 업로드되어 본문에 보인다.
   3. 파일을 하나 첨부하고 글을 등록한다.
3. **댓글**
   1. 다른 사용자가 글 상세 화면에서 댓글을 단다.
   2. 작성자가 그 댓글에 대댓글(1단계)을 단다.
4. **검색과 탐색**
   1. 누구나 목록에서 제목+내용으로 검색하고 페이지를 넘긴다.
   2. 공지로 지정한 글은 목록 맨 위에 고정된다.
5. **운영**
   1. 관리자가 `/admin/posts`에서 문제 글을 "숨김" 처리한다. 사용자 화면의 목록과 상세에서 그 글이 사라진다.
   2. 관리자가 문제 사용자를 "정지"한다. 정지된 사용자는 로그인과 토큰 재발급이 거부된다.
6. **개발자 확장**
   1. 개발자가 README의 "새 도메인 추가 절차"대로 `notice`, `faq` 같은 새 도메인을 추가한다.
   2. CI의 ArchUnit 테스트가 계층 규칙을 어긴 코드를 잡아낸다.
7. **배포**
   1. 개발자가 PR을 머지하면 이미지가 빌드되어 staging에 자동 배포된다.
   2. 승인 버튼을 누르면 production에 배포된다.

## 5. 성공 기준 요약

- [ ] 시나리오 1~5가 PostgreSQL과 MySQL **각각**에서 동일하게 동작한다.
- [ ] CI(백엔드 DB 2종 매트릭스, 프론트, ArchUnit)가 모두 통과한다.
- [ ] 백엔드 인스턴스 2개로 띄워도 로그인, 업로드, 스케줄러가 정상 동작한다.
- [ ] main 머지 → staging 자동 배포 → 승인 → production 배포 흐름과 실패 시 롤백이 동작한다.
- [ ] `docs/devlog/`에 M0~M8 튜토리얼 로그가 템플릿대로 채워져 있다.

자세한 체크리스트는 [19. 최종 수용 기준](#19-최종-수용-기준-e2e-체크리스트)에 있다.

---

# Part B. 구현 명세

## 6. 기술 스택과 버전

> **규칙(MUST)**
> - 구현 시작(M0) 시 각 라이브러리의 **실제 최신 안정 버전을 확인**하고, 아래 표의 "확정 버전" 열을 채워 `docs/decisions.md`에도 기록한다. 버전을 추측해서 쓰지 않는다.
> - Spring Boot 4.x와 MyBatis Spring Boot Starter, springdoc-openapi, ShedLock의 호환 버전을 반드시 확인한다. 호환되지 않으면 Spring Boot 3.5.x로 내리고, 그 이유를 기록한다.

### 6.1 백엔드
| 구분 | 기술 | 기준 | 확정 버전 |
|---|---|---|---|
| 언어 | Java (Temurin) | **JDK 21** | |
| 프레임워크 | Spring Boot | 최신 안정판 (4.x 우선, 불가 시 3.5.x) | |
| 빌드 | Gradle | 최신 안정판, **Groovy DSL** (`build.gradle`) | |
| ORM | Spring Data JPA (Hibernate) | Boot 관리 버전 | |
| SQL 매퍼 | MyBatis Spring Boot Starter | Boot 버전과 호환되는 최신 | |
| 마이그레이션 | Flyway + `flyway-database-postgresql`, `flyway-mysql` | Boot 관리 버전 | |
| DB | PostgreSQL | 17 | |
| DB | MySQL | 8.4 LTS | |
| 보안 | Spring Security, JJWT(또는 Nimbus JOSE) | 최신 | |
| 분산 락 | ShedLock (JDBC provider) | 최신 | |
| 캐시 | Spring Cache + Caffeine | 최신 | |
| Rate limit | Bucket4j | 최신 | |
| HTML 정제 | OWASP Java HTML Sanitizer | 최신 | |
| API 문서 | springdoc-openapi | Boot 호환 버전 | |
| 로깅 | Logback + logstash-logback-encoder(prod JSON) | 최신 | |
| 테스트 | JUnit 5, AssertJ, Testcontainers, ArchUnit, JaCoCo | 최신 | |
| 포매터 | Spotless (palantir-java-format 또는 google-java-format) | 최신 | |

### 6.2 프론트엔드
| 구분 | 기술 | 기준 | 확정 버전 |
|---|---|---|---|
| 런타임 | Node.js | 24 LTS | |
| 패키지 매니저 | npm | Node 번들 버전 | |
| 빌드 | Vite | 최신 안정판 | |
| UI | React + TypeScript | React 19, TS 최신 | |
| 스타일 | Tailwind CSS + shadcn/ui | Tailwind v4 | |
| 라우팅 | React Router | 최신 (Data/Library 모드 중 Library 모드) | |
| 서버 상태 | TanStack Query | v5 이상 | |
| 클라이언트 상태 | Zustand | 최신 | |
| 폼 | React Hook Form + Zod | 최신 | |
| HTTP | axios | 최신 | |
| 에디터 | TipTap | 최신 | |
| 테스트 | Vitest, Testing Library, MSW, Playwright | 최신 | |
| 린트 | ESLint(flat config), Prettier | 최신 | |

### 6.3 인프라
| 구분 | 기술 |
|---|---|
| 컨테이너 | Docker, Docker Compose v2 |
| CI/CD | GitHub Actions |
| 이미지 저장소 | GHCR (GitHub Container Registry) |
| 리버스 프록시 / HTTPS | Caddy(권장, 자동 HTTPS). nginx + certbot을 고르면 이유를 기록한다. |
| 서버 | 단일 리눅스 서버 (Ubuntu 24.04 LTS 기준) |

## 7. 레포지토리 구조

```text
fullstack-template/
├── backend/
│   ├── build.gradle                  # Groovy DSL (Kotlin DSL 금지)
│   ├── settings.gradle
│   ├── gradlew, gradlew.bat, gradle/
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/example/template/
│       │   ├── TemplateApplication.java
│       │   ├── global/
│       │   │   ├── config/           # Web, Jpa, MyBatis, Cache, Scheduling(ShedLock), OpenAPI
│       │   │   ├── security/         # SecurityConfig, JwtProvider, JwtFilter, 권한 평가
│       │   │   ├── error/            # ErrorCode 인터페이스, BusinessException, GlobalExceptionHandler
│       │   │   ├── response/         # PageResponse 등 공통 응답
│       │   │   ├── logging/          # TraceIdFilter, 요청 로깅
│       │   │   ├── storage/          # StorageService 인터페이스, LocalStorageService
│       │   │   ├── ratelimit/        # RateLimiter 인터페이스, InMemoryRateLimiter
│       │   │   └── common/           # BaseTimeEntity, BaseEntity, 유틸
│       │   └── domain/
│       │       ├── auth/  user/  board/  post/  comment/  file/  admin/
│       │       └── (도메인마다 아래 구조)
│       │           ├── api/          # Controller, Request/Response DTO(record)
│       │           ├── application/  # Service(트랜잭션 경계), Command/Query DTO
│       │           ├── domain/       # Entity, 도메인 규칙, Repository 인터페이스, ErrorCode enum
│       │           └── infra/
│       │               ├── jpa/      # Spring Data JPA Repository 구현
│       │               └── mybatis/  # MyBatis Mapper 인터페이스
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-local.yml / application-test.yml / application-prod.yml
│       │   ├── application-db-postgresql.yml / application-db-mysql.yml
│       │   ├── mapper/{domain}/*.xml             # MyBatis XML
│       │   ├── db/migration/postgresql/V*.sql
│       │   ├── db/migration/mysql/V*.sql
│       │   └── logback-spring.xml
│       └── test/java/com/example/template/
│           ├── architecture/         # ArchUnit 테스트
│           ├── support/              # Testcontainers 기반 통합 테스트 베이스
│           └── domain/...            # 도메인별 단위/통합 테스트
├── frontend/
│   ├── package.json, vite.config.ts, tsconfig*.json, eslint.config.js, components.json
│   ├── Dockerfile, nginx.conf
│   ├── e2e/                          # Playwright
│   └── src/
│       ├── app/                      # main.tsx, router.tsx, providers.tsx, layouts/
│       ├── features/
│       │   ├── auth/  board/  post/  comment/  file/  admin/
│       │   └── (feature마다) api.ts, hooks.ts, types.ts, components/, pages/
│       ├── shared/
│       │   ├── api/                  # axios 클라이언트, 인터셉터, 에러 변환
│       │   ├── ui/                   # 공통 조합 컴포넌트 (DataTable, Pagination, ConfirmDialog 등)
│       │   ├── hooks/  lib/          # 유틸, 상수, zod 공통 스키마
│       │   └── store/                # Zustand (authStore, uiStore)
│       ├── components/ui/            # shadcn/ui 생성 컴포넌트
│       └── test/                     # MSW 핸들러, 테스트 유틸
├── deploy/
│   ├── docker-compose.prod.yml
│   ├── Caddyfile
│   ├── deploy.sh                     # pull → up → healthcheck → 실패 시 롤백
│   ├── server-setup.sh               # 서버 최초 준비
│   ├── backup.sh                     # DB_VENDOR에 따라 pg_dump / mysqldump
│   └── .env.prod.example
├── docs/
│   ├── devlog/
│   │   ├── 00-index.md
│   │   └── M0-scaffolding.md … M8-cd.md
│   ├── decisions.md                  # 결정 기록 (ADR 요약)
│   └── conventions.md                # 코딩 컨벤션
├── .github/workflows/
│   ├── ci.yml
│   └── cd.yml
├── docker-compose.yml                # 로컬 개발용
├── .env.example
├── Makefile                          # 자주 쓰는 명령 모음 (Windows 사용자를 위해 scripts/*.ps1도 제공)
├── scripts/
├── .editorconfig, .gitignore, .gitattributes
├── README.md
└── CLAUDE.md                         # 에이전트용 작업 규칙 요약
```

## 8. 아키텍처 원칙: 확장성과 유지보수성 (FR-ARCH)

> 이 장의 규칙은 모두 **반드시(MUST)** 지킨다. 예외가 필요하면 `docs/decisions.md`에 이유를 기록한다.

### 8.1 계층과 의존 방향

```text
api (Controller, Request/Response DTO)
  │
  ▼
application (Service, @Transactional, Command/Query DTO)
  │
  ▼
domain (Entity, 도메인 규칙, Repository 인터페이스, ErrorCode)
  ▲
  │ 구현
infra (jpa/, mybatis/)
```

| ID | 규칙 |
|---|---|
| ARCH-01 | 의존은 `api → application → domain ← infra` 방향으로만 흐른다. `domain`은 `api`, `application`, `infra`에 의존하지 않는다. |
| ARCH-02 | Controller는 Entity를 반환하거나 받지 않는다. 항상 Request/Response DTO(record)로 변환한다. |
| ARCH-03 | `@Transactional`은 `application` 계층에만 붙인다. 조회 전용 메서드는 `readOnly = true`로 한다. |
| ARCH-04 | 다른 도메인이 필요하면 **상대 도메인의 application 서비스를 호출하거나 ID로만 참조**한다. 다른 도메인의 Repository나 Mapper를 직접 주입하지 않는다. (`admin` 도메인은 조회용 MyBatis Mapper를 자체적으로 가질 수 있다.) |
| ARCH-05 | 패키지 사이에 순환 의존이 없어야 한다. |
| ARCH-06 | 의존성은 생성자로만 주입한다(`@RequiredArgsConstructor` 또는 명시적 생성자). 필드 주입은 금지한다. |
| ARCH-07 | 외부 자원(파일 저장, 레이트리밋, 캐시, 시간)은 인터페이스 뒤에 둔다. 예: `StorageService`, `RateLimiter`, `Clock` 주입. |

**자동 검증 (MUST)**: `src/test/java/.../architecture/ArchitectureTest.java`에서 ArchUnit으로 아래를 검사하고, CI에서 실패하면 빌드를 막는다.
- ARCH-01 계층 의존 규칙 (`layeredArchitecture()`)
- ARCH-02 `..api..` 패키지의 public 메서드가 `@Entity` 타입을 반환하거나 파라미터로 받지 않음
- ARCH-03 `@Transactional`이 `..application..` 밖에 없음 (infra의 Spring Data 기본 구현은 제외)
- ARCH-04 도메인 A의 클래스가 도메인 B의 `..infra..`나 Repository에 의존하지 않음 (admin 예외 규칙 포함)
- ARCH-05 `slices().matching("..domain.(*)..").should().beFreeOfCycles()`
- ARCH-06 `@Autowired` 필드 주입 금지

### 8.2 JPA와 MyBatis 역할 분담 (가벼운 CQRS)

| 상황 | 사용 기술 | 이유 |
|---|---|---|
| 생성, 수정, 삭제(soft delete 포함) | **JPA** | 변경 감지, 연관관계, Auditing, 낙관적 락을 활용할 수 있다. |
| ID로 단건 조회 후 수정 | **JPA** | 같은 트랜잭션에서 엔티티 상태를 바로 바꿀 수 있다. |
| 권한 확인용 단건 조회 (예: 게시글 작성자 확인) | **JPA** | 엔티티 도메인 규칙 메서드를 재사용할 수 있다. |
| 조건이 붙은 목록, 검색, 페이징 | **MyBatis** | 동적 SQL이 명확하고, 조회 DTO로 바로 매핑해 N+1이 생기지 않는다. |
| 여러 테이블 조인, 집계, 통계(대시보드) | **MyBatis** | SQL을 직접 튜닝할 수 있다. |
| 벤더별로 SQL이 다른 조회 | **MyBatis** (`databaseId` 분기) | 차이를 XML 한 곳에 모을 수 있다. |
| 단순 원자적 증감 (조회수 +1 등) | **JPA** `@Modifying` JPQL | 두 DB에서 똑같이 동작하고 동시성에 안전하다. |

규칙:
- **MB-01** 두 기술은 같은 `DataSource`와 같은 트랜잭션 매니저(`JpaTransactionManager`)를 공유한다. MyBatis는 `SpringManagedTransactionFactory`로 JPA 트랜잭션에 참여한다.
- **MB-02** 같은 트랜잭션에서 JPA로 변경한 데이터를 MyBatis로 다시 조회해야 하면, 조회 전에 `entityManager.flush()`를 호출한다. 이 경우를 가능한 한 만들지 않는다.
- **MB-03** MyBatis 결과는 `application` 계층의 조회 전용 DTO(record)로 받는다. MyBatis로 Entity를 조회하지 않는다.
- **MB-04** Mapper XML은 `resources/mapper/{domain}/{Domain}QueryMapper.xml`에 두고, Mapper 인터페이스 이름은 `{Domain}QueryMapper`로 한다.
- **MB-05** `map-underscore-to-camel-case: true`로 설정한다. `${}` 문자열 치환은 정렬 컬럼 화이트리스트 외에는 금지하고 `#{}`만 쓴다(SQL Injection 방지).

### 8.3 DB 이식성 (PostgreSQL / MySQL)

**전환 방식**
- 환경변수 `DB_VENDOR=postgresql|mysql` 값에 따라 `application-db-${DB_VENDOR}.yml` 프로필을 활성화한다.
  - 예: `spring.profiles.active=local,db-postgresql`. 또는 `application.yml`에서 `spring.profiles.group`으로 `DB_VENDOR`를 참조한다.
- 벤더 프로필에서 정하는 항목: JDBC URL/드라이버, `spring.flyway.locations=classpath:db/migration/{vendor}`, MyBatis `databaseId`, Hibernate 설정

**규칙**
| ID | 규칙 |
|---|---|
| DB-01 | Flyway 마이그레이션은 `db/migration/postgresql/`과 `db/migration/mysql/`에 **같은 버전 번호로 1:1** 작성한다. 한쪽만 추가하는 것은 금지한다. |
| DB-02 | JPA는 `spring.jpa.hibernate.ddl-auto=validate`를 쓴다. 스키마는 오직 Flyway로만 바꾼다. |
| DB-03 | ID는 `BIGINT` + `@GeneratedValue(strategy = IDENTITY)`로 한다. |
| DB-04 | 엔티티에 벤더 전용 `columnDefinition`을 쓰지 않는다. 긴 텍스트는 `@JdbcTypeCode(SqlTypes.LONG32VARCHAR)`로 매핑하고, 두 DB에서 `validate`가 통과하는지 M1 통합 테스트로 확인한다. |
| DB-05 | 모든 시간은 **UTC**로 저장한다. 엔티티는 `Instant`를 쓰고, `hibernate.jdbc.time_zone=UTC`, MySQL JDBC URL은 `connectionTimeZone=UTC`로 설정한다. |
| DB-06 | MySQL은 `utf8mb4` / `utf8mb4_0900_ai_ci`, PostgreSQL은 `UTF8`로 설정한다. |
| DB-07 | enum은 `VARCHAR`로 저장한다(`@Enumerated(EnumType.STRING)`). DB 전용 enum 타입은 쓰지 않는다. |
| DB-08 | 검색은 두 DB에서 모두 동작하는 `LIKE` 기반으로 한다. 대소문자 무시는 `LOWER(col) LIKE LOWER(#{kw})`로 통일한다. 전문 검색은 향후 확장으로 둔다. |
| DB-09 | MyBatis는 `VendorDatabaseIdProvider`를 등록한다(`PostgreSQL → postgresql`, `MySQL → mysql`). 페이징은 두 DB 공통인 `LIMIT #{size} OFFSET #{offset}`을 기본으로 쓰고, upsert나 날짜 함수처럼 다른 부분만 `databaseId`로 분기한다. |
| DB-10 | 예약어와 겹치는 테이블/컬럼명(`user`, `order`, `comment` 등 단수형)을 피한다. 테이블명은 복수형(`users`, `comments`)으로 한다. |

**타입 매핑 표**
| 논리 타입 | Java | PostgreSQL | MySQL |
|---|---|---|---|
| ID | `Long` | `BIGINT GENERATED BY DEFAULT AS IDENTITY` | `BIGINT AUTO_INCREMENT` |
| 짧은 문자열 | `String` | `VARCHAR(n)` | `VARCHAR(n)` |
| 긴 텍스트 (본문 HTML) | `String` + `LONG32VARCHAR` | `TEXT` | `LONGTEXT` |
| 불리언 | `boolean` | `BOOLEAN` | `TINYINT(1)` (`BOOLEAN`) |
| 시각 | `Instant` | `TIMESTAMP(6) WITH TIME ZONE` | `DATETIME(6)` (UTC 값) |
| 정수 카운트 | `long` | `BIGINT` | `BIGINT` |
| enum | `enum` | `VARCHAR(20)` | `VARCHAR(20)` |

### 8.4 수평 확장(여러 인스턴스) 대비

| ID | 요구사항 |
|---|---|
| SCALE-01 | 백엔드는 **stateless**로 만든다. 인증은 JWT로 하고, 서버 세션은 쓰지 않는다(`SessionCreationPolicy.STATELESS`). |
| SCALE-02 | 파일은 `StorageService` 인터페이스를 거쳐 저장한다. 기본 구현 `LocalStorageService`는 여러 인스턴스가 **같은 볼륨**을 마운트한다는 전제로 동작한다. `S3StorageService`로 교체할 수 있게 설계한다(구현은 향후 확장). |
| SCALE-03 | 스케줄러 작업(고아 파일 정리, 만료 refresh token 정리)은 **ShedLock(JDBC)** 으로 여러 인스턴스 중 하나에서만 실행한다. |
| SCALE-04 | 캐시는 Spring Cache 추상화를 쓴다. 기본은 Caffeine(로컬)이고, 캐시 대상은 "게시판 목록/설정"처럼 변경이 드문 데이터로 한정한다. Redis로 바꿔도 코드 변경이 없어야 한다. |
| SCALE-05 | 로그인 rate limit은 `RateLimiter` 인터페이스로 분리한다. 기본은 인스턴스별 인메모리(Bucket4j)이고, 이 한계(인스턴스마다 따로 셈)를 문서에 명시한다. |
| SCALE-06 | 모든 설정은 환경변수로 외부화한다. 비밀값은 코드와 yml에 기본값을 두지 않는다(local 프로필 제외). |
| SCALE-07 | HikariCP `maximum-pool-size`는 기본 10으로 하고, 환경변수 `DB_POOL_SIZE`로 조정한다. "인스턴스 수 × 풀 크기 < DB 최대 연결 수" 공식을 문서화한다. |
| SCALE-08 | `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=20s`로 설정한다. |
| SCALE-09 | 목록 API는 페이징이 필수다. 기본 `size=20`, 최대 `size=100`이고, 초과하면 100으로 제한한다. |
| SCALE-10 | 관리자 계정 시드처럼 기동 시 실행되는 로직은 여러 인스턴스가 동시에 떠도 안전해야 한다(유니크 제약 + 중복 예외 무시). |
| SCALE-11 | 조회수 증가처럼 동시에 쓰는 카운트는 `UPDATE ... SET x = x + 1`로 원자적으로 처리한다. |

### 8.5 유지보수 규칙

- **기능(도메인) 단위 패키지**: 기술 단위(`controller/`, `service/` 최상위 폴더)로 나누지 않는다.
- **에러 코드**: 공통 `ErrorCode` 인터페이스(`getStatus()`, `getCode()`, `getMessage()`)를 두고, 도메인별 enum으로 구현한다.
  - 예: `PostErrorCode.POST_NOT_FOUND`, `AuthErrorCode.INVALID_CREDENTIALS`
- **DTO는 record**로 만들고, Bean Validation 어노테이션으로 입력을 검증한다.
- **매직 넘버와 문자열 금지**: 설정(`@ConfigurationProperties` + record)이나 상수로 뺀다.
- **Entity 규칙**
  - setter를 열지 않는다. 의미 있는 메서드(`post.hide()`, `post.update(title, content)`)로만 상태를 바꾼다.
  - 기본 생성자는 `protected`로 둔다.
- **컨벤션 문서**: `docs/conventions.md`에 네이밍, 패키지, 테스트, 커밋 메시지(Conventional Commits) 규칙을 적는다.
- **새 도메인 추가 절차 (README에 체크리스트로 제공)**
  1. `domain/{name}/domain`: Entity, Repository 인터페이스, ErrorCode enum
  2. `db/migration/postgresql`, `db/migration/mysql`: 같은 버전 번호로 마이그레이션 2개
  3. `infra/jpa`: Repository, `infra/mybatis` + `mapper/{name}/*.xml`: 조회 Mapper
  4. `application`: Service, Command/Query DTO
  5. `api`: Controller, Request/Response DTO, OpenAPI 어노테이션
  6. 테스트: 단위 테스트, Repository/Mapper 통합 테스트(DB 2종), Controller 테스트
  7. 프론트: `src/features/{name}/` 아래 api.ts, hooks.ts, types.ts, pages, 라우트 등록
  8. `./gradlew test`(두 DB)와 `npm run test`가 통과하는지 확인

### 8.6 프론트엔드 확장성

- **FE-01** `src/features/{feature}` 단위로 코드를 모은다. feature끼리 내부 파일을 직접 import하지 않고, 필요하면 `shared`로 올린다.
- **FE-02** HTTP 호출은 `shared/api/client.ts`의 axios 인스턴스만 쓴다. 서버 에러(ProblemDetail)는 `ApiError` 타입으로 변환해 한 곳에서 처리한다.
- **FE-03** 서버 데이터는 **TanStack Query**, UI와 인증 상태는 **Zustand**로 역할을 나눈다. 서버 데이터를 Zustand에 복사하지 않는다.
- **FE-04** Query key는 feature별 `queryKeys` 팩토리로 관리한다. 예: `postKeys.list(slug, params)`
- **FE-05** 라우트 페이지는 `React.lazy`로 코드 분할한다. 특히 `/admin` 이하는 별도 청크로 나눈다.
- **FE-06** 폼은 React Hook Form + Zod 스키마로 검증한다. 규칙(길이 등)은 백엔드 Bean Validation과 같게 맞춘다.
- **FE-07** 환경값은 `import.meta.env.VITE_*`로만 읽고, `shared/lib/env.ts`에서 Zod로 검증한다.

## 9. 기능 요구사항

> 표기: **[JPA]** 쓰기·단건, **[MB]** MyBatis 조회

### 9.1 FR-AUTH 인증

| ID | 요구사항 | 담당 |
|---|---|---|
| FR-AUTH-01 | 회원가입: email(유니크, 형식 검증), password(8~64자, 영문+숫자 포함), nickname(2~20자, 유니크). 가입하면 역할 USER, 상태 ACTIVE로 저장한다. | [JPA] |
| FR-AUTH-02 | 로그인: 이메일과 비밀번호를 검증한다. 성공하면 Access Token을 응답 바디로, Refresh Token을 쿠키로 발급하고 `last_login_at`을 갱신한다. SUSPENDED 계정은 거부한다(`AUTH_ACCOUNT_SUSPENDED`). | [JPA] |
| FR-AUTH-03 | Access Token: JWT(HS256), 유효기간 15분(`JWT_ACCESS_TTL`). 클레임은 `sub`(userId), `role`, `iat`, `exp`. 프론트는 **메모리(Zustand)에만** 보관한다. | – |
| FR-AUTH-04 | Refresh Token: 불투명 랜덤 문자열(256bit 이상). 유효기간 14일(`JWT_REFRESH_TTL`). DB에는 **SHA-256 해시만** 저장한다. 쿠키 설정은 `HttpOnly`, `Secure`(prod), `SameSite=Strict`, `Path=/api/v1/auth`. | [JPA] |
| FR-AUTH-05 | 토큰 재발급(rotation): 재발급할 때마다 기존 refresh token을 폐기(`revoked_at`)하고 새 토큰을 발급한다. **이미 폐기된 토큰으로 요청이 오면(재사용 탐지)** 해당 사용자의 모든 refresh token을 폐기하고 401을 반환한다. 재발급 시 사용자가 SUSPENDED이면 거부한다. | [JPA] |
| FR-AUTH-06 | 로그아웃: 현재 refresh token을 폐기하고 쿠키를 삭제한다. | [JPA] |
| FR-AUTH-07 | 비밀번호는 BCrypt(strength 10 이상)로 해시한다. | – |
| FR-AUTH-08 | 초기 관리자: 기동 시 `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NICKNAME` 환경변수가 있고 해당 이메일 계정이 없으면 ADMIN 계정을 만든다(`AdminAccountInitializer`, SCALE-10 준수). | [JPA] |
| FR-AUTH-09 | 로그인 rate limit: 같은 IP+이메일 기준 5분에 10회를 넘으면 429(`AUTH_TOO_MANY_ATTEMPTS`)를 반환한다. | – |
| FR-AUTH-10 | 프론트 앱을 새로고침하면 앱 부팅 시 `/auth/refresh`를 호출해 세션을 복원한다. | – |

### 9.2 FR-USER 사용자

| ID | 요구사항 | 담당 |
|---|---|---|
| FR-USER-01 | 역할(Role)은 `USER`, `ADMIN` 두 가지다. | – |
| FR-USER-02 | 상태(Status)는 `ACTIVE`, `SUSPENDED` 두 가지다. | – |
| FR-USER-03 | 내 정보 조회: id, email, nickname, role, createdAt | [JPA] |
| FR-USER-04 | 내 정보 수정: nickname 변경(중복 검사) | [JPA] |
| FR-USER-05 | 비밀번호 변경: 현재 비밀번호를 확인한 뒤 변경한다. 변경하면 해당 사용자의 모든 refresh token을 폐기한다. | [JPA] |

### 9.3 FR-BOARD 게시판

| ID | 요구사항 | 담당 |
|---|---|---|
| FR-BOARD-01 | 관리자가 게시판을 생성한다. 설정 항목은 `name`(1~50자), `slug`(소문자, 숫자, 하이픈 2~30자, 유니크), `description`, `readPermission`(ANONYMOUS/USER/ADMIN), `writePermission`(USER/ADMIN), `commentEnabled`, `attachmentEnabled`, `active`, `sortOrder`. | [JPA] |
| FR-BOARD-02 | 관리자가 게시판을 수정한다. slug 변경도 허용한다(중복 검사). | [JPA] |
| FR-BOARD-03 | 관리자가 게시판을 삭제한다. 게시글이 있으면 409(`BOARD_HAS_POSTS`)를 반환하고 비활성화를 안내한다. 게시글이 없으면 soft delete한다. | [JPA] |
| FR-BOARD-04 | 관리자가 게시판 정렬 순서를 한 번에 변경한다(id 배열). | [JPA] |
| FR-BOARD-05 | 사용자용 게시판 목록: `active=true`이고 현재 사용자가 읽을 수 있는 게시판만 `sortOrder` 순으로 반환한다. 결과를 캐시하고, 게시판을 변경하면 캐시를 무효화한다. | [MB] |
| FR-BOARD-06 | 권한 판정 규칙: ANONYMOUS < USER < ADMIN. 사용자의 레벨이 요구 레벨 이상이면 허용한다. 비활성 게시판은 ADMIN만 접근할 수 있다. | – |
| FR-BOARD-07 | 초기 데이터: `V2__seed.sql`로 "공지사항"(`notice`, 읽기 ANONYMOUS, 쓰기 ADMIN, 댓글 비허용)과 "자유게시판"(`free`, 읽기 ANONYMOUS, 쓰기 USER)을 만든다. | – |

### 9.4 FR-POST 게시글

| ID | 요구사항 | 담당 |
|---|---|---|
| FR-POST-01 | 목록: 게시판 slug 기준. 공지(`is_notice=true`)는 **페이지와 관계없이 첫 페이지 상단에** 따로 반환한다(`notices` 배열). 일반 글은 `created_at DESC` 페이징. `status=PUBLISHED`이고 삭제되지 않은 글만 보인다. 각 항목에 작성자 닉네임, 댓글 수, 조회수, 첨부 여부를 포함한다. | [MB] |
| FR-POST-02 | 검색: `searchType` = `title` / `content` / `title_content` / `author`, `keyword`는 2~50자. 내용 검색은 태그를 제거한 `content_text` 컬럼을 대상으로 한다. | [MB] |
| FR-POST-03 | 상세: 게시판 읽기 권한을 확인하고 조회수를 원자적으로 +1한다(SCALE-11). 작성자 정보, 첨부 파일 목록, `canEdit`/`canDelete` 플래그를 포함한다. HIDDEN/삭제된 글은 일반 사용자에게 404로 응답한다(ADMIN은 조회 가능). | [JPA] + [MB](첨부 목록) |
| FR-POST-04 | 작성: 게시판 쓰기 권한을 확인한다. `title` 1~200자, `content` HTML 최대 100KB. `isNotice`는 ADMIN만 true로 지정할 수 있다. `fileIds`는 첨부 허용 게시판에서만 받는다. 본인이 업로드한 TEMP 파일만 연결할 수 있고, 연결하면 LINKED로 바꾼다. | [JPA] |
| FR-POST-05 | 본문 정제: 저장 전에 **OWASP Java HTML Sanitizer**로 허용 태그만 남긴다. 허용 목록: `p, br, strong, em, u, s, h1~h3, ul, ol, li, blockquote, code, pre, a[href](http/https만, rel=noopener 강제), img[src](자체 파일 URL만)`. 태그를 제거한 텍스트는 `content_text`에 저장한다. | – |
| FR-POST-06 | 수정: 작성자 본인 또는 ADMIN만 가능하다. 제목, 본문, 공지 여부(ADMIN), 첨부 목록을 교체한다. 빠진 첨부는 연결을 해제하고 TEMP로 되돌려 정리 대상이 되게 한다. | [JPA] |
| FR-POST-07 | 삭제: 작성자 본인 또는 ADMIN. soft delete(`deleted_at`) 방식이다. | [JPA] |
| FR-POST-08 | 댓글 수(`comment_count`)는 댓글을 작성하거나 삭제할 때 원자적으로 증감한다. | [JPA] |

### 9.5 FR-COMMENT 댓글

| ID | 요구사항 | 담당 |
|---|---|---|
| FR-COMMENT-01 | 목록: 게시글 기준. 최상위 댓글은 `created_at ASC` 페이징(size 50)이고, 각 댓글에 대댓글 전체를 포함한다(1단계). 삭제된 댓글에 대댓글이 있으면 "삭제된 댓글입니다"로 자리를 남긴다. | [MB] |
| FR-COMMENT-02 | 작성: 로그인 사용자만 가능하고, 게시판 읽기 권한이 필요하다. `commentEnabled=false`이면 403(`COMMENT_DISABLED`). `content` 1~1000자(plain text). | [JPA] |
| FR-COMMENT-03 | 대댓글: `parentId`는 최상위 댓글만 가리킬 수 있다. 대댓글에 대댓글을 달면 400(`COMMENT_DEPTH_EXCEEDED`). | [JPA] |
| FR-COMMENT-04 | 수정과 삭제: 작성자 본인 또는 ADMIN. 삭제는 soft delete. | [JPA] |
| FR-COMMENT-05 | 댓글 내용은 plain text로 저장하고, 프론트에서 이스케이프해서 렌더링한다. | – |

### 9.6 FR-FILE 파일 첨부와 에디터

| ID | 요구사항 | 담당 |
|---|---|---|
| FR-FILE-01 | 업로드 `POST /api/v1/files`(multipart, 로그인 필요). 응답은 `{id, url, originalName, contentType, size}`. 상태 TEMP로 저장한다. | [JPA] |
| FR-FILE-02 | 검증: 최대 크기 `FILE_MAX_SIZE`(기본 10MB). 확장자 화이트리스트(이미지 `jpg, jpeg, png, gif, webp` / 문서 `pdf, txt, zip, docx, xlsx, pptx`). **파일 내용의 매직 넘버로 MIME을 확인**(Apache Tika 등)해 확장자와 일치하지 않으면 거부한다. SVG는 금지한다(XSS 위험). | – |
| FR-FILE-03 | 저장 키는 `yyyy/MM/dd/{UUID}.{ext}`로 하고 원본 파일명은 DB에만 둔다. 경로 조작(`..`)을 방지한다. | – |
| FR-FILE-04 | `StorageService` 인터페이스: `store(InputStream, key, contentType)`, `load(key)`, `delete(key)`, `exists(key)`. 기본 구현은 `LocalStorageService`(`STORAGE_LOCAL_PATH`)이고, `STORAGE_TYPE` 설정으로 구현체를 선택한다. | – |
| FR-FILE-05 | 다운로드 `GET /api/v1/files/{id}`. TEMP 파일은 업로더만 받을 수 있다. LINKED 파일은 연결된 게시글의 게시판 읽기 권한이 필요하다. 이미지는 `inline`, 그 외는 `attachment`로 `Content-Disposition`을 설정하고 파일명은 RFC 5987로 인코딩한다. `X-Content-Type-Options: nosniff`를 붙인다. | [JPA] |
| FR-FILE-06 | 정리 스케줄러: 1시간마다 생성 후 24시간이 지난 TEMP 파일의 실제 파일과 DB 행을 삭제한다. 삭제된 게시글의 파일도 30일 후 삭제한다. ShedLock을 적용한다(SCALE-03). | [MB](대상 조회) + [JPA] |
| FR-FILE-07 | 에디터: TipTap(StarterKit, Link, Image, Placeholder). 이미지를 붙여넣거나 드래그하거나 툴바로 넣으면 즉시 업로드하고, 반환된 URL을 `img src`로 넣는다. 업로드 중에는 로딩 표시를 보여준다. 본문에 들어간 이미지의 fileId와 별도 첨부 fileId를 합쳐 `fileIds`로 전송한다. | – |

### 9.7 FR-ADMIN 관리자 페이지

모든 `/api/v1/admin/**` API와 `/admin/**` 화면은 **ADMIN만** 접근할 수 있다.

| ID | 요구사항 | 담당 |
|---|---|---|
| FR-ADMIN-01 | 대시보드: 전체 사용자, 게시글, 댓글 수와 최근 7일 가입자·게시글 일별 추이, 최근 게시글 5개를 보여준다. | [MB] |
| FR-ADMIN-02 | 게시판 관리: 목록(비활성 포함), 생성·수정 폼, 삭제, 드래그로 정렬(FR-BOARD-01~04) | [MB] 목록 / [JPA] 변경 |
| FR-ADMIN-03 | 게시글 관리: 전체 게시판 대상. 필터는 게시판, 상태(PUBLISHED/HIDDEN), 키워드, 기간. 숨김/복구(`PATCH status`), 삭제, 여러 건 선택 일괄 처리. | [MB] 목록 / [JPA] 변경 |
| FR-ADMIN-04 | 댓글 관리: 필터는 키워드, 상태. 숨김/복구, 삭제. | [MB] 목록 / [JPA] 변경 |
| FR-ADMIN-05 | 사용자 관리: 필터는 키워드(email/nickname), 역할, 상태. 역할 변경, 정지/해제. 정지하면 해당 사용자의 refresh token을 모두 폐기한다. **자기 자신의 역할 변경과 정지는 금지**한다(`ADMIN_SELF_MODIFICATION`). | [MB] 목록 / [JPA] 변경 |
| FR-ADMIN-06 | 숨김(HIDDEN) 처리된 글과 댓글은 사용자 API 목록과 상세에서 보이지 않는다. 숨긴 댓글에 대댓글이 있으면 "관리자에 의해 숨겨진 댓글입니다"로 표시한다. | – |

### 9.8 FR-INFRA 공통 인프라

| ID | 요구사항 |
|---|---|
| FR-INFRA-01 | **에러 응답**: RFC 9457 `ProblemDetail`에 확장 필드 `code`, `traceId`, `errors`(필드 검증 오류)를 더한다. 예시는 [12.2](#122-공통-규약)에 있다. |
| FR-INFRA-02 | **전역 예외 처리**: `@RestControllerAdvice`에서 `BusinessException`(ErrorCode 기반), `MethodArgumentNotValidException`, `ConstraintViolationException`, `AccessDeniedException`, `AuthenticationException`, `MaxUploadSizeExceededException`, 그 밖의 예외(500, 내부 메시지 숨김)를 처리한다. |
| FR-INFRA-03 | **공통 페이지 응답** `PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean hasNext)`. page는 0부터 시작한다. |
| FR-INFRA-04 | **traceId**: `TraceIdFilter`가 요청 헤더 `X-Request-Id`가 있으면 그 값을, 없으면 UUID를 MDC `traceId`에 넣고 응답 헤더로 돌려준다. 로그 패턴에 traceId를 포함한다. |
| FR-INFRA-05 | **로깅**: local은 컬러 텍스트, prod는 JSON(logstash encoder)으로 남긴다. 요청마다 method, path, status, 처리시간(ms), userId를 기록한다. 비밀번호와 토큰은 로그에 남기지 않는다. |
| FR-INFRA-06 | **CORS**: `CORS_ALLOWED_ORIGINS`(쉼표 구분)만 허용하고, `allowCredentials=true`로 설정한다. |
| FR-INFRA-07 | **프로필**: `local`(개발), `test`(Testcontainers), `prod`(운영)와 DB 프로필 `db-postgresql`, `db-mysql`을 조합해 쓴다. |
| FR-INFRA-08 | **API 문서**: springdoc Swagger UI `/swagger-ui.html`. local에서는 공개, prod에서는 비활성화한다(`SWAGGER_ENABLED`). JWT Bearer 인증 스키마를 등록한다. |
| FR-INFRA-09 | **헬스체크**: Actuator는 `health`, `info`만 노출하고, `/actuator/health`에 DB 상태를 포함한다. readiness/liveness 그룹을 켠다. |
| FR-INFRA-10 | **Auditing**: `BaseTimeEntity`(`createdAt`, `updatedAt`), `BaseEntity`(+`createdBy`). `AuditorAware`는 SecurityContext의 userId를 반환한다. |
| FR-INFRA-11 | **보안 헤더**: `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, 기본 CSP(프론트 nginx/Caddy에서 설정) |

## 10. 비기능 요구사항

### 10.1 보안
| ID | 요구사항 |
|---|---|
| NFR-SEC-01 | XSS: 본문은 서버 sanitize(FR-POST-05)하고, 댓글은 plain text로 둔다. 프론트는 sanitize된 HTML만 `dangerouslySetInnerHTML`로 렌더링하며, 한 번 더 DOMPurify를 적용한다. |
| NFR-SEC-02 | CSRF: API는 `Authorization: Bearer` 헤더로 인증하므로 CSRF 토큰은 끈다. 쿠키를 쓰는 `/auth/refresh`와 `/auth/logout`은 `SameSite=Strict` 쿠키에 더해 **`Origin` 헤더가 허용 목록에 있는지 검사**한다. |
| NFR-SEC-03 | SQL Injection: MyBatis는 `#{}`만 쓴다. 정렬 컬럼은 화이트리스트 enum으로 받는다(MB-05). |
| NFR-SEC-04 | 비밀값(`JWT_SECRET`, DB 비밀번호, 관리자 비밀번호)은 환경변수로만 주입하고 커밋하지 않는다. `JWT_SECRET`은 256bit 이상이며, 없거나 짧으면 기동을 실패시킨다. |
| NFR-SEC-05 | 파일 업로드: 확장자, MIME, 크기를 검증하고 SVG를 금지한다(FR-FILE-02). |
| NFR-SEC-06 | 권한 없는 리소스 접근은 존재 여부를 숨겨야 하는 경우(HIDDEN 글 등) 404로 응답한다. |
| NFR-SEC-07 | 의존성 취약점: CI에서 `npm audit --audit-level=high` 결과를 보고하고, 백엔드는 Dependabot을 설정한다. |

### 10.2 성능
| ID | 요구사항 |
|---|---|
| NFR-PERF-01 | 목록과 검색은 MyBatis 조인 쿼리 1~2회로 끝낸다(N+1 금지). |
| NFR-PERF-02 | [11장](#11-데이터-모델)에 적은 인덱스를 반드시 만든다. |
| NFR-PERF-03 | 로컬 기준 게시글 1만 건에서 목록 API p95 < 200ms(검색 제외)를 권장 목표로 한다. |
| NFR-PERF-04 | JPA `open-in-view=false`로 설정한다. |
| NFR-PERF-05 | 프론트 초기 번들(관리자 제외)은 gzip 기준 300KB 이하를 권장한다. |

### 10.3 개발자 경험 (DX)
| ID | 요구사항 |
|---|---|
| NFR-DX-01 | README에 필수 도구 설치(JDK 21, Node 24, Docker), 5분 안에 실행하는 방법, 프로젝트 이름/패키지 변경 방법, 새 도메인 추가 절차를 적는다. |
| NFR-DX-02 | Windows와 macOS/Linux 모두에서 실행할 수 있게 한다. `Makefile`과 같은 기능의 `scripts/*.ps1`을 제공하고, `.gitattributes`로 `gradlew`와 `*.sh`의 줄바꿈을 LF로 고정한다. |
| NFR-DX-03 | `CLAUDE.md`에 에이전트가 지켜야 할 규칙([20장](#20-에이전트-작업-규칙))과 자주 쓰는 명령을 요약한다. |

## 11. 데이터 모델

### 11.1 ERD 개요

```text
users 1───* refresh_tokens
users 1───* posts *───1 boards
users 1───* comments *───1 posts
comments 1───* comments (parent_id, 1단계)
users 1───* files
posts *───* files  (post_files)
shedlock (독립)
```

### 11.2 공통 컬럼 규칙
| 컬럼 | 설명 | 적용 |
|---|---|---|
| `created_at` | 생성 시각(UTC), NOT NULL | 모든 테이블 |
| `updated_at` | 수정 시각(UTC), NOT NULL | 변경 가능한 테이블 |
| `created_by` | 생성자 user id, NULL 허용 | boards, posts, comments |
| `deleted_at` | soft delete 시각, NULL이면 삭제 안 됨 | users, boards, posts, comments |

> PostgreSQL 타입은 `TIMESTAMP(6) WITH TIME ZONE`, MySQL 타입은 `DATETIME(6)`이다. 이하 표에서는 `TIMESTAMP`로 줄여 쓴다. 타입 차이는 [8.3 타입 매핑 표](#83-db-이식성-postgresql--mysql)를 따른다.

### 11.3 테이블 정의

#### `users`
| 컬럼 | 타입 (PG / MySQL) | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT IDENTITY / BIGINT AUTO_INCREMENT | PK | |
| email | VARCHAR(255) | NOT NULL, UNIQUE | 소문자로 정규화 |
| password_hash | VARCHAR(100) | NOT NULL | BCrypt |
| nickname | VARCHAR(20) | NOT NULL, UNIQUE | |
| role | VARCHAR(20) | NOT NULL | USER / ADMIN |
| status | VARCHAR(20) | NOT NULL | ACTIVE / SUSPENDED |
| last_login_at | TIMESTAMP | NULL | |
| created_at, updated_at | TIMESTAMP | NOT NULL | |
| deleted_at | TIMESTAMP | NULL | |

인덱스: `uk_users_email`, `uk_users_nickname`, `idx_users_role_status (role, status)`

#### `refresh_tokens`
| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | |
| user_id | BIGINT | NOT NULL, FK → users.id | |
| token_hash | VARCHAR(64) | NOT NULL, UNIQUE | SHA-256 hex |
| expires_at | TIMESTAMP | NOT NULL | |
| revoked_at | TIMESTAMP | NULL | 폐기 시각 |
| user_agent | VARCHAR(255) | NULL | |
| ip_address | VARCHAR(45) | NULL | IPv6 대응 |
| created_at | TIMESTAMP | NOT NULL | |

인덱스: `uk_refresh_tokens_token_hash`, `idx_refresh_tokens_user_id`, `idx_refresh_tokens_expires_at`

#### `boards`
| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | |
| slug | VARCHAR(30) | NOT NULL, UNIQUE | |
| name | VARCHAR(50) | NOT NULL | |
| description | VARCHAR(255) | NULL | |
| read_permission | VARCHAR(20) | NOT NULL | ANONYMOUS / USER / ADMIN |
| write_permission | VARCHAR(20) | NOT NULL | USER / ADMIN |
| comment_enabled | BOOLEAN / TINYINT(1) | NOT NULL | |
| attachment_enabled | BOOLEAN / TINYINT(1) | NOT NULL | |
| active | BOOLEAN / TINYINT(1) | NOT NULL | |
| sort_order | INT | NOT NULL, DEFAULT 0 | |
| created_at, updated_at | TIMESTAMP | NOT NULL | |
| created_by | BIGINT | NULL | |
| deleted_at | TIMESTAMP | NULL | |

인덱스: `uk_boards_slug`, `idx_boards_active_sort (active, sort_order)`

> soft delete한 게시판의 slug를 재사용하려면, 삭제할 때 slug를 `{slug}__deleted__{id}`로 바꿔 유니크 충돌을 피한다. 두 DB 모두 부분 인덱스 방식은 쓰지 않는다.

#### `posts`
| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | |
| board_id | BIGINT | NOT NULL, FK → boards.id | |
| author_id | BIGINT | NOT NULL, FK → users.id | |
| title | VARCHAR(200) | NOT NULL | |
| content | TEXT / LONGTEXT | NOT NULL | sanitize된 HTML |
| content_text | TEXT / LONGTEXT | NOT NULL | 검색용 plain text |
| is_notice | BOOLEAN / TINYINT(1) | NOT NULL, DEFAULT false | |
| status | VARCHAR(20) | NOT NULL | PUBLISHED / HIDDEN |
| view_count | BIGINT | NOT NULL, DEFAULT 0 | |
| comment_count | BIGINT | NOT NULL, DEFAULT 0 | |
| created_at, updated_at | TIMESTAMP | NOT NULL | |
| created_by | BIGINT | NULL | |
| deleted_at | TIMESTAMP | NULL | |

인덱스: `idx_posts_board_list (board_id, deleted_at, status, is_notice, created_at)`, `idx_posts_author (author_id)`, `idx_posts_created_at (created_at)`

#### `comments`
| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | |
| post_id | BIGINT | NOT NULL, FK → posts.id | |
| author_id | BIGINT | NOT NULL, FK → users.id | |
| parent_id | BIGINT | NULL, FK → comments.id | NULL이면 최상위 |
| content | VARCHAR(1000) | NOT NULL | plain text |
| status | VARCHAR(20) | NOT NULL | PUBLISHED / HIDDEN |
| created_at, updated_at | TIMESTAMP | NOT NULL | |
| created_by | BIGINT | NULL | |
| deleted_at | TIMESTAMP | NULL | |

인덱스: `idx_comments_post (post_id, parent_id, created_at)`, `idx_comments_author (author_id)`

#### `files`
| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | |
| storage_key | VARCHAR(255) | NOT NULL, UNIQUE | `yyyy/MM/dd/uuid.ext` |
| original_name | VARCHAR(255) | NOT NULL | |
| content_type | VARCHAR(100) | NOT NULL | |
| size_bytes | BIGINT | NOT NULL | |
| uploader_id | BIGINT | NOT NULL, FK → users.id | |
| status | VARCHAR(20) | NOT NULL | TEMP / LINKED |
| created_at, updated_at | TIMESTAMP | NOT NULL | |

인덱스: `uk_files_storage_key`, `idx_files_status_created (status, created_at)`

#### `post_files`
| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| post_id | BIGINT | PK(복합), FK → posts.id | |
| file_id | BIGINT | PK(복합), FK → files.id, UNIQUE | 파일 1개는 게시글 1개에만 연결 |
| usage_type | VARCHAR(20) | NOT NULL | INLINE(본문 이미지) / ATTACHMENT |
| sort_order | INT | NOT NULL, DEFAULT 0 | |

#### `shedlock`
ShedLock 공식 JDBC 스키마를 따른다. 컬럼은 `name` PK, `lock_until`, `locked_at`, `locked_by`이며, 벤더별 공식 DDL을 사용한다.

### 11.4 마이그레이션 파일
| 버전 | 파일 (postgresql/, mysql/ 각각) | 내용 |
|---|---|---|
| V1 | `V1__init.sql` | users, refresh_tokens, boards, posts, comments, files, post_files 생성과 인덱스·FK |
| V2 | `V2__seed.sql` | 기본 게시판 `notice`, `free` (관리자 계정은 SQL이 아니라 FR-AUTH-08 초기화 로직으로 생성) |
| V3 | `V3__shedlock.sql` | shedlock 테이블 |

## 12. API 명세

### 12.1 권한 표기
- `PUBLIC`: 누구나
- `USER`: 로그인 필요
- `OWNER|ADMIN`: 작성자 본인 또는 관리자
- `ADMIN`: 관리자
- `BOARD:READ`, `BOARD:WRITE`: 게시판 설정 기준으로 판정

### 12.2 공통 규약

- Base path: `/api/v1`. JSON은 camelCase이고, 시각은 ISO-8601 UTC(`2026-09-15T08:00:00Z`)로 표기한다.
- 인증: `Authorization: Bearer {accessToken}`
- 페이징 쿼리: `page`(0부터), `size`(기본 20, 최대 100)
- 페이징 응답: `PageResponse` (FR-INFRA-03)
- 에러 응답 예:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값이 올바르지 않습니다.",
  "instance": "/api/v1/auth/signup",
  "code": "COMMON_INVALID_INPUT",
  "traceId": "8f1c2b7e4d5a4f0e",
  "errors": [
    { "field": "email", "reason": "이메일 형식이 아닙니다." }
  ]
}
```

- 주요 에러 코드 (도메인별 enum):

| 코드 | HTTP | 설명 |
|---|---|---|
| COMMON_INVALID_INPUT | 400 | 입력 검증 실패 |
| COMMON_UNAUTHORIZED | 401 | 인증 필요 / 토큰 만료 |
| COMMON_FORBIDDEN | 403 | 권한 없음 |
| COMMON_NOT_FOUND | 404 | 리소스 없음 |
| COMMON_INTERNAL_ERROR | 500 | 서버 오류 |
| AUTH_INVALID_CREDENTIALS | 401 | 이메일/비밀번호 불일치 |
| AUTH_ACCOUNT_SUSPENDED | 403 | 정지된 계정 |
| AUTH_INVALID_REFRESH_TOKEN | 401 | 재발급 토큰 무효/만료/재사용 |
| AUTH_TOO_MANY_ATTEMPTS | 429 | 로그인 시도 초과 |
| USER_EMAIL_DUPLICATED | 409 | 이메일 중복 |
| USER_NICKNAME_DUPLICATED | 409 | 닉네임 중복 |
| USER_PASSWORD_MISMATCH | 400 | 현재 비밀번호 불일치 |
| BOARD_NOT_FOUND | 404 | 게시판 없음 |
| BOARD_SLUG_DUPLICATED | 409 | slug 중복 |
| BOARD_HAS_POSTS | 409 | 게시글이 있어 삭제 불가 |
| POST_NOT_FOUND | 404 | 게시글 없음(숨김 포함) |
| COMMENT_NOT_FOUND | 404 | 댓글 없음 |
| COMMENT_DISABLED | 403 | 댓글 비허용 게시판 |
| COMMENT_DEPTH_EXCEEDED | 400 | 대댓글 깊이 초과 |
| FILE_TOO_LARGE | 413 | 크기 초과 |
| FILE_TYPE_NOT_ALLOWED | 400 | 허용되지 않은 형식 |
| FILE_ATTACHMENT_DISABLED | 403 | 첨부 비허용 게시판 |
| FILE_NOT_OWNED | 403 | 본인 파일이 아님 |
| ADMIN_SELF_MODIFICATION | 400 | 자기 자신 역할 변경/정지 금지 |

### 12.3 엔드포인트

#### 인증 / 사용자
| 메서드 | 경로 | 권한 | 요청 | 응답 |
|---|---|---|---|---|
| POST | `/auth/signup` | PUBLIC | `{email, password, nickname}` | 201 `UserResponse` |
| POST | `/auth/login` | PUBLIC | `{email, password}` | 200 `{accessToken, expiresIn, user}` + `Set-Cookie: refresh_token` |
| POST | `/auth/refresh` | PUBLIC(쿠키) | – | 200 `{accessToken, expiresIn, user}` + 새 쿠키 |
| POST | `/auth/logout` | PUBLIC(쿠키) | – | 204 + 쿠키 삭제 |
| GET | `/users/me` | USER | – | 200 `UserResponse` |
| PATCH | `/users/me` | USER | `{nickname}` | 200 `UserResponse` |
| PATCH | `/users/me/password` | USER | `{currentPassword, newPassword}` | 204 |

`UserResponse = {id, email, nickname, role, createdAt}`

#### 게시판 (사용자)
| 메서드 | 경로 | 권한 | 요청 | 응답 |
|---|---|---|---|---|
| GET | `/boards` | PUBLIC | – | 200 `BoardSummary[]` (읽을 수 있는 활성 게시판) |
| GET | `/boards/{slug}` | BOARD:READ | – | 200 `BoardDetail {id, slug, name, description, commentEnabled, attachmentEnabled, canWrite}` |

#### 게시글
| 메서드 | 경로 | 권한 | 요청 | 응답 |
|---|---|---|---|---|
| GET | `/boards/{slug}/posts` | BOARD:READ | `?page&size&searchType&keyword` | 200 `{notices: PostListItem[], page: PageResponse<PostListItem>}` |
| POST | `/boards/{slug}/posts` | BOARD:WRITE | `{title, content, isNotice, fileIds[]}` | 201 `PostDetail` |
| GET | `/posts/{id}` | BOARD:READ | – | 200 `PostDetail` |
| PUT | `/posts/{id}` | OWNER\|ADMIN | `{title, content, isNotice, fileIds[]}` | 200 `PostDetail` |
| DELETE | `/posts/{id}` | OWNER\|ADMIN | – | 204 |

- `PostListItem = {id, title, authorNickname, isNotice, viewCount, commentCount, hasAttachment, createdAt}`
- `PostDetail = {id, board: {slug, name}, title, content, author: {id, nickname}, isNotice, viewCount, commentCount, attachments: FileResponse[], canEdit, canDelete, createdAt, updatedAt}`

#### 댓글
| 메서드 | 경로 | 권한 | 요청 | 응답 |
|---|---|---|---|---|
| GET | `/posts/{postId}/comments` | BOARD:READ | `?page&size` | 200 `PageResponse<CommentItem>` |
| POST | `/posts/{postId}/comments` | USER + BOARD:READ | `{content, parentId?}` | 201 `CommentItem` |
| PUT | `/comments/{id}` | OWNER\|ADMIN | `{content}` | 200 `CommentItem` |
| DELETE | `/comments/{id}` | OWNER\|ADMIN | – | 204 |

`CommentItem = {id, content, author: {id, nickname}, deleted, hidden, canEdit, canDelete, createdAt, updatedAt, replies: CommentItem[]}`

- 삭제되거나 숨긴 댓글은 `content`를 비우고 `deleted`/`hidden` 플래그를 true로 보낸다.

#### 파일
| 메서드 | 경로 | 권한 | 요청 | 응답 |
|---|---|---|---|---|
| POST | `/files` | USER | multipart `file` | 201 `FileResponse` |
| GET | `/files/{id}` | FR-FILE-05 규칙 | – | 200 바이너리 |

`FileResponse = {id, url, originalName, contentType, size}` (`url = /api/v1/files/{id}`)

#### 관리자
| 메서드 | 경로 | 권한 | 요청 | 응답 |
|---|---|---|---|---|
| GET | `/admin/dashboard` | ADMIN | – | 200 `{totals: {users, posts, comments}, dailySignups[], dailyPosts[], recentPosts[]}` |
| GET | `/admin/boards` | ADMIN | – | 200 `AdminBoard[]` |
| POST | `/admin/boards` | ADMIN | `BoardUpsertRequest` | 201 `AdminBoard` |
| PUT | `/admin/boards/{id}` | ADMIN | `BoardUpsertRequest` | 200 `AdminBoard` |
| DELETE | `/admin/boards/{id}` | ADMIN | – | 204 / 409 |
| PUT | `/admin/boards/order` | ADMIN | `{boardIds: number[]}` | 204 |
| GET | `/admin/posts` | ADMIN | `?boardId&status&keyword&from&to&page&size` | 200 `PageResponse<AdminPostItem>` |
| PATCH | `/admin/posts/status` | ADMIN | `{ids: number[], status}` | 204 |
| DELETE | `/admin/posts` | ADMIN | `{ids: number[]}` | 204 |
| GET | `/admin/comments` | ADMIN | `?status&keyword&page&size` | 200 `PageResponse<AdminCommentItem>` |
| PATCH | `/admin/comments/status` | ADMIN | `{ids: number[], status}` | 204 |
| DELETE | `/admin/comments` | ADMIN | `{ids: number[]}` | 204 |
| GET | `/admin/users` | ADMIN | `?keyword&role&status&page&size` | 200 `PageResponse<AdminUserItem>` |
| PATCH | `/admin/users/{id}/role` | ADMIN | `{role}` | 204 |
| PATCH | `/admin/users/{id}/status` | ADMIN | `{status}` | 204 |

- `BoardUpsertRequest = {slug, name, description, readPermission, writePermission, commentEnabled, attachmentEnabled, active}`
- `AdminBoard = BoardUpsertRequest + {id, sortOrder, postCount, createdAt}`

## 13. 프론트엔드 라우트와 화면

### 13.1 라우트
| 경로 | 화면 | 접근 |
|---|---|---|
| `/` | 홈: 게시판 목록과 각 게시판의 최근 글 5개 | 공개 |
| `/login` | 로그인 | 비로그인 전용 (로그인 상태면 `/`로 이동) |
| `/signup` | 회원가입 | 비로그인 전용 |
| `/boards/:slug` | 게시글 목록: 공지 상단 고정, 검색, 페이지네이션 | 게시판 읽기 권한 |
| `/boards/:slug/posts/:id` | 게시글 상세: 본문, 첨부, 댓글/대댓글 | 게시판 읽기 권한 |
| `/boards/:slug/write` | 글쓰기: TipTap 에디터, 첨부 | 로그인 + 쓰기 권한 |
| `/boards/:slug/posts/:id/edit` | 글 수정 | 작성자/관리자 |
| `/me` | 내 정보, 닉네임 변경, 비밀번호 변경 | 로그인 |
| `/admin` | 대시보드 | ADMIN |
| `/admin/boards` | 게시판 관리 (목록, 생성·수정 다이얼로그, 드래그 정렬) | ADMIN |
| `/admin/posts` | 게시글 관리 (필터, 일괄 숨김·삭제) | ADMIN |
| `/admin/comments` | 댓글 관리 | ADMIN |
| `/admin/users` | 사용자 관리 (역할, 정지) | ADMIN |
| `*` | 404 | 공개 |

### 13.2 레이아웃
- **MainLayout**: 상단 헤더(로고, 게시판 메뉴, 로그인/사용자 메뉴, 다크모드 토글), 본문, 푸터
- **AdminLayout**: 좌측 사이드바(대시보드/게시판/게시글/댓글/사용자), 상단바. 모바일에서는 사이드바를 Sheet로 전환한다.
- 반응형: 모바일 폭(약 400px)에서도 모든 화면을 쓸 수 있어야 한다. 관리자 테이블은 가로 스크롤을 허용한다.

### 13.3 인증 처리
- **FE-AUTH-01** `authStore`(Zustand): `accessToken`, `user`, `status: 'idle' | 'loading' | 'authenticated' | 'anonymous'`. 토큰은 localStorage에 저장하지 않는다.
- **FE-AUTH-02** 앱 부팅 시 `POST /auth/refresh`로 세션 복원을 시도하고, 끝날 때까지 스플래시를 보여준다.
- **FE-AUTH-03** axios 요청 인터셉터가 `Authorization` 헤더를 붙인다. 모든 요청은 `withCredentials: true`로 보낸다.
- **FE-AUTH-04** 응답 인터셉터가 401을 받으면 **single-flight**로 재발급한다. 동시에 들어온 401 요청들은 하나의 refresh Promise를 기다렸다가 재시도한다. 재발급이 실패하면 스토어를 비우고 `/login?redirect=현재경로`로 이동한다. `/auth/*` 요청 자체의 401은 재발급을 시도하지 않는다.
- **FE-AUTH-05** 라우트 가드 컴포넌트: `<RequireAuth>`, `<RequireRole role="ADMIN">`, `<GuestOnly>`.

### 13.4 공통 UI (shadcn/ui 기반)
- 사용 컴포넌트: Button, Input, Textarea, Form, Label, Select, Checkbox, Switch, Dialog, AlertDialog, DropdownMenu, Sheet, Table, Tabs, Badge, Card, Skeleton, Sonner(토스트), Pagination
- 공통 조합 컴포넌트: `DataTable`(TanStack Table, 선택/정렬), `ConfirmDialog`, `EmptyState`, `ErrorState`, `PageHeader`, `RichTextEditor`, `RichTextViewer`(DOMPurify)
- 다크모드: `class` 전략이다. 시스템 설정을 기본으로 따르고, 사용자가 선택하면 localStorage에 저장한다.
- 에러 처리: 조회 실패는 `ErrorState`, 변경 실패는 토스트로 보여준다. ProblemDetail의 `detail`을 표시하고, 필드 오류(`errors`)는 폼 필드에 매핑한다.

### 13.5 선택 사항
- `openapi-typescript`로 백엔드 OpenAPI 스펙에서 TS 타입을 생성하는 `npm run gen:api` 스크립트. 도입하면 feature `types.ts`를 생성 타입으로 대체한다.

## 14. 개발 환경과 실행

### 14.1 로컬 `docker-compose.yml`
| 서비스 | 프로필 | 설명 |
|---|---|---|
| `postgres` | `postgresql` | PostgreSQL 17, 포트 5432, 볼륨 `pgdata` |
| `mysql` | `mysql` | MySQL 8.4, 포트 3306, utf8mb4, 볼륨 `mysqldata` |
| `backend` | `full` | backend 이미지. `DB_VENDOR`에 맞는 DB를 쓴다. 호스트 포트를 고정하지 않아 `--scale backend=2`가 가능하다. 업로드 볼륨 `uploads`를 공유한다. |
| `frontend` | `full` | nginx 정적 서빙, `/api` → `backend:8080` 프록시, 호스트 포트 `8081` |

실행 예:
```bash
# DB만 띄우고 IDE/터미널에서 개발
docker compose --profile postgresql up -d
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local,db-postgresql'   # Windows: gradlew.bat
cd frontend && npm install && npm run dev        # http://localhost:5173 (Vite가 /api를 8080으로 프록시)

# 전체 실행 (.env의 DB_VENDOR 기준)
docker compose --profile full --profile postgresql up -d --build
# 백엔드 2개 인스턴스 테스트
docker compose --profile full --profile postgresql up -d --scale backend=2
```

### 14.2 Makefile / scripts 명령 (같은 기능을 `scripts/*.ps1`로도 제공)
| 명령 | 동작 |
|---|---|
| `make db-up DB=postgresql` | 선택한 DB 컨테이너 실행 |
| `make be-run` | 백엔드 로컬 실행 |
| `make fe-run` | 프론트 개발 서버 |
| `make test` | 백엔드(기본 DB) + 프론트 테스트 |
| `make test-all-db` | 백엔드 테스트를 PostgreSQL과 MySQL로 각각 실행 |
| `make up` / `make down` | 전체 compose 실행 / 종료 |
| `make lint` | spotlessCheck + eslint + typecheck |

### 14.3 환경변수
전체 목록은 [부록 22.2](#222-환경변수-envexample)에 있다. `.env.example`을 복사해 `.env`를 만든다. `.env`는 `.gitignore`에 넣는다.

## 15. 품질 도구 (FR-QUALITY)

### 15.1 백엔드
| ID | 요구사항 |
|---|---|
| FR-QUALITY-01 | JUnit 5 + AssertJ로 단위 테스트를 쓴다. 대상은 도메인 규칙(Entity 메서드), 권한 판정, 토큰 로직, sanitize 규칙. |
| FR-QUALITY-02 | **통합 테스트는 Testcontainers**로 실제 DB를 띄워 실행한다. Gradle 속성 `-PdbVendor=postgresql\|mysql`(기본 postgresql)로 대상 DB를 고른다. 테스트 베이스 클래스가 이 값을 읽어 컨테이너와 프로필(`test,db-{vendor}`)을 선택한다. |
| FR-QUALITY-03 | **같은 Repository/Mapper 통합 테스트가 두 DB 모두에서 통과**해야 한다. JPA `validate`, Flyway 마이그레이션, MyBatis 쿼리를 포함한다. |
| FR-QUALITY-04 | Controller 테스트는 `@WebMvcTest` 또는 `@SpringBootTest` + MockMvc로 쓴다. 인증/권한 시나리오(401, 403, 404)를 포함한다. |
| FR-QUALITY-05 | ArchUnit 아키텍처 테스트(8.1)를 넣는다. |
| FR-QUALITY-06 | Spotless: `./gradlew spotlessCheck`를 CI에서 검사하고, 로컬에서는 `spotlessApply`로 고친다. |
| FR-QUALITY-07 | JaCoCo 리포트를 생성한다. 커버리지 하한은 강제하지 않고, 리포트를 CI 아티팩트로 올린다(권장 목표: application 계층 라인 70%). |

### 15.2 프론트엔드
| ID | 요구사항 |
|---|---|
| FR-QUALITY-08 | Vitest + Testing Library + MSW. 대상은 인증 인터셉터(single-flight 재발급), 라우트 가드, 로그인/회원가입 폼 검증, 게시글 목록 렌더링. |
| FR-QUALITY-09 | ESLint(flat config, typescript-eslint, react-hooks, jsx-a11y)와 Prettier. `npm run lint`, `npm run format:check`. |
| FR-QUALITY-10 | `npm run typecheck`(`tsc --noEmit`) |
| FR-QUALITY-11 | Playwright 스모크 테스트 1개: 관리자 로그인 → 게시판 생성 → 글 작성 → 목록에 표시되는지 확인 |

### 15.3 Docker 이미지
| ID | 요구사항 |
|---|---|
| FR-QUALITY-12 | `backend/Dockerfile`: 멀티스테이지(빌드는 JDK 21, 런타임은 JRE 21, Temurin 기반)로 만든다. layered jar를 활용한다. non-root 사용자로 실행하고 `HEALTHCHECK`로 `/actuator/health`를 호출한다. JVM 옵션은 `JAVA_OPTS` 환경변수로 받는다(`-XX:MaxRAMPercentage=75` 기본). |
| FR-QUALITY-13 | `frontend/Dockerfile`: 멀티스테이지(Node 24로 빌드, nginx 알파인으로 런타임)로 만든다. SPA fallback(`try_files ... /index.html`), 정적 자산 장기 캐시, `index.html`은 no-cache, `/api` 프록시, 보안 헤더를 설정한다. |
| FR-QUALITY-14 | 이미지에 OCI 라벨(`org.opencontainers.image.source`, `revision`)을 넣는다. |

## 16. CI/CD 파이프라인 (FR-CICD)

> 목표: **실제로 동작하는** 파이프라인. 필요한 Secrets만 등록하면 바로 배포까지 된다.

### 16.1 CI: `.github/workflows/ci.yml`

- 트리거: `pull_request`(모든 브랜치), `push`(main)
- 동시 실행 제어: `concurrency: ci-${{ github.ref }}`, `cancel-in-progress: true`

| Job | 조건 | 단계 |
|---|---|---|
| `changes` | 항상 | `dorny/paths-filter`로 `backend/**`, `frontend/**`, `deploy/**` 변경 여부를 출력한다. |
| `backend` | backend 변경 시 | `strategy.matrix.db: [postgresql, mysql]`. `actions/setup-java`(temurin 21), `gradle/actions/setup-gradle`(캐시), `./gradlew spotlessCheck test build -PdbVendor=${{ matrix.db }}`. 테스트 리포트와 JaCoCo를 아티팩트로 업로드한다. Testcontainers는 러너의 Docker를 쓴다. |
| `frontend` | frontend 변경 시 | `actions/setup-node`(24, npm 캐시), `npm ci`, `lint`, `format:check`, `typecheck`, `test -- --run`, `build`, `npm audit --audit-level=high`(실패해도 경고만) |
| `docker-build` | backend 또는 frontend 변경 시 | `docker/build-push-action`으로 이미지 빌드만 한다(push 안 함). GHA 캐시를 사용한다. |
| `e2e` | main push 시 (PR에서는 선택) | compose `full` + postgresql을 띄우고 Playwright 스모크 테스트를 실행한다. 실패하면 리포트를 업로드한다. |

main 브랜치 보호 규칙(README에 설정 방법 안내): `backend (postgresql)`, `backend (mysql)`, `frontend` job 통과를 필수로 한다.

### 16.2 CD: `.github/workflows/cd.yml`

- 트리거: `push`(main), `push tags: v*`, `workflow_dispatch`(입력: `environment` = staging|production, `image_tag` = 롤백할 태그, 선택)
- 권한: `contents: read`, `packages: write`
- 동시 실행 제어: `concurrency: deploy-${{ environment }}`, `cancel-in-progress: false`

```text
build-and-push ──► deploy-staging ──► deploy-production (Environment 승인 필요)
```

| Job | 내용 |
|---|---|
| `build-and-push` | `docker/login-action`으로 GHCR에 로그인한다(`GITHUB_TOKEN`). backend와 frontend 이미지를 빌드해 푸시한다. 이미지 이름은 `ghcr.io/{owner}/{repo}-backend`, `-frontend`. 태그는 `sha-{짧은 커밋}`과 `latest`이고, `v*` 태그로 실행되면 `vX.Y.Z`도 붙인다(`docker/metadata-action`). 입력 `image_tag`가 있으면 빌드를 건너뛴다(롤백 모드). |
| `deploy-staging` | `environment: staging`. SSH로 서버에 접속(`appleboy/ssh-action` 또는 `ssh` 직접 사용, `SSH_KNOWN_HOSTS`로 호스트를 검증)해 `deploy/deploy.sh {태그}`를 실행한다. |
| `deploy-production` | `environment: production`. GitHub Environment의 **Required reviewers**로 수동 승인을 받는다. main push 흐름에서는 staging이 성공한 뒤에 실행된다. |

### 16.3 서버 배포 스크립트: `deploy/deploy.sh`

동작 순서 (MUST):
1. `set -euo pipefail`. 인자로 `IMAGE_TAG`를 받는다.
2. 서버의 `$DEPLOY_PATH/.deploy/current_tag`를 읽어 `PREVIOUS_TAG`로 보관한다.
3. `echo $GHCR_READ_TOKEN | docker login ghcr.io -u $GHCR_USER --password-stdin`을 실행한다(패키지가 공개면 생략 가능).
4. `IMAGE_TAG=$IMAGE_TAG docker compose -f docker-compose.prod.yml --env-file .env.prod pull`
5. `... up -d --remove-orphans`
6. 헬스체크: 최대 60초 동안 5초 간격으로 backend 컨테이너의 `/actuator/health`가 `UP`인지 확인한다. 프록시를 거쳐 `https://{DOMAIN}/api/actuator/health`를 확인하는 방식도 가능하다.
7. 성공하면 `current_tag`를 `IMAGE_TAG`로 갱신하고, `docker image prune -f`를 실행한 뒤 종료 코드 0으로 끝낸다.
8. **실패하면 자동 롤백**한다. `PREVIOUS_TAG`로 `up -d`를 다시 실행하고 헬스체크한 다음, 종료 코드 1로 끝내 워크플로를 실패 처리한다. 이전 태그가 없으면 롤백 불가를 로그로 남긴다.

### 16.4 운영 Compose: `deploy/docker-compose.prod.yml`
| 서비스 | 설명 |
|---|---|
| `caddy` | 80/443 공개, `Caddyfile`로 자동 HTTPS. `/api/*`와 `/swagger-ui*`(비활성)는 backend로, 나머지는 frontend로 보낸다. |
| `frontend` | `ghcr.io/...-frontend:${IMAGE_TAG}` |
| `backend` | `ghcr.io/...-backend:${IMAGE_TAG}`, `deploy.replicas: ${BACKEND_REPLICAS:-1}`, `uploads` 볼륨 공유, `restart: unless-stopped` |
| `postgres` / `mysql` | compose 프로필로 `DB_VENDOR`에 맞는 것 하나만 띄운다. 볼륨으로 보존하고, 외부 포트는 열지 않는다. |

- staging과 production은 **서로 다른 서버**여도 되고, 같은 서버의 다른 디렉터리(`/opt/app-staging`, `/opt/app-prod`)와 다른 compose 프로젝트명·도메인을 써도 된다. GitHub Environment별 Secrets로 구분한다.
- Flyway는 앱 기동 시 자동 적용된다. 여러 인스턴스가 동시에 떠도 Flyway 자체 락으로 안전하다.

### 16.5 서버 준비: `deploy/server-setup.sh`
1. Docker Engine과 Compose 플러그인 설치(Ubuntu 24.04 공식 저장소 방식)
2. 배포 사용자 `deploy` 생성, docker 그룹 추가, SSH 공개키 등록 안내
3. `$DEPLOY_PATH` 디렉터리 생성, `docker-compose.prod.yml`, `Caddyfile`, `deploy.sh`, `backup.sh` 배치 방법 안내
4. `.env.prod.example`을 복사해 `.env.prod`를 만들라는 안내. **`.env.prod`는 서버에만 두고 절대 커밋하지 않는다.**
5. 방화벽(ufw): 22, 80, 443만 허용

> 설정 파일을 서버에 올리는 방법: CD의 deploy job이 SSH 전에 `scp`로 `deploy/` 파일(`.env.prod` 제외)을 동기화한다.

### 16.6 백업: `deploy/backup.sh`
- `DB_VENDOR`에 따라 `pg_dump` 또는 `mysqldump`를 DB 컨테이너 안에서 실행하고, gzip으로 `$BACKUP_PATH/{날짜}.sql.gz`에 저장한다. 7일이 지난 백업은 삭제한다.
- 업로드 볼륨은 `tar`로 백업한다.
- README에 cron 예시를 적는다: `0 3 * * * /opt/app-prod/backup.sh >> /var/log/app-backup.log 2>&1`

### 16.7 롤백 수동 절차
1. GitHub → Actions → `cd.yml` → **Run workflow**
2. `environment`와 `image_tag`(예: `sha-1a2b3c4`)를 입력하고 실행한다.
3. 빌드를 건너뛰고 해당 태그로 `deploy.sh`가 실행된다.

> 주의: DB 마이그레이션은 되돌리지 않는다. 롤백할 버전이 새 스키마와 호환되도록 마이그레이션은 **확장 → 코드 배포 → 축소** 순서(Expand–Contract)로 작성한다는 원칙을 `docs/conventions.md`에 적는다.

### 16.8 GitHub Secrets / Variables
전체 표는 [부록 22.3](#223-github-secrets--variables)에 있다.

## 17. 개발 로그: 학습용 튜토리얼 (FR-DEVLOG)

> 목적: 나중에 이 로그만 읽고도 **처음부터 따라 만들 수 있고, 왜 그렇게 했는지 이해할 수 있게** 하는 것.

| ID | 요구사항 |
|---|---|
| FR-DEVLOG-01 | 위치는 `docs/devlog/`. `00-index.md`(목차, 단계별 진행 상태, 소요 시간)와 마일스톤별 파일 `M0-scaffolding.md`, `M1-backend-infra.md`, `M2-auth.md`, `M3-frontend-base.md`, `M4-cms-backend.md`, `M5-cms-frontend.md`, `M6-admin.md`, `M7-quality-ci.md`, `M8-cd.md`를 둔다. |
| FR-DEVLOG-02 | 각 파일은 [부록 22.1 템플릿](#221-devlog-템플릿)의 7개 섹션을 **모두** 채운다. |
| FR-DEVLOG-03 | "배경 개념" 섹션은 해당 기술을 처음 보는 사람도 이해할 수 있게 비유와 그림(텍스트 다이어그램)으로 설명한다. 단계별 필수 개념은 아래와 같다. |
| FR-DEVLOG-04 | "따라하기" 섹션은 실행한 명령어를 복사해 쓸 수 있는 코드 블록으로 적고, 만든 파일은 경로와 핵심 코드 발췌(설명 주석 포함)로 적는다. Windows(PowerShell)와 macOS/Linux 명령이 다르면 둘 다 적는다. |
| FR-DEVLOG-05 | "겪은 문제와 해결"은 실제로 발생한 에러 메시지 원문, 원인 분석, 해결 방법을 적는다. 문제가 없었으면 "초보자가 겪기 쉬운 실수" 2개 이상을 적는다. |
| FR-DEVLOG-06 | 코드 예시는 **Java, Groovy(Gradle), TypeScript, YAML, SQL, Shell**만 쓴다. **Kotlin 예시는 금지**한다. |
| FR-DEVLOG-07 | 스크린샷 대신 명령 출력 텍스트를 붙인다. |
| FR-DEVLOG-08 | 해당 마일스톤 커밋에 devlog 파일을 포함한다. **devlog가 없으면 마일스톤 미완료**로 본다. |

단계별 필수 개념 (FR-DEVLOG-03):
| 마일스톤 | 반드시 설명할 개념 |
|---|---|
| M0 | 모노레포란?, Gradle Wrapper와 Groovy DSL 기초, Vite란?, Docker Compose 프로필 |
| M1 | 계층형 아키텍처와 의존 방향, JPA vs MyBatis 역할 분담(CQRS 입문), Flyway 마이그레이션, Spring 프로필로 DB 전환, ProblemDetail, ArchUnit, ShedLock과 분산 락 |
| M2 | 세션 vs JWT, Access/Refresh Token과 Rotation·재사용 탐지, BCrypt, Spring Security 필터 체인, httpOnly 쿠키와 CSRF |
| M3 | SPA와 라우팅, 서버 상태 vs 클라이언트 상태(TanStack Query vs Zustand), axios 인터셉터와 single-flight, shadcn/ui 방식 |
| M4 | soft delete, 권한 모델(레벨 비교), XSS와 HTML sanitize, 파일 업로드 보안(매직 넘버), 동적 SQL과 페이징, 원자적 카운트 증가 |
| M5 | 리치 텍스트 에디터 구조(TipTap), 즉시 업로드 흐름, 폼 검증(RHF + Zod), 낙관적 업데이트 |
| M6 | 관리자 권한 가드, DataTable과 일괄 처리, 대시보드 집계 쿼리 |
| M7 | 테스트 피라미드, Testcontainers, 멀티스테이지 Docker 빌드, GitHub Actions와 매트릭스 빌드 |
| M8 | CI와 CD 차이, 컨테이너 레지스트리(GHCR), SSH 배포, 헬스체크와 자동 롤백, GitHub Environments 승인, Expand–Contract 마이그레이션, 백업 |

## 18. 구현 마일스톤과 완료 조건

> 에이전트는 **M0부터 순서대로** 진행한다. 각 마일스톤의 "완료 조건"을 모두 만족하고 "확인 명령"이 성공하며 devlog를 작성해야 다음으로 넘어간다. 마일스톤마다 커밋한다(Conventional Commits, 예: `feat(m2): JWT 인증과 사용자 API`).
>
> 확인 명령의 `./gradlew`는 Windows에서 `gradlew.bat`으로 바꾼다.

### M0. 레포 스캐폴딩과 개발환경
- **작업**
  - 모노레포 구조 생성([7장](#7-레포지토리-구조)), 루트 파일 작성: `.gitignore`, `.gitattributes`, `.editorconfig`, `README.md` 초안, `CLAUDE.md`, `docs/decisions.md`, `docs/conventions.md`
  - 라이브러리 최신 안정 버전을 확인해 [6장](#6-기술-스택과-버전) 표와 `decisions.md`에 기록
  - backend: Spring Initializr 기반 Gradle **Groovy DSL** 프로젝트(Web, Security, Validation, Data JPA, Actuator, Flyway, PostgreSQL/MySQL 드라이버) + MyBatis starter
  - frontend: Vite React TS 프로젝트, Tailwind v4, shadcn/ui 초기화, ESLint/Prettier
  - `docker-compose.yml`(postgres, mysql 프로필), `.env.example`, Makefile, `scripts/*.ps1`
- **완료 조건**
  - [ ] `backend/build.gradle`이 Groovy DSL이고, `*.kts` 파일이 하나도 없다.
  - [ ] 두 DB 컨테이너가 각각 뜬다.
  - [ ] 백엔드가 빈 상태로 기동하고 프론트 개발 서버가 뜬다.
- **확인 명령**: `docker compose --profile postgresql up -d` → `./gradlew bootRun`(health UP), `npm run dev`, `git ls-files | grep -c "\.kts$"`의 결과가 0
- **devlog**: `M0-scaffolding.md`

### M1. 백엔드 공통 인프라
- **작업**
  - 패키지와 계층 구조, `global/*`
  - DB 프로필 2종과 `DB_VENDOR` 전환, Flyway 이중 경로, `V1__init.sql`(두 DB), `V2__seed.sql`, `V3__shedlock.sql`
  - JPA 설정(validate, UTC, open-in-view false), MyBatis 설정(`VendorDatabaseIdProvider`, camelCase, mapper 경로), 트랜잭션 매니저 공유
  - Auditing, `BaseTimeEntity`/`BaseEntity`, ErrorCode 체계, `GlobalExceptionHandler`(ProblemDetail), `PageResponse`
  - `TraceIdFilter`, 로깅(logback local/prod), CORS, springdoc, Actuator
  - ShedLock 설정, Spring Cache(Caffeine), `StorageService`/`RateLimiter` 인터페이스와 기본 구현
  - Testcontainers 테스트 베이스(`-PdbVendor`), ArchUnit 테스트(규칙 ARCH-01~06)
- **완료 조건**
  - [ ] PostgreSQL과 MySQL 각각에서 마이그레이션과 `validate`가 통과한다.
  - [ ] MyBatis 샘플 쿼리 통합 테스트가 두 DB에서 통과한다.
  - [ ] 존재하지 않는 경로로 요청하면 ProblemDetail 형식에 traceId가 포함된 에러가 반환된다.
  - [ ] ArchUnit 테스트가 통과한다. 규칙을 일부러 어긴 샘플로 실패하는 것을 확인한 뒤 샘플은 제거한다.
- **확인 명령**: `./gradlew test -PdbVendor=postgresql`, `./gradlew test -PdbVendor=mysql`
- **devlog**: `M1-backend-infra.md`

### M2. 인증과 사용자
- **작업**: FR-AUTH-01~09, FR-USER-01~05, SecurityConfig(stateless, JWT 필터, 인증 실패/권한 거부 핸들러를 ProblemDetail로), 관리자 초기화, 로그인 rate limit, 만료 refresh token 정리 스케줄러(ShedLock)
- **완료 조건**
  - [ ] 가입, 로그인, 재발급(rotation), 로그아웃, 내 정보 조회·수정, 비밀번호 변경 API가 동작한다.
  - [ ] 폐기된 refresh token을 재사용하면 해당 사용자의 모든 토큰이 폐기된다(테스트).
  - [ ] SUSPENDED 사용자의 로그인과 재발급이 거부된다(테스트).
  - [ ] 로그인을 11회 연속 실패하면 429가 반환된다(테스트).
  - [ ] 두 DB에서 모든 테스트가 통과한다.
- **확인 명령**: `./gradlew test -PdbVendor=postgresql`, `./gradlew test -PdbVendor=mysql`, Swagger UI에서 수동 호출
- **devlog**: `M2-auth.md`

### M3. 프론트엔드 기반과 인증 화면
- **작업**
  - `app/`(router, providers, MainLayout, AdminLayout 뼈대), `shared/api`(axios, 인터셉터, ApiError), `authStore`, 라우트 가드, 앱 부팅 시 세션 복원
  - `/login`, `/signup`, `/me` 화면, 다크모드, 토스트, 404
  - Vitest + MSW 설정과 인증 관련 테스트(FR-QUALITY-08)
- **완료 조건**
  - [ ] 가입 → 로그인 → 새로고침해도 로그인이 유지된다 → 로그아웃
  - [ ] access token 만료(짧은 TTL로 테스트) 후 여러 요청이 동시에 나가도 재발급은 1번만 일어난다(테스트).
  - [ ] USER로 `/admin`에 접근하면 차단된다.
- **확인 명령**: `npm run lint && npm run typecheck && npm run test -- --run && npm run build`
- **devlog**: `M3-frontend-base.md`

### M4. CMS 백엔드
- **작업**: FR-BOARD-01~07, FR-POST-01~08, FR-COMMENT-01~05, FR-FILE-01~06. Command는 JPA, Query는 MyBatis(8.2 표 준수). 관리자 API 중 게시판 관리 부분.
- **완료 조건**
  - [ ] 게시판 권한 조합(ANONYMOUS/USER/ADMIN × 읽기/쓰기) 테스트가 통과한다.
  - [ ] 목록/검색 Mapper 테스트가 두 DB에서 통과하고, 공지 고정과 HIDDEN/삭제 제외가 확인된다.
  - [ ] `<script>`, `onerror=` 등이 들어간 본문이 sanitize되는지 테스트로 확인한다.
  - [ ] 확장자를 위장한 파일(예: .exe를 .png로 이름 변경)이 거부된다(테스트).
  - [ ] 대댓글 깊이 제한과 댓글 비허용 게시판 검증이 동작한다.
  - [ ] 고아 파일 정리 스케줄러가 ShedLock 테이블에 락을 기록한다.
- **확인 명령**: `./gradlew test -PdbVendor=postgresql`, `./gradlew test -PdbVendor=mysql`
- **devlog**: `M4-cms-backend.md`

### M5. CMS 사용자 화면과 에디터
- **작업**: 홈, 게시글 목록(검색, 페이지네이션, 공지), 상세(RichTextViewer, 첨부 다운로드, 댓글/대댓글), 글쓰기·수정(TipTap, 이미지 즉시 업로드, 첨부), 권한에 따른 버튼 노출
- **완료 조건**
  - [ ] 4장 시나리오 2~4를 브라우저에서 수행할 수 있다.
  - [ ] 이미지 붙여넣기 → 업로드 → 저장 → 상세에서 이미지가 표시된다.
  - [ ] 모바일 폭(400px)에서 레이아웃이 깨지지 않는다.
- **확인 명령**: `npm run lint && npm run typecheck && npm run test -- --run && npm run build`
- **devlog**: `M5-cms-frontend.md`

### M6. 관리자 페이지
- **작업**: FR-ADMIN-01~06 백엔드(MyBatis 조회 + JPA 변경)와 `/admin/*` 화면(DataTable, 일괄 처리, 게시판 드래그 정렬, 대시보드)
- **완료 조건**
  - [ ] 4장 시나리오 1과 5가 동작한다.
  - [ ] 관리자 자기 자신의 역할 변경과 정지가 거부된다(테스트).
  - [ ] 사용자를 정지하면 기존 refresh token으로 재발급할 수 없다(테스트).
  - [ ] 두 DB에서 관리자 조회 Mapper 테스트가 통과한다.
- **확인 명령**: 백엔드 테스트 2종 + 프론트 테스트/빌드
- **devlog**: `M6-admin.md`

### M7. 품질 도구, Docker, CI
- **작업**: FR-QUALITY-01~14 보완(JaCoCo, Spotless 전체 적용, Playwright 스모크), backend/frontend Dockerfile, compose `full` 프로필, `ci.yml`, Dependabot 설정, README 완성(실행법, 새 도메인 추가 절차, 브랜치 보호 설정법)
- **완료 조건**
  - [ ] `docker compose --profile full --profile postgresql up -d --build`로 전체가 동작하고, `mysql` 프로필로도 동작한다.
  - [ ] `--scale backend=2`에서 로그인, 업로드, 상세 조회가 정상이고, 스케줄러 로그가 한 인스턴스에서만 찍힌다.
  - [ ] PR에서 CI의 모든 job(DB 매트릭스 2종 포함)이 녹색이다.
- **확인 명령**: `make up`, `make test-all-db`, GitHub Actions 실행 결과
- **devlog**: `M7-quality-ci.md`

### M8. CD와 운영 문서
- **작업**: `cd.yml`, `deploy/docker-compose.prod.yml`, `Caddyfile`, `deploy.sh`(헬스체크 + 자동 롤백), `server-setup.sh`, `backup.sh`, `.env.prod.example`, GitHub Environments(staging/production) 설정 가이드, 롤백 절차 문서
- **완료 조건**
  - [ ] main 머지 → GHCR에 `sha-*` 이미지가 푸시되고 → staging에 자동 배포되며 → 헬스체크가 통과한다.
  - [ ] production 배포는 승인 후에만 실행된다.
  - [ ] 헬스체크를 일부러 실패시키면(예: 잘못된 DB 비밀번호 태그) 이전 태그로 자동 롤백되고 워크플로가 실패로 표시된다.
  - [ ] `workflow_dispatch`로 특정 태그 롤백이 동작한다.
  - [ ] 백업 스크립트가 두 DB 벤더에서 덤프 파일을 만든다.
- **확인 명령**: GitHub Actions 실행 기록, 서버에서 `docker compose ps`, `curl https://{DOMAIN}/api/actuator/health`
- **서버가 준비되지 않은 경우**: 에이전트는 파일 작성과 `actionlint`, `shellcheck` 검사까지만 완료한다. 실제 배포 검증 항목은 `00-index.md`에 "사용자 확인 필요"로 표시하고, 사용자가 해야 할 설정(서버, Secrets, Environments)을 체크리스트로 남긴다.
- **devlog**: `M8-cd.md`

## 19. 최종 수용 기준 (E2E 체크리스트)

**기능**
- [ ] `docker compose --profile full --profile postgresql up -d` 후, 시드된 관리자로 로그인해 게시판을 생성할 수 있다.
- [ ] 일반 사용자가 가입해 이미지가 포함된 글을 첨부 파일과 함께 작성할 수 있다.
- [ ] 다른 사용자가 댓글을 달고, 작성자가 대댓글을 달 수 있다.
- [ ] 제목+내용 검색, 페이지네이션, 공지 상단 고정이 동작한다.
- [ ] 관리자가 글을 숨기면 사용자 화면의 목록과 상세에서 보이지 않는다.
- [ ] 관리자가 사용자를 정지하면 해당 사용자는 로그인과 재발급이 불가하다.

**DB와 아키텍처**
- [ ] `DB_VENDOR=postgresql`과 `DB_VENDOR=mysql` **각각에서** 위 기능 시나리오가 똑같이 동작한다.
- [ ] 목록·검색·통계는 MyBatis, 쓰기는 JPA로 구현되어 있다(8.2 표 준수).
- [ ] ArchUnit 테스트가 CI에서 실행되고 통과한다.
- [ ] 백엔드 2개 인스턴스(`--scale backend=2`)에서 로그인, 업로드, 스케줄러가 정상이고, 스케줄러는 한 번만 실행된다.

**품질과 배포**
- [ ] PR에서 CI 전체(backend × 2 DB, frontend, docker-build)가 통과한다.
- [ ] main 머지 시 staging 자동 배포와 헬스체크가 통과하고, 승인 후 production에 배포된다.
- [ ] 헬스체크 실패 시 자동 롤백이 동작한다.

**문서**
- [ ] README만 보고 새 개발자가 로컬 실행과 새 도메인 추가를 할 수 있다.
- [ ] `docs/devlog/`에 M0~M8 로그가 템플릿의 7개 섹션을 모두 채운 상태로 있다.
- [ ] 레포 어디에도 Kotlin 파일(`*.kt`, `*.kts`)이 없다.

**요구사항 추적표**
| 요구사항 그룹 | 마일스톤 | 수용 기준 |
|---|---|---|
| FR-ARCH | M1 (이후 전 단계 유지) | DB와 아키텍처 |
| FR-AUTH, FR-USER | M2, M3 | 기능 1, 6 |
| FR-BOARD, FR-POST, FR-COMMENT, FR-FILE | M4, M5 | 기능 1~5 |
| FR-ADMIN | M6 | 기능 5, 6 |
| FR-INFRA | M1 | 전체 |
| FR-QUALITY | M1~M7 | 품질 |
| FR-CICD | M7, M8 | 품질과 배포 |
| FR-DEVLOG | M0~M8 | 문서 |

## 20. 에이전트 작업 규칙

> 이 규칙은 `CLAUDE.md`에도 요약해 넣는다.

1. **순서 준수**: M0부터 순서대로 진행하고, 마일스톤마다 커밋한다. 커밋 메시지는 Conventional Commits 형식으로 쓴다.
2. **테스트 게이트**: 해당 마일스톤의 확인 명령(백엔드는 **두 DB 모두**)이 통과하지 않으면 다음 단계로 넘어가지 않는다.
3. **버전 확인**: 라이브러리 버전을 추측하지 않는다. 실제 최신 안정 버전과 호환성을 확인하고 기록한다.
4. **Kotlin 금지**: `build.gradle.kts`, `*.kt`를 만들지 않는다. Gradle은 Groovy DSL, 코드는 Java만 쓴다. 문서 예시도 같다.
5. **아키텍처 규칙**: FR-ARCH(8장)를 어기는 코드를 만들지 않는다. 예외가 필요하면 `docs/decisions.md`에 이유를 기록한다.
6. **마이그레이션 쌍**: 스키마 변경은 항상 `postgresql/`과 `mysql/`에 같은 버전 번호로 함께 작성한다.
7. **결정 기록**: PRD에 없거나 PRD와 다르게 결정한 사항은 `docs/decisions.md`에 `날짜 / 결정 / 대안 / 이유` 형식으로 기록한다.
8. **devlog 필수**: 마일스톤을 끝낼 때 튜토리얼 로그를 작성하고 같은 커밋에 포함한다.
9. **비밀값 금지**: `.env`, `.env.prod`, 키, 토큰을 커밋하지 않는다. 예시 파일(`*.example`)만 커밋한다.
10. **범위 준수**: 비목표(2.2)에 있는 기능을 임의로 추가하지 않는다. 필요해 보이면 `docs/decisions.md`의 "제안" 섹션에 적는다.
11. **막히면 기록**: 외부 자원(서버, 도메인, Secrets)이 필요해 검증할 수 없는 항목은 건너뛰지 말고 "사용자 확인 필요"로 명시한다.

## 21. 향후 확장과 오픈 이슈

### 21.1 향후 확장 후보
| 항목 | 연결 지점 |
|---|---|
| 소셜 로그인(OAuth2: Google, Kakao, Naver) | Spring Security OAuth2 Client, `users`에 provider 컬럼 |
| 이메일 인증, 비밀번호 재설정 | 메일 발송 인터페이스 |
| S3/MinIO 파일 저장 | `StorageService` 구현체 추가 |
| Redis 캐시, 분산 rate limit | Spring Cache, `RateLimiter` 구현체 교체 |
| 전문 검색(PostgreSQL FTS, MySQL FULLTEXT, OpenSearch) | MyBatis `databaseId` 분기 또는 검색 인터페이스 |
| 다국어(i18n) | 프론트 i18next, 백엔드 MessageSource |
| 알림(이메일, 웹 푸시) | 도메인 이벤트(`ApplicationEventPublisher`) |
| 감사 로그(관리자 행위 기록) | `admin_audit_logs` 테이블, AOP |
| ECS / Kubernetes 배포 | 이미지 그대로 사용, 배포 매니페스트만 추가 |
| 모니터링(Prometheus, Grafana, Loki) | Actuator `prometheus` 엔드포인트, JSON 로그 |

### 21.2 오픈 이슈
| # | 이슈 | 기본안 |
|---|---|---|
| O1 | Spring Boot 4.x와 MyBatis/springdoc/ShedLock 호환성 | M0에서 확인하고, 문제가 있으면 3.5.x로 고정 |
| O2 | 운영 도메인과 서버 사양 | 사용자가 결정. 최소 2 vCPU / 4GB RAM 권장 |
| O3 | staging과 production 분리 방식(서버 2대 vs 1대) | 기본은 1대에 디렉터리와 도메인만 분리 |
| O4 | GHCR 패키지 공개 여부 | 기본은 비공개 + `GHCR_READ_TOKEN` |
| O5 | 인메모리 rate limit의 다중 인스턴스 한계 | 문서에 명시하고, Redis 도입 시 교체 |

---

## 22. 부록

### 22.1 devlog 템플릿

````markdown
# M{n}. {마일스톤 이름}

> 작성일: YYYY-MM-DD · 소요 시간: 약 N시간 · 관련 커밋: `abc1234`

## 1. 이번 단계의 목표
- 이 단계가 끝나면 무엇이 가능해지는지 2~4줄로 적는다.
- 관련 요구사항: FR-XXX-01, FR-XXX-02

## 2. 배경 개념
### 2.1 {개념 이름}
- 한 줄 정의:
- 비유로 이해하기:
- 왜 필요한가 (없으면 생기는 문제):
- 그림:
```text
(텍스트 다이어그램)
```

## 3. 따라하기
### 3.1 {작업 단계}
실행한 명령:
```bash
# macOS/Linux
...
```
```powershell
# Windows
...
```
만든/수정한 파일: `backend/src/main/java/...`
```java
// 핵심 코드 발췌 + 한 줄씩 설명 주석
```

## 4. 결정과 이유
| 결정 | 고려한 대안 | 선택 이유 |
|---|---|---|
| | | |

## 5. 겪은 문제와 해결
### 문제 1: {요약}
- 에러 메시지 원문:
```text
...
```
- 원인:
- 해결:
- 배운 점:

(문제가 없었다면: "초보자가 겪기 쉬운 실수" 2개 이상)

## 6. 확인 방법
```bash
./gradlew test -PdbVendor=postgresql
```
기대 출력:
```text
BUILD SUCCESSFUL
```

## 7. 회고와 다음 단계
- 잘된 점:
- 아쉬운 점 / 개선할 점:
- 다음 단계(M{n+1})에서 할 일:
````

### 22.2 환경변수 (`.env.example`)

| 변수 | 예시 | 필수 | 설명 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | O | 실행 프로필 (DB 프로필은 `DB_VENDOR`로 자동 추가) |
| `DB_VENDOR` | `postgresql` | O | `postgresql` 또는 `mysql` |
| `DB_HOST` | `localhost` | O | |
| `DB_PORT` | `5432` | O | MySQL은 `3306` |
| `DB_NAME` | `template` | O | |
| `DB_USERNAME` | `template` | O | |
| `DB_PASSWORD` | `change-me` | O | |
| `DB_POOL_SIZE` | `10` | | HikariCP 최대 풀 |
| `JWT_SECRET` | (base64 32바이트 이상) | O | 부족하면 기동 실패 |
| `JWT_ACCESS_TTL` | `PT15M` | | ISO-8601 기간 |
| `JWT_REFRESH_TTL` | `P14D` | | |
| `COOKIE_SECURE` | `false` | | prod는 `true` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | O | 쉼표 구분 |
| `ADMIN_EMAIL` | `admin@example.com` | | 있으면 초기 관리자 생성 |
| `ADMIN_PASSWORD` | `change-me-please` | | |
| `ADMIN_NICKNAME` | `관리자` | | |
| `STORAGE_TYPE` | `local` | | `local` (향후 `s3`) |
| `STORAGE_LOCAL_PATH` | `./uploads` | | 컨테이너에서는 `/app/uploads` |
| `FILE_MAX_SIZE` | `10MB` | | |
| `SWAGGER_ENABLED` | `true` | | prod는 `false` |
| `LOGIN_RATE_LIMIT` | `10` | | 5분당 허용 횟수 |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75` | | 컨테이너 JVM 옵션 |
| `VITE_API_BASE_URL` | `/api/v1` | | 프론트 빌드 시점 값 |
| `IMAGE_TAG` | `sha-1a2b3c4` | 운영 | `.env.prod` 전용 |
| `BACKEND_REPLICAS` | `1` | 운영 | |
| `DOMAIN` | `app.example.com` | 운영 | Caddy 인증서 도메인 |
| `GHCR_USER`, `GHCR_READ_TOKEN` | – | 운영 | 비공개 이미지 pull (서버 `.env.prod`) |
| `BACKUP_PATH` | `/opt/backups` | 운영 | |

### 22.3 GitHub Secrets / Variables

**Environment별(staging, production) Secrets**
| 이름 | 용도 | 설정 방법 |
|---|---|---|
| `SSH_HOST` | 배포 서버 주소 | 서버 IP 또는 도메인 |
| `SSH_PORT` | SSH 포트 | 기본 22 |
| `SSH_USER` | 배포 사용자 | `server-setup.sh`로 만든 `deploy` |
| `SSH_PRIVATE_KEY` | 접속용 개인키 | `ssh-keygen -t ed25519 -C deploy`로 만든 개인키 전체. 공개키는 서버 `~deploy/.ssh/authorized_keys`에 등록 |
| `SSH_KNOWN_HOSTS` | 호스트 검증 | `ssh-keyscan -p 22 {host}` 출력 |

**Environment별 Variables**
| 이름 | 예시 | 용도 |
|---|---|---|
| `DEPLOY_PATH` | `/opt/app-staging` | 서버의 배포 디렉터리 |
| `APP_URL` | `https://staging.example.com` | 배포 후 외부 헬스체크 URL |

**Repository 설정**
| 항목 | 설정 |
|---|---|
| `GITHUB_TOKEN` 권한 | 워크플로에서 `packages: write` 지정 (GHCR push에 별도 토큰 불필요) |
| Environments | `staging`(보호 없음), `production`(Required reviewers 지정) |
| Branch protection | main: PR 필수, CI 필수 체크 지정 |
| Dependabot | `gradle`, `npm`, `github-actions`, `docker` 주간 업데이트 |

> 서버에서 이미지를 pull할 때 쓰는 `GHCR_READ_TOKEN`(PAT, `read:packages` 권한)은 GitHub Secrets가 아니라 **서버의 `.env.prod`** 에 둔다. 패키지를 공개로 하면 필요 없다.

---

*문서 끝.*
