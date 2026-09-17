# M6. 관리자 페이지

> 작성일: 2026-09-17 · 소요 시간: 약 2시간 · 관련 요구사항: PRD 9.7(FR-ADMIN)

## 1. 이번 단계의 목표

운영에 필요한 관리 기능을 완성한다. 이 단계가 끝나면 다음이 가능해진다.

- 대시보드에서 전체 규모(사용자·글·댓글)와 최근 7일 추이를 한눈에 본다.
- 문제가 되는 글이나 댓글을 **숨기거나 되돌린다**(삭제하지 않고).
- 여러 건을 선택해 한 번에 처리한다.
- 사용자의 역할을 바꾸거나 계정을 정지한다.

## 2. 배경 개념

### 2.1 숨김(soft hide)과 삭제(soft delete)의 차이
운영에서 "지운다"는 말은 보통 세 가지를 뜻한다.

| 방식 | DB에서 | 복구 | 언제 쓰나 |
|---|---|---|---|
| 숨김(HIDDEN) | `status` 컬럼만 변경 | 쉽다 | 신고 접수, 검토 중인 글 |
| 논리 삭제(soft delete) | `deleted_at` 기록 | 가능 | 일반적인 삭제 |
| 물리 삭제(DELETE) | 행 제거 | 불가 | 법적 요구, 개인정보 삭제 |

관리자 기능은 **숨김**을 기본으로 한다. 판단이 잘못됐을 때 되돌릴 수 있어야 하기 때문이다.

### 2.2 관리자 조회는 왜 쿼리가 따로인가
일반 사용자 화면의 목록 쿼리에는 이런 조건이 항상 붙는다.

```sql
WHERE deleted_at IS NULL AND status = 'PUBLISHED'
```

관리자는 **숨긴 글과 삭제된 글까지 봐야** 한다. 조건이 정반대라 같은 쿼리를 재사용할 수 없다. 그래서 `AdminQueryMapper`를 따로 두었다. 조건을 파라미터로 분기하면 "실수로 관리자 조건이 사용자 화면에 새는" 사고가 나기 쉽다.

### 2.3 일별 집계와 DB 이식성
"최근 7일 가입자 수"는 날짜별로 묶어야 한다. 그런데 날짜 함수는 DB마다 다르다.

```sql
-- PostgreSQL 전용
DATE_TRUNC('day', created_at)
-- MySQL 전용
DATE(created_at)
-- 둘 다 동작 (표준 SQL)
CAST(created_at AS DATE)
```

이 프로젝트는 세 번째를 쓴다. PRD DB-09가 말하는 "표준 SQL을 쓰고, 정말 다른 것만 분기한다"의 실제 사례다.

### 2.4 역할이 바뀌면 토큰을 폐기해야 하는 이유
Access Token 안에는 `role` 값이 들어 있다(M2). 서버는 요청마다 DB를 조회하지 않고 이 값을 믿는다. 그래서 문제가 생긴다.

```text
1. 사용자 A가 ADMIN으로 로그인 → 토큰에 role=ADMIN 이 박힘
2. 관리자가 A를 USER로 강등
3. A는 이미 받은 토큰으로 15분 동안 계속 관리자 행세를 할 수 있다 ❌
```

그래서 역할을 바꾸거나 계정을 정지하면 **그 사용자의 refresh token을 모두 폐기**한다. Access Token 자체는 취소할 수 없지만(JWT의 특성), 최대 15분 뒤에는 재발급이 막혀 접근이 끊긴다.

> 즉시 차단이 필요하다면 토큰 블랙리스트나 짧은 TTL이 필요하다. 지금 규모에서는 15분 지연을 감수하고, 이 판단을 문서에 남긴다.

### 2.5 자기 자신을 못 바꾸게 막는 이유
관리자가 실수로 자기를 일반 회원으로 바꾸면, **시스템에 관리자가 한 명도 없어질 수 있다.** DB를 직접 고치지 않으면 복구할 수 없다.

```java
private void requireNotSelf(Long actorId, Long targetUserId) {
    if (actorId != null && actorId.equals(targetUserId)) {
        throw new BusinessException(AdminErrorCode.ADMIN_SELF_MODIFICATION);
    }
}
```

