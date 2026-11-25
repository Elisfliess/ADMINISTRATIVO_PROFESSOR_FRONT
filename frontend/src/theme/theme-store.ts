import { createContext } from 'react'
import type { ThemeMode } from './tokens'

export const THEME_STORAGE_KEY = 'compasso-tema'

export interface ThemeContextValue {
  theme: ThemeMode
  setTheme: (theme: ThemeMode) => void
  toggleTheme: () => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)
