# M5. CMS 사용자 화면과 에디터

> 작성일: 2026-09-16 · 소요 시간: 약 2시간 · 관련 요구사항: PRD 13장(라우트와 화면), FR-POST, FR-COMMENT, FR-FILE-07

## 1. 이번 단계의 목표

M4에서 만든 API를 실제로 쓰는 화면을 만든다. 이 단계가 끝나면 다음이 가능해진다.

- 홈에서 게시판을 고르고, 목록에서 글을 찾는다(검색·페이지 이동·공지 고정).
- 글을 읽고, 첨부 파일을 내려받고, 댓글과 대댓글을 단다.
- 에디터에서 **이미지를 붙여넣으면 바로 업로드**되어 본문에 들어간다.
- 권한이 없는 버튼(글쓰기·수정·삭제)은 아예 보이지 않는다.

## 2. 배경 개념

### 2.1 리치 텍스트 에디터는 무엇을 하는가
`<textarea>`는 글자만 받는다. 굵게, 목록, 이미지 같은 서식을 넣으려면 편집 가능한 HTML 영역이 필요하다.

브라우저에는 `contenteditable`이라는 기능이 있는데, 그냥 쓰면 브라우저마다 만들어내는 HTML이 제각각이라 다루기 어렵다. **TipTap**은 그 위에 문서 모델을 얹어, 편집 결과를 예측 가능한 구조로 관리해준다.

```text
사용자 입력 → TipTap 문서 모델(JSON 구조) → getHTML() → 서버로 전송
```

TipTap은 기능을 **확장(extension)** 단위로 켠다.

```ts
extensions: [
  StarterKit,      // 굵게, 기울임, 목록, 인용, 코드블록 등 기본 묶음
  Image,           // <img> 삽입
  Link,            // 링크
  Placeholder,     // 비어 있을 때 안내 문구
]
```

### 2.2 이미지 즉시 업로드
글을 저장할 때 이미지를 한꺼번에 올리면, 사용자는 저장을 누르기 전까지 이미지를 볼 수 없다. 그래서 **붙여넣는 즉시 업로드**한다.

```text
1. 사용자가 이미지 붙여넣기(Ctrl+V)
2. handlePaste가 가로챔 → POST /api/v1/files
3. 서버가 {id, url} 반환 → 에디터에 <img src="url"> 삽입
4. 글 저장 시 fileIds에 그 id를 담아 전송 → 서버가 TEMP → LINKED로 변경
```

**여기서 생기는 문제**: 글을 저장하지 않고 나가면 파일만 서버에 남는다. 그래서 M4에서 24시간 뒤 정리하는 스케줄러를 만들어 두었다. 프론트와 백엔드가 짝을 이루는 설계다.

또 하나: 사용자가 이미지를 넣었다가 **지울 수도** 있다. 그래서 저장할 때 본문에 아직 남아 있는 이미지만 골라 보낸다.

```ts
const usedInline = inlineFiles.filter((file) => content.includes(file.url))
```

### 2.3 XSS 방어를 두 겹으로
게시글 본문은 HTML이라 화면에 그릴 때 `dangerouslySetInnerHTML`을 쓸 수밖에 없다. 이름 그대로 위험한 API다.

이 프로젝트는 방어를 두 겹으로 둔다.

```text
1겹: 서버가 저장 전에 정제 (OWASP Java HTML Sanitizer) ← 진짜 방어선
2겹: 브라우저가 그리기 전에 한 번 더 정제 (DOMPurify)   ← 보조
```

왜 두 번 하나? 서버 정제 규칙에 구멍이 생기거나, 과거에 저장된 데이터가 정제 전 것일 수 있다. 비용이 거의 없으니 한 겹 더 둔다.

### 2.4 URL이 곧 화면 상태
검색어와 페이지 번호를 `useState`에 두면, 새로고침하거나 링크를 공유할 때 상태가 사라진다. 그래서 **URL 쿼리 문자열**에 담는다.

```text
/boards/free?searchType=title&keyword=공지&page=2
```

```ts
const [searchParams, setSearchParams] = useSearchParams()
const page = Number(searchParams.get('page') ?? '0')
```

덤으로 TanStack Query의 캐시 키에 이 값들을 넣으면, 뒤로 가기를 눌렀을 때 이전 목록이 즉시 나타난다.