화면에서도 본인 행의 버튼을 감춘다. 다만 **버튼을 감추는 것은 보안이 아니다.** 서버가 막는 것이 진짜 방어다.

### 2.6 일괄 처리(bulk) 설계
목록에서 체크박스로 고르고 한 번에 처리한다. API는 id 배열을 받는다.

```json
PATCH /api/v1/admin/posts/status
{ "ids": [11, 12, 13], "status": "HIDDEN" }
```

한 건씩 API를 호출하면 요청이 N번 나가고, 중간에 실패하면 일부만 처리된 상태가 된다. 배열로 받으면 **한 트랜잭션에서 처리**되어 전부 되거나 전부 안 된다.

## 3. 따라하기

### 3.1 백엔드 구조

```text
domain/admin/
  domain/AdminErrorCode.java              자기 자신 변경 금지 등
  application/AdminDtos.java              조회 전용 DTO 모음
  application/AdminService.java           조회는 Mapper, 변경은 각 도메인 서비스에 위임
  infra/mybatis/AdminQueryMapper.java     집계·목록 쿼리
  api/AdminController.java                대시보드 + 게시글/댓글/사용자 관리
```

`AdminService`는 **직접 저장소를 만지지 않는다.** 다른 도메인의 application 서비스를 부른다(PRD ARCH-04).

```java
@Transactional
public int changePostStatus(List<Long> postIds, PostStatus status) {
    return postService.changeStatusByAdmin(postIds, status);   // post 도메인이 처리
}
```

### 3.2 대시보드 쿼리
숫자 세 개를 각각 조회하면 쿼리가 3번 나간다. 서브쿼리로 한 번에 가져온다.

```xml
<select id="selectTotals" resultType="...AdminDtos$Totals">
    SELECT (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL)    AS users,
           (SELECT COUNT(*) FROM posts WHERE deleted_at IS NULL)    AS posts,
           (SELECT COUNT(*) FROM comments WHERE deleted_at IS NULL) AS comments
</select>
```

MyBatis에서 중첩 record를 쓸 때는 `$` 표기로 참조한다(`AdminDtos$Totals`).

### 3.3 동적 필터
관리자 목록은 조건이 선택적이다. `<if>`로 필요한 조건만 붙인다.

```xml
<sql id="adminPostCondition">
    <if test="condition.boardId != null"> AND p.board_id = #{condition.boardId} </if>
    <if test="condition.status != null">  AND p.status = #{condition.status} </if>
    <if test="condition.keyword != null">
        AND (LOWER(p.title) LIKE LOWER(CONCAT('%', #{condition.keyword}, '%'))
             OR LOWER(p.content_text) LIKE LOWER(CONCAT('%', #{condition.keyword}, '%')))
    </if>
</sql>
```

`WHERE 1 = 1`로 시작해 두면 조건이 하나도 없을 때도 SQL이 깨지지 않는다.

### 3.4 프론트 일괄 선택

```tsx
const [selected, setSelected] = useState<number[]>([])

const toggle = (id: number) =>
  setSelected((prev) => (prev.includes(id) ? prev.filter((v) => v !== id) : [...prev, id]))

// 선택된 항목이 있을 때만 액션 바가 나타난다
{selected.length > 0 && (
  <div>
    <span>{selected.length}건 선택</span>
    <Button onClick={() => changeStatus.mutate({ ids: selected, next: 'HIDDEN' })}>숨기기</Button>
    ...
  </div>
)}
```

처리 후에는 선택을 비우고 목록을 다시 불러온다.

```ts
const refresh = async () => {
  setSelected([])
  await queryClient.invalidateQueries({ queryKey: adminKeys.all })
  await queryClient.invalidateQueries({ queryKey: postKeys.all })   // 사용자 화면 목록도 갱신
}
```

### 3.5 차트 없이 추이 그리기
막대 몇 개를 위해 차트 라이브러리를 넣으면 번들이 수십 KB 커진다. div 높이로 충분하다.

```tsx
const max = Math.max(1, ...data.map((point) => point.count))
...
<div
  className="w-full rounded-t bg-primary"
  style={{ height: `${Math.max(4, (point.count / max) * 100)}%` }}
  role="img"
  aria-label={`${point.date}: ${point.count}건`}
/>
```

