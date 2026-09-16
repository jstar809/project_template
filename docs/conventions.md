# 코딩 컨벤션

## 공통
- 들여쓰기: Java 4칸, 그 외 2칸 (`.editorconfig` 참고)
- 줄바꿈은 LF로 저장한다(`.gitattributes`). Windows 전용 파일(`*.bat`, `*.ps1`)만 CRLF.
- 커밋 메시지는 Conventional Commits를 따른다: `feat(m1): 공통 에러 응답 추가`
  - 타입: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`
- 주석은 "왜"를 적는다. "무엇"은 코드가 말하게 한다.

## 백엔드 (Java)
- 포맷은 Spotless(palantir-java-format)가 담당한다. 커밋 전 `./gradlew spotlessApply`.
- 패키지는 기능(도메인) 단위로 나눈다. `domain/{name}/{api,application,domain,infra}`
- 클래스 이름
  - Controller: `PostController`
  - Service: `PostService` (조회 전용이 커지면 `PostQueryService`)
  - JPA Repository: `PostRepository` / MyBatis Mapper: `PostQueryMapper`
  - 요청/응답 DTO: `PostCreateRequest`, `PostDetailResponse` (모두 `record`)
- Entity
  - setter를 만들지 않는다. 의미 있는 메서드로 상태를 바꾼다: `post.hide()`, `post.update(...)`
  - 기본 생성자는 `protected`
  - 연관관계는 `LAZY`가 기본이다.
- 예외는 `BusinessException(ErrorCode)`로 던진다. 도메인별 `ErrorCode` enum에 코드를 정의한다.
- 상수와 설정값은 `@ConfigurationProperties` record 또는 상수 클래스로 뺀다. 매직 넘버 금지.
- 테스트 이름은 한글로 `@DisplayName`을 쓴다. given-when-then 주석으로 구분한다.

## 데이터베이스
- 테이블명은 복수형 snake_case(`posts`), 컬럼도 snake_case.
- 예약어와 겹치는 이름을 피한다(`user` → `users`).
- 마이그레이션 파일명: `V{번호}__{설명}.sql` (예: `V4__add_post_status.sql`)
- **PostgreSQL과 MySQL 두 폴더에 같은 번호로 작성한다.**
- 운영 중 스키마 변경은 Expand–Contract 순서로 한다.
  1. Expand: 새 컬럼을 nullable로 추가하고 배포
  2. 코드가 새 컬럼을 채우도록 배포
  3. Contract: 제약 추가, 옛 컬럼 제거
  - 롤백 시 이전 버전 코드가 새 스키마에서도 동작해야 하기 때문이다.

## 프론트엔드 (TypeScript)
- 포맷은 Prettier, 규칙 검사는 ESLint가 담당한다. 커밋 전 `npm run format && npm run lint`.
- 파일 이름: 컴포넌트는 PascalCase(`PostList.tsx`), 그 외는 camelCase(`useAuth.ts`).
- `src/features/{feature}` 안에서만 그 기능의 코드를 둔다. 공용이 되면 `src/shared`로 올린다.
- feature끼리 서로의 내부 파일을 import하지 않는다.
- 서버 데이터는 TanStack Query, 클라이언트 상태는 Zustand. 서버 데이터를 Zustand에 복사하지 않는다.
- HTTP 호출은 `shared/api/client.ts`의 axios 인스턴스만 사용한다.
- `any` 금지. 모르는 타입은 `unknown`으로 받고 좁힌다.
- 환경변수는 `import.meta.env.VITE_*`만 쓰고 `shared/lib/env.ts`에서 검증한다.
