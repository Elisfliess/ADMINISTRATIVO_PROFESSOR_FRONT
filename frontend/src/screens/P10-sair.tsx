import { navigate } from '../router/hashRouter'
import { P21Agenda } from './P21-agenda'

/**
 * Frame do Figma: P10-sair — claro / P10-sair — escuro.
 * O frame mostra a agenda com o menu da conta aberto no canto superior direito.
 */
export function P10Sair() {
  return (
    <div className="relative">
      <P21Agenda />

      <div
        className="absolute right-2 top-0 z-40 w-64 overflow-hidden rounded-2xl border shadow-lg"
        style={{ background: '#FAF6F0', borderColor: '#D8CEBF' }}
      >
        <button
          type="button"
          onClick={() => navigate('/P43-configuracoes-de-conta')}
          className="block w-full border-b px-4 py-3 text-left text-[11px] font-semibold transition hover:bg-black/5"
          style={{ borderColor: '#E5DDD2', color: '#6E5D52' }}
        >
          Configurações de conta
        </button>
        <button
          type="button"
          onClick={() => navigate('/P01-login')}
          className="block w-full px-4 py-3 text-left text-xs font-semibold transition hover:bg-black/5"
          style={{ color: '#9E2A22' }}
        >
          Sair
        </button>
      </div>
    </div>
  )
}
