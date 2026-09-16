# M0. 레포 스캐폴딩과 개발환경

> 작성일: 2026-09-16 · 소요 시간: 약 1시간 · 관련 요구사항: PRD 6장(기술 스택), 7장(레포 구조), 14장(개발 환경)

## 1. 이번 단계의 목표

빈 폴더에서 시작해 **백엔드와 프론트엔드가 각각 실행되는 상태**까지 만든다. 기능은 아직 없다. 이 단계가 끝나면 다음이 가능해진다.

- `docker compose`로 PostgreSQL 또는 MySQL을 띄운다.
- 백엔드가 그 DB에 붙어서 기동하고, 상태 확인 주소가 응답한다.
- 프론트 개발 서버가 뜨고, 린트·타입검사·빌드가 통과한다.

## 2. 배경 개념

### 2.1 모노레포(Monorepo)
- **한 줄 정의**: 백엔드와 프론트엔드처럼 서로 다른 프로젝트를 **하나의 git 저장소**에 두고 관리하는 방식.
- **비유**: 집을 지을 때 설계도, 자재, 공구를 창고 한 곳에 모아두는 것과 같다. 창고가 여러 개면 "어느 창고의 설계도가 최신이지?"를 늘 확인해야 한다.
- **왜 필요한가**: 저장소가 나뉘면 API를 바꿀 때 백엔드와 프론트를 각각 커밋해야 하고, 두 저장소의 버전이 어긋나 "프론트는 새 API를 부르는데 백엔드는 아직 옛날 버전"인 상황이 생긴다. 한 저장소면 한 커밋에 양쪽 변경이 같이 들어가고, CI도 한 번에 돌릴 수 있다.
- **구조**

```text
firstproject/
├── backend/    ← Spring Boot
├── frontend/   ← React
├── docs/       ← 문서
└── docker-compose.yml  ← 둘이 함께 쓰는 DB
```

### 2.2 Gradle Wrapper와 Groovy DSL
- **Gradle**: 자바 프로젝트의 빌드 도구. 라이브러리를 내려받고, 컴파일하고, 테스트하고, 실행 파일(jar)을 만든다.
- **Wrapper(`gradlew`)**: 저장소에 들어 있는 작은 실행 스크립트. 처음 실행하면 **프로젝트가 지정한 버전의 Gradle을 자동으로 내려받아** 그 버전으로 빌드한다.
  - 왜 필요한가: 각자 PC에 설치된 Gradle 버전이 다르면 누군가의 PC에서만 빌드가 깨진다. Wrapper를 쓰면 모두 같은 버전(여기선 9.7.1)을 쓴다. 그래서 Gradle을 따로 설치할 필요도 없다.
  - Windows는 `gradlew.bat`, macOS/Linux는 `./gradlew`를 쓴다.
- **Groovy DSL vs Kotlin DSL**: 빌드 설정 파일을 쓰는 두 가지 문법이다.
  - Groovy DSL → `build.gradle`
  - Kotlin DSL → `build.gradle.kts`
  - 이 프로젝트는 **Groovy DSL만** 쓴다(PRD 비목표). 하는 일은 같고 문법만 다르다.

### 2.3 Vite
- **한 줄 정의**: 프론트엔드 개발 서버 겸 빌드 도구.
- **왜 필요한가**: 브라우저는 TypeScript나 JSX를 그대로 이해하지 못한다. 누군가 이를 자바스크립트로 바꿔줘야 한다. Vite가 그 일을 한다.
- **두 가지 모드**
  - 개발(`npm run dev`): 파일을 저장하면 바뀐 부분만 즉시 브라우저에 반영한다(HMR). 전체를 다시 빌드하지 않아 빠르다.
  - 빌드(`npm run build`): 배포용으로 전부 묶어 `dist/` 폴더에 정적 파일을 만든다. 이 폴더를 nginx 같은 웹서버가 서비스한다.
- **프록시**: 개발 중에는 프론트가 5173, 백엔드가 8080 포트를 쓴다. 포트가 다르면 브라우저가 CORS로 막는다. 그래서 Vite 설정에 "`/api`로 시작하는 요청은 8080으로 대신 전달"하도록 적어둔다.

```text
브라우저 → localhost:5173/api/v1/posts → (Vite 프록시) → localhost:8080/api/v1/posts
```

