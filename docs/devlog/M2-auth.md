# M2. 인증과 사용자

> 작성일: 2026-09-16 · 소요 시간: 약 2시간 · 관련 요구사항: PRD 9.1(FR-AUTH), 9.2(FR-USER)

## 1. 이번 단계의 목표

회원가입부터 로그인, 토큰 재발급, 로그아웃까지 동작하게 만든다. 이 단계가 끝나면 다음이 가능해진다.

- 사용자가 가입하고 로그인해 Access Token을 받는다.
- Access Token이 만료되어도 Refresh Token으로 다시 받을 수 있다.
- 토큰이 탈취되면 서버가 눈치채고 그 사용자의 토큰을 전부 끊는다.
- 서버를 처음 띄우면 환경변수로 지정한 관리자 계정이 자동으로 만들어진다.
- 로그인 시도가 너무 많으면 잠시 막는다.

## 2. 배경 개념

### 2.1 세션 방식 vs 토큰(JWT) 방식
- **세션 방식**: 로그인하면 서버가 "세션"을 메모리에 저장하고, 브라우저에는 세션 ID만 준다. 서버가 상태를 갖는다.
  - 문제: 서버를 2대로 늘리면 A서버에서 로그인한 사용자가 B서버로 요청할 때 세션이 없다. (해결하려면 Redis 같은 공용 저장소가 필요하다.)
- **토큰 방식**: 로그인하면 서버가 **서명된 문자열(JWT)** 을 준다. 요청마다 이 문자열을 보내면, 서버는 서명만 검증하면 된다. 저장할 게 없다.
  - 그래서 서버를 몇 대로 늘려도 문제가 없다(PRD SCALE-01).

```text
[세션]  브라우저: 세션ID  ←→  서버 메모리: {세션ID → 사용자}   (서버가 기억)
[JWT ]  브라우저: 토큰(사용자 정보 + 서명)  ←→  서버: 서명만 검증  (서버는 기억 안 함)
```

### 2.2 JWT의 구조
JWT는 점 두 개로 나뉜 세 부분이다.

```text
eyJhbGciOiJIUzM4NCJ9 . eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIn0 . 0JxX5kUMumWrIohezuOa...
└─ 헤더(알고리즘)      └─ 내용(사용자 id, 역할, 만료)        └─ 서명
```

- 앞 두 부분은 **암호화가 아니라 인코딩**이다. 누구나 열어볼 수 있다. → 비밀번호 같은 걸 넣으면 안 된다.
- 서명은 서버만 아는 비밀 키로 만든다. 내용을 한 글자라도 바꾸면 서명이 맞지 않아 위조가 드러난다.

### 2.3 Access Token과 Refresh Token을 나누는 이유
- **Access Token**(15분): 실제 API 호출에 쓴다. 짧게 만든다. 탈취돼도 15분 뒤에는 쓸모없다.
- **Refresh Token**(14일): Access Token이 만료됐을 때 새로 받기 위한 것. 길게 만든다.

왜 나누나? 하나로 길게 만들면 탈취당했을 때 오래 악용된다. 짧게만 만들면 사용자가 15분마다 다시 로그인해야 한다. 둘로 나누면 "짧은 노출 + 편한 사용"을 동시에 얻는다.

| | 저장 위치 | 이유 |
|---|---|---|
| Access Token | 브라우저 **메모리**(JS 변수) | localStorage에 두면 XSS 공격 스크립트가 훔쳐갈 수 있다 |
| Refresh Token | **httpOnly 쿠키** | 자바스크립트가 아예 읽지 못한다. 브라우저가 알아서 보낸다 |

### 2.4 Rotation과 재사용 탐지
- **Rotation(교체)**: 재발급할 때마다 Refresh Token을 새것으로 바꾸고 옛것은 폐기한다.
- **재사용 탐지**: 이미 폐기된 토큰이 다시 들어오면? 정상 사용자는 그럴 일이 없다. **누군가 복사해 간 것**이다.

```text
정상:  T1 → (재발급) → T2 → (재발급) → T3
탈취:  공격자가 T1을 복사 → 사용자가 T2로 재발급 → 공격자가 T1 사용
       → 서버: "폐기된 T1이 왔다! 탈취다" → 이 사용자의 토큰 전부 폐기 → 둘 다 재로그인
```

완벽한 방어는 아니지만, 탈취를 **감지하고 피해를 끊을 수 있다**는 점이 크다.

