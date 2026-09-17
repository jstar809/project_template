/// <reference types="vitest/config" />
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 개발 중 /api 요청은 백엔드(8080)로 넘긴다.
      '/api': {
        target: process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    // Vitest는 src/ 의 단위·통합 테스트만 담당한다.
    // e2e/ 는 Playwright가 실행하므로 제외하지 않으면 "test()를 여기서 부를 수 없다" 오류가 난다.
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
  },
})