`Math.max(1, ...)`로 나눗셈에서 0으로 나누는 것을 막고, `Math.max(4, ...)`로 값이 0일 때도 막대가 보이게 한다.

### 3.6 게시판 순서 변경
드래그 앤 드롭은 라이브러리가 필요하고 모바일에서 다루기 까다롭다. 위/아래 버튼으로 충분하다.

```tsx
const move = (index: number, direction: -1 | 1) => {
  const ids = boards.data.map((board) => board.id)
  const target = index + direction
  if (target < 0 || target >= ids.length) return
  ;[ids[index], ids[target]] = [ids[target], ids[index]]   // 두 항목 교환
  reorder.mutate(ids)                                       // 전체 순서를 서버에 전달
}
```

서버는 받은 배열 순서대로 `sortOrder`를 1부터 다시 매긴다. "무엇이 몇 번째인지"를 서버가 계산하지 않아도 되어 단순하다.

## 4. 결정과 이유

| 결정 | 고려한 대안 | 선택 이유 |
|---|---|---|
| 관리자 조회 Mapper를 별도로 | 사용자 쿼리에 조건 분기 | 관리자 조건(숨김·삭제 포함)이 사용자 화면에 새는 사고를 원천 차단한다. |
| 일괄 처리 API는 id 배열 | 한 건씩 호출 | 한 트랜잭션으로 처리되어 일부만 적용되는 상태가 없다. |
| 숨김을 기본 수단으로 | 삭제 | 판단을 되돌릴 수 있어야 한다. |
| 역할 변경·정지 시 토큰 폐기 | 그대로 두기 | 토큰에 role이 들어 있어, 폐기하지 않으면 최대 15분간 옛 권한이 유지된다. |
| 자기 자신 변경 금지(서버+화면) | 화면에서만 숨기기 | 화면 숨김은 보안이 아니다. 서버가 막아야 한다. |
| 추이 그래프는 div로 구현 | 차트 라이브러리 | 막대 7개를 위해 번들을 키울 이유가 없다. |
| 순서 변경은 위/아래 버튼 | 드래그 앤 드롭 | 라이브러리 없이 되고 모바일에서도 쓸 수 있다. |
| 집계는 `CAST(... AS DATE)` | DATE_TRUNC / DATE() | 두 DB에서 모두 동작하는 표준 SQL이다. |

## 5. 겪은 문제와 해결

### 문제 1: 테스트에서 "공개"라는 글자가 두 곳에 있다
```text
TestingLibraryElementError: Found multiple elements with the text: 공개
```
- **원인**: 상태 필터 `<select>`의 선택지에도 "공개"가 있고, 표 안의 배지에도 "공개"가 있었다.
- **해결**: 검사 범위를 표 안으로 좁혔다.

```tsx
const table = within(screen.getByRole('table'))
expect(table.getByText('공개')).toBeInTheDocument()
```
- **배운 점**: 화면 전체에서 텍스트를 찾으면 이런 충돌이 자주 난다. 역할(`role`)이나 영역으로 범위를 좁히는 습관이 낫다.

### 문제 2 (설계로 예방): 관리자 화면 번들이 일반 사용자에게 내려갈 뻔
관리자 화면 5개를 추가하면 번들이 커진다. M3에서 라우트 단위 코드 분할을 해둔 덕분에 **각각 별도 파일**로 나뉘었다.

```text
dist/assets/AdminDashboardPage-CaBeOJ1u.js   2.85 kB │ gzip: 1.16 kB
dist/assets/AdminBoardsPage-6ahi3Ya3.js      6.60 kB │ gzip: 2.26 kB
dist/assets/AdminPostsPage-CtSV8TPM.js       4.05 kB │ gzip: 1.71 kB
```

일반 사용자는 이 파일들을 받지 않는다. 초기 설계가 나중 단계를 편하게 만든 사례다.

