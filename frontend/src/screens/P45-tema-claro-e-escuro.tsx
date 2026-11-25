import { useTheme } from '../theme/useTheme'

type Tema = 'claro' | 'escuro'

/** Frame do Figma: P45-tema-claro-e-escuro — claro / P45-tema-claro-e-escuro — escuro. */
export function P45TemaClaroEEscuro() {
  const { theme, setTheme } = useTheme()

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
          Tema claro e escuro
        </h1>
        <p className="mt-1 text-xs" style={{ color: 'var(--muted)' }}>
          Escolha como a área do professor aparece
        </p>
      </div>

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
        <CartaoTema
          tema="claro"
          titulo="Claro"
          selecionado={theme === 'claro'}
          onSelecionar={() => setTheme('claro')}
        />
        <CartaoTema
          tema="escuro"
          titulo="Escuro"
          selecionado={theme === 'escuro'}
          onSelecionar={() => setTheme('escuro')}
        />
      </div>
    </section>
  )
}

function CartaoTema({
  tema,
  titulo,
  selecionado,
  onSelecionar,
}: {
  tema: Tema
  titulo: string
  selecionado: boolean
  onSelecionar: () => void
}) {
  const escuro = tema === 'escuro'

  return (
    <button
      type="button"
      onClick={onSelecionar}
      aria-pressed={selecionado}
      className="overflow-hidden rounded-2xl border p-2 text-left transition"
      style={{
        background: '#FAF6F0',
        borderColor: selecionado ? '#5C3419' : '#E5DDD2',
        boxShadow: selecionado ? '0 0 0 2px #5C3419' : 'none',
      }}
    >
      <div
        className="overflow-hidden rounded-xl border"
        style={{
          borderColor: escuro ? '#3A2315' : '#E6DDD0',
          background: escuro ? '#140E0A' : '#F7F2EA',
        }}
        aria-hidden="true"
      >
        <div
          className="flex items-center justify-between px-4 py-3"
          style={{ background: escuro ? '#23140A' : '#5C3419' }}
        >
          <span className="text-[11px] font-semibold text-white">Administrativo</span>
          <span
            className="grid h-6 w-6 place-items-center rounded-full text-[10px] font-bold"
            style={{
              background: escuro ? '#342217' : '#E7DDD0',
              color: escuro ? '#F2C99E' : '#5C3419',
            }}
          >
            JC
          </span>
        </div>
        <div className="grid grid-cols-[70px_1fr] gap-2 p-3">
          <div
            className="space-y-2 rounded-lg p-2"
            style={{ background: escuro ? '#1F150F' : '#FFFFFF' }}
          >
            {[0, 1, 2, 3].map((linha) => (
              <div
                key={linha}
                className="h-2 rounded-full"
                style={{ background: escuro ? '#38261B' : '#E7DDD0' }}
              />
            ))}
          </div>
          <div
            className="space-y-2 rounded-lg p-2"
            style={{ background: escuro ? '#1F150F' : '#FFFFFF' }}
          >
            <div
              className="h-3 w-2/3 rounded-full"
              style={{ background: escuro ? '#D49A6A' : '#5C3419' }}
            />
            <div
              className="h-2 rounded-full"
              style={{ background: escuro ? '#38261B' : '#E7DDD0' }}
            />
            <div
              className="h-2 w-5/6 rounded-full"
              style={{ background: escuro ? '#38261B' : '#E7DDD0' }}
            />
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between px-3 py-3">
        <span className="text-sm font-semibold" style={{ color: '#2B1B10' }}>
          {titulo}
        </span>
        <span
          className="grid h-5 w-5 place-items-center rounded-full border"
          style={{ borderColor: selecionado ? '#5C3419' : '#B9A899' }}
          aria-hidden="true"
        >
          {selecionado ? (
            <span className="h-2.5 w-2.5 rounded-full" style={{ background: '#5C3419' }} />
          ) : null}
        </span>
      </div>
    </button>
  )
}