### 2.5 권한에 따른 버튼 노출
버튼을 숨기는 것은 **보안이 아니라 편의**다. 진짜 차단은 서버가 한다. 다만 서버가 이미 판단한 결과를 응답에 담아주면, 프론트는 그대로 따르기만 하면 된다.

```json
{ "canEdit": true, "canDelete": true, "board": { "canWrite": true } }
```

프론트가 "내가 작성자인가?"를 직접 계산하면 규칙이 두 곳에 생기고, 언젠가 어긋난다.

### 2.6 React 훅 규칙: useEffect로 상태를 덮어쓰지 않기
수정 화면에서 흔히 이렇게 쓴다.

```tsx
const { data } = useQuery(...)
useEffect(() => {
  if (data) setTitle(data.title)   // ❌ 린트가 경고한다
}, [data])
```

문제는 렌더가 두 번 일어나고(빈 값 → 채워진 값), 데이터가 갱신될 때 사용자가 입력 중이던 값이 덮어쓰인다는 점이다.

**대신 컴포넌트를 나눈다.** 데이터를 불러오는 바깥 컴포넌트와, 데이터가 준비된 뒤에만 렌더되는 폼 컴포넌트로 쪼개면 초기값을 그냥 `useState(initial)`로 넣을 수 있다.

## 3. 따라하기

### 3.1 설치

```bash
npm install @tiptap/react @tiptap/starter-kit @tiptap/extension-image \
            @tiptap/extension-link @tiptap/extension-placeholder dompurify
npx shadcn@latest add textarea alert-dialog badge -y
```

### 3.2 화면 구성

```text
src/features/
  board/   api.ts, hooks.ts                         게시판 목록·상세
  post/    api.ts, pages/PostListPage.tsx           목록(검색·페이징·공지)
                  pages/PostDetailPage.tsx          상세(본문·첨부·삭제)
                  pages/PostEditorPage.tsx          작성·수정
  comment/ api.ts, components/CommentSection.tsx    댓글·대댓글
  file/    api.ts                                   업로드
src/shared/ui/
  RichTextEditor.tsx   TipTap 에디터
  RichTextViewer.tsx   본문 렌더링(DOMPurify)
  Pagination.tsx       페이지 이동
  States.tsx           빈 목록 / 오류 표시
```

### 3.3 붙여넣기 업로드

```ts
editorProps: {
  handlePaste: (_view, event) => {
    const files = Array.from(event.clipboardData?.files ?? [])
    const image = files.find((file) => file.type.startsWith('image/'))
    if (!image || !editor) return false
    event.preventDefault()          // 기본 붙여넣기 동작을 막고
    void uploadAndInsert(editor, image)  // 업로드 후 삽입
    return true                     // "내가 처리했다"고 알린다
  },
  handleDrop: ...                    // 드래그 앤 드롭도 같은 방식
}
```

업로드 후 삽입은 TipTap의 명령 체인으로 한다.

```ts
editor.chain().focus().setImage({ src: uploaded.url, alt: uploaded.originalName }).run()
```

### 3.4 목록 화면의 공지 처리
서버가 공지와 일반 글을 나눠서 주므로, 화면에서는 공지를 먼저 그리기만 하면 된다.

```tsx
{posts.data.notices.map((post) => <PostRow key={`notice-${post.id}`} post={post} highlight />)}
{posts.data.page.content.map((post) => <PostRow key={post.id} post={post} />)}
```

`key`에 접두사를 붙인 이유: 공지 글이 일반 목록에도 나타나는 상황(다른 정렬 기준을 쓸 경우)에서 key가 겹치는 것을 막기 위해서다.

### 3.5 댓글 트리 렌더링
서버가 이미 1단계 트리(`replies` 배열)로 내려주므로 그대로 두 단계만 그린다.

```tsx
{comments.map((comment) => (
  <li key={comment.id}>
    <CommentItem comment={comment} canReply />
    {comment.replies.length > 0 && (
      <ul className="border-l pl-4">                  {/* 들여쓰기로 답글 표시 */}
        {comment.replies.map((reply) => <CommentItem key={reply.id} comment={reply} canReply={false} />)}
      </ul>
    )}
  </li>
))}
```

`canReply={false}`로 대댓글에는 답글 버튼을 주지 않는다. 서버도 거부하지만(M4), 애초에 누를 수 없게 하는 편이 낫다.

### 3.6 댓글 작성 후 화면 갱신
댓글을 달면 목록과 **게시글의 댓글 수**가 함께 바뀐다. 두 캐시를 모두 무효화한다.

