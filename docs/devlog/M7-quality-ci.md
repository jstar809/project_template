# M7. 품질 도구, Docker, CI

> 작성일: 2026-09-17 · 소요 시간: 약 2.5시간 · 관련 요구사항: PRD 15장(FR-QUALITY), 16.1(CI)

## 1. 이번 단계의 목표

지금까지 만든 것을 **어디서든 같은 방식으로 실행하고 검증**할 수 있게 만든다. 이 단계가 끝나면 다음이 가능해진다.

- `docker compose up` 한 번으로 백엔드·프론트·DB가 모두 뜬다(배포와 같은 형태).
- 백엔드를 여러 대로 늘려도 정상 동작한다.
- 실제 브라우저로 핵심 흐름(로그인 → 게시판 생성 → 글 → 댓글)을 자동 검증한다.
- PR을 올리면 GitHub Actions가 두 DB로 테스트를 돌린다.

## 2. 배경 개념

### 2.1 멀티스테이지 Docker 빌드
하나의 Dockerfile에 단계를 여러 개 두고, **마지막 단계만 이미지로 남긴다.**

```text
[1단계 builder]  JDK + Gradle + 소스 → jar 생성   (크다: ~700MB)
       ↓ 결과물만 복사
[2단계 runtime]  JRE + jar                        (작다: ~300MB)
```

빌드 도구는 실행에 필요 없다. 이미지가 작을수록 배포가 빠르고 공격 표면도 줄어든다.

### 2.2 레이어 캐시와 Spring Boot layered jar
도커 이미지는 명령 단위로 레이어가 쌓이고, **바뀐 레이어부터 다시 만든다.** 그래서 순서가 중요하다.

```dockerfile
COPY gradlew settings.gradle build.gradle ./   # 의존성 목록만 먼저
RUN ./gradlew dependencies                     # 여기까지 캐시 재사용
COPY src ./src                                 # 소스가 바뀌면 여기부터 다시
```

Spring Boot는 한 걸음 더 나간다. jar 안을 **자주 바뀌는 것과 아닌 것**으로 나눠주는 기능이 있다.

```text
dependencies/           라이브러리 (거의 안 바뀜)  → 캐시 잘 됨
spring-boot-loader/     로더
snapshot-dependencies/  스냅샷 의존성
application/            내 코드 (자주 바뀜)       → 이 레이어만 새로 전송
```

코드 한 줄 고쳤을 때 100MB가 아니라 수백 KB만 다시 올라간다.

### 2.3 컨테이너를 root로 실행하지 않는 이유
기본값으로 두면 컨테이너 안에서 root로 실행된다. 앱에 취약점이 생겼을 때 피해가 커진다.

```dockerfile
RUN useradd --system --create-home --uid 10001 appuser
USER appuser
```

**함정**: 볼륨을 마운트하면 그 디렉터리는 기본적으로 root 소유라 일반 사용자가 쓸 수 없다. 이미지 안에 디렉터리를 미리 만들고 소유권을 지정해두면, 도커가 새 볼륨을 만들 때 그 소유권을 가져간다(5장 문제 3).

### 2.4 nginx와 SPA
빌드된 React는 정적 파일 묶음이다. nginx가 해야 할 일은 두 가지다.

```nginx
# 1) SPA fallback: /me 로 직접 들어와도 index.html을 준다
location / { try_files $uri $uri/ /index.html; }

# 2) API는 백엔드로 전달
location /api/ { proxy_pass http://$backend_upstream; }
```

두 번째에서 `$backend_upstream` 변수를 쓰는 게 핵심이다. 주소를 직접 쓰면 nginx가 **시작할 때** 이름을 찾는데, 그때 백엔드가 아직 없으면 nginx가 죽는다. 변수로 쓰면 요청할 때마다 찾으므로 나중에 떠도 되고, 인스턴스를 늘려도 바로 반영된다.

### 2.5 수평 확장 확인
`--scale backend=2`로 같은 서비스를 두 개 띄운다. 이때 세 가지를 확인해야 한다.

| 확인 항목 | 왜 | 어떻게 |
|---|---|---|
| 요청 분산 | 둘 다 일을 하는가 | 로그인 10번 → 각 인스턴스 로그 수 확인 |
| 파일 공유 | 한쪽에 올린 파일을 다른 쪽이 보는가 | 같은 볼륨 마운트 후 `find` |
| 스케줄러 중복 | 작업이 두 번 실행되지 않는가 | `shedlock` 테이블에 락이 하나만 |

### 2.6 E2E(종단 간) 테스트가 잡는 것
단위·통합 테스트가 다 통과해도 이런 건 못 잡는다.

