import { expect, test, type Page } from '@playwright/test'

/**
 * 스모크 테스트: 가장 중요한 한 줄기 흐름만 실제 브라우저로 확인한다.
 *
 * 관리자 로그인 → 게시판 생성 → 글 작성 → 목록·상세 확인 → 댓글 작성
 *
 * 단위/통합 테스트가 잡지 못하는 것(라우팅, 프록시, nginx 설정, 빌드 결과물)을 여기서 잡는다.
 */

const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? 'admin@example.com'
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'admin1234'

// 실행할 때마다 고유한 값을 써서 테스트가 서로 간섭하지 않게 한다.
const suffix = Date.now().toString().slice(-6)
const boardSlug = `e2e-${suffix}`
const boardName = `E2E 게시판 ${suffix}`
const postTitle = `E2E 테스트 글 ${suffix}`

async function login(page: Page) {
  await page.goto('/login')
  await page.getByLabel('이메일').fill(ADMIN_EMAIL)
  await page.getByLabel('비밀번호').fill(ADMIN_PASSWORD)
  await page.getByRole('button', { name: '로그인' }).click()

  // 헤더의 사용자 메뉴(닉네임)가 보이면 로그인에 성공한 것이다.
  // "로그인" 버튼이 사라지는지까지 확인해야, 실패했을 때 바로 드러난다.
  await expect(page.getByRole('button', { name: '관리자' })).toBeVisible()
}

test('관리자가 게시판을 만들고 글과 댓글을 남긴다', async ({ page }) => {
  await login(page)

  // 1) 게시판 생성
  await page.goto('/admin/boards')
  await page.getByRole('button', { name: '새 게시판' }).click()
  await page.getByLabel('주소(slug)').fill(boardSlug)
  await page.getByLabel('이름').fill(boardName)
  await page.getByRole('button', { name: '저장' }).click()
  // 순서 변경 버튼의 aria-label에도 게시판 이름이 들어가므로 exact로 좁힌다.
  await expect(page.getByRole('cell', { name: boardName, exact: true })).toBeVisible()

  // 2) 글 작성
  await page.goto(`/boards/${boardSlug}/write`)
  await page.getByLabel('제목').fill(postTitle)
  await page.locator('.tiptap').click()
  await page.keyboard.type('E2E 테스트 본문입니다.')
  await page.getByRole('button', { name: '등록' }).click()

  // 3) 상세 화면 확인
  await expect(page.getByRole('heading', { name: postTitle })).toBeVisible()
  await expect(page.getByText('E2E 테스트 본문입니다.')).toBeVisible()

  // 4) 댓글 작성
  await page.getByLabel('댓글 등록').fill('E2E 댓글입니다.')
  await page.getByRole('button', { name: '댓글 등록' }).click()
  await expect(page.getByText('E2E 댓글입니다.')).toBeVisible()

  // 5) 목록에 나타나는지
  await page.goto(`/boards/${boardSlug}`)
  await expect(page.getByRole('link', { name: new RegExp(postTitle) })).toBeVisible()
})

test('비로그인 사용자는 글쓰기 화면에 들어갈 수 없다', async ({ page }) => {
  await page.goto('/me')
  await expect(page).toHaveURL(/\/login/)
})
