import { useState } from 'react'
import type { ModalidadePublica } from './P40-previa-simples'

export interface P35AbaModalidadesEValoresProps {
  /** Lista controlada pelo editor (P33). Quando ausente, a aba mantém estado próprio. */
  modalidades?: ModalidadePublica[]
  /** Callback do editor. Quando ausente, a aba adiciona ao estado local. */
  onAdicionarModalidade?: (nova: ModalidadePublica) => void
}

const MODALIDADES_PADRAO: ModalidadePublica[] = [
  {
    id: 'm1',
    modalidade: 'Aula individual',
    descricao: 'Atenção total ao seu ritmo.',
    duracao: '60 minutos',
    valor: 'R$ 150,00',
  },
  {
    id: 'm2',
    modalidade: 'Aula em dupla',
    descricao: 'Aprenda junto com alguém.',
    duracao: '60 minutos',
    valor: 'R$ 270,00',
  },
]

export function P35AbaModalidadesEValores({
  modalidades,
  onAdicionarModalidade,
}: P35AbaModalidadesEValoresProps) {
  const [modalidadesLocais, setModalidadesLocais] =
    useState<ModalidadePublica[]>(MODALIDADES_PADRAO)
  const [adicionando, setAdicionando] = useState(false)
  const [nome, setNome] = useState('')
  const [descricao, setDescricao] = useState('')
  const [duracao, setDuracao] = useState('60 minutos')
  const [valor, setValor] = useState('R$ 180,00')

  // Blindagem: a rota registrada pode renderizar a aba sem props.
  const lista = Array.isArray(modalidades) ? modalidades : modalidadesLocais

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!nome.trim()) return

    const nova: ModalidadePublica = {
      id: `mod-${Date.now()}`,
      modalidade: nome.trim(),
      descricao: descricao.trim() || 'Acompanhamento personalizado.',
      duracao: duracao.trim() || '60 minutos',
      valor: valor.trim() || 'R$ 150,00',
    }

    if (onAdicionarModalidade) {
      onAdicionarModalidade(nova)
    } else {
      setModalidadesLocais((prev) => [...prev, nova])
    }

    setNome('')
    setDescricao('')
    setDuracao('60 minutos')
    setValor('R$ 180,00')
    setAdicionando(false)
  }

  return (
    <div
      className="space-y-5 rounded-3xl border p-6"
      style={{
        background: '#FAF6F0',
        borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
      }}
    >
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left">
          <thead>
            <tr className="border-b text-xs font-medium" style={{ borderColor: '#E5DDD2', color: '#6E5D52' }}>
              <th className="px-4 py-3.5 font-medium">Modalidade</th>
              <th className="px-4 py-3.5 font-medium">Duração</th>
              <th className="px-4 py-3.5 font-medium">Valor</th>
            </tr>
          </thead>
          <tbody className="divide-y text-xs" style={{ borderColor: '#E5DDD2' }}>
            {lista.length === 0 ? (
              <tr>
                <td className="px-4 py-6 text-center text-xs" colSpan={3} style={{ color: '#6E5D52' }}>
                  Nenhuma modalidade cadastrada. Adicione a primeira abaixo.
                </td>
              </tr>
            ) : (
              lista.map((mod) => (
                <tr key={mod.id}>
                  <td className="px-4 py-5 font-semibold" style={{ color: '#2B1B10' }}>
                    {mod.modalidade}
                  </td>
                  <td className="px-4 py-5" style={{ color: '#3B281B' }}>
                    {mod.duracao}
                  </td>
                  <td className="px-4 py-5" style={{ color: '#3B281B' }}>
                    {mod.valor}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {adicionando ? (
        <form
          onSubmit={handleSubmit}
          className="grid grid-cols-1 gap-4 rounded-2xl border p-4 sm:grid-cols-2"
          style={{ background: '#F5EFE6', borderColor: '#E5DDD2' }}
        >
          <div>
            <label htmlFor="nova-mod-nome" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Modalidade
            </label>
            <input
              id="nova-mod-nome"
              type="text"
              placeholder="Ex.: Aula em grupo"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              className="mt-1 w-full rounded-xl border px-3 py-2 text-xs"
              style={{ background: '#FAF6F0', borderColor: '#E5DDD2', color: '#2B1B10' }}
            />
          </div>

          <div>
            <label htmlFor="nova-mod-valor" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Valor
            </label>
            <input
              id="nova-mod-valor"
              type="text"
              value={valor}
              onChange={(e) => setValor(e.target.value)}
              className="mt-1 w-full rounded-xl border px-3 py-2 text-xs"
              style={{ background: '#FAF6F0', borderColor: '#E5DDD2', color: '#2B1B10' }}
            />
          </div>

          <div>
            <label htmlFor="nova-mod-duracao" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Duração
            </label>
            <input
              id="nova-mod-duracao"
              type="text"
              value={duracao}
              onChange={(e) => setDuracao(e.target.value)}
              className="mt-1 w-full rounded-xl border px-3 py-2 text-xs"
              style={{ background: '#FAF6F0', borderColor: '#E5DDD2', color: '#2B1B10' }}
            />
          </div>

          <div>
            <label htmlFor="nova-mod-desc" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Descrição curta
            </label>
            <input
              id="nova-mod-desc"
              type="text"
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              className="mt-1 w-full rounded-xl border px-3 py-2 text-xs"
              style={{ background: '#FAF6F0', borderColor: '#E5DDD2', color: '#2B1B10' }}
            />
          </div>

          <div className="flex gap-2 sm:col-span-2">
            <button
              type="submit"
              className="rounded-xl px-5 py-2 text-xs font-semibold"
              style={{ background: '#5C3419', color: '#FFFFFF' }}
            >
              Adicionar
            </button>
            <button
              type="button"
              onClick={() => setAdicionando(false)}
              className="rounded-xl px-4 py-2 text-xs font-medium"
              style={{ background: '#EAE1D5', color: '#3B281B' }}
            >
              Cancelar
            </button>
          </div>
        </form>
      ) : (
        <button
          type="button"
          onClick={() => setAdicionando(true)}
          className="rounded-xl px-6 py-3 text-xs font-medium transition"
          style={{ background: '#EAE1D5', color: '#3B281B' }}
        >
          + Adicionar modalidade
        </button>
      )}
    </div>
  )
}