- nginx 설정 오류(SPA fallback, 프록시 경로)
- 빌드된 결과물에서만 나는 문제(환경변수 주입 실패 등)
- CORS, 쿠키 속성 같은 브라우저 규칙
- 라우팅과 화면 전환

그래서 **가장 중요한 한 줄기**만 실제 브라우저로 확인한다. 많이 만들면 느리고 깨지기 쉬우니 스모크(연기) 수준으로 유지한다.

### 2.7 CI 매트릭스
같은 작업을 조건만 바꿔 여러 번 돌리는 기능이다.

```yaml
strategy:
  matrix:
    db: [postgresql, mysql]
```

두 DB 지원이 이 프로젝트의 핵심 약속이므로, **CI가 매번 두 번 돌면서 그 약속을 지키는지 확인**한다. 로컬에서 한쪽만 돌려도 CI가 나머지를 잡아준다.

## 3. 따라하기

### 3.1 백엔드 Dockerfile 핵심

```dockerfile
FROM eclipse-temurin:21-jdk AS builder
...
RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre AS runtime
RUN useradd --system --create-home --uid 10001 appuser
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser
WORKDIR /app

# 레이어를 "같은 디렉터리"에 차례로 펼친다 (순서 = 캐시 효율)
COPY --from=builder --chown=appuser:appuser /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /workspace/extracted/application/ ./

HEALTHCHECK CMD curl -fsS http://localhost:${SERVER_PORT}/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar *.jar"]
```

### 3.2 compose의 full 프로필

```yaml
backend:
  profiles: ["full"]
  # container_name과 ports를 지정하지 않는다 → --scale 로 여러 개 띄울 수 있다
  environment:
    SPRING_PROFILES_ACTIVE: prod,db-${DB_VENDOR:-postgresql}
    DB_HOST: ${DB_VENDOR:-postgresql}      # 서비스 이름과 같게 맞춰 둔다
  volumes:
    - uploads:/app/uploads                 # 인스턴스끼리 파일 공유
```

### 3.3 GitHub Actions

```yaml
jobs:
  changes:      # 바뀐 영역 판별 (backend/ frontend/)
  backend:      # matrix: [postgresql, mysql] → 두 번 실행
  frontend:     # lint, format, typecheck, test, build
  docker-build: # 이미지 빌드 확인 (푸시는 CD에서)
  e2e:          # main 한정. compose 실행 후 Playwright
```

### 3.4 Playwright 스모크 테스트

```ts
test('관리자가 게시판을 만들고 글과 댓글을 남긴다', async ({ page }) => {
  await login(page)                                   // 로그인
  await page.goto('/admin/boards')                    // 게시판 생성
  await page.getByRole('button', { name: '새 게시판' }).click()
  ...
  await page.goto(`/boards/${boardSlug}/write`)       // 글 작성
  await page.locator('.tiptap').click()
  await page.keyboard.type('E2E 테스트 본문입니다.')
  ...
  await page.getByLabel('댓글 등록').fill('E2E 댓글입니다.')  // 댓글
})
```

선택자는 **화면에 보이는 것**(역할, 라벨, 텍스트)으로 쓴다. CSS 클래스로 찾으면 스타일만 바꿔도 테스트가 깨진다.

## 4. 결정과 이유

| 결정 | 고려한 대안 | 선택 이유 |
|---|---|---|
| layered jar로 레이어 분리 | fat jar 통째로 복사 | 코드만 고쳤을 때 전송량이 수백 KB로 줄어든다. |
| non-root 사용자로 실행 | 기본(root) | 컨테이너 침해 시 피해를 줄인다. |
| nginx upstream을 변수로 | 주소 직접 지정 | 백엔드보다 먼저 떠도 죽지 않고, `--scale` 에도 대응한다. |
| compose에서 backend 포트 미지정 | 8080 고정 | 포트를 고정하면 인스턴스를 늘릴 수 없다. |
| E2E는 스모크 1~2개만 | 화면별 E2E 다수 | 느리고 깨지기 쉽다. 세부 검증은 단위·통합 테스트가 맡는다. |
| E2E는 main에서만 | 모든 PR | PR 피드백 속도를 지키기 위해서다. |
| 경로 필터로 job 분기 | 항상 전체 실행 | 문서만 고쳤는데 10분씩 기다릴 이유가 없다. |
| 스케줄러 주기를 설정값으로 | 코드에 고정 | 다중 인스턴스 동작을 짧은 주기로 확인할 수 있다. |

## 5. 겪은 문제와 해결

이번 단계는 문제가 가장 많았다. **로컬에서 되던 것이 컨테이너에서 안 되는** 전형적인 사례들이다.

