# 결정 기록 (Decisions)

PRD에 없거나 PRD와 다르게 결정한 사항을 기록한다. 형식: 날짜 / 결정 / 대안 / 이유.

---

## 2026-09-16 · M0

### D-001. 라이브러리 버전 확정
구현 시작 시점(2026-09-16)에 조회한 최신 안정 버전으로 확정했다.

**백엔드**

| 항목 | 확정 버전 | 비고 |
|---|---|---|
| JDK | 21 (Temurin/Oracle 21.0.11) | PRD 기준 |
| Spring Boot | 4.1.1 | 4.2.0-M1은 마일스톤이라 제외 |
| Gradle | 9.7.1 (wrapper) | Groovy DSL |
| MyBatis Spring Boot Starter | 4.1.0 | Spring Boot 4.1 대응 버전 |
| springdoc-openapi | 3.1.1 | Boot 4 대응 |
| ShedLock | 7.10.1 | |
| JJWT | 0.13.0 | |
| OWASP Java HTML Sanitizer | 20260313.1 | |
| Bucket4j | 8.19.0 (`bucket4j_jdk17-core`) | |
| Apache Tika | 4.0.0 | 파일 MIME 검사 |
| logstash-logback-encoder | 9.0 | prod JSON 로그 |
| ArchUnit | 1.5.0 | |
| Spotless | 8.10.2 (palantir-java-format) | |
| PostgreSQL | 17 (컨테이너 17-alpine, 실측 17.11) | |
| MySQL | 8.4 LTS | |

**프론트엔드**

| 항목 | 확정 버전 |
|---|---|
| Node.js | 22 LTS (로컬 22.22.3) |
| Vite | 8.3 |
| React | 19.2 |
| TypeScript | 6.0 |
| Tailwind CSS | 4.3 |
| shadcn/ui | CLI 4.21 (radix base, nova preset) |
| ESLint | 10.10 + typescript-eslint 8.70 |
| Prettier | 3.9 |

### D-002. Node는 24가 아니라 22 LTS를 쓴다
- **PRD 기준**: Node 24 LTS
- **결정**: Node 22 LTS로 통일한다. `package.json`의 `engines.node`는 `>=22.12.0`, `.nvmrc`는 `22`로 맞췄다. M7의 Dockerfile과 CI도 22로 맞춘다.
- **이유**: 사용자의 로컬 환경이 22이고, 개발·CI·배포 버전이 어긋나면 "로컬에선 되는데 CI에서 깨지는" 문제가 생긴다. Node 22는 2027년 4월까지 지원되는 LTS이며 Vite 8, Vitest 5, ESLint 10의 요구 조건(>=22.12)을 모두 만족한다.

### D-003. TypeScript는 7이 아니라 6을 쓴다
- **결정**: TypeScript 6.0을 쓴다.
- **이유**: 최신은 7.0이지만 `typescript-eslint` 8.70(최신 안정)의 peer 범위가 `>=4.8.4 <6.1.0`이라 TS 7에서는 린트가 동작하지 않는다. `typescript-eslint`가 TS 7을 지원하면 올린다.

### D-004. 프론트 린터는 oxlint 대신 ESLint를 쓴다
- **배경**: `create-vite` 9부터 기본 린터가 oxlint로 바뀌어, 스캐폴딩 직후 oxlint가 설치되어 있었다.
- **결정**: PRD 기준대로 ESLint 10 + typescript-eslint로 교체했다(oxlint 제거).
- **이유**: PRD에 ESLint flat config로 명시되어 있고, 자료와 플러그인 생태계가 넓다. 속도가 문제가 되면 나중에 oxlint 병행을 검토한다.

### D-005. `eslint-plugin-jsx-a11y`는 지금 넣지 않는다
- **이유**: 최신 6.10.2가 아직 ESLint 10을 peer로 지원하지 않아 설치가 실패한다(ESLint 9까지 지원).
- **후속**: 플러그인이 ESLint 10을 지원하면 추가한다. 접근성 검사는 그때까지 수동 확인한다.

