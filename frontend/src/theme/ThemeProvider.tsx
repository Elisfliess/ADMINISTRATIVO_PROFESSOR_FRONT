import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ThemeContext, THEME_STORAGE_KEY } from './theme-store'
import type { ThemeMode } from './tokens'

function getInitialTheme(): ThemeMode {
  if (typeof window === 'undefined') return 'claro'

  const saved = window.localStorage.getItem(THEME_STORAGE_KEY)
  if (saved === 'claro' || saved === 'escuro') return saved

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<ThemeMode>(getInitialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    window.localStorage.setItem(THEME_STORAGE_KEY, theme)
  }, [theme])

  const toggleTheme = useCallback(() => {
    setTheme((current) => (current === 'claro' ? 'escuro' : 'claro'))
  }, [])

  const value = useMemo(() => ({ theme, setTheme, toggleTheme }), [theme, toggleTheme])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