### 2.4 Docker Compose 프로필
- **Compose**: 여러 컨테이너(DB, 백엔드, 프론트)를 YAML 파일 하나로 정의하고 한 번에 띄우는 도구.
- **프로필(profile)**: 서비스에 이름표를 붙여 **필요한 것만 골라 띄우는** 기능.
  - 이 프로젝트는 PostgreSQL과 MySQL을 모두 지원하지만, 개발할 때는 보통 하나만 필요하다.
  - `docker compose --profile postgresql up -d` → postgres만 실행
  - `docker compose --profile mysql up -d` → mysql만 실행
- **볼륨(volume)**: 컨테이너를 지워도 DB 데이터가 남도록 데이터를 컨테이너 밖에 저장하는 공간이다.
- **헬스체크**: DB가 "실행 중"인 것과 "접속을 받을 준비가 된 것"은 다르다. 헬스체크는 실제 접속 가능 상태를 주기적으로 확인해준다.

## 3. 따라하기

### 3.1 백엔드 프로젝트 생성
Spring Initializr(start.spring.io)를 명령줄로 호출해 프로젝트 뼈대를 받는다.

```bash
curl -G "https://start.spring.io/starter.zip" \
  --data-urlencode "type=gradle-project" \
  --data-urlencode "language=java" \
  --data-urlencode "bootVersion=4.1.1" \
  --data-urlencode "javaVersion=21" \
  --data-urlencode "groupId=com.example" \
  --data-urlencode "artifactId=template" \
  --data-urlencode "packageName=com.example.template" \
  --data-urlencode "dependencies=web,security,validation,data-jpa,actuator,flyway,postgresql,mysql,lombok,configuration-processor,testcontainers" \
  -o backend.zip
```

- `type=gradle-project`가 **Groovy DSL**이다. Kotlin DSL은 `gradle-project-kotlin`이므로 헷갈리지 않게 주의한다.
- 압축을 풀어 `backend/`에 넣는다.

### 3.2 build.gradle에 라이브러리 추가
Initializr가 만들어준 기본 의존성에 이 프로젝트에 필요한 것들을 더했다.

```groovy
ext {
    mybatisVersion = '4.1.0'      // 조회 전용 SQL 매퍼
    springdocVersion = '3.1.1'    // Swagger 문서
    shedlockVersion = '7.10.1'    // 스케줄러 분산 락
    jjwtVersion = '0.13.0'        // JWT 토큰
    // ...
}

dependencies {
    implementation "org.mybatis.spring.boot:mybatis-spring-boot-starter:${mybatisVersion}"
    implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}"
    // ...
}

// 테스트 대상 DB 선택: ./gradlew test -PdbVendor=mysql
ext.dbVendor = (project.findProperty('dbVendor') ?: 'postgresql').toString()

tasks.named('test') {
    useJUnitPlatform()
    systemProperty 'db.vendor', dbVendor   // M1의 테스트 베이스가 이 값을 읽는다
}
```

`-PdbVendor=...`는 Gradle에 값을 넘기는 방법이다. 이 값을 시스템 속성으로 넘겨두면, 나중에 테스트 코드가 "이번엔 어떤 DB로 테스트할지" 알 수 있다.

### 3.3 설정 파일을 DB별로 나누기
`application.yml`에는 DB와 무관한 공통 설정만 두고, DB마다 다른 부분은 프로필 파일로 나눈다.

```yaml
# application-db-postgresql.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:template}
    driver-class-name: org.postgresql.Driver
  flyway:
    locations: classpath:db/migration/postgresql
```

```yaml
# application-db-mysql.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:template}?connectionTimeZone=UTC&characterEncoding=UTF-8
    driver-class-name: com.mysql.cj.jdbc.Driver
  flyway:
    locations: classpath:db/migration/mysql
```

- `${DB_HOST:localhost}`는 "환경변수 `DB_HOST`가 있으면 그 값, 없으면 `localhost`"라는 뜻이다.
- 실행할 때 `local,db-postgresql` 프로필을 켜면 두 파일(`application-local.yml`, `application-db-postgresql.yml`)이 함께 적용된다.
- MySQL URL의 `connectionTimeZone=UTC`는 시간대 문제를 막기 위한 것이다. 모든 시간을 UTC로 저장하기로 했기 때문이다(PRD DB-05).