### 2.5 Refresh Token은 왜 JWT가 아닌가
Refresh Token은 **서버가 폐기할 수 있어야** 한다. 그런데 JWT는 한 번 발급하면 만료 전까지 취소할 방법이 없다(서버가 기억하지 않으니까). 그래서 Refresh Token은 그냥 긴 랜덤 문자열로 만들고 DB에 저장한다.

DB에는 원본이 아니라 **SHA-256 해시**를 저장한다. 비밀번호를 해시로 저장하는 것과 같은 이유다. DB가 유출돼도 토큰을 그대로 쓸 수 없다.

### 2.6 BCrypt
비밀번호는 절대 원문으로 저장하지 않는다. BCrypt는 두 가지 특징이 있다.

1. **일부러 느리다.** 공격자가 초당 수억 번 시도하는 것을 막는다.
2. **같은 비밀번호도 매번 다른 해시가 된다.** 무작위 값(salt)을 섞기 때문이다. 그래서 "해시가 같은 사람은 비밀번호도 같다"를 알 수 없다.

### 2.7 Spring Security 필터 체인
요청은 컨트롤러에 닿기 전에 필터들을 순서대로 통과한다.

```text
요청 → TraceIdFilter → ... → JwtAuthenticationFilter → 인가 검사 → DispatcherServlet → Controller
                                   ↓ 토큰 해석 성공
                            SecurityContext에 사용자 정보 저장
```

- `JwtAuthenticationFilter`는 토큰이 있으면 해석해서 넣고, 없으면 **그냥 통과**시킨다. 접근을 막을지는 다음 단계(인가 설정)가 정한다.
- 필터에서 발생한 401/403은 컨트롤러까지 가지 않으므로 `@RestControllerAdvice`가 잡지 못한다. 그래서 `SecurityProblemHandler`를 따로 만들어 같은 ProblemDetail 형식으로 응답한다.

### 2.8 httpOnly 쿠키와 CSRF
- **httpOnly**: 자바스크립트가 읽을 수 없는 쿠키. XSS로도 훔치기 어렵다.
- **CSRF(교차 사이트 요청 위조)**: 쿠키는 브라우저가 **자동으로** 보내므로, 악성 사이트가 사용자를 속여 우리 서버에 요청을 보내게 할 수 있다.
- 이 프로젝트의 방어
  1. API 인증은 쿠키가 아니라 `Authorization` 헤더로 한다 → 대부분의 API는 CSRF와 무관하다.
  2. 쿠키를 쓰는 `/auth/refresh`, `/auth/logout`만 `SameSite=Strict`(다른 사이트에서 온 요청에는 쿠키를 안 보냄) + **Origin 헤더 검사**로 막는다.

## 3. 따라하기

### 3.1 도메인 구조
PRD의 계층 규칙에 맞춰 두 도메인을 만들었다.

```text
domain/user/
  domain/       User(엔티티), Role, UserStatus, UserRepository(포트), UserErrorCode
  infra/jpa/    UserJpaRepository(Spring Data), UserRepositoryAdapter(포트 구현)
  application/  UserService, AdminAccountInitializer
  api/          UserController, UserResponse
domain/auth/
  domain/       RefreshToken, RefreshTokenRepository(포트), AuthErrorCode
  infra/jpa/    RefreshTokenJpaRepository, RefreshTokenRepositoryAdapter
  application/  AuthService, TokenRevocationService, RefreshTokenCleanupScheduler
  api/          AuthController, SignupRequest, LoginRequest, TokenResponse
```

**포트와 어댑터**: `domain`은 `UserRepository`라는 인터페이스(포트)만 알고, 실제 JPA 구현은 `infra`에 둔다. 이렇게 하면 나중에 저장 방식이 바뀌어도 도메인 코드는 그대로다.

### 3.2 엔티티: setter 없이 상태 바꾸기

```java
@Entity
@Table(name = "users")
public class User extends SoftDeleteEntity {

    protected User() {}            // JPA가 쓰는 기본 생성자. 외부에서 못 만들게 protected.

    public static User createUser(String email, String passwordHash, String nickname) {
        return new User(email, passwordHash, nickname, Role.USER);
    }

    public void suspend()  { this.status = UserStatus.SUSPENDED; }   // setStatus가 아니라 suspend
    public void activate() { this.status = UserStatus.ACTIVE; }

    public boolean isActive() {
        return status == UserStatus.ACTIVE && !isDeleted();
    }
}
```

