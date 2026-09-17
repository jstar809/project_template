# CLAUDE.md — 에이전트 작업 규칙

이 저장소에서 작업할 때 반드시 지킨다. 기준 문서는 [docs/PRD.md](docs/PRD.md), 진행 상태는 [PROGRESS.md](PROGRESS.md), 사용법은 [docs/usage.md](docs/usage.md)다.

## 진행 방식
1. **마일스톤 순서대로** 진행한다(M0 → M8). 현재 단계는 `PROGRESS.md` 표에서 확인한다.
2. 한 마일스톤을 끝낼 때마다: 완료 조건 검증 → devlog 작성 → 커밋 → `PROGRESS.md` 상태를 🙋(사용자 확인 필요)로 변경.
3. 사용자 확인 전에는 다음 마일스톤으로 넘어가지 않는다.
4. 테스트가 실패하면 다음 단계로 진행하지 않는다. 백엔드는 **두 DB 모두** 통과해야 한다.

## 금지 사항
- **Kotlin 금지**: `*.kt`, `build.gradle.kts`를 만들지 않는다. Gradle은 Groovy DSL, 코드는 Java만 쓴다. 문서 예시도 마찬가지다.
- **비밀값 커밋 금지**: `.env`, `.env.prod`, 키, 토큰. 예시 파일(`*.example`)만 커밋한다.
- 비목표 기능(소셜 로그인, 다국어, 결제, SSR, K8s 등)을 임의로 추가하지 않는다.
- 사용자의 다른 프로세스를 죽이지 않는다. 포트 충돌은 `.env`에서 포트를 바꿔 해결한다.

## 아키텍처 규칙 (PRD 8장, ArchUnit이 검사)
- 의존 방향: `api → application → domain ← infra`
- Controller는 Entity를 반환하거나 받지 않는다. DTO(record)를 쓴다.
- `@Transactional`은 application 계층에만 붙인다. 조회는 `readOnly = true`.
- 다른 도메인은 application 서비스나 ID로만 참조한다. 다른 도메인의 Repository/Mapper를 직접 주입하지 않는다.
- 생성자 주입만 쓴다. 필드 주입(`@Autowired` 필드) 금지.
- 외부 자원(파일 저장, rate limit, 캐시)은 인터페이스 뒤에 둔다.

## 데이터 접근
- **JPA**: 생성·수정·삭제, ID 단건 조회
- **MyBatis**: 목록, 검색, 페이징, 집계, 관리자 조회 (`#{}`만 사용, 정렬 컬럼은 화이트리스트)
- 스키마 변경은 **Flyway로만** 한다. `db/migration/postgresql`과 `db/migration/mysql`에 **같은 버전 번호로 함께** 작성한다.
- 시간은 UTC(`Instant`), enum은 `VARCHAR`, ID는 `BIGINT IDENTITY`, 벤더 전용 `columnDefinition` 금지.

## 기록
- PRD와 다르게 결정했거나 PRD에 없는 결정은 `docs/decisions.md`에 기록한다.
- 마일스톤마다 `docs/devlog/M{n}-*.md`를 **학습용 튜토리얼 형식**(PRD 부록 22.1 템플릿)으로 작성한다. devlog가 없으면 그 단계는 미완료다.
- 외부 자원(서버, 도메인, Secrets)이 없어 검증할 수 없는 항목은 건너뛰지 말고 "사용자 확인 필요"로 남긴다.

## 자주 쓰는 명령
```bash
# 백엔드
cd backend && ./gradlew test -PdbVendor=postgresql
cd backend && ./gradlew test -PdbVendor=mysql
cd backend && ./gradlew spotlessApply
cd backend && ./gradlew bootRun

# 프론트
cd frontend && npm run lint && npm run typecheck && npm run build

# DB
docker compose --profile postgresql up -d
docker compose --profile mysql up -d
```

## 커밋
- Conventional Commits: `feat(m2): JWT 인증과 사용자 API`
- 마일스톤 단위로 커밋하고, devlog 파일을 같은 커밋에 포함한다.