### 문제 1: 컨테이너가 jar를 찾지 못한다
```text
Error: Unable to access jarfile application.jar
```
- **원인**: `extract --layers` 결과를 통째로 복사했더니 `/app/application/`, `/app/dependencies/` 같은 폴더 구조가 그대로 생겼다. 실행 파일은 `/app/application/` 안에 있었다.
- **1차 시도(실패)**: jar를 `application.jar`로 이름을 바꿨더니 이번엔 다른 오류가 났다.

### 문제 2: 이름을 바꿨더니 클래스를 못 찾는다
```text
java.lang.NoClassDefFoundError: org/springframework/boot/SpringApplication
```
- **원인 추적**: jar 안의 매니페스트를 열어봤다.

```text
Main-Class: com.example.template.TemplateApplication
Class-Path: lib/flyway-database-postgresql-12.4.0.jar lib/... (126개)
```

`Class-Path`가 **자기 옆의 `lib/`** 를 가리킨다. 그런데 라이브러리는 `dependencies/lib/`에 있었다. 폴더 구조를 유지한 채 복사한 게 근본 원인이었다.
- **해결**: 레이어를 각각 **같은 디렉터리**에 펼친다. 그러면 `/app/lib/`와 `/app/*.jar`가 나란히 놓여 경로가 맞는다. 이름도 바꾸지 않는다.

```dockerfile
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./
```
- **배운 점**: 오류 메시지만 보고 고치면 겉만 바뀐다. **매니페스트를 열어 실제 기대 경로를 확인**하니 바로 풀렸다.

### 문제 3: 파일 업로드가 Permission denied
```text
touch: cannot touch '/app/uploads/shared-test.txt': Permission denied
```
- **원인**: 컨테이너를 uid 10001로 실행하는데, 도커가 만든 빈 볼륨은 root 소유였다.
- **해결**: 이미지 안에 디렉터리를 미리 만들고 소유권을 지정했다. 도커는 **비어 있는 새 볼륨을 만들 때 이미지 쪽 디렉터리의 소유권과 내용을 복사**한다.

```dockerfile
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
```
기존 볼륨은 이미 root 소유라 `docker volume rm template_uploads`로 지우고 다시 만들었다.
- **배운 점**: non-root 실행과 볼륨은 항상 같이 확인해야 한다. 이 버그는 **업로드를 실제로 해보기 전까지 드러나지 않았다.**

### 문제 4: nginx가 시작하자마자 죽는다
```text
[emerg] host not found in upstream "backend"
```
- **원인**: nginx는 설정에 적힌 upstream 이름을 **시작할 때** 해석한다. 그 순간 backend가 아직 준비되지 않으면 그대로 종료된다.
- **해결**: 변수로 바꾸고 도커 내부 DNS를 지정했다.

```nginx
resolver 127.0.0.11 valid=10s ipv6=off;
location /api/ {
    set $backend_upstream backend:8080;
    proxy_pass http://$backend_upstream;
}
```
- **덤**: 이 방식은 `--scale backend=2`로 늘어난 인스턴스도 곧바로 분배한다.

### 문제 5: DB 호스트를 찾지 못한다
```text
java.net.UnknownHostException: postgresql
```
- **원인**: compose 서비스 이름은 `postgres`인데 `DB_VENDOR` 값은 `postgresql`이었다. 백엔드는 `DB_HOST=${DB_VENDOR}`로 접속하니 이름이 어긋났다.
- **해결**: 서비스 이름을 `postgresql`로 바꿔 둘을 일치시켰다. 설정 하나로 DB를 바꾸는 구조를 유지하려면 이름 규칙이 같아야 한다.

### 문제 6: 브라우저에서만 로그인이 403
curl로는 200인데 Playwright(실제 브라우저)에서는 403이었다.

```text
POST /api/v1/auth/login -> 403
Invalid CORS request
```
- **원인 추적**: 브라우저는 POST에 `Origin` 헤더를 붙인다. curl에 같은 헤더를 붙이니 재현됐다.

```bash
curl -X POST .../auth/login -H 'Origin: http://localhost:8081'   # → Invalid CORS request
```

두 가지가 겹쳤다.
1. `.env`의 허용 출처가 `http://localhost:5173`(개발용)뿐이었다. 도커 실행 주소 8081이 빠져 있었다.
2. nginx 뒤에 있어서 서버가 자기 주소를 `localhost:8080`으로 알고 있었다. 그래서 같은 사이트에서 온 요청도 "다른 출처"로 봤다.
- **해결**: 두 가지를 모두 고쳤다.

```yaml
server:
  forward-headers-strategy: framework   # X-Forwarded-* 를 반영해 원래 주소를 인식
```
```properties
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:8081
```
- **배운 점**: 리버스 프록시 뒤에 서면 서버가 "자기가 누구인지"를 잘못 안다. 배포 형태로 한 번은 돌려봐야 드러나는 문제다.