`setStatus(x)`가 아니라 `suspend()`로 만들면, 코드를 읽을 때 "무엇을 하는지"가 드러나고 잘못된 값이 들어갈 수 없다.

### 3.3 토큰 발급

```java
public String createAccessToken(Long userId, String role) {
    Instant now = clock.instant();
    return Jwts.builder()
            .subject(String.valueOf(userId))   // 누구인지
            .claim("role", role)               // 무엇을 할 수 있는지
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.accessTtl())))
            .signWith(key)                     // 서버 비밀 키로 서명
            .compact();
}
```

`Clock`을 주입받는 이유: 테스트에서 "16분 뒤"를 만들어 만료를 검증하기 위해서다. `Instant.now()`를 직접 부르면 시간을 조작할 수 없다.

### 3.4 재발급과 재사용 탐지

```java
@Transactional
public AuthResult refresh(String rawRefreshToken, String clientIp, String userAgent) {
    RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
            .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

    if (stored.isRevoked()) {
        // 폐기된 토큰이 다시 왔다 = 탈취 가능성
        tokenRevocationService.revokeAllInNewTransaction(stored.getUserId(), now);  // 별도 트랜잭션!
        throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
    ...
    stored.revoke(now);                    // 옛 토큰 폐기
    return issueTokens(user, clientIp, userAgent);   // 새 토큰 발급
}
```

`revokeAllInNewTransaction`이 왜 별도 트랜잭션인지는 5장 문제 3에서 설명한다.

### 3.5 초기 관리자 계정

```java
@Component
public class AdminAccountInitializer implements ApplicationRunner {
    public void run(ApplicationArguments args) {
        if (!admin.isConfigured()) return;                      // 환경변수 없으면 건너뜀
        if (userService.findByEmail(admin.email()).isPresent()) return;
        try {
            userService.registerAdmin(admin.email(), admin.password(), admin.nickname());
        } catch (DataIntegrityViolationException e) {
            // 다른 인스턴스가 먼저 만들었다. 유니크 제약이 중복을 막아준다.
        }
    }
}
```

서버 2대가 동시에 뜨면 둘 다 "계정이 없네" 하고 만들 수 있다. 이메일 유니크 제약이 하나만 성공시키고, 진 쪽은 예외를 무시한다(PRD SCALE-10).

### 3.6 인가 규칙

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()          // 로그인/가입은 누구나
        .requestMatchers(GET, "/actuator/health").permitAll()
        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")     // 관리자 전용
        .requestMatchers("/api/v1/**").authenticated()            // 그 외 API는 로그인 필요
        .anyRequest().permitAll())
```

## 4. 결정과 이유

| 결정 | 고려한 대안 | 선택 이유 |
|---|---|---|
| Refresh Token을 랜덤 문자열 + DB 저장 | Refresh Token도 JWT로 | JWT는 발급 후 취소할 수 없다. 로그아웃과 재사용 탐지를 하려면 서버가 폐기할 수 있어야 한다. |
| DB에 토큰 해시만 저장 | 원문 저장 | DB 유출 시 토큰을 그대로 재사용당하는 것을 막는다. |
| Access Token은 응답 바디로 | Access도 쿠키로 | 헤더 인증이면 대부분의 API가 CSRF와 무관해진다. |
| `User`가 `SoftDeleteEntity`를 상속 | `BaseEntity`(작성자 포함) | users 테이블에는 created_by 컬럼이 없다(자기 자신을 만든 사람이 없으니까). 5장 문제 1 참고. |
| 재사용 탐지 시 REQUIRES_NEW | 같은 트랜잭션에서 폐기 | 예외를 던지면 같은 트랜잭션의 폐기까지 롤백된다. 5장 문제 3 참고. |
| 로그인 실패 시 이메일/비밀번호 구분 안 함 | "없는 이메일입니다" 안내 | 어떤 이메일이 가입되어 있는지 알려주면 계정 수집에 쓰인다. |
| rate limit 키 = IP + 이메일 | IP만 | IP만 쓰면 회사나 학교처럼 IP를 공유하는 환경에서 무고한 사용자가 막힌다. |

## 5. 겪은 문제와 해결

### 문제 1: 스키마 검증 실패 — users에 created_by가 없다
```text
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException:
Schema validation: missing column [created_by] in table [users]
```
- **원인**: `User`가 `BaseEntity`를 상속했는데, 이 클래스에는 `createdBy` 필드가 있다. 그런데 PRD의 users 테이블에는 그 컬럼이 없다. 사용자를 만든 사람은 사용자 자신이라 의미가 없기 때문이다.
- **해결**: 공통 부모를 3단계로 나눴다.

```text
BaseTimeEntity      createdAt, updatedAt
  └ SoftDeleteEntity  + deletedAt          ← User가 상속
      └ BaseEntity      + createdBy        ← boards, posts, comments가 상속
