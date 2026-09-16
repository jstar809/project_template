import { create } from 'zustand'

export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'theme'

function readStoredTheme(): Theme {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored === 'light' || stored === 'dark') return stored
  } catch {
    // 시크릿 모드 등에서 localStorage 접근이 막힐 수 있다. 시스템 설정으로 넘어간다.
  }
  const prefersDark =
    typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches
  return prefersDark ? 'dark' : 'light'
}

function applyTheme(theme: Theme) {
  document.documentElement.classList.toggle('dark', theme === 'dark')
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    // 저장 실패는 무시한다. 화면은 이미 바뀌었다.
  }
}

interface ThemeState {
  theme: Theme
  toggle: () => void
  init: () => void
}

export const useThemeStore = create<ThemeState>((set, get) => ({
  theme: 'light',
  toggle: () => {
    const next: Theme = get().theme === 'dark' ? 'light' : 'dark'
    applyTheme(next)
    set({ theme: next })
  },
  init: () => {
    const theme = readStoredTheme()
    applyTheme(theme)
    set({ theme })
  },
}))