```ts
const refresh = async () => {
  await queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) })
  await queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) })
}
```

## 4. 결정과 이유

| 결정 | 고려한 대안 | 선택 이유 |
|---|---|---|
| 검색·페이지를 URL 쿼리에 저장 | 컴포넌트 state | 새로고침·뒤로가기·링크 공유가 모두 자연스럽게 동작한다. |
| 이미지 즉시 업로드 | 저장 시 한꺼번에 업로드 | 사용자가 결과를 바로 본다. 대신 서버에 정리 스케줄러가 필요하다(M4에 이미 있음). |
| 권한 판단은 서버 응답(`canEdit` 등)을 따름 | 프론트에서 작성자 비교 | 규칙이 두 곳에 생기면 언젠가 어긋난다. |
| 본문에 DOMPurify 한 번 더 | 서버 정제만 신뢰 | 비용이 거의 없고, 서버 규칙에 구멍이 생겨도 막힌다. |
| 작성·수정 화면을 하나로 | 두 화면 분리 | 폼과 검증이 완전히 같다. id 유무로 모드만 구분한다. |
| 에디터 페이지를 로딩/폼 두 컴포넌트로 분리 | useEffect로 초기값 주입 | 훅 규칙 위반을 피하고, 입력 중 값이 덮어쓰이지 않는다(5장 문제 1). |
| 삭제는 확인 다이얼로그 | 즉시 삭제 | 되돌리기 어려운 동작이므로 한 번 확인한다. |

## 5. 겪은 문제와 해결

### 문제 1: 린트 오류 — useEffect 안에서 setState
```text
error: react-hooks/set-state-in-effect
  useEffect(() => { ... setTitle(existing.data.title) ... }, [existing.data])
```
- **원인**: 수정 화면에서 불러온 글 데이터를 `useEffect`로 폼 상태에 복사했다. React 19의 훅 린트 규칙은 이 패턴을 경고한다. 렌더가 한 번 더 일어나고, 데이터가 다시 불려오면 사용자가 입력 중이던 내용이 날아갈 수 있기 때문이다.
- **해결**: 컴포넌트를 둘로 나눴다.

```tsx
export default function PostEditorPage() {
  const existing = useQuery(...)
  if (isEdit && !existing.data) return <Skeleton />        // 준비될 때까지 대기
  return <PostForm initial={existing.data ?? null} />       // 준비된 뒤에만 렌더
}

function PostForm({ initial }) {
  const [title, setTitle] = useState(initial?.title ?? '')  // 초기값을 그냥 넣는다
```
- **배운 점**: "로딩 중에는 폼을 렌더하지 않는다"로 바꾸면 문제 자체가 사라진다. 린트 규칙이 더 나은 구조를 가리키는 경우였다.

### 문제 2: 상세 화면에 작성자 이름이 없다
- **원인**: M4에서 만든 상세 API가 `author: { id }`만 내려주고 닉네임이 없었다. 목록 API에는 있었는데 상세에서 빠졌다.
- **해결**: 백엔드 `PostService`가 `UserService`를 통해 닉네임을 조회해 응답에 담도록 했다. 다른 도메인은 application 서비스로만 참조한다는 규칙(ARCH-04)을 지켰다.

```java
String authorNickname = userService.getById(post.getAuthorId()).getNickname();
```
- **배운 점**: 화면을 만들다 보면 API에서 빠진 값이 드러난다. 프론트에서 임시로 우회하지 말고 API를 고치는 편이 낫다.

### 문제 3: 한글 요청이 400 (실제로는 도구 문제)
```text
bad request: JSON parse error: Invalid UTF-8 middle byte 0xda
```
- **상황**: curl로 한글이 든 JSON을 보냈더니 400이 났다. 서버 버그로 보였다.
- **원인**: Windows Git Bash가 한글을 UTF-8이 아닌 인코딩으로 보냈다. 서버는 정상이었다. MockMvc 테스트(한글 포함)는 모두 통과하고 있었다.
- **해결**: JSON을 UTF-8 파일로 저장해 `--data-binary @file.json`으로 보냈더니 정상 동작했다.
- **배운 점**: 재현 환경이 의심스러울 때는 **이미 통과하는 테스트가 있는지** 먼저 본다. 있으면 도구 문제일 가능성이 높다.