```
- **배운 점**: `ddl-auto=validate`가 이런 불일치를 기동 시점에 바로 잡아준다. `update`였다면 조용히 컬럼을 추가해버려서, 운영 DB와 코드가 어긋난 걸 나중에야 알았을 것이다.

같은 이유로 `RefreshToken`도 `BaseTimeEntity` 상속을 떼고 `created_at`만 직접 갖게 했다(refresh_tokens에는 updated_at이 없다).

### 문제 2: ObjectMapper 빈을 찾을 수 없다
```text
No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available
```
- **원인**: Spring Boot 4는 **Jackson 3**을 쓴다. 패키지 이름이 `com.fasterxml.jackson`에서 `tools.jackson`으로 바뀌었다.
- **확인 방법**

```bash
./gradlew dependencies --configuration runtimeClasspath | grep -i jackson
# tools.jackson.core:jackson-databind:3.1.5
```
- **해결**: import를 `tools.jackson.databind.ObjectMapper`로 바꿨다.
- **배운 점**: Boot 4 마이그레이션에서 가장 자주 만나게 될 변화다. 인터넷 예제 코드는 대부분 Jackson 2 기준이다.

### 문제 3: 재사용 탐지가 동작하지 않음 (가장 중요한 버그)
테스트는 이랬다.

```text
1. T1으로 재발급 → T2 발급, T1 폐기
2. 탈취된 T1을 다시 사용 → 401 (여기까진 정상)
3. 정상 토큰 T2도 막혀야 한다 → 그런데 200이 나왔다 ❌
```

```text
java.lang.AssertionError: Status expected:<401> but was:<200>
```

- **원인**: 코드 순서가 이랬다.

```java
tokenRepository.revokeAllByUserId(userId, now);      // 전부 폐기
throw new BusinessException(INVALID_REFRESH_TOKEN);  // 예외!
```

`@Transactional` 메서드에서 런타임 예외가 나면 **그 트랜잭션 전체가 롤백된다**. 즉 방금 한 폐기까지 취소되어 아무 일도 일어나지 않았다.

- **해결**: 폐기를 별도 트랜잭션(`REQUIRES_NEW`)으로 분리해 **즉시 커밋**되게 했다.

```java
@Service
public class TokenRevocationService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revokeAllInNewTransaction(Long userId, Instant now) { ... }
}
```

**주의**: 같은 클래스 안에서 메서드를 호출하면 Spring의 프록시를 거치지 않아 `REQUIRES_NEW`가 무시된다(self-invocation 문제). 그래서 반드시 **별도 빈**으로 분리해야 한다.

- **배운 점**: "예외를 던지기 전에 뭔가 저장한다"는 코드는 항상 의심해야 한다. 그리고 이 버그는 테스트가 없었다면 실제 공격 상황에서야 드러났을 것이다.

### 문제 4: 없는 경로 테스트가 404 대신 401
M1에서 만든 테스트가 깨졌다. `/api/v1/does-not-exist`를 호출했는데 401이 나왔다.
- **원인**: M2에서 `/api/v1/**`에 인증을 요구하도록 바꿨다. 보안 필터가 컨트롤러 탐색보다 먼저 동작하므로, 없는 경로라도 인증이 없으면 401이 먼저 난다.
- **해결**: 테스트 경로를 인증이 필요 없는 `/does-not-exist`로 바꿨다. 이 동작 자체는 옳다(존재 여부를 인증 없이 알려주지 않는 것이 더 안전하다).

### 문제 5: ArchUnit 규칙이 실제 설계보다 엄격했다
```text
ARCH-02 위반: UserResponse.from(User) 가 Entity에 의존한다
ARCH-04 위반: Slice auth depends on Slice user
```
- **원인**: M1에서 규칙을 러프하게 썼다. PRD는 "Controller가 Entity를 **주고받지** 않는다", "다른 도메인의 **Repository/infra**에 직접 접근하지 않는다"인데, 테스트는 "api 패키지가 Entity에 **의존조차** 하지 않는다", "도메인끼리 **아무 의존도** 없다"로 구현되어 있었다.
- **해결**: 규칙을 PRD 문구에 맞게 다시 썼다.
  - ARCH-02: `@RestController`의 public 메서드 **반환 타입과 파라미터 타입**만 검사
  - ARCH-04: 다른 도메인의 **`infra` 패키지** 의존만 금지 (application 서비스 참조는 허용)
- **배운 점**: 아키텍처 규칙은 너무 엄격하면 개발자가 규칙을 끄게 된다. "무엇을 진짜 막고 싶은가"를 정확히 표현하는 게 중요하다.

## 6. 확인 방법

### 자동 테스트

```bash
cd backend
./gradlew test -PdbVendor=postgresql
./gradlew test -PdbVendor=mysql
```

두 실행 모두 26개 테스트가 통과한다(M1의 11개 + JWT 단위 4개 + 인증 통합 11개).

```text
인증 API > 회원가입 후 로그인하면 Access Token과 Refresh Token 쿠키를 받는다 PASSED
인증 API > Access Token으로 내 정보를 조회한다 PASSED
인증 API > 토큰 없이 보호된 API를 호출하면 401과 ProblemDetail을 받는다 PASSED
인증 API > 중복 이메일로 가입하면 409를 받는다 PASSED
인증 API > 비밀번호가 규칙에 맞지 않으면 400과 필드 오류를 받는다 PASSED
인증 API > 재발급하면 새 토큰이 나오고 기존 refresh token은 폐기된다(rotation) PASSED
인증 API > 폐기된 refresh token을 재사용하면 해당 사용자의 모든 토큰이 폐기된다 PASSED
인증 API > 로그아웃하면 refresh token이 폐기되고 쿠키가 삭제된다 PASSED
인증 API > 정지된 계정은 로그인과 재발급이 모두 막힌다 PASSED
인증 API > 비밀번호를 바꾸면 기존 refresh token이 폐기된다 PASSED
인증 API > 로그인을 반복해서 실패하면 429를 받는다 PASSED
BUILD SUCCESSFUL
```

### 직접 호출해서 확인

```powershell
docker compose --profile postgresql up -d
cd backend
$env:ADMIN_EMAIL="admin@example.com"; $env:ADMIN_PASSWORD="admin1234"
.\gradlew.bat bootRun
```

기동 로그에 관리자 생성이 찍힌다.

```text
c.e.t.d.u.a.AdminAccountInitializer - 초기 관리자 계정을 생성했다: admin@example.com
```

로그인과 내 정보 조회:

```bash
curl -c cookie.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"admin1234"}'
# {"accessToken":"eyJhbGciOiJIUzM4NCJ9...","expiresIn":900,"user":{...}}

curl http://localhost:8080/api/v1/users/me -H "Authorization: Bearer <accessToken>"
# {"id":1,"email":"admin@example.com","nickname":"관리자","role":"ADMIN",...}

curl http://localhost:8080/api/v1/users/me
# {"status":401,"code":"COMMON_UNAUTHORIZED","traceId":"8cd76f6da7624091",...}
```

Swagger UI(http://localhost:8080/swagger-ui.html)에서는 우측 상단 **Authorize**에 Access Token을 넣으면 인증이 필요한 API를 눌러볼 수 있다.

## 7. 회고와 다음 단계

**잘된 점**
- 재사용 탐지 버그를 테스트가 잡아냈다. 이 버그는 평소에는 절대 드러나지 않고 실제 공격 때만 문제가 되는 종류다.
- 보안 결정(토큰 저장 위치, 해시 저장, 에러 메시지)을 하나씩 근거와 함께 정했다.
- ArchUnit 규칙을 실제 설계에 맞게 다듬었다. 규칙과 코드가 싸우지 않게 됐다.

**아쉬운 점 / 개선할 점**
- rate limit이 인스턴스별이라 서버가 2대면 한도가 2배가 된다. 로그인 보호 용도로는 감수할 만하지만, 문서에 남겨두었다.
- 비밀번호 찾기(이메일 발송)가 없다. 비목표라 이번에는 넣지 않았다.
- 토큰 정리 스케줄러는 만들었지만 실제 동작(ShedLock 락 획득)은 M7의 다중 인스턴스 테스트에서 확인할 예정이다.

**다음 단계 (M3)**
- 프론트 라우터와 레이아웃
- axios 인터셉터로 401 시 자동 재발급 (동시에 여러 요청이 401을 받아도 재발급은 한 번만)
- 로그인/회원가입/내 정보 화면과 라우트 가드
