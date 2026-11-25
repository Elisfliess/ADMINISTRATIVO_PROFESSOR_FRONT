import { useMemo, useState } from 'react'
import {
  BADGE_COBRANCA_FINANCEIRO,
  COBRANCAS_MOCK,
  FILTROS_FINANCEIRO,
  RESUMO_FINANCEIRO,
  type Cobranca,
  type FiltroFinanceiro,
  type ModalFinanceiro,
} from './dados'

export interface FinanceiroProps {
  filtroInicial?: FiltroFinanceiro
  modalInicial?: ModalFinanceiro
  /** Aluno alvo dos modais de baixa/cancelamento. Padrão: primeira cobrança em aberto. */
  alunoAlvoInicial?: string
}

const CARD = {
  background: '#FAF6F0',
  borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
} as const

export function Financeiro({
  filtroInicial = 'Todos',
  modalInicial = null,
  alunoAlvoInicial,
}: FinanceiroProps) {
  const [cobrancas, setCobrancas] = useState<Cobranca[]>(COBRANCAS_MOCK)
  const [filtro, setFiltro] = useState<FiltroFinanceiro>(filtroInicial)
  const [modalAberto, setModalAberto] = useState<ModalFinanceiro>(modalInicial)
  const [alunoAlvo, setAlunoAlvo] = useState<string>(
    alunoAlvoInicial ?? (modalInicial === 'cancelar' ? 'Rafaela Lima' : 'Letícia Aguiar'),
  )
  const [aviso, setAviso] = useState<string | null>(null)

  const visiveis = useMemo(() => {
    if (filtro === 'Todos') return cobrancas
    if (filtro === 'Pendentes') return cobrancas.filter((cob) => cob.status === 'Pendente')
    return cobrancas.filter((cob) => cob.status === 'Em atraso')
  }, [cobrancas, filtro])

  const alvo =
    cobrancas.find((cob) => cob.aluno === alunoAlvo) ??
    cobrancas.find((cob) => cob.status !== 'Pago' && cob.status !== 'Cancelado') ??
    cobrancas[0]!

  function abrirModal(tipo: Exclude<ModalFinanceiro, null>, aluno: string) {
    setAlunoAlvo(aluno)
    setModalAberto(tipo)
  }

  function darBaixa() {
    setCobrancas((prev) =>
      prev.map((cob) => (cob.id === alvo.id ? { ...cob, status: 'Pago' } : cob)),
    )
    setModalAberto(null)
    setAviso(`Pagamento de ${alvo.valor} de ${alvo.aluno} registrado.`)
  }

  function cancelarCobranca() {
    setCobrancas((prev) =>
      prev.map((cob) => (cob.id === alvo.id ? { ...cob, status: 'Cancelado' } : cob)),
    )
    setModalAberto(null)
    setAviso(`Cobrança de ${alvo.aluno} cancelada.`)
  }

  return (
    <section className="relative space-y-6">
      <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
        Financeiro
      </h1>

      <p className="text-xs" style={{ color: 'var(--muted)' }}>
        Só registro de status. O Compasso não processa pagamentos.
      </p>

      {aviso ? (
        <div
          role="status"
          className="rounded-xl border px-4 py-2.5 text-xs font-medium"
          style={{ background: '#FAF6F0', borderColor: '#D8CEBF', color: '#5C3419' }}
        >
          {aviso}
        </div>
      ) : null}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-sm font-semibold" style={{ color: '#2B1B10' }}>
          <span aria-hidden="true">‹</span>
          <span>Agosto de 2026</span>
        </div>

        <button
          type="button"
          onClick={() => abrirModal('exportar', alvo.aluno)}
          className="rounded-xl px-5 py-2.5 text-xs font-medium transition"
          style={{ background: '#EAE1D5', color: '#3B281B' }}
        >
          Exportar recebimentos
        </button>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <ResumoCard titulo="Recebido" valor={RESUMO_FINANCEIRO.recebido} cor="#23533A" />
        <ResumoCard titulo="Pendente" valor={RESUMO_FINANCEIRO.pendente} cor="#6E4F16" />
        <ResumoCard titulo="Em atraso" valor={RESUMO_FINANCEIRO.emAtraso} cor="#7A2733" />
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {FILTROS_FINANCEIRO.map((opcao) => {
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

      <div className="overflow-hidden rounded-2xl border" style={CARD}>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b text-xs font-medium" style={{ borderColor: '#E5DDD2', color: '#6E5D52' }}>
                <th className="px-6 py-4 font-medium">Aluno</th>
                <th className="px-6 py-4 font-medium">Vencimento</th>
                <th className="px-6 py-4 font-medium">Valor</th>
                <th className="px-6 py-4 font-medium">Status</th>
                <th className="px-6 py-4 font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y text-xs" style={{ borderColor: '#E5DDD2' }}>
              {visiveis.length === 0 ? (
                <tr>
                  <td className="px-6 py-8 text-center" colSpan={5} style={{ color: '#6E5D52' }}>
                    Nenhuma cobrança neste filtro.
                  </td>
                </tr>
              ) : (
                visiveis.map((cob) => {
                  const badge = BADGE_COBRANCA_FINANCEIRO[cob.status]
                  const emAberto = cob.status !== 'Pago' && cob.status !== 'Cancelado'
                  return (
                    <tr key={cob.id}>
                      <td className="px-6 py-5 font-semibold" style={{ color: '#2B1B10' }}>
                        {cob.aluno}
                      </td>
                      <td className="px-6 py-5" style={{ color: '#3B281B' }}>
                        {cob.vencimento}
                      </td>
                      <td className="px-6 py-5 font-semibold" style={{ color: '#2B1B10' }}>
                        {cob.valor}
                      </td>
                      <td className="px-6 py-5">
                        <span
                          className="inline-block min-w-[84px] rounded-full px-4 py-1.5 text-center text-xs font-medium"
                          style={{ background: badge.bg, color: badge.fg }}
                        >
                          {cob.status}
                        </span>
                      </td>
                      <td className="px-6 py-5">
                        {emAberto ? (
                          <div className="flex flex-wrap items-center gap-3">
                            <button
                              type="button"
                              onClick={() => abrirModal('baixa', cob.aluno)}
                              className="text-xs font-semibold transition hover:underline"
                              style={{ color: '#5C3419' }}
                            >
                              Dar baixa
                            </button>
                            <button
                              type="button"
                              onClick={() => abrirModal('cancelar', cob.aluno)}
                              className="text-xs font-medium transition hover:underline"
                              style={{ color: '#9E2A22' }}
                            >
                              Cancelar
                            </button>
                          </div>
                        ) : (
                          <span style={{ color: '#6E5D52' }}>—</span>
                        )}
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {modalAberto === 'baixa' ? (
        <Modal titulo="Dar baixa na cobrança" codigo="N2" onFechar={() => setModalAberto(null)}>
          <p className="text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
            Registrar o pagamento de {alvo.valor} de {alvo.aluno}, com vencimento em{' '}
            {alvo.vencimento}.
          </p>

          <div className="space-y-3">
            <Campo id="baixa-data" label="Data do pagamento">
              <input
                id="baixa-data"
                type="text"
                defaultValue="Hoje"
                className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
                style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#2B1B10' }}
              />
            </Campo>
            <Campo id="baixa-forma" label="Forma de pagamento">
              <select
                id="baixa-forma"
                defaultValue="Pix"
                className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
                style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#2B1B10' }}
              >
                <option>Pix</option>
                <option>Dinheiro</option>
                <option>Transferência</option>
              </select>
            </Campo>
          </div>

          <div className="space-y-2.5 pt-1">
            <button
              type="button"
              onClick={darBaixa}
              className="w-full rounded-xl py-3 text-xs font-semibold transition"
              style={{ background: '#5C3419', color: '#FFFFFF' }}
            >
              Dar baixa
            </button>
            <BotaoVoltar onClick={() => setModalAberto(null)} />
          </div>
        </Modal>
      ) : null}

      {modalAberto === 'exportar' ? (
        <Modal titulo="Exportar recebimentos" codigo="N2" onFechar={() => setModalAberto(null)}>
          <p className="text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
            Gera um arquivo com os recebimentos do período.
          </p>

          <div className="space-y-3">
            <Campo id="exportar-periodo" label="Período">
              <select
                id="exportar-periodo"
                defaultValue="Agosto de 2026"
                className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
                style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#2B1B10' }}
              >
                <option>Agosto de 2026</option>
                <option>Julho de 2026</option>
                <option>Junho de 2026</option>
              </select>
            </Campo>
            <Campo id="exportar-formato" label="Formato">
              <select
                id="exportar-formato"
                defaultValue="CSV"
                className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
                style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#2B1B10' }}
              >
                <option>CSV</option>
              </select>
            </Campo>
          </div>

          <div className="space-y-2.5 pt-1">
            <button
              type="button"
              onClick={() => {
                setModalAberto(null)
                setAviso('Arquivo de recebimentos gerado com sucesso.')
              }}
              className="w-full rounded-xl py-3 text-xs font-semibold transition"
              style={{ background: '#5C3419', color: '#FFFFFF' }}
            >
              Exportar
            </button>
            <BotaoVoltar onClick={() => setModalAberto(null)} />
          </div>
        </Modal>
      ) : null}

      {modalAberto === 'cancelar' ? (
        <Modal titulo="Cancelar cobrança?" codigo="N1" onFechar={() => setModalAberto(null)}>
          <p className="text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
            A cobrança de {alvo.valor} de {alvo.aluno} sai do controle do mês. Fica registrado no
            histórico do aluno.
          </p>
          <div className="space-y-2.5 pt-1">
            <button
              type="button"
              onClick={cancelarCobranca}
              className="w-full rounded-xl py-3 text-xs font-semibold transition"
              style={{ background: '#9E2A22', color: '#FFFFFF' }}
            >
              Cancelar cobrança
            </button>
            <button
              type="button"
              onClick={() => setModalAberto(null)}
              className="w-full rounded-xl py-3 text-xs font-medium transition"
              style={{ background: '#EAE1D5', color: '#3B281B' }}
            >
              Manter cobrança
            </button>
          </div>
        </Modal>
      ) : null}
    </section>
  )
}

function ResumoCard({ titulo, valor, cor }: { titulo: string; valor: string; cor: string }) {
  return (
    <div className="rounded-2xl border p-6" style={CARD}>
      <div className="text-xs font-medium" style={{ color: '#6E5D52' }}>
        {titulo}
      </div>
      <div className="mt-3 text-2xl font-bold" style={{ color: cor }}>
        {valor}
      </div>
    </div>
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

function Modal({
  titulo,
  codigo,
  onFechar,
  children,
}: {
  titulo: string
  codigo?: string
  onFechar: () => void
  children: React.ReactNode
}) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={titulo}
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(43, 27, 16, 0.45)' }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onFechar()
      }}
    >
      <div
        className="w-full max-w-md space-y-5 rounded-3xl p-7 shadow-xl"
        style={{ background: '#FAF6F0', color: '#2B1B10' }}
      >
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-base font-bold">{titulo}</h2>
          {codigo ? (
            <span
              className="rounded-full px-2.5 py-0.5 text-[11px] font-medium"
              style={{ background: '#EAE1D5', color: '#5C3419' }}
            >
              {codigo}
            </span>
          ) : null}
        </div>
        {children}
      </div>
    </div>
  )
}

function BotaoVoltar({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full rounded-xl py-3 text-xs font-medium transition"
      style={{ background: '#EAE1D5', color: '#3B281B' }}
    >
      Voltar
    </button>
  )
}
