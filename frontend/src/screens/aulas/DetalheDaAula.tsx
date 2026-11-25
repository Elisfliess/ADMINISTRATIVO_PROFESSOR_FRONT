import { useState } from 'react'
import { navigate } from '../../router/hashRouter'
import {
  AULA_DETALHE,
  BADGE_AULA_STATUS,
  type AulaDetalhe,
  type ModalAula,
  type StatusAula,
} from './dados'

export interface DetalheDaAulaProps {
  modalInicial?: ModalAula
}

const CARD = {
  background: '#FAF6F0',
  borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
} as const

export function DetalheDaAula({ modalInicial = null }: DetalheDaAulaProps) {
  const [aula, setAula] = useState<AulaDetalhe>(AULA_DETALHE)
  const [modalAberto, setModalAberto] = useState<ModalAula>(modalInicial)
  const [aviso, setAviso] = useState<string | null>(null)

  const badge = BADGE_AULA_STATUS[aula.status]

  function aplicarStatus(status: StatusAula) {
    setAula((prev) => ({ ...prev, status }))
    setModalAberto(null)
    setAviso(`Status da aula atualizado para ${status}.`)
  }

  return (
    <section className="relative space-y-6">
      <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
        Detalhe da aula
      </h1>

      <div className="text-sm font-medium" style={{ color: 'var(--muted)' }}>
        {aula.diaLabel}
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

      <div
        className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border p-6"
        style={CARD}
      >
        <div className="flex items-center gap-4">
          <div
            className="grid h-14 w-14 shrink-0 place-items-center rounded-full text-sm font-medium"
            style={{ background: '#EAE1D5', color: '#3B281B' }}
            aria-hidden="true"
          >
            {aula.iniciais}
          </div>
          <div>
            <div className="text-lg font-bold leading-snug" style={{ color: '#2B1B10' }}>
              {aula.aluno}
            </div>
            <div className="mt-1 text-xs" style={{ color: '#6E5D52' }}>
              {aula.horarioInicio} às {aula.horarioFim}
            </div>
          </div>
        </div>

        <span
          className="inline-block min-w-[92px] rounded-full px-4 py-1.5 text-center text-xs font-medium"
          style={{ background: badge.bg, color: badge.fg }}
        >
          {aula.status}
        </span>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <InfoCard titulo="Modalidade" valor={aula.modalidade} />
        <InfoCard titulo="Local" valor={aula.local} />
        <InfoCard titulo="Observação" valor={aula.observacao} />
        <InfoCard titulo="Aluno" valor={aula.planoAluno} />
        <InfoCard titulo="Próxima cobrança" valor={aula.proximaCobranca}>
          <span
            className="mt-2 inline-block rounded-full px-3 py-1 text-[11px] font-medium"
            style={{ background: '#DCECE3', color: '#23533A' }}
          >
            {aula.statusCobranca}
          </span>
        </InfoCard>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={() => setModalAberto('status')}
          className="rounded-xl px-6 py-3 text-xs font-semibold transition"
          style={{ background: '#5C3419', color: '#FFFFFF' }}
        >
          Marcar status
        </button>
        <button
          type="button"
          onClick={() => setModalAberto('remarcar')}
          className="rounded-xl px-6 py-3 text-xs font-medium transition"
          style={{ background: '#EAE1D5', color: '#3B281B' }}
        >
          Remarcar
        </button>
        <button
          type="button"
          onClick={() => setModalAberto('cancelar')}
          className="rounded-xl px-6 py-3 text-xs font-semibold transition"
          style={{ background: '#9E2A22', color: '#FFFFFF' }}
        >
          Cancelar aula
        </button>
        <button
          type="button"
          onClick={() => navigate('/P22-criar-ou-editar-aula')}
          className="rounded-xl px-6 py-3 text-xs font-medium transition"
          style={{ background: '#EAE1D5', color: '#3B281B' }}
        >
          Editar
        </button>
      </div>

      {modalAberto === 'remarcar' ? (
        <Modal titulo="Remarcar aula" onFechar={() => setModalAberto(null)}>
          <p className="text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
            A aula de {aula.aluno} sai de {aula.dataCurta}, às {aula.horarioInicio}.
          </p>

          <div className="space-y-3">
            <Campo id="remarcar-data" label="Nova data">
              <input
                id="remarcar-data"
                type="text"
                defaultValue="10/08/2026"
                className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
                style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#2B1B10' }}
              />
            </Campo>
            <Campo id="remarcar-horario" label="Novo horário">
              <input
                id="remarcar-horario"
                type="text"
                defaultValue={aula.horarioInicio}
                className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
                style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#2B1B10' }}
              />
            </Campo>
            <Campo id="remarcar-motivo" label="Motivo (opcional)">
              <input
                id="remarcar-motivo"
                type="text"
                className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
                style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#2B1B10' }}
              />
            </Campo>
          </div>

          <div className="space-y-2.5 pt-1">
            <button
              type="button"
              onClick={() => {
                setModalAberto(null)
                setAviso('Aula remarcada com sucesso.')
              }}
              className="w-full rounded-xl py-3 text-xs font-semibold transition"
              style={{ background: '#5C3419', color: '#FFFFFF' }}
            >
              Salvar remarcação
            </button>
            <BotaoVoltar onClick={() => setModalAberto(null)} />
          </div>
        </Modal>
      ) : null}

      {modalAberto === 'cancelar' ? (
        <Modal titulo="Cancelar aula?" codigo="N1" onFechar={() => setModalAberto(null)}>
          <p className="text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
            A aula de {aula.aluno} em {aula.dataCurta}, às {aula.horarioInicio}, será cancelada em
            definitivo e fica no histórico.
          </p>
          <div className="space-y-2.5 pt-1">
            <button
              type="button"
              onClick={() => aplicarStatus('Cancelado')}
              className="w-full rounded-xl py-3 text-xs font-semibold transition"
              style={{ background: '#9E2A22', color: '#FFFFFF' }}
            >
              Cancelar aula
            </button>
            <button
              type="button"
              onClick={() => setModalAberto(null)}
              className="w-full rounded-xl py-3 text-xs font-medium transition"
              style={{ background: '#EAE1D5', color: '#3B281B' }}
            >
              Manter aula
            </button>
          </div>
        </Modal>
      ) : null}

      {modalAberto === 'status' ? (
        <Modal
          titulo={`Como foi a aula de ${aula.aluno} em ${aula.dataCurta}, às ${aula.horarioInicio}?`}
          onFechar={() => setModalAberto(null)}
        >
          <div className="space-y-2.5 pt-1">
            <button
              type="button"
              onClick={() => aplicarStatus('Dado')}
              className="w-full rounded-xl py-3 text-xs font-semibold transition"
              style={{ background: '#5C3419', color: '#FFFFFF' }}
            >
              Aula dada
            </button>
            <button
              type="button"
              onClick={() => aplicarStatus('Falta')}
              className="w-full rounded-xl py-3 text-xs font-semibold transition"
              style={{ background: '#9E2A22', color: '#FFFFFF' }}
            >
              Faltou
            </button>
            <BotaoVoltar onClick={() => setModalAberto(null)} />
          </div>
        </Modal>
      ) : null}
    </section>
  )
}

function InfoCard({
  titulo,
  valor,
  children,
}: {
  titulo: string
  valor: string
  children?: React.ReactNode
}) {
  return (
    <div className="rounded-2xl border p-6" style={CARD}>
      <div className="text-xs font-medium" style={{ color: '#6E5D52' }}>
        {titulo}
      </div>
      <div className="mt-3 text-sm font-semibold" style={{ color: '#2B1B10' }}>
        {valor}
      </div>
      {children}
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
