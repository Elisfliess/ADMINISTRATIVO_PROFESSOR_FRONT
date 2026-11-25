import { useTheme } from '../theme/useTheme'

export function ThemeToggle() {
  const { theme, toggleTheme } = useTheme()
  const proximo = theme === 'claro' ? 'escuro' : 'claro'

  return (
    <button
      className="btn btn-secondary"
      onClick={toggleTheme}
      aria-label={`Ativar tema ${proximo}`}
    >
      <span aria-hidden="true">{theme === 'claro' ? '◐' : '◑'}</span>
      Tema {theme}
    </button>
  )
}
