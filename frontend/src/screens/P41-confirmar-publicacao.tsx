import { useState } from 'react'

export interface P41ConfirmarPublicacaoProps {
  aberto?: boolean
  onConfirmar?: () => void
  onRejeitar?: () => void
}

export function P41ConfirmarPublicacao({
  aberto = true,
  onConfirmar,
  onRejeitar,
}: P41ConfirmarPublicacaoProps) {
  const [visivelInterno, setVisivelInterno] = useState(true)

  const estaAberto = onConfirmar || onRejeitar ? aberto : visivelInterno
  if (!estaAberto) return null

  function handleConfirmar() {
    if (onConfirmar) {
      onConfirmar()
    } else {
      setVisivelInterno(false)
    }
  }

  function handleRejeitar() {
    if (onRejeitar) {
      onRejeitar()
    } else {
      setVisivelInterno(false)
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-modal-publicacao"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(43, 27, 16, 0.45)' }}
    >
      <div
        className="w-full max-w-md space-y-5 rounded-3xl p-7 shadow-xl"
        style={{ background: '#FAF6F0', color: '#2B1B10' }}
      >
        <div className="flex items-center justify-between gap-2">
          <h2 id="titulo-modal-publicacao" className="text-base font-bold">
            Aprovar a publicação?
          </h2>
          <span
            className="rounded-full px-2.5 py-0.5 text-[11px] font-medium"
            style={{ background: '#EAE1D5', color: '#5C3419' }}
          >
            N2
          </span>
        </div>

        <div className="space-y-2 text-xs leading-relaxed" style={{ color: '#6E5D52' }}>
          <p>Qualquer pessoa com o link poderá ver a página.</p>
          <p>Se rejeitar, a página pública atual continua como está.</p>
        </div>

        <div className="space-y-2.5 pt-1">
          <button
            type="button"
            onClick={handleConfirmar}
            className="w-full rounded-xl py-3 text-xs font-semibold transition hover:brightness-95"
            style={{ background: '#5C3419', color: '#FFFFFF' }}
          >
            Publicar
          </button>
          <button
            type="button"
            onClick={handleRejeitar}
            className="w-full rounded-xl py-3 text-xs font-medium transition hover:brightness-95"
            style={{ background: '#EAE1D5', color: '#3B281B' }}
          >
            Rejeitar
          </button>
        </div>
      </div>
    </div>
  )
}