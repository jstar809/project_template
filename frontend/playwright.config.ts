import { defineConfig, devices } from '@playwright/test'

/**
 * E2E 스모크 테스트 설정.
 *
 * 이미 떠 있는 스택을 대상으로 한다(docker compose --profile full ... up).
 * 기본 주소는 compose의 프론트 포트(8081)이며, E2E_BASE_URL로 바꿀 수 있다.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI ? [['html', { open: 'never' }], ['list']] : [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8081',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'ko-KR',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
