import { useState } from 'react'

const CARD = {
  background: '#FAF6F0',
  borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
} as const

const CAMPO = {
  background: '#F5EFE6',
  borderColor: '#E5DDD2',
  color: '#2B1B10',
} as const

/** Frame do Figma: P49-plano-encerrar-e-novo — claro / P49-plano-encerrar-e-novo — escuro. */
export function P49PlanoEncerrarENovo() {
  const [planoAtivo, setPlanoAtivo] = useState(true)
  const [modalidade, setModalidade] = useState('')
  const [valor, setValor] = useState('R$ 0,00')
  const [inicio, setInicio] = useState('Hoje')
  const [aviso, setAviso] = useState<string | null>(null)

  function encerrarPlano() {
    setPlanoAtivo(false)
    setAviso('Plano atual encerrado. Crie um novo plano quando quiser.')
  }

  function comecarNovoPlano(e: React.FormEvent) {
    e.preventDefault()
    if (!modalidade) {
      setAviso('Selecione uma modalidade para o novo plano.')
      return
    }
    setPlanoAtivo(true)
    setAviso(`Novo plano iniciado: ${modalidade}, ${valor}.`)
  }

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
          Plano do aluno
        </h1>
        <p className="mt-1 text-xs" style={{ color: 'var(--muted)' }}>
          Vagner Santos
        </p>
      </div>

      {aviso ? (
        <div
          role="status"
          className="rounded-xl border px-4 py-2.5 text-xs font-medium"
          style={{ background: '#FAF6F0', borderColor: '#D8CEBF', color: '#5C3419' }}
        >
          {aviso}
        </div>
      ) : null}

      <div className="grid grid-cols-1 items-center gap-5 lg:grid-cols-[1fr_auto_1fr]">
        <div className="space-y-4 rounded-2xl border p-6" style={CARD}>
          <h2 className="text-base font-bold" style={{ color: '#2B1B10' }}>
            Plano atual
          </h2>

          <div>
            <div className="text-sm font-semibold" style={{ color: '#2B1B10' }}>
              {planoAtivo ? 'Mensal, 2x por semana' : 'Sem plano ativo'}
            </div>
            <div className="mt-1 text-xs" style={{ color: '#6E5D52' }}>
              {planoAtivo ? 'R$ 270,00 por mês' : 'Plano encerrado'}
            </div>
            <div className="mt-1 text-xs" style={{ color: '#6E5D52' }}>
              {planoAtivo ? 'Desde 05/03/2026' : '—'}
            </div>
          </div>

          <button
            type="button"
            onClick={encerrarPlano}
            disabled={!planoAtivo}
            className="rounded-xl px-5 py-2.5 text-xs font-medium transition disabled:opacity-50"
            style={{ background: '#EAE1D5', color: '#3B281B' }}
          >
            Encerrar plano
          </button>
        </div>

        <div
          className="hidden text-center text-xl lg:block"
          style={{ color: '#8A786B' }}
          aria-hidden="true"
        >
          →
        </div>

        <form className="space-y-4 rounded-2xl border p-6" style={CARD} onSubmit={comecarNovoPlano}>
          <h2 className="text-base font-bold" style={{ color: '#2B1B10' }}>
            Novo plano
          </h2>

          <div className="space-y-1.5">
            <label htmlFor="plano-modalidade" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Modalidade
            </label>
            <select
              id="plano-modalidade"
              value={modalidade}
              onChange={(e) => setModalidade(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            >
              <option value="">Selecionar</option>
              <option>Aula individual</option>
              <option>Aula em dupla</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="plano-valor" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Valor mensal
            </label>
            <input
              id="plano-valor"
              type="text"
              value={valor}
              onChange={(e) => setValor(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="plano-inicio" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Início
            </label>
            <input
              id="plano-inicio"
              type="text"
              value={inicio}
              onChange={(e) => setInicio(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </div>

          <button
            type="submit"
            className="rounded-xl px-6 py-3 text-xs font-semibold transition"
            style={{ background: '#5C3419', color: '#FFFFFF' }}
          >
            Começar novo plano
          </button>
        </form>
      </div>
    </section>
  )
}