### D-006. Spring Initializr에서 MyBatis 의존성은 직접 추가한다
- **배경**: `start.spring.io`가 "Dependency 'mybatis' is not compatible with Spring Boot 4.1.1"로 400을 반환했다.
- **확인**: `mybatis-spring-boot-starter:4.1.0`의 부모 POM은 `spring-boot.version=4.1.0`이라 Boot 4.1과 호환된다. Initializr 메타데이터가 아직 갱신되지 않은 것으로 보인다.
- **결정**: MyBatis 없이 프로젝트를 생성한 뒤 `build.gradle`에 `mybatis-spring-boot-starter:4.1.0`을 직접 추가했다. 실제 쿼리 동작은 M1에서 두 DB로 검증한다.

### D-007. 로컬 포트 충돌 회피 (이 PC 한정)
- **배경**: 8080은 다른 Java 앱이, 3306은 XAMPP MySQL이 이미 사용 중이었다.
- **결정**: `.env`(커밋하지 않음)에서 `MYSQL_PORT=3307`을 쓴다. 백엔드 포트는 기본 8080을 유지하되, 충돌 시 `SERVER_PORT`로 바꾼다. M0 검증은 8081(PostgreSQL)과 8082(MySQL)에서 수행했다.
- **이유**: 사용자의 기존 프로세스를 건드리지 않기 위해서다. 템플릿 기본값(`.env.example`)은 8080/3306을 유지한다.

### D-008. 관리자 계정은 SQL 시드가 아니라 기동 시 코드로 생성한다
- **PRD 기준**: `V2__seed.sql`에서 기본 게시판과 함께 시드
- **결정**: `V2__seed.sql`은 기본 게시판만 넣고, 관리자 계정은 M2에서 `AdminAccountInitializer`(ApplicationRunner)가 환경변수를 읽어 생성한다.
- **이유**: 비밀번호를 BCrypt로 해시해야 하는데 고정 SQL로는 환경변수 기반 해시를 만들 수 없다. PRD 11.4에 반영했다.


---

## 2026-09-16 · M4

### D-009. MyBatis Mapper가 application 계층 DTO를 반환하도록 허용
- **배경**: PRD MB-03은 "MyBatis 결과는 application 계층의 조회 전용 DTO로 받는다"이고, PRD 7장 구조는 Mapper 인터페이스를 `infra/mybatis`에 둔다. 이 둘을 합치면 `infra → application` 의존이 생겨 ARCH-01(계층 규칙)에 걸린다.
- **결정**: ArchUnit 규칙에서 `application` 계층 접근 허용 목록에 `infra`를 추가했다.
- **이유**: DTO를 domain으로 내리면 화면용 조회 모델이 도메인에 섞인다. 다른 도메인 침범은 ARCH-04가 별도로 막고 있어 실질적 위험이 없다.

### D-010. 긴 텍스트 컬럼은 length로 매핑한다
- **배경**: `@JdbcTypeCode(SqlTypes.LONG32VARCHAR)`는 PostgreSQL에서는 통과했지만 MySQL에서 `expecting tinytext (CLOB)` 오류가 났다.
- **결정**: `@Column(length = Integer.MAX_VALUE)`로 매핑한다. Hibernate가 길이를 보고 PostgreSQL은 `text`, MySQL은 `longtext`를 기대하게 된다.
- **이유**: 벤더 전용 `columnDefinition`을 쓰지 않으면서 두 DB에서 validate를 통과하는 방법이다(PRD DB-04 준수).

### D-011. 공개 읽기 API는 인증 없이 허용한다
- **결정**: `GET /api/v1/boards/**`, `/api/v1/posts/**`, `/api/v1/files/**`는 `permitAll`로 두고, 실제 접근 범위는 게시판 권한(서비스 계층)이 판단한다.
- **이유**: 공개 게시판은 비로그인도 읽을 수 있어야 한다. 읽기 권한이 USER/ADMIN인 게시판은 서비스에서 403을 던지므로 보안이 유지된다.

### D-012. 통합 테스트는 컨테이너를 공유하고 고유 값으로 격리한다
- **배경**: 컨테이너를 JVM당 하나만 띄워 재사용하므로 테스트 간 데이터가 누적된다.
- **결정**: 각 테스트는 고유한 slug와 이메일을 만들어 자기 데이터만 검증한다. 시드 데이터 검증은 "N개 이상"으로 한다.
- **이유**: 테스트마다 DB를 초기화하면 느려지고, REQUIRES_NEW 트랜잭션과도 충돌한다.
