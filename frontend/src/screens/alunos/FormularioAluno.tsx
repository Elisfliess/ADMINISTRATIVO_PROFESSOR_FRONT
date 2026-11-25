import { useState } from 'react'
import {
  DIAS_VENCIMENTO,
  PLANOS_OPCOES,
  type AlunoFicha,
} from './dados'

export interface DadosFormularioAluno {
  nome: string
  email: string
  telefone: string
  dataNascimento: string
  responsavel: string
  plano: string
  valorMensalidade: string
  diaVencimento: string
  lgpdAutorizado: boolean
}

export interface FormularioAlunoProps {
  titulo: string
  aluno?: AlunoFicha
  onSalvar: (dados: DadosFormularioAluno) => void
  onCancelar: () => void
}

const CAMPO = {
  background: '#F5EFE6',
  borderColor: '#E5DDD2',
  color: '#3B281B',
} as const

export function FormularioAluno({ titulo, aluno, onSalvar, onCancelar }: FormularioAlunoProps) {
  const [nome, setNome] = useState(aluno?.nome ?? '')
  const [email, setEmail] = useState(aluno?.email ?? '')
  const [telefone, setTelefone] = useState(aluno?.telefone ?? '')
  const [dataNascimento, setDataNascimento] = useState(aluno?.dataNascimento ?? '')
  const [responsavel, setResponsavel] = useState(
    aluno?.responsavel && aluno.responsavel !== '—' ? aluno.responsavel : '',
  )
  const [plano, setPlano] = useState(aluno?.planoTitulo ?? '')
  const [valorMensalidade, setValorMensalidade] = useState(aluno?.valorMensalidade ?? '')
  const [diaVencimento, setDiaVencimento] = useState(aluno?.diaVencimento ?? '')
  const [lgpdAutorizado, setLgpdAutorizado] = useState(aluno?.lgpdAutorizado ?? false)
  const [erros, setErros] = useState<Record<string, string>>({})

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const proximosErros: Record<string, string> = {}
    if (!nome.trim()) proximosErros.nome = 'Informe o nome completo.'
    if (email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      proximosErros.email = 'Informe um e-mail válido.'
    }
    if (!telefone.trim()) proximosErros.telefone = 'Informe o WhatsApp.'
    if (!dataNascimento.trim()) proximosErros.dataNascimento = 'Informe a data de nascimento.'
    if (!plano) proximosErros.plano = 'Selecione um plano.'
    setErros(proximosErros)
    if (Object.keys(proximosErros).length > 0) return

    onSalvar({
      nome: nome.trim(),
      email: email.trim(),
      telefone: telefone.trim(),
      dataNascimento: dataNascimento.trim(),
      responsavel: responsavel.trim() || '—',
      plano,
      valorMensalidade: valorMensalidade.trim(),
      diaVencimento: diaVencimento.trim() || '—',
      lgpdAutorizado,
    })
  }

  return (
    <section className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
        {titulo}
      </h1>

      <form
        onSubmit={handleSubmit}
        noValidate
        className="space-y-6 rounded-2xl border p-6 sm:p-8"
        style={{
          background: '#FAF6F0',
          borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
        }}
      >
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          <Campo id="aluno-nome" label="Nome completo" erro={erros.nome}>
            <input
              id="aluno-nome"
              type="text"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </Campo>

          <Campo id="aluno-email" label="E-mail" erro={erros.email}>
            <input
              id="aluno-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </Campo>

          <Campo id="aluno-whatsapp" label="WhatsApp" erro={erros.telefone}>
            <input
              id="aluno-whatsapp"
              type="text"
              value={telefone}
              onChange={(e) => setTelefone(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </Campo>

          <Campo id="aluno-nascimento" label="Data de nascimento" erro={erros.dataNascimento}>
            <input
              id="aluno-nascimento"
              type="text"
              placeholder="dd/mm/aaaa"
              value={dataNascimento}
              onChange={(e) => setDataNascimento(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </Campo>

          <Campo id="aluno-responsavel" label="Responsável (se menor de idade)">
            <input
              id="aluno-responsavel"
              type="text"
              value={responsavel}
              onChange={(e) => setResponsavel(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </Campo>

          <Campo id="aluno-plano" label="Plano" erro={erros.plano}>
            <select
              id="aluno-plano"
              value={plano}
              onChange={(e) => setPlano(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            >
              <option value="">Selecionar</option>
              {PLANOS_OPCOES.map((opcao) => (
                <option key={opcao} value={opcao}>
                  {opcao}
                </option>
              ))}
            </select>
          </Campo>

          <Campo id="aluno-valor" label="Valor da mensalidade">
            <input
              id="aluno-valor"
              type="text"
              placeholder="R$ 0,00"
              value={valorMensalidade}
              onChange={(e) => setValorMensalidade(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            />
          </Campo>

          <Campo id="aluno-vencimento" label="Dia de vencimento">
            <select
              id="aluno-vencimento"
              value={diaVencimento}
              onChange={(e) => setDiaVencimento(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={CAMPO}
            >
              <option value="">Selecionar</option>
              {DIAS_VENCIMENTO.map((dia) => (
                <option key={dia} value={dia}>
                  {dia}
                </option>
              ))}
            </select>
          </Campo>
        </div>

        <label className="flex cursor-pointer items-center gap-2.5 pt-1 text-xs" style={{ color: '#3B281B' }}>
          <input
            type="checkbox"
            checked={lgpdAutorizado}
            onChange={(e) => setLgpdAutorizado(e.target.checked)}
            className="h-4 w-4 rounded accent-[#5C3419]"
          />
          <span>O aluno (ou responsável) autorizou o tratamento dos dados, conforme a LGPD.</span>
        </label>

        <div className="flex flex-wrap items-center gap-3 pt-2">
          <button
            type="submit"
            className="rounded-xl px-8 py-3 text-xs font-semibold transition"
            style={{ background: '#5C3419', color: '#FFFFFF' }}
          >
            Salvar aluno
          </button>
          <button
            type="button"
            onClick={onCancelar}
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
  erro,
  children,
}: {
  id: string
  label: string
  erro?: string
  children: React.ReactNode
}) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
        {label}
      </label>
      {children}
      {erro ? (
        <p role="alert" className="text-xs font-medium" style={{ color: '#9E2A22' }}>
          {erro}
        </p>
      ) : null}
    </div>
  )
}