### 문제 3 (판단): 역할 변경 후 즉시 차단은 못 한다
- **상황**: 관리자가 사용자를 강등해도, 그 사용자의 Access Token(15분)은 여전히 유효하다.
- **검토한 방법**
  1. 토큰 블랙리스트(Redis) → 인프라가 늘고, 요청마다 조회가 생긴다.
  2. Access Token TTL을 1분으로 → 재발급 요청이 15배 늘어난다.
  3. 요청마다 DB에서 role 조회 → JWT를 쓰는 의미가 사라진다.
- **결정**: refresh token만 폐기하고 최대 15분 지연을 감수한다. 화면에도 "해당 사용자는 다시 로그인해야 합니다"라고 안내한다.
- **배운 점**: 모든 것을 즉시 반영하려면 대가가 크다. 한계를 알고 문서에 남기는 편이 낫다.

## 6. 확인 방법

### 자동 테스트

```bash
cd backend
./gradlew test -PdbVendor=postgresql
./gradlew test -PdbVendor=mysql

cd ../frontend
npm run lint && npm run typecheck && npm run test && npm run build
```

백엔드(두 DB 각각):

```text
관리자 API > 대시보드가 통계와 최근 글을 반환한다 PASSED
관리자 API > 일반 사용자는 관리자 API에 접근할 수 없다 PASSED
관리자 API > 게시글을 숨기면 사용자 목록과 상세에서 사라진다 PASSED
관리자 API > 게시글을 일괄 삭제한다 PASSED
관리자 API > 댓글을 숨기면 사용자 화면에서 내용이 감춰진다 PASSED
관리자 API > 사용자 목록을 검색하고 역할을 바꾼다 PASSED
관리자 API > 관리자는 자기 자신의 역할이나 상태를 바꿀 수 없다 PASSED
관리자 API > 사용자를 정지하면 로그인과 기존 토큰 사용이 막힌다 PASSED
```

프론트:

```text
 Test Files  7 passed (7)
      Tests  31 passed (31)
   (신규) 관리자 게시글 관리 화면 3건, 관리자 사용자 관리 화면 4건
```

### 브라우저에서 (사용자 확인 필요)

```powershell
docker compose --profile postgresql up -d
cd backend;  $env:ADMIN_EMAIL="admin@example.com"; $env:ADMIN_PASSWORD="admin1234"; .\gradlew.bat bootRun
cd frontend; npm run dev
```

1. 관리자로 로그인 → 우측 상단 메뉴 → **관리자**
2. **대시보드**: 숫자 3개와 최근 7일 막대, 최근 글 목록
3. **게시판 관리**: 새 게시판 만들기 → 위/아래 버튼으로 순서 변경 → 사용자 화면 헤더 메뉴 순서도 바뀐다
4. **게시글 관리**: 체크박스로 선택 → 숨기기 → 사용자 화면에서 사라지는지 확인 → 복구
5. **댓글 관리**: 댓글 숨기기 → 글 상세에서 "관리자에 의해 숨겨진 댓글입니다."로 표시
6. **사용자 관리**: 본인 행에는 버튼 대신 "본인" 표시 → 다른 사용자를 정지 → 그 계정으로 로그인 시도하면 막힌다

## 7. 회고와 다음 단계

**잘된 점**
- 관리자 조회를 별도 Mapper로 분리해, 사용자 화면에 숨김 글이 새는 사고 가능성을 없앴다.
- "자기 자신 변경 금지"를 서버와 화면 양쪽에 두되, 서버를 진짜 방어선으로 삼았다.
- 라이브러리를 늘리지 않고 필요한 UI(막대 그래프, 순서 변경)를 구현했다.

**아쉬운 점 / 개선할 점**
- 역할 변경이 즉시 반영되지 않는다(최대 15분). 한계를 문서에 남겼다.
- 관리자 행위 기록(누가 무엇을 숨겼는지)이 없다. 운영 규모가 커지면 감사 로그가 필요하다. PRD의 향후 확장 항목이다.
- 게시글 관리에서 게시판별 필터는 API만 있고 화면 UI는 넣지 않았다. 게시판이 많아지면 추가한다.

**다음 단계 (M7)**
- Docker 이미지, 전체 compose 실행, 다중 인스턴스 확인
- GitHub Actions CI(두 DB 매트릭스), Playwright 스모크 테스트
- README 완성
