import { navigate } from '../router/hashRouter'

/** Frame do Figma: V03-pagina-nao-encontrada — claro / V03-pagina-nao-encontrada — escuro. */
export function V03PaginaNaoEncontrada() {
  return (
    <div className="flex min-h-screen flex-col" style={{ background: 'var(--bg)', color: 'var(--text)' }}>
      <header className="border-b" style={{ borderColor: 'color-mix(in srgb, var(--border) 30%, transparent)' }}>
        <div className="mx-auto flex w-full max-w-5xl items-center gap-2 px-6 py-4">
          <span
            className="grid h-9 w-9 place-items-center rounded-xl text-base font-bold"
            style={{ background: 'var(--link)', color: 'var(--bg)' }}
            aria-hidden="true"
          >
            C
          </span>
          <strong className="text-base">Compasso</strong>
        </div>
      </header>

      <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col items-center justify-center px-6 py-16 text-center">
        <div
          className="text-6xl font-bold tracking-tight"
          style={{ color: '#7A4926' }}
          aria-hidden="true"
        >
          404
        </div>
        <h1 className="mt-4 text-2xl font-bold">Página não encontrada</h1>
        <p className="mt-2 max-w-md text-sm leading-relaxed" style={{ color: 'var(--muted)' }}>
          Esta página não existe ou o endereço está incorreto.
        </p>
        <button
          type="button"
          onClick={() => navigate('/P01-login')}
          className="mt-8 rounded-xl px-7 py-3 text-xs font-semibold transition"
          style={{ background: '#5C3419', color: '#FFFFFF' }}
        >
          Ir para o Compasso
        </button>
      </main>
    </div>
  )
}