### 문제 7: 테스트가 로그인 제한에 걸렸다
E2E가 처음엔 로그인 화면에서 멈췄다. 원인은 내가 앞선 확인 작업에서 curl로 로그인을 여러 번 한 것이었다. **5분에 10회** 제한(M2)에 걸린 것이다.
- **해결**: compose에 `LOGIN_RATE_LIMIT`를 넘길 수 있게 하고, 로컬 `.env`에서는 넉넉히 뒀다. 기본값(10)은 템플릿 그대로 유지한다.
- **덤**: 이때 테스트의 검증이 느슨한 것도 드러났다. 로그인 성공 여부를 "헤더 링크가 보이는가"로 확인했는데, 그 링크는 로그인 화면에도 있었다. **닉네임 버튼이 보이는지**로 바꿔 실패가 곧바로 드러나게 했다.

### 문제 8: Vitest가 Playwright 파일을 실행하려 한다
```text
Error: Playwright Test did not expect test() to be called here.
```
- **원인**: Vitest가 기본적으로 모든 테스트 파일을 찾는데, `e2e/`의 Playwright 파일까지 포함했다.
- **해결**: Vitest 범위를 `src/` 로 한정했다.

```ts
test: { include: ['src/**/*.{test,spec}.{ts,tsx}'] }
```

## 6. 확인 방법

### 전체 도커 실행

```bash
docker compose --profile full --profile postgresql up -d --build
curl http://localhost:8081/actuator/health     # {"status":"UP"}
curl http://localhost:8081/api/v1/boards       # 게시판 목록
```

### 수평 확장 (실제 확인 결과)

```bash
docker compose --profile full --profile postgresql up -d --scale backend=2
```

```text
SERVICE      NAME                    STATUS
backend      template-backend-1      Up (healthy)
backend      template-backend-2      Up (healthy)

# 1) 요청 분산: 로그인 10회
backend-1 처리: 6건
backend-2 처리: 4건

# 2) 스케줄러 중복 실행 방지 (shedlock 테이블)
       name       |  locked_by   |        locked_at        |       lock_until
 cleanupTempFiles | 6cc59b7f4bfe | 2026-09-17 01:28:00.007 | 2026-09-17 01:29:00.007
(1 row)   ← 한 인스턴스만 락을 잡았다

# 3) 업로드 파일 공유
인스턴스 1: /app/uploads/2026/09/17/4c4daac9-....png
인스턴스 2: /app/uploads/2026/09/17/4c4daac9-....png   ← 같은 파일
```

### E2E

```bash
cd frontend && npm run e2e
```

```text
  ✓  1 [chromium] › e2e\smoke.spec.ts:31:1 › 관리자가 게시판을 만들고 글과 댓글을 남긴다 (4.4s)
  ✓  2 [chromium] › e2e\smoke.spec.ts:64:1 › 비로그인 사용자는 글쓰기 화면에 들어갈 수 없다 (297ms)

  2 passed (5.4s)
```

### 전체 검사

```bash
cd backend && ./gradlew test -PdbVendor=postgresql && ./gradlew test -PdbVendor=mysql
cd ../frontend && npm run lint && npm run typecheck && npm run test && npm run build
```

### CI (사용자 확인 필요)
GitHub에 push하거나 PR을 열면 Actions 탭에서 job들이 실행된다. main 브랜치에 push하면 e2e job까지 돈다.

## 7. 회고와 다음 단계

**잘된 점**
- "실제로 띄워서 확인한다"는 원칙 덕분에 **배포에서야 드러났을 버그 4개**(jar 경로, 볼륨 권한, nginx 기동 순서, CORS)를 여기서 잡았다.
- 수평 확장 약속(PRD SCALE-01~03)을 말이 아니라 **측정 결과로** 확인했다.
- E2E가 처음부터 실패하면서 오히려 테스트 자체의 약한 검증을 드러냈다.

**아쉬운 점 / 개선할 점**
- CI는 아직 GitHub에서 실제로 돌려보지 못했다(push 후 확인 필요).
- 목록 API 성능(PRD NFR-PERF-03: 1만 건에서 p95 200ms)을 측정하지 못했다. 데이터 생성 스크립트가 필요하다.
- 이미지 크기를 줄일 여지가 있다(distroless, jlink 커스텀 런타임). 지금은 명확성을 우선했다.

**다음 단계 (M8)**
- GHCR에 이미지 푸시, SSH로 서버 배포
- 헬스체크 실패 시 자동 롤백
- 서버 준비 스크립트, 백업, 운영 문서
