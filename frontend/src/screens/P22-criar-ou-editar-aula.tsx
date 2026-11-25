import { useState } from 'react'
import { navigate } from '../router/hashRouter'
import { ALUNOS_AULA } from './aulas/dados'

const CAMPO = {
  background: '#F5EFE6',
  borderColor: '#E5DDD2',
  color: '#3B281B',
} as const

/** Frame do Figma: P22-criar-ou-editar-aula — claro / P22-criar-ou-editar-aula — escuro. */
export function P22CriarOuEditarAula() {
  const [aluno, setAluno] = useState(ALUNOS_AULA[0]!)
  const [data, setData] = useState('03/08/2026')
  const [horario, setHorario] = useState('19:00')
  const [duracao, setDuracao] = useState('60 minutos')
  const [observacao, setObservacao] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  function salvar(e: React.FormEvent) {
    e.preventDefault()
    if (!aluno || !data.trim() || !horario.trim() || !duracao.trim()) {
      setErro('Preencha aluno, data, horário e duração.')
      return
    }
    setErro(null)
    navigate('/P23-detalhe-da-aula')
  }

  return (
    <section className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
        Criar ou editar aula
      </h1>

      <form
        onSubmit={salvar}
        noValidate
        className="space-y-6 rounded-2xl border p-6 sm:p-8"
        style={{
          background: '#FAF6F0',
          borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
        }}
      >
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          <Campo id="aula-aluno" label="Aluno">
            <select
              id="aula-aluno"
              value={aluno}
              onChange={(e) => setAluno(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            >
              {ALUNOS_AULA.map((nome) => (
                <option key={nome} value={nome}>
                  {nome}
                </option>
              ))}
            </select>
          </Campo>

          <Campo id="aula-data" label="Data">
            <input
              id="aula-data"
              type="text"
              placeholder="dd/mm/aaaa"
              value={data}
              onChange={(e) => setData(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </Campo>

          <Campo id="aula-horario" label="Horário">
            <input
              id="aula-horario"
              type="text"
              placeholder="hh:mm"
              value={horario}
              onChange={(e) => setHorario(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </Campo>

          <Campo id="aula-duracao" label="Duração">
            <input
              id="aula-duracao"
              type="text"
              value={duracao}
              onChange={(e) => setDuracao(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </Campo>

          <div className="space-y-1.5 md:col-span-2">
            <label htmlFor="aula-observacao" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Observação
            </label>
            <textarea
              id="aula-observacao"
              rows={3}
              value={observacao}
              onChange={(e) => setObservacao(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </div>
        </div>

        {erro ? (
          <p role="alert" className="text-xs font-medium" style={{ color: '#9E2A22' }}>
            {erro}
          </p>
        ) : null}

        <div className="flex flex-wrap items-center gap-3 pt-2">
          <button
            type="submit"
            className="rounded-xl px-8 py-3 text-xs font-semibold transition"
            style={{ background: '#5C3419', color: '#FFFFFF' }}
          >
            Salvar aula
          </button>
          <button
            type="button"
            onClick={() => navigate('/P21-agenda')}
            className="rounded-xl px-7 py-3 text-xs font-medium transition"
            style={{ background: '#EAE1D5', color: '#3B281B' }}
          >
            Cancelar
          </button>
        </div>
      </form>
    </section>
  )
}

function Campo({
  id,
  label,
  children,
}: {
  id: string
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
        {label}
      </label>
      {children}
    </div>
  )
}