### 3.4 프론트엔드 프로젝트 생성

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install tailwindcss @tailwindcss/vite clsx tailwind-merge class-variance-authority lucide-react
npm install -D eslint @eslint/js typescript-eslint eslint-plugin-react-hooks eslint-plugin-react-refresh prettier eslint-config-prettier globals
npx shadcn@latest init -b radix -p nova -y
npx shadcn@latest add button card -y
```

- Tailwind v4부터는 설정 파일(`tailwind.config.js`) 없이 **Vite 플러그인 + CSS의 `@import 'tailwindcss'`** 만으로 동작한다.
- shadcn/ui는 라이브러리가 아니라 **컴포넌트 코드를 내 프로젝트에 복사해 넣는 방식**이다. 그래서 `src/components/ui/button.tsx`가 실제 파일로 생기고, 마음대로 고칠 수 있다.

Vite 설정에 경로 별칭과 프록시를 넣었다.

```ts
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },   // import { x } from '@/lib/utils'
  },
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
  },
})
```

### 3.5 Docker Compose

```yaml
services:
  postgres:
    image: postgres:17-alpine
    profiles: ["postgresql"]          # 이 프로필일 때만 뜬다
    ports: ["${POSTGRES_PORT:-5432}:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-template} -d ${DB_NAME:-template}"]
