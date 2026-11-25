import { useState } from 'react'
import { navigate } from '../router/hashRouter'
import { useTheme } from '../theme/useTheme'

export interface P43ConfiguracoesDeContaProps {
  abrirEncerramentoInicial?: boolean
}

const CARD = {
  background: '#FAF6F0',
  borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
} as const

const CAMPO = {
  background: '#F5EFE6',
  borderColor: '#E5DDD2',
  color: '#2B1B10',
} as const

export function P43ConfiguracoesDeConta({
  abrirEncerramentoInicial = false,
}: P43ConfiguracoesDeContaProps) {
  const { theme, setTheme } = useTheme()
  const [nome, setNome] = useState('Prof.º José Carlos')
  const [email, setEmail] = useState('jose@email.com')
  const [aviso, setAviso] = useState<string | null>(null)

  const [modalEncerrar, setModalEncerrar] = useState(abrirEncerramentoInicial)
  const [confirmacao, setConfirmacao] = useState('')
  const [senha, setSenha] = useState('')
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  function salvarDados(e: React.FormEvent) {
    e.preventDefault()
    if (!nome.trim()) {
      setAviso('Informe o nome da conta.')
      return
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setAviso('Informe um e-mail válido.')
      return
    }
    setAviso('Dados da conta salvos.')
  }

  function encerrarConta(e: React.FormEvent) {
    e.preventDefault()
    if (confirmacao.trim() !== 'ENCERRAR') {
      setErro('Digite exatamente ENCERRAR para confirmar.')
      return
    }
    if (!senha.trim()) {
      setErro('Informe sua senha para confirmar o encerramento.')
      return
    }
    navigate('/P01-login')
  }

  return (
    <section className="relative space-y-6">
      <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
        Configurações de conta
      </h1>

      {aviso ? (
        <div
          role="status"
          className="rounded-xl border px-4 py-2.5 text-xs font-medium"
          style={{ background: '#FAF6F0', borderColor: '#D8CEBF', color: '#5C3419' }}
        >
          {aviso}
        </div>
      ) : null}

      <form className="space-y-5 rounded-2xl border p-6" style={CARD} onSubmit={salvarDados} noValidate>
        <h2 className="text-base font-bold" style={{ color: '#2B1B10' }}>
          Dados da conta
        </h2>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="space-y-1.5">
            <label htmlFor="conta-nome" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Nome
            </label>
            <input
              id="conta-nome"
              type="text"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="conta-email" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              E-mail
            </label>
            <input
              id="conta-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </div>
        </div>

        <button
          type="submit"
          className="rounded-xl px-7 py-3 text-xs font-semibold transition"
          style={{ background: '#5C3419', color: '#FFFFFF' }}
        >
          Salvar alterações
        </button>
      </form>

      <div className="space-y-4 rounded-2xl border p-6" style={CARD}>
        <h2 className="text-base font-bold" style={{ color: '#2B1B10' }}>
          Aparência
        </h2>
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={() => setTheme('claro')}
            aria-pressed={theme === 'claro'}
            className="rounded-xl px-6 py-3 text-xs font-semibold transition"
            style={{
              background: theme === 'claro' ? '#5C3419' : '#EAE1D5',
              color: theme === 'claro' ? '#FFFFFF' : '#3B281B',
            }}
          >
            Claro
          </button>
          <button
            type="button"
            onClick={() => setTheme('escuro')}
            aria-pressed={theme === 'escuro'}
            className="rounded-xl px-6 py-3 text-xs font-semibold transition"
            style={{
              background: theme === 'escuro' ? '#5C3419' : '#EAE1D5',
              color: theme === 'escuro' ? '#FFFFFF' : '#3B281B',
            }}
          >
            Escuro
          </button>
        </div>
      </div>

      <div
        className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border p-6"
        style={{ background: '#F7E1E4', borderColor: '#E7C6CA' }}
      >
        <div>
          <div className="text-sm font-bold" style={{ color: '#7A2733' }}>
            Encerrar conta
          </div>
          <p className="mt-1 text-xs" style={{ color: '#7A2733' }}>
            Sua conta e a página pública saem do ar.
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            setErro(null)
            setConfirmacao('')
            setSenha('')
            setModalEncerrar(true)
          }}
          className="rounded-xl px-6 py-3 text-xs font-semibold transition"
          style={{ background: '#9E2A22', color: '#FFFFFF' }}
        >
          Encerrar conta
        </button>
      </div>

      {modalEncerrar ? (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Encerrar conta"
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          style={{ background: 'rgba(43, 27, 16, 0.45)' }}
          onClick={(event) => {
            if (event.target === event.currentTarget) setModalEncerrar(false)
          }}
        >
          <form
            onSubmit={encerrarConta}
            className="w-full max-w-md space-y-5 rounded-3xl p-7 shadow-xl"
            style={{ background: '#FAF6F0', color: '#2B1B10' }}
          >
            <div className="flex items-center justify-between gap-2">
              <h2 className="text-base font-bold">Encerrar conta</h2>
              <span
                className="rounded-full px-2.5 py-0.5 text-[11px] font-medium"
                style={{ background: '#EAE1D5', color: '#5C3419' }}
              >
                N3
              </span>
            </div>

            <p className="text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
              Sua conta e a página pública saem do ar. Digite ENCERRAR e sua senha para confirmar.
            </p>

            <div className="space-y-1.5">
              <label htmlFor="encerrar-confirmacao" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
                Confirmação
              </label>
              <input
                id="encerrar-confirmacao"
                type="text"
                placeholder="Digite ENCERRAR"
                value={confirmacao}
                onChange={(e) => {
                  setConfirmacao(e.target.value)
                  if (erro) setErro(null)
                }}
                className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
                style={CAMPO}
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="encerrar-senha" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
                Senha
              </label>
              <div
                className="flex items-center rounded-xl border px-3.5 py-2.5"
                style={{ background: '#F5EFE6', borderColor: '#E5DDD2' }}
              >
                <input
                  id="encerrar-senha"
                  type={mostrarSenha ? 'text' : 'password'}
                  value={senha}
                  onChange={(e) => {
                    setSenha(e.target.value)
                    if (erro) setErro(null)
                  }}
                  className="w-full bg-transparent text-sm outline-none"
                  style={{ color: '#2B1B10' }}
                />
                <button
                  type="button"
                  onClick={() => setMostrarSenha((prev) => !prev)}
                  className="ml-2 text-xs font-medium"
                  style={{ color: '#5A4A3F' }}
                >
                  {mostrarSenha ? 'Ocultar' : 'Mostrar'}
                </button>
              </div>
              {erro ? (
                <p role="alert" className="text-xs font-medium" style={{ color: '#9E2A22' }}>
                  {erro}
                </p>
              ) : null}
            </div>

            <div className="space-y-2.5 pt-1">
              <button
                type="submit"
                className="w-full rounded-xl py-3 text-xs font-semibold transition"
                style={{ background: '#9E2A22', color: '#FFFFFF' }}
              >
                Encerrar conta
              </button>
              <button
                type="button"
                onClick={() => setModalEncerrar(false)}
                className="w-full rounded-xl py-3 text-xs font-medium transition"
                style={{ background: '#EAE1D5', color: '#3B281B' }}
              >
                Cancelar
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </section>
  )
}
