import { useState } from 'react'
import { navigate, useHashRoute } from '../../router/hashRouter'
import {
  ALUNOS_MOCK,
  BADGE_AULA,
  BADGE_COBRANCA,
  iniciaisDe,
  type AbaFicha,
  type AlunoFicha,
  type ModalAluno,
} from './dados'
import { FormularioAluno, type DadosFormularioAluno } from './FormularioAluno'

export interface FichaDoAlunoProps {
  alunoId?: string
  abaInicial?: AbaFicha
  modalInicial?: ModalAluno
  modoEdicaoInicial?: boolean
}

const ABAS: AbaFicha[] = ['Resumo', 'Histórico de aulas', 'Cobranças']

const CARD = {
  background: '#FAF6F0',
  borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
} as const

export function FichaDoAluno({
  alunoId,
  abaInicial = 'Resumo',
  modalInicial = null,
  modoEdicaoInicial = false,
}: FichaDoAlunoProps) {
  const { search } = useHashRoute()
  const [alunos, setAlunos] = useState<AlunoFicha[]>(ALUNOS_MOCK)
  const [alunoAtivoId] = useState(
    alunoId ?? search.get('id') ?? ALUNOS_MOCK[0]!.id,
  )
  const [abaAtiva, setAbaAtiva] = useState<AbaFicha>(abaInicial)
  const [editando, setEditando] = useState(modoEdicaoInicial)
  const [modalAberto, setModalAberto] = useState<ModalAluno>(modalInicial)
  const [avisoAcao, setAvisoAcao] = useState<string | null>(null)

  const [senhaExportar, setSenhaExportar] = useState('')
  const [mostrarSenhaExportar, setMostrarSenhaExportar] = useState(false)
  const [confirmacaoAnonimizar, setConfirmacaoAnonimizar] = useState('')
  const [senhaAnonimizar, setSenhaAnonimizar] = useState('')
  const [mostrarSenhaAnonimizar, setMostrarSenhaAnonimizar] = useState(false)
  const [erroModal, setErroModal] = useState<string | null>(null)

  const aluno = alunos.find((item) => item.id === alunoAtivoId) ?? alunos[0]!

  function abrirModal(tipo: Exclude<ModalAluno, null>) {
    setErroModal(null)
    setSenhaExportar('')
    setMostrarSenhaExportar(false)
    setConfirmacaoAnonimizar('')
    setSenhaAnonimizar('')
    setMostrarSenhaAnonimizar(false)
    setModalAberto(tipo)
  }

  function salvarEdicao(dados: DadosFormularioAluno) {
    setAlunos((prev) =>
      prev.map((item) =>
        item.id === aluno.id
          ? {
              ...item,
              nome: dados.nome,
              iniciais: iniciaisDe(dados.nome),
              responsavel: dados.responsavel,
              email: dados.email || item.email,
              telefone: dados.telefone,
              dataNascimento: dados.dataNascimento,
              planoTitulo: dados.plano,
              valorMensalidade: dados.valorMensalidade || item.valorMensalidade,
              diaVencimento: dados.diaVencimento,
              planoDetalhe:
                dados.plano === 'Sem plano ativo'
                  ? 'Plano encerrado'
                  : `${dados.valorMensalidade || item.valorMensalidade}, vence dia ${dados.diaVencimento}`,
              lgpdAutorizado: dados.lgpdAutorizado,
            }
          : item
      )
    )
    setEditando(false)
    setAvisoAcao('Dados do aluno salvos.')
    navigate(`/P13-ficha-do-aluno?id=${aluno.id}`)
  }

  function confirmarInativacao() {
    setAlunos((prev) =>
      prev.map((item) =>
        item.id === aluno.id
          ? {
              ...item,
              status: 'Inativo',
              planoTitulo: 'Sem plano ativo',
              planoDetalhe: 'Plano encerrado',
            }
          : item
      )
    )
    setModalAberto(null)
    setAvisoAcao(`${aluno.nome} foi inativado e o plano foi encerrado.`)
  }

  function confirmarEncerrarPlano() {
    setAlunos((prev) =>
      prev.map((item) =>
        item.id === aluno.id
          ? {
              ...item,
              planoTitulo: 'Sem plano ativo',
              planoDetalhe: 'Plano encerrado',
            }
          : item
      )
    )
    setModalAberto(null)
    setAvisoAcao(`Plano de ${aluno.nome} encerrado.`)
  }

  function confirmarExportacao(e: React.FormEvent) {
    e.preventDefault()
    if (!senhaExportar.trim()) {
      setErroModal('Informe sua senha para confirmar a exportação.')
      return
    }
    setModalAberto(null)
    setAvisoAcao(`Arquivo com os dados de ${aluno.nome} gerado com sucesso.`)
  }

  function confirmarAnonimizacao(e: React.FormEvent) {
    e.preventDefault()
    if (confirmacaoAnonimizar.trim() !== 'ANONIMIZAR') {
      setErroModal('Digite exatamente ANONIMIZAR para confirmar.')
      return
    }
    if (!senhaAnonimizar.trim()) {
      setErroModal('Informe sua senha para confirmar a anonimização.')
      return
    }

    setAlunos((prev) =>
      prev.map((item) =>
        item.id === aluno.id
          ? {
              ...item,
              nome: 'Aluno anonimizado',
              iniciais: 'AN',
              status: 'Inativo',
              email: 'anonimizado@compasso.local',
              telefone: '(00) 00000-0000',
              responsavel: '—',
              lgpdAutorizado: false,
            }
          : item
      )
    )
    setModalAberto(null)
    setAvisoAcao('Dados do aluno anonimizados com sucesso.')
  }

  function darBaixaCobranca(cobrancaId: string) {
    setAlunos((prev) =>
      prev.map((item) =>
        item.id === aluno.id
          ? {
              ...item,
              cobrancas: item.cobrancas.map((cob) =>
                cob.id === cobrancaId ? { ...cob, status: 'Pago' } : cob,
              ),
            }
          : item
      )
    )
    setAvisoAcao('Cobrança marcada como Paga.')
  }

  if (editando) {
    return (
      <FormularioAluno
        titulo="Editar aluno"
        aluno={aluno}
        onSalvar={salvarEdicao}
        onCancelar={() => {
          setEditando(false)
          navigate(`/P13-ficha-do-aluno?id=${aluno.id}`)
        }}
      />
    )
  }

  return (
    <section className="relative space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
          Ficha do aluno
        </h1>
      </div>

      {avisoAcao ? (
        <div
          role="status"
          className="rounded-xl border px-4 py-2.5 text-xs font-medium"
          style={{ background: '#FAF6F0', borderColor: '#D8CEBF', color: '#5C3419' }}
        >
          {avisoAcao}
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
            {aluno.iniciais}
          </div>

          <div>
            <div className="text-lg font-bold leading-snug" style={{ color: '#2B1B10' }}>
              {aluno.nome}
            </div>
            <span
              className="mt-1.5 inline-block rounded-full px-3.5 py-0.5 text-xs font-medium"
              style={{
                background: aluno.status === 'Ativo' ? '#DCECE3' : '#EAE1D5',
                color: aluno.status === 'Ativo' ? '#23533A' : '#5A4A3F',
              }}
            >
              {aluno.status}
            </span>
          </div>
        </div>

        <button
          type="button"
          onClick={() => setEditando(true)}
          className="rounded-xl px-5 py-2.5 text-xs font-medium transition"
          style={{ background: '#EAE1D5', color: '#3B281B' }}
        >
          Editar aluno
        </button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        {ABAS.map((aba) => {
          const ativa = abaAtiva === aba
          return (
            <button
              key={aba}
              type="button"
              onClick={() => setAbaAtiva(aba)}
              className="rounded-full px-6 py-2.5 text-xs font-medium transition"
              style={{
                background: ativa ? '#5C3419' : '#EAE1D5',
                color: ativa ? '#FFFFFF' : '#3B281B',
              }}
            >
              {aba}
            </button>
          )
        })}
      </div>

      {abaAtiva === 'Resumo' ? (
        <>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <CardResumo titulo="Contato">
              <div className="mt-3 text-sm font-semibold" style={{ color: '#2B1B10' }}>
                {aluno.email}
              </div>
              <div className="mt-1.5 text-xs" style={{ color: '#6E5D52' }}>
                {aluno.telefone}
              </div>
            </CardResumo>

            <CardResumo titulo="Plano">
              <div className="mt-3 text-sm font-semibold" style={{ color: '#2B1B10' }}>
                {aluno.planoTitulo}
              </div>
              <div className="mt-1.5 text-xs" style={{ color: '#6E5D52' }}>
                {aluno.planoDetalhe}
              </div>
            </CardResumo>

            <CardResumo titulo="Próxima aula">
              <div className="mt-3 text-sm font-semibold" style={{ color: '#2B1B10' }}>
                {aluno.proximaAulaDataHora}
              </div>
              <div className="mt-1.5 text-xs" style={{ color: '#6E5D52' }}>
                {aluno.proximaAulaTipo}
              </div>
            </CardResumo>
          </div>

          <div className="space-y-4 rounded-2xl border p-6" style={CARD}>
            <div className="text-xs font-medium" style={{ color: '#6E5D52' }}>
              Ações do cadastro
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={() => abrirModal('encerrar-plano')}
                className="rounded-xl px-6 py-3 text-xs font-medium transition"
                style={{ background: '#EAE1D5', color: '#3B281B' }}
              >
                Encerrar plano
              </button>

              <button
                type="button"
                onClick={() => abrirModal('inativar')}
                className="rounded-xl px-6 py-3 text-xs font-medium transition"
                style={{ background: '#EAE1D5', color: '#3B281B' }}
              >
                Inativar aluno
              </button>

              <button
                type="button"
                onClick={() => abrirModal('exportar')}
                className="rounded-xl px-6 py-3 text-xs font-medium transition"
                style={{ background: '#EAE1D5', color: '#3B281B' }}
              >
                Exportar dados
              </button>

              <button
                type="button"
                onClick={() => abrirModal('anonimizar')}
                className="rounded-xl px-6 py-3 text-xs font-semibold transition"
                style={{ background: '#9E2A22', color: '#FFFFFF' }}
              >
                Excluir ou anonimizar
              </button>
            </div>
          </div>
        </>
      ) : null}

      {abaAtiva === 'Histórico de aulas' ? (
        <div className="overflow-hidden rounded-2xl border" style={CARD}>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left">
              <thead>
                <tr className="border-b text-xs font-medium" style={{ borderColor: '#E5DDD2', color: '#6E5D52' }}>
                  <th className="px-6 py-4 font-medium">Data</th>
                  <th className="px-6 py-4 font-medium">Horário</th>
                  <th className="px-6 py-4 font-medium">Modalidade</th>
                  <th className="px-6 py-4 font-medium">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y text-xs" style={{ borderColor: '#E5DDD2' }}>
                {aluno.historico.length === 0 ? (
                  <tr>
                    <td className="px-6 py-6 text-center" colSpan={4} style={{ color: '#6E5D52' }}>
                      Nenhuma aula registrada para este aluno.
                    </td>
                  </tr>
                ) : (
                  aluno.historico.map((aula) => {
                    const styleStatus = BADGE_AULA[aula.status]
                    return (
                      <tr key={aula.id}>
                        <td className="px-6 py-5 font-semibold" style={{ color: '#2B1B10' }}>
                          {aula.data}
                        </td>
                        <td className="px-6 py-5" style={{ color: '#3B281B' }}>
                          {aula.horario}
                        </td>
                        <td className="px-6 py-5" style={{ color: '#3B281B' }}>
                          {aula.tipo}
                        </td>
                        <td className="px-6 py-5">
                          <span
                            className="inline-block min-w-[92px] rounded-full px-4 py-1.5 text-center text-xs font-medium"
                            style={{ background: styleStatus.bg, color: styleStatus.fg }}
                          >
                            {aula.status}
                          </span>
                        </td>
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}

      {abaAtiva === 'Cobranças' ? (
        <div className="overflow-hidden rounded-2xl border" style={CARD}>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left">
              <thead>
                <tr className="border-b text-xs font-medium" style={{ borderColor: '#E5DDD2', color: '#6E5D52' }}>
                  <th className="px-6 py-4 font-medium">Vencimento</th>
                  <th className="px-6 py-4 font-medium">Valor</th>
                  <th className="px-6 py-4 font-medium">Status</th>
                  <th className="px-6 py-4 font-medium">Ação</th>
                </tr>
              </thead>
              <tbody className="divide-y text-xs" style={{ borderColor: '#E5DDD2' }}>
                {aluno.cobrancas.length === 0 ? (
                  <tr>
                    <td className="px-6 py-6 text-center" colSpan={4} style={{ color: '#6E5D52' }}>
                      Nenhuma cobrança registrada para este aluno.
                    </td>
                  </tr>
                ) : (
                  aluno.cobrancas.map((cob) => {
                    const styleBadge = BADGE_COBRANCA[cob.status]
                    const podeDarBaixa = cob.status !== 'Pago'
                    return (
                      <tr key={cob.id}>
                        <td className="px-6 py-5 font-semibold" style={{ color: '#2B1B10' }}>
                          {cob.vencimento}
                        </td>
                        <td className="px-6 py-5" style={{ color: '#3B281B' }}>
                          {cob.valor}
                        </td>
                        <td className="px-6 py-5">
                          <span
                            className="inline-block min-w-[84px] rounded-full px-4 py-1.5 text-center text-xs font-medium"
                            style={{ background: styleBadge.bg, color: styleBadge.fg }}
                          >
                            {cob.status}
                          </span>
                        </td>
                        <td className="px-6 py-5">
                          {podeDarBaixa ? (
                            <button
                              type="button"
                              onClick={() => darBaixaCobranca(cob.id)}
                              className="text-xs font-medium transition hover:underline"
                              style={{ color: '#7A4926' }}
                            >
                              Dar baixa
                            </button>
                          ) : null}
                        </td>
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}

      {modalAberto === 'inativar' ? (
        <Modal titulo="Inativar aluno?" codigo="N2" onFechar={() => setModalAberto(null)}>
          <div className="space-y-2 text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
            <p>
              {aluno.nome} sai da lista de ativos. 2 aulas futuras e 1 cobrança pendente delas serão
              canceladas e o plano será encerrado. Cobranças já pagas não mudam.
            </p>
            <p>Reativar não restaura aulas nem o plano.</p>
          </div>
          <div className="space-y-2.5 pt-1">
            <button
              type="button"
              onClick={confirmarInativacao}
              className="w-full rounded-xl py-3 text-xs font-semibold transition"
              style={{ background: '#5C3419', color: '#FFFFFF' }}
            >
              Inativar aluno
            </button>
            <BotaoVoltar onClick={() => setModalAberto(null)} />
          </div>
        </Modal>
      ) : null}

      {modalAberto === 'encerrar-plano' ? (
        <Modal titulo="Encerrar plano?" onFechar={() => setModalAberto(null)}>
          <p className="text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
            O plano atual de {aluno.nome} ({aluno.planoTitulo}) será encerrado. Cobranças já pagas
            permanecem inalteradas.
          </p>
          <div className="space-y-2.5 pt-1">
            <button
              type="button"
              onClick={confirmarEncerrarPlano}
              className="w-full rounded-xl py-3 text-xs font-semibold transition"
              style={{ background: '#5C3419', color: '#FFFFFF' }}
            >
              Encerrar plano
            </button>
            <BotaoVoltar onClick={() => setModalAberto(null)} />
          </div>
        </Modal>
      ) : null}

      {modalAberto === 'exportar' ? (
        <Modal titulo="Exportar dados do aluno" codigo="N3" onFechar={() => setModalAberto(null)}>
          <form onSubmit={confirmarExportacao} className="space-y-5">
            <p className="text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
              Gera um arquivo com os dados de {aluno.nome}. Por segurança, confirme sua senha.
            </p>

            <CampoSenha
              id="senha-exportar"
              label="Senha"
              valor={senhaExportar}
              onChange={(valor) => {
                setSenhaExportar(valor)
                if (erroModal) setErroModal(null)
              }}
              visivel={mostrarSenhaExportar}
              onToggleVisivel={() => setMostrarSenhaExportar((prev) => !prev)}
              erro={erroModal}
            />

            <div className="space-y-2.5 pt-1">
              <button
                type="submit"
                className="w-full rounded-xl py-3 text-xs font-semibold transition"
                style={{ background: '#5C3419', color: '#FFFFFF' }}
              >
                Exportar dados
              </button>
              <BotaoVoltar onClick={() => setModalAberto(null)} />
            </div>
          </form>
        </Modal>
      ) : null}

      {modalAberto === 'anonimizar' ? (
        <Modal
          titulo="Excluir ou anonimizar aluno"
          codigo="N3"
          onFechar={() => setModalAberto(null)}
        >
          <form onSubmit={confirmarAnonimizacao} className="space-y-5">
            <p className="text-xs leading-relaxed" style={{ color: '#5A4A3F' }}>
              Digite ANONIMIZAR e sua senha para confirmar.
            </p>

            <div className="space-y-1.5">
              <label htmlFor="confirmacao-anonimizar" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
                Confirmação
              </label>
              <input
                id="confirmacao-anonimizar"
                type="text"
                placeholder="Digite ANONIMIZAR"
                value={confirmacaoAnonimizar}
                onChange={(e) => {
                  setConfirmacaoAnonimizar(e.target.value)
                  if (erroModal) setErroModal(null)
                }}
                className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
                style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#2B1B10' }}
              />
            </div>

            <CampoSenha
              id="senha-anonimizar"
              label="Senha"
              valor={senhaAnonimizar}
              onChange={(valor) => {
                setSenhaAnonimizar(valor)
                if (erroModal) setErroModal(null)
              }}
              visivel={mostrarSenhaAnonimizar}
              onToggleVisivel={() => setMostrarSenhaAnonimizar((prev) => !prev)}
              erro={erroModal}
            />

            <div className="space-y-2.5 pt-1">
              <button
                type="submit"
                className="w-full rounded-xl py-3 text-xs font-semibold transition"
                style={{ background: '#9E2A22', color: '#FFFFFF' }}
              >
                Anonimizar aluno
              </button>
              <BotaoVoltar onClick={() => setModalAberto(null)} />
            </div>
          </form>
        </Modal>
      ) : null}
    </section>
  )
}

function CardResumo({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <div className="rounded-2xl border p-6" style={CARD}>
      <div className="text-xs font-medium" style={{ color: '#6E5D52' }}>
        {titulo}
      </div>
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

function CampoSenha({
  id,
  label,
  valor,
  onChange,
  visivel,
  onToggleVisivel,
  erro,
}: {
  id: string
  label: string
  valor: string
  onChange: (valor: string) => void
  visivel: boolean
  onToggleVisivel: () => void
  erro?: string | null
}) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
        {label}
      </label>
      <div
        className="flex items-center rounded-xl border px-3.5 py-2.5"
        style={{ background: '#F5EFE6', borderColor: '#E5DDD2' }}
      >
        <input
          id={id}
          type={visivel ? 'text' : 'password'}
          value={valor}
          onChange={(e) => onChange(e.target.value)}
          className="w-full bg-transparent text-sm outline-none"
          style={{ color: '#2B1B10' }}
        />
        <button
          type="button"
          onClick={onToggleVisivel}
          className="ml-2 text-xs font-medium"
          style={{ color: '#5A4A3F' }}
        >
          {visivel ? 'Ocultar' : 'Mostrar'}
        </button>
      </div>
      {erro ? (
        <p role="alert" className="text-xs font-medium" style={{ color: '#9E2A22' }}>
          {erro}
        </p>
      ) : null}
    </div>
  )
}