### 문제 4: 번들 크기 증가
```text
dist/assets/PostEditorPage-DGPBEp9Y.js   408.44 kB │ gzip: 128.45 kB
dist/assets/index-CSiqhu4K.js            412.61 kB │ gzip: 130.12 kB
```
- **상황**: TipTap이 들어오면서 커졌다.
- **판단**: 라우트 분할 덕분에 **에디터는 글쓰기 화면에 들어갈 때만** 내려받는다. 첫 화면 번들은 gzip 130KB로 PRD 목표(300KB) 안이다.
- **후속**: M7에서 vendor 청크 분리를 검토한다.

## 6. 확인 방법

### 자동 테스트

```bash
cd frontend
npm run lint && npm run typecheck && npm run test && npm run build
```

```text
 ✓ src/shared/api/client.test.ts (5)
 ✓ src/shared/ui/RouteGuards.test.tsx (6)
 ✓ src/features/auth/pages/LoginPage.test.tsx (3)
 ✓ src/features/post/pages/PostListPage.test.tsx (5)
   ✓ 게시판 이름과 글 목록을 보여준다
   ✓ 공지는 목록 위쪽에 따로 표시된다
   ✓ 첨부와 댓글 수 표시가 나타난다
   ✓ 검색하면 결과만 남는다
   ✓ 글쓰기 버튼은 쓰기 권한이 있을 때만 보인다
 ✓ src/features/post/pages/PostDetailPage.test.tsx (5)
   ✓ 제목, 작성자, 본문, 첨부를 보여준다
   ✓ 본문의 위험한 태그는 렌더링되지 않는다
   ✓ 댓글과 대댓글을 트리로 보여준다
   ✓ 권한이 있으면 수정/삭제 버튼이 보인다
   ✓ 권한이 없으면 수정/삭제 버튼이 보이지 않는다

 Test Files  5 passed (5)
      Tests  24 passed (24)
```

### API 확인 (서버만 띄우고)

```bash
# 글 작성 시 스크립트가 제거되는지
curl -X POST http://localhost:8080/api/v1/boards/free/posts \
  -H "Authorization: Bearer <token>" -H 'Content-Type: application/json' \
  --data-binary @post.json      # {"title":"...","content":"<p>본문</p><script>alert(1)</script>"}

curl http://localhost:8080/api/v1/posts/1
# content: <p>본문입니다</p>      ← script 제거됨
# author: {"id":1,"nickname":"관리자"}
```

### 브라우저에서 (사용자 확인 필요)

```powershell
docker compose --profile postgresql up -d
cd backend;  $env:ADMIN_EMAIL="admin@example.com"; $env:ADMIN_PASSWORD="admin1234"; .\gradlew.bat bootRun
cd frontend; npm run dev
```

1. 관리자로 로그인 → 게시판이 헤더 메뉴에 보인다
2. 게시판 → **글쓰기** → 제목 입력 → 에디터에 **이미지 붙여넣기**(Ctrl+V) → 바로 이미지가 보인다
3. 파일 추가로 첨부 → 등록 → 상세 화면에서 이미지와 첨부 확인
4. 댓글 작성 → 답글 작성 → 들여쓰기된 트리로 보인다
5. 목록에서 검색, 페이지 이동
6. 로그아웃 후 같은 글을 보면 수정·삭제 버튼이 사라진다
7. 개발자도구에서 화면 폭을 모바일로 줄여 레이아웃 확인

## 7. 회고와 다음 단계

**잘된 점**
- 서버가 내려준 권한 플래그를 그대로 쓰는 방식이라, 화면과 서버의 판단이 어긋날 수 없다.
- 이미지 즉시 업로드와 서버의 임시 파일 정리가 맞물려 동작한다. 프론트와 백엔드를 같이 설계한 효과다.
- 린트 경고를 우회하지 않고 구조를 바꿔 해결했다.

**아쉬운 점 / 개선할 점**
- 에디터 자체의 테스트는 없다. jsdom에서 contenteditable 동작을 재현하기 어려워, M7의 Playwright 스모크 테스트에서 다루는 편이 낫다고 판단했다.
- 브라우저에서 눈으로 하는 확인이 아직 남아 있다(M3부터 누적).
- 이미지 업로드 진행률 표시가 없다. 큰 파일에서는 답답할 수 있다.

**다음 단계 (M6)**
- 관리자 대시보드와 게시글·댓글·사용자 관리 화면
- 백엔드 관리자 조회 API(MyBatis 집계)
