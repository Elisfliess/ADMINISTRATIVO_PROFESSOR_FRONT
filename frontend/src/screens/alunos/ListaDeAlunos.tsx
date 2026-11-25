import { useMemo, useState } from 'react'
import { navigate } from '../../router/hashRouter'
import { ALUNOS_MOCK, BADGE_COBRANCA, type StatusAluno } from './dados'

type Filtro = 'Todos' | 'Ativos' | 'Inativos'

const FILTROS: Filtro[] = ['Todos', 'Ativos', 'Inativos']

export function ListaDeAlunos() {
  const [busca, setBusca] = useState('')
  const [filtro, setFiltro] = useState<Filtro>('Todos')

  const ativos = ALUNOS_MOCK.filter((aluno) => aluno.status === 'Ativo').length
  const inativos = ALUNOS_MOCK.length - ativos

  const alunos = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return ALUNOS_MOCK.filter((aluno) => {
      const combinaFiltro =
        filtro === 'Todos' ||
        (filtro === 'Ativos' && aluno.status === 'Ativo') ||
        (filtro === 'Inativos' && aluno.status === 'Inativo')
      const combinaBusca =
        !termo ||
        aluno.nome.toLowerCase().includes(termo) ||
        aluno.email.toLowerCase().includes(termo) ||
        aluno.planoTitulo.toLowerCase().includes(termo)
      return combinaFiltro && combinaBusca
    })
  }, [busca, filtro])

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
            Alunos
          </h1>
          <p className="mt-1 text-xs" style={{ color: 'var(--muted)' }}>
            {ativos} ativos e {inativos} {inativos === 1 ? 'inativo' : 'inativos'}
          </p>
        </div>

        <button
          type="button"
          onClick={() => navigate('/P12-cadastrar-aluno')}
          className="rounded-xl px-6 py-3 text-xs font-semibold transition"
          style={{ background: '#5C3419', color: '#FFFFFF' }}
        >
          + Cadastrar aluno
        </button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="min-w-[220px] flex-1">
          <label htmlFor="buscar-aluno" className="sr-only">
            Buscar aluno
          </label>
          <input
            id="buscar-aluno"
            type="search"
            placeholder="Buscar aluno"
            value={busca}
            onChange={(event) => setBusca(event.target.value)}
            className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
            style={{ background: '#FAF6F0', borderColor: '#E5DDD2', color: '#2B1B10' }}
          />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {FILTROS.map((opcao) => {
            const ativa = filtro === opcao
            return (
              <button
                key={opcao}
                type="button"
                onClick={() => setFiltro(opcao)}
                aria-pressed={ativa}
                className="rounded-full px-5 py-2.5 text-xs font-medium transition"
                style={{
                  background: ativa ? '#5C3419' : '#EAE1D5',
                  color: ativa ? '#FFFFFF' : '#3B281B',
                }}
              >
                {opcao}
              </button>
            )
          })}
        </div>
      </div>

      <div
        className="overflow-hidden rounded-2xl border"
        style={{
          background: '#FAF6F0',
          borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
        }}
      >
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b text-xs font-medium" style={{ borderColor: '#E5DDD2', color: '#6E5D52' }}>
                <th className="px-6 py-4 font-medium">Aluno</th>
                <th className="px-6 py-4 font-medium">Plano</th>
                <th className="px-6 py-4 font-medium">Próxima aula</th>
                <th className="px-6 py-4 font-medium">Cobrança</th>
                <th className="px-6 py-4 font-medium">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y text-xs" style={{ borderColor: '#E5DDD2' }}>
              {alunos.length === 0 ? (
                <tr>
                  <td className="px-6 py-8 text-center" colSpan={5} style={{ color: '#6E5D52' }}>
                    Nenhum aluno encontrado para “{busca}”.
                  </td>
                </tr>
              ) : (
                alunos.map((aluno) => (
                  <tr key={aluno.id}>
                    <td className="px-6 py-5">
                      <button
                        type="button"
                        onClick={() => navigate(`/P13-ficha-do-aluno?id=${aluno.id}`)}
                        className="text-left font-semibold hover:underline"
                        style={{ color: '#2B1B10' }}
                      >
                        {aluno.nome}
                      </button>
                    </td>
                    <td className="px-6 py-5" style={{ color: '#3B281B' }}>
                      {aluno.planoTitulo}
                    </td>
                    <td className="px-6 py-5" style={{ color: '#3B281B' }}>
                      {aluno.proximaAulaDataHora}
                    </td>
                    <td className="px-6 py-5">
                      <Badge status={aluno.cobrancaStatus} />
                    </td>
                    <td className="px-6 py-5">
                      <Badge status={aluno.status} />
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}

function Badge({ status }: { status: StatusAluno | keyof typeof BADGE_COBRANCA }) {
  const cores =
    status === 'Ativo'
      ? { bg: '#DCECE3', fg: '#23533A' }
      : status === 'Inativo'
        ? { bg: '#EAE1D5', fg: '#5A4A3F' }
        : BADGE_COBRANCA[status]
  return (
    <span
      className="inline-block min-w-[84px] rounded-full px-4 py-1.5 text-center text-xs font-medium"
      style={{ background: cores.bg, color: cores.fg }}
    >
      {status}
    </span>
  )
}