```

`${POSTGRES_PORT:-5432}`는 `.env` 파일의 값을 쓰되 없으면 5432를 쓴다는 뜻이다. 포트가 겹칠 때 `.env`만 고치면 된다.

### 3.6 실행 스크립트
Windows에서 매번 긴 명령을 치지 않도록 `scripts/*.ps1`을, macOS/Linux용으로 `Makefile`을 만들었다.

```powershell
.\scripts\db-up.ps1 -Db postgresql
.\scripts\be-run.ps1
.\scripts\fe-run.ps1
```

## 4. 결정과 이유

| 결정 | 고려한 대안 | 선택 이유 |
|---|---|---|
| Node 22 LTS | Node 24 LTS(PRD 기준) | 로컬이 22였다. 개발·CI·Docker 버전을 하나로 맞추는 것이 "로컬에선 되는데 CI에선 깨지는" 문제를 막는다. 22도 2027년 4월까지 지원되는 LTS다. |
| TypeScript 6 | TypeScript 7(최신) | 최신 `typescript-eslint`가 아직 TS 7을 지원하지 않아 린트가 동작하지 않는다. |
| ESLint | oxlint (create-vite 9 기본값) | PRD 기준이고 자료와 플러그인이 많다. 속도가 문제되면 나중에 재검토한다. |
| MyBatis를 build.gradle에 직접 추가 | Initializr 의존성으로 추가 | Initializr가 Boot 4.1과 호환되지 않는다고 거부했지만, 실제 라이브러리는 호환된다(5장 참고). |
| 백엔드 포트 8080 유지 | 8081로 변경 | 8080 충돌은 이 PC 사정이라 템플릿 기본값은 표준대로 둔다. `.env`의 `SERVER_PORT`로 바꿀 수 있게 했다. |

전체 결정 기록은 [decisions.md](../decisions.md)에 있다.

## 5. 겪은 문제와 해결

### 문제 1: Initializr가 MyBatis를 거부했다
```text
{"status":400,"error":"Bad Request",
 "message":"Dependency 'mybatis' is not compatible with Spring Boot 4.1.1"}
```
- **원인**: Initializr의 호환성 메타데이터가 아직 갱신되지 않았다. 실제로 `mybatis-spring-boot-starter:4.1.0`의 부모 POM을 열어보니 `<spring-boot.version>4.1.0</spring-boot.version>`이라 Boot 4.1과 호환된다.
- **해결**: MyBatis 없이 프로젝트를 생성하고 `build.gradle`에 직접 추가했다. 빌드가 성공했고, 실제 쿼리 동작은 M1에서 검증한다.
- **배운 점**: 도구가 "호환 안 됨"이라고 해도 근거를 직접 확인해볼 가치가 있다. 라이브러리의 POM을 보면 어떤 버전을 기준으로 만들어졌는지 알 수 있다.

### 문제 2: 포트 8080이 이미 사용 중
```text
Description:
Web server failed to start. Port 8080 was already in use.
```
- **원인**: 이 PC에서 다른 Java 앱(PID 8112)이 8080을 쓰고 있었다.
- **확인 방법 (PowerShell)**
```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen |
  ForEach-Object { Get-Process -Id $_.OwningProcess | Select-Object Id, ProcessName, Path }
```
- **해결**: 남의 프로세스를 죽이지 않고, 검증할 때만 `SERVER_PORT=8081`로 띄웠다.

### 문제 3: MySQL 포트 3306도 이미 사용 중
```text
Error response from daemon: ports are not available: exposing port TCP 0.0.0.0:3306
```
- **원인**: XAMPP의 MySQL이 3306을 쓰고 있었다.
- **해결**: `.env`에 `MYSQL_PORT=3307`로 두고, 접속할 때 `DB_PORT=3307`을 쓴다. compose 파일을 고칠 필요가 없도록 포트를 환경변수로 빼둔 덕분이다.

### 문제 4: TypeScript 6에서 `baseUrl`이 에러
```text
error TS5101: Option 'baseUrl' is deprecated and will stop functioning in TypeScript 7.0.
```
- **원인**: `@/` 경로 별칭을 쓰려고 `baseUrl`과 `paths`를 넣었는데, TS 6부터 `baseUrl`이 폐기 예정이 되었다.
- **해결**: `baseUrl`을 지우고 `paths`만 남겼다. 요즘 TypeScript는 `paths`를 tsconfig 파일 위치 기준으로 해석하므로 `baseUrl` 없이도 동작한다.

### 문제 5: `eslint-plugin-jsx-a11y` 설치 실패
```text
npm error ERESOLVE could not resolve
npm error Conflicting peer dependency: eslint@9.39.5
```
- **원인**: 접근성 검사 플러그인이 아직 ESLint 10을 지원하지 않는다.
- **해결**: 이 플러그인만 빼고 설치했다. `--force`나 `--legacy-peer-deps`로 억지로 넣으면 나중에 이상한 오류가 나므로 쓰지 않았다.

### 문제 6: shadcn CLI 옵션이 바뀌어 있었다
```text
Validation failed:
- base: Invalid enum value. Expected 'radix' | 'base' | 'aria', received 'neutral'
```
- **원인**: 예전에는 `-b`가 색상(base color)이었는데, 지금은 컴포넌트 기반 라이브러리(radix/base/aria)를 고르는 옵션으로 바뀌었다.
- **해결**: `npx shadcn@latest init --help`로 현재 옵션을 확인한 뒤 `-b radix -p nova -y`로 실행했다.
- **배운 점**: CLI 도구는 메이저 버전에서 옵션이 자주 바뀐다. 블로그 글보다 `--help`가 정확하다.

## 6. 확인 방법

```bash
# 1) Kotlin 파일이 없는지 (0이어야 한다)
git ls-files | grep -c "\.kts$"

# 2) 백엔드 빌드
cd backend && ./gradlew build -x test
# → BUILD SUCCESSFUL

# 3) PostgreSQL로 기동
docker compose --profile postgresql up -d
cd backend && ./gradlew bootRun
curl http://localhost:8080/actuator/health
# → {"groups":["liveness","readiness"],"status":"UP"}

# 4) MySQL로 기동 (환경변수만 바꾼다)
docker compose --profile mysql up -d
DB_VENDOR=mysql DB_PORT=3307 ./gradlew bootRun
# 로그: Database: jdbc:mysql://localhost:3307/template (MySQL 8.4)

# 5) 프론트
cd frontend && npm run lint && npm run typecheck && npm run build
# → dist/ 생성, 에러 0
```

실제 실행 결과 요약

```text
PostgreSQL: Database: jdbc:postgresql://localhost:5432/template (PostgreSQL 17.11)
            Started TemplateApplication in 4.03 seconds
MySQL     : Database: jdbc:mysql://localhost:3307/template (MySQL 8.4)
            Started TemplateApplication in 5.99 seconds
health    : {"groups":["liveness","readiness"],"status":"UP"}
swagger   : HTTP 200 (/swagger-ui.html)
frontend  : built in 702ms (index.js 254 kB, gzip 82 kB)
```

## 7. 회고와 다음 단계

**잘된 점**
- 환경변수로 포트와 DB를 빼둔 덕분에, 포트 충돌 두 건을 설정 파일 수정 없이 넘겼다.
- 시작 단계에서 PostgreSQL과 MySQL 양쪽 기동을 모두 확인해, "나중에 DB를 바꾸려다 크게 고치는" 위험을 줄였다.

**아쉬운 점 / 개선할 점**
- 아직 Flyway 마이그레이션이 없어 DB 연결만 확인한 수준이다. 실제 스키마 호환성은 M1에서 확인해야 한다.
- 접근성 린트(jsx-a11y)가 빠져 있다. 플러그인이 ESLint 10을 지원하면 넣는다.

**다음 단계 (M1)**
- 계층 구조와 공통 인프라(에러 응답, 로깅, traceId)
- Flyway 마이그레이션을 두 DB에 같은 번호로 작성
- MyBatis 설정과 DB별 SQL 분기, ArchUnit으로 아키텍처 규칙 자동 검사
