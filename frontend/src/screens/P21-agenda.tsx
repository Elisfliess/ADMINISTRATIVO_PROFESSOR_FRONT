import { useMemo, useState } from 'react'

type StatusAula = 'Agendado' | 'Dado' | 'Falta' | 'Cancelado'
type FiltroAgenda = 'Hoje' | 'Mês'
type TelaAgenda = 'lista' | 'formulario' | 'detalhe'
type ModalAula = null | 'remarcar' | 'cancelar' | 'status'

interface AulaItem {
  id: string
  /** Data no formato YYYY-MM-DD. */
  data: string
  aluno: string
  iniciais: string
  horarioInicio: string
  horarioFim: string
  duracao: string
  modalidade: string
  local: string
  observacao: string
  status: StatusAula
  planoAluno: string
  proximaCobranca: string
  statusCobranca: 'Pago' | 'Pendente'
}

const MESES = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
] as const

const DIAS_SEMANA_LONGOS = [
  'Domingo',
  'Segunda-feira',
  'Terça-feira',
  'Quarta-feira',
  'Quinta-feira',
  'Sexta-feira',
  'Sábado',
] as const

const DIAS_SEMANA_CURTOS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'] as const

const ALUNOS_DISPONIVEIS = [
  { nome: 'Vagner Santos', iniciais: 'VS', plano: 'Mensal, 2x por semana', cobranca: '05/12/2026, R$ 270,00', statusCob: 'Pago' as const },
  { nome: 'Leticia Aguiar', iniciais: 'LA', plano: 'Mensal, 1x por semana', cobranca: '10/08/2026, R$ 180,00', statusCob: 'Pago' as const },
  { nome: 'André Soares', iniciais: 'AS', plano: 'Mensal, 2x por semana', cobranca: '05/08/2026, R$ 270,00', statusCob: 'Pendente' as const },
  { nome: 'Tercia Souza', iniciais: 'TS', plano: 'Mensal, 1x por semana', cobranca: '15/08/2026, R$ 180,00', statusCob: 'Pago' as const },
]

const ESTILO_STATUS: Record<StatusAula, { bg: string; fg: string; label: string }> = {
  Agendado: { bg: '#E9DFD3', fg: '#5C3419', label: 'Agendado' },
  Dado: { bg: '#DCECE3', fg: '#23533A', label: 'Dado' },
  Falta: { bg: '#F7E1E4', fg: '#7A2733', label: 'Falta' },
  Cancelado: { bg: '#F4E6C8', fg: '#6E4F16', label: 'Cancelado' },
}

/** Data de referência da sessão ("hoje"). */
const HOJE = new Date()
const HOJE_ISO = isoDaData(HOJE)

function isoDaData(data: Date): string {
  const ano = data.getFullYear()
  const mes = String(data.getMonth() + 1).padStart(2, '0')
  const dia = String(data.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}

/** Converte YYYY-MM-DD em Date (meia-noite local) validando datas inexistentes. */
function dataDoISO(iso: string): Date | null {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso.trim())
  if (!partes) return null
  const ano = Number(partes[1])
  const mes = Number(partes[2])
  const dia = Number(partes[3])
  const data = new Date(ano, mes - 1, dia)
  if (
    data.getFullYear() !== ano ||
    data.getMonth() !== mes - 1 ||
    data.getDate() !== dia
  ) {
    return null
  }
  return data
}

function formatarDataCurta(iso: string): string {
  const data = dataDoISO(iso)
  if (!data) return iso
  const dia = String(data.getDate()).padStart(2, '0')
  const mes = String(data.getMonth() + 1).padStart(2, '0')
  return `${dia}/${mes}/${data.getFullYear()}`
}

function formatarDataLonga(iso: string): string {
  const data = dataDoISO(iso)
  if (!data) return iso
  const diaSemana = DIAS_SEMANA_LONGOS[data.getDay()] ?? ''
  const mes = MESES[data.getMonth()] ?? ''
  return `${diaSemana}, ${data.getDate()} de ${mes.toLowerCase()} de ${data.getFullYear()}`
}

function nomeDoMes(mes: number): string {
  return MESES[mes] ?? ''
}

function somarDias(base: Date, dias: number): Date {
  const data = new Date(base.getFullYear(), base.getMonth(), base.getDate())
  data.setDate(data.getDate() + dias)
  return data
}

function chaveMes(ano: number, mes: number): string {
  return `${ano}-${String(mes + 1).padStart(2, '0')}`
}

function calcularHoraFim(inicio: string, duracaoStr: string): string {
  const match = inicio.match(/^(\d{1,2}):(\d{2})/)
  const minutosDuracao = parseInt(duracaoStr, 10) || 60
  if (!match) return '20:00'
  const horas = parseInt(match[1]!, 10)
  const minutos = parseInt(match[2]!, 10)
  const totalMinutos = (horas * 60 + minutos + minutosDuracao) % (24 * 60)
  const hFim = String(Math.floor(totalMinutos / 60)).padStart(2, '0')
  const mFim = String(totalMinutos % 60).padStart(2, '0')
  return `${hFim}:${mFim}`
}

interface DadosAula {
  id: string
  data: string
  aluno: string
  horarioInicio: string
  status?: StatusAula
  duracao?: string
  local?: string
  observacao?: string
}

function montarAula({
  id,
  data,
  aluno,
  horarioInicio,
  status = 'Agendado',
  duracao = '60 minutos',
  local = 'Online',
  observacao = '',
}: DadosAula): AulaItem {
  const dados = ALUNOS_DISPONIVEIS.find((item) => item.nome === aluno) ?? ALUNOS_DISPONIVEIS[0]!
  return {
    id,
    data,
    aluno: dados.nome,
    iniciais: dados.iniciais,
    horarioInicio,
    horarioFim: calcularHoraFim(horarioInicio, duracao),
    duracao,
    modalidade: 'Aula individual',
    local,
    observacao,
    status,
    planoAluno: dados.plano,
    proximaCobranca: dados.cobranca,
    statusCobranca: dados.statusCob,
  }
}

/** Exemplos espalhados pelo ano corrente e ao redor de hoje. */
function criarAulasIniciais(): AulaItem[] {
  const ano = HOJE.getFullYear()
  const deslocada = (dias: number) => isoDaData(somarDias(HOJE, dias))
  const statusDe = (data: string, quandoPassado: StatusAula = 'Dado'): StatusAula =>
    data < HOJE_ISO ? quandoPassado : 'Agendado'

  return [
    montarAula({
      id: 'aula-hoje-1',
      data: HOJE_ISO,
      aluno: 'Leticia Aguiar',
      horarioInicio: '10:00',
      status: 'Dado',
      observacao: 'Prática de conversação',
    }),
    montarAula({
      id: 'aula-hoje-2',
      data: HOJE_ISO,
      aluno: 'Vagner Santos',
      horarioInicio: '19:00',
      observacao: 'Revisar o conteúdo da semana',
    }),
    montarAula({
      id: 'aula-antes-1',
      data: deslocada(-2),
      aluno: 'André Soares',
      horarioInicio: '14:00',
      status: 'Dado',
      local: 'Presencial',
      observacao: 'Exercícios de fixação',
    }),
    montarAula({
      id: 'aula-depois-1',
      data: deslocada(1),
      aluno: 'Tercia Souza',
      horarioInicio: '09:00',
      observacao: 'Retomar tópicos pendentes',
    }),
    montarAula({
      id: 'aula-depois-2',
      data: deslocada(3),
      aluno: 'Vagner Santos',
      horarioInicio: '16:00',
      status: 'Cancelado',
      observacao: 'Aula cancelada a pedido do aluno',
    }),
    montarAula({
      id: 'aula-mes-anterior',
      data: deslocada(-35),
      aluno: 'Tercia Souza',
      horarioInicio: '15:00',
      status: 'Dado',
    }),
    montarAula({
      id: 'aula-proximo-mes',
      data: deslocada(40),
      aluno: 'André Soares',
      horarioInicio: '18:00',
      status: 'Agendado',
    }),
    montarAula({
      id: 'aula-marco',
      data: `${ano}-03-10`,
      aluno: 'Leticia Aguiar',
      horarioInicio: '11:00',
      status: statusDe(`${ano}-03-10`),
    }),
    montarAula({
      id: 'aula-junho',
      data: `${ano}-06-17`,
      aluno: 'André Soares',
      horarioInicio: '14:00',
      status: statusDe(`${ano}-06-17`),
    }),
    montarAula({
      id: 'aula-setembro',
      data: `${ano}-09-08`,
      aluno: 'Tercia Souza',
      horarioInicio: '09:00',
      status: statusDe(`${ano}-09-08`, 'Falta'),
    }),
    montarAula({
      id: 'aula-dezembro',
      data: `${ano}-12-15`,
      aluno: 'Vagner Santos',
      horarioInicio: '19:00',
      status: statusDe(`${ano}-12-15`),
    }),
  ]
}

export function P21Agenda() {
  const [aulas, setAulas] = useState<AulaItem[]>(criarAulasIniciais)
  const [filtro, setFiltro] = useState<FiltroAgenda>('Mês')
  const [mesRef, setMesRef] = useState(() => ({ ano: HOJE.getFullYear(), mes: HOJE.getMonth() }))
  const [telaAtual, setTelaAtual] = useState<TelaAgenda>('lista')
  const [aulaSelecionadaId, setAulaSelecionadaId] = useState<string>('aula-hoje-2')
  const [editandoId, setEditandoId] = useState<string | null>(null)
  const [modalAberto, setModalAberto] = useState<ModalAula>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  // Campos do formulário "Criar ou editar aula"
  const [alunoForm, setAlunoForm] = useState('Vagner Santos')
  const [dataForm, setDataForm] = useState(HOJE_ISO)
  const [horarioForm, setHorarioForm] = useState('19:00')
  const [duracaoForm, setDuracaoForm] = useState('60 minutos')
  const [observacaoForm, setObservacaoForm] = useState('')

  // Campos do modal "Remarcar aula"
  const [novaDataModal, setNovaDataModal] = useState(HOJE_ISO)
  const [novoHorarioModal, setNovoHorarioModal] = useState('19:00')
  const [motivoModal, setMotivoModal] = useState('')

  const aulasOrdenadas = useMemo(
    () =>
      [...aulas].sort(
        (a, b) => a.data.localeCompare(b.data) || a.horarioInicio.localeCompare(b.horarioInicio),
      ),
    [aulas],
  )

  const aulasHoje = useMemo(
    () => aulasOrdenadas.filter((aula) => aula.data === HOJE_ISO),
    [aulasOrdenadas],
  )

  const prefixoMes = chaveMes(mesRef.ano, mesRef.mes)

  const aulasDoMes = useMemo(
    () => aulasOrdenadas.filter((aula) => aula.data.startsWith(prefixoMes)),
    [aulasOrdenadas, prefixoMes],
  )

  const calendario = useMemo(() => {
    const primeiroDiaSemana = new Date(mesRef.ano, mesRef.mes, 1).getDay()
    const totalDias = new Date(mesRef.ano, mesRef.mes + 1, 0).getDate()
    const dias = Array.from({ length: totalDias }, (_, indice) =>
      isoDaData(new Date(mesRef.ano, mesRef.mes, indice + 1)),
    )
    const vaziosFinais = (7 - ((primeiroDiaSemana + totalDias) % 7)) % 7

    const porData = new Map<string, AulaItem[]>()
    for (const aula of aulasDoMes) {
      const lista = porData.get(aula.data) ?? []
      lista.push(aula)
      porData.set(aula.data, lista)
    }
    for (const lista of porData.values()) {
      lista.sort((a, b) => a.horarioInicio.localeCompare(b.horarioInicio))
    }

    return { primeiroDiaSemana, dias, vaziosFinais, porData }
  }, [mesRef, aulasDoMes])

  const anosDisponiveis = useMemo(() => {
    const anos = new Set<number>([HOJE.getFullYear(), mesRef.ano])
    for (const aula of aulas) {
      const data = dataDoISO(aula.data)
      if (data) anos.add(data.getFullYear())
    }
    const menor = Math.min(...anos) - 1
    const maior = Math.max(...anos) + 1
    return Array.from({ length: maior - menor + 1 }, (_, indice) => menor + indice)
  }, [aulas, mesRef.ano])

  const aulaDetalhe = aulas.find((item) => item.id === aulaSelecionadaId) ?? aulas[0]!

  function navegarMes(delta: number) {
    setMesRef((atual) => {
      const data = new Date(atual.ano, atual.mes + delta, 1)
      return { ano: data.getFullYear(), mes: data.getMonth() }
    })
  }

  /** Leva a agenda para o mês da data informada (e sai do modo "Hoje" se necessário). */
  function focarData(iso: string) {
    const data = dataDoISO(iso)
    if (!data) return
    setMesRef({ ano: data.getFullYear(), mes: data.getMonth() })
    if (iso !== HOJE_ISO) setFiltro('Mês')
  }

  function abrirNovaAula() {
    setEditandoId(null)
    setAlunoForm('Vagner Santos')
    setDataForm(HOJE_ISO)
    setHorarioForm('19:00')
    setDuracaoForm('60 minutos')
    setObservacaoForm('')
    setAviso(null)
    setTelaAtual('formulario')
  }

  function abrirEditarAula(aula: AulaItem) {
    setEditandoId(aula.id)
    setAlunoForm(aula.aluno)
    setDataForm(aula.data)
    setHorarioForm(aula.horarioInicio)
    setDuracaoForm(aula.duracao)
    setObservacaoForm(aula.observacao)
    setAviso(null)
    setTelaAtual('formulario')
  }

  function abrirDetalheAula(aula: AulaItem) {
    setAulaSelecionadaId(aula.id)
    setModalAberto(null)
    setAviso(null)
    setTelaAtual('detalhe')
  }

  function salvarAula(e: React.FormEvent) {
    e.preventDefault()
    const dataISO = dataForm.trim()
    if (!dataDoISO(dataISO)) {
      setAviso('Informe uma data válida no formato DD/MM/AAAA.')
      return
    }

    const dadosAluno =
      ALUNOS_DISPONIVEIS.find((a) => a.nome === alunoForm) ?? ALUNOS_DISPONIVEIS[0]!
    const inicioLimpo = horarioForm.trim() || '19:00'
    const fimCalculado = calcularHoraFim(inicioLimpo, duracaoForm)

    if (editandoId) {
      setAulas((prev) =>
        prev.map((item) =>
          item.id === editandoId
            ? {
                ...item,
                data: dataISO,
                aluno: dadosAluno.nome,
                iniciais: dadosAluno.iniciais,
                horarioInicio: inicioLimpo,
                horarioFim: fimCalculado,
                duracao: duracaoForm,
                observacao: observacaoForm.trim(),
                planoAluno: dadosAluno.plano,
                proximaCobranca: dadosAluno.cobranca,
                statusCobranca: dadosAluno.statusCob,
              }
            : item
        )
      )
      setAulaSelecionadaId(editandoId)
      focarData(dataISO)
      setAviso('Aula atualizada com sucesso.')
      setTelaAtual('detalhe')
      return
    }

    const novaAula: AulaItem = {
      id: `aula-${Date.now()}`,
      data: dataISO,
      aluno: dadosAluno.nome,
      iniciais: dadosAluno.iniciais,
      horarioInicio: inicioLimpo,
      horarioFim: fimCalculado,
      duracao: duracaoForm,
      modalidade: 'Aula individual',
      local: 'Online',
      observacao: observacaoForm.trim(),
      status: 'Agendado',
      planoAluno: dadosAluno.plano,
      proximaCobranca: dadosAluno.cobranca,
      statusCobranca: dadosAluno.statusCob,
    }

    setAulas((prev) => [...prev, novaAula])
    setAulaSelecionadaId(novaAula.id)
    focarData(dataISO)
    setAviso('Nova aula adicionada à agenda.')
    setTelaAtual('lista')
  }

  function abrirModalRemarcar() {
    setNovaDataModal(aulaDetalhe.data)
    setNovoHorarioModal(aulaDetalhe.horarioInicio)
    setMotivoModal('')
    setModalAberto('remarcar')
  }

  function confirmarRemarcacao(e: React.FormEvent) {
    e.preventDefault()
    const dataLimpa = novaDataModal.trim()
    if (!dataDoISO(dataLimpa)) {
      setAviso('Informe uma data válida para remarcar.')
      return
    }
    const horaLimpa = novoHorarioModal.trim() || aulaDetalhe.horarioInicio

    setAulas((prev) =>
      prev.map((item) =>
        item.id === aulaDetalhe.id
          ? {
              ...item,
              data: dataLimpa,
              horarioInicio: horaLimpa,
              horarioFim: calcularHoraFim(horaLimpa, item.duracao),
              status: 'Agendado',
              observacao: motivoModal.trim() ? motivoModal.trim() : item.observacao,
            }
          : item
      )
    )
    setModalAberto(null)
    focarData(dataLimpa)
    setAviso(`Aula remarcada para ${formatarDataCurta(dataLimpa)} às ${horaLimpa}.`)
  }

  function confirmarCancelamentoAula() {
    setAulas((prev) =>
      prev.map((item) =>
        item.id === aulaDetalhe.id ? { ...item, status: 'Cancelado' } : item
      )
    )
    setModalAberto(null)
    setAviso('Aula cancelada e mantida no histórico.')
  }

  function confirmarStatusAula(novoStatus: 'Dado' | 'Falta') {
    setAulas((prev) =>
      prev.map((item) =>
        item.id === aulaDetalhe.id ? { ...item, status: novoStatus } : item
      )
    )
    setModalAberto(null)
    setAviso(novoStatus === 'Dado' ? 'Aula marcada como dada.' : 'Aula marcada como falta.')
  }

  // Tela 2: Criar ou editar aula
  if (telaAtual === 'formulario') {
    return (
      <section className="space-y-6">
        <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
          {editandoId ? 'Editar aula' : 'Criar ou editar aula'}
        </h1>

        <form
          onSubmit={salvarAula}
          className="max-w-2xl space-y-5 rounded-3xl border p-6 sm:p-8"
          style={{
            background: '#FAF6F0',
            borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
          }}
        >
          <div className="space-y-1.5">
            <label htmlFor="form-aluno" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Aluno
            </label>
            <select
              id="form-aluno"
              value={alunoForm}
              onChange={(e) => setAlunoForm(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
            >
              {ALUNOS_DISPONIVEIS.map((a) => (
                <option key={a.nome} value={a.nome}>
                  {a.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="form-data" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Data
            </label>
            <input
              id="form-data"
              type="date"
              value={dataForm}
              onChange={(e) => setDataForm(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
            />
            <p className="text-[11px]" style={{ color: '#6E5D52' }}>
              Qualquer data de qualquer mês ou ano. Ao salvar, a agenda abre no mês escolhido.
            </p>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="form-horario" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Horário
            </label>
            <input
              id="form-horario"
              type="time"
              value={horarioForm}
              onChange={(e) => setHorarioForm(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
            />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="form-duracao" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Duração
            </label>
            <select
              id="form-duracao"
              value={duracaoForm}
              onChange={(e) => setDuracaoForm(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
            >
              <option value="30 minutos">30 minutos</option>
              <option value="45 minutos">45 minutos</option>
              <option value="60 minutos">60 minutos</option>
              <option value="90 minutos">90 minutos</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="form-observacao" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
              Observação
            </label>
            <textarea
              id="form-observacao"
              rows={4}
              value={observacaoForm}
              onChange={(e) => setObservacaoForm(e.target.value)}
              className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
              style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
            />
          </div>

          <div className="flex flex-wrap items-center gap-3 pt-2">
            <button
              type="submit"
              className="rounded-xl px-7 py-3 text-xs font-semibold transition"
              style={{ background: '#5C3419', color: '#FFFFFF' }}
            >
              Salvar aula
            </button>
            <button
              type="button"
              onClick={() => setTelaAtual(editandoId ? 'detalhe' : 'lista')}
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

  // Tela 3: Detalhe da aula + Modais (Remarcar, Cancelar, Marcar status)
  if (telaAtual === 'detalhe') {
    const badgeAula = ESTILO_STATUS[aulaDetalhe.status]
    const badgeCob =
      aulaDetalhe.statusCobranca === 'Pago'
        ? { bg: '#DCECE3', fg: '#23533A' }
        : { bg: '#F4E6C8', fg: '#6E4F16' }

    return (
      <section className="relative space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
              Detalhe da aula
            </h1>
            <p className="mt-1 text-xs" style={{ color: 'var(--muted)' }}>
              {formatarDataLonga(aulaDetalhe.data)}
            </p>
          </div>

          <button
            type="button"
            onClick={() => {
              setModalAberto(null)
              setAviso(null)
              setTelaAtual('lista')
            }}
            className="rounded-xl px-4 py-2 text-xs font-medium transition"
            style={{ background: '#EAE1D5', color: '#3B281B' }}
          >
            Voltar para agenda
          </button>
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

        <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
          {/* Coluna principal da aula */}
          <div
            className="space-y-6 rounded-3xl border p-6 sm:p-7 lg:col-span-2"
            style={{
              background: '#FAF6F0',
              borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
            }}
          >
            <div className="flex items-start gap-4">
              <div
                className="grid h-14 w-14 shrink-0 place-items-center rounded-full text-sm font-medium"
                style={{ background: '#EAE1D5', color: '#3B281B' }}
              >
                {aulaDetalhe.iniciais}
              </div>

              <div>
                <div className="text-base font-bold" style={{ color: '#2B1B10' }}>
                  {aulaDetalhe.aluno}
                </div>
                <div className="mt-0.5 text-xs" style={{ color: '#6E5D52' }}>
                  {aulaDetalhe.horarioInicio} às {aulaDetalhe.horarioFim} · {formatarDataCurta(aulaDetalhe.data)}
                </div>
                <span
                  className="mt-2.5 inline-block rounded-full px-4 py-1 text-xs font-medium"
                  style={{ background: badgeAula.bg, color: badgeAula.fg }}
                >
                  {badgeAula.label}
                </span>
              </div>
            </div>

            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-[120px_1fr] gap-2">
                <span style={{ color: '#6E5D52' }}>Data</span>
                <span style={{ color: '#2B1B10' }}>{formatarDataCurta(aulaDetalhe.data)}</span>
              </div>
              <div className="grid grid-cols-[120px_1fr] gap-2">
                <span style={{ color: '#6E5D52' }}>Modalidade</span>
                <span style={{ color: '#2B1B10' }}>{aulaDetalhe.modalidade}</span>
              </div>
              <div className="grid grid-cols-[120px_1fr] gap-2">
                <span style={{ color: '#6E5D52' }}>Local</span>
                <span style={{ color: '#2B1B10' }}>{aulaDetalhe.local}</span>
              </div>
              <div className="grid grid-cols-[120px_1fr] gap-2">
                <span style={{ color: '#6E5D52' }}>Observação</span>
                <span style={{ color: '#2B1B10' }}>{aulaDetalhe.observacao || '—'}</span>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3 pt-2">
              <button
                type="button"
                onClick={() => setModalAberto('status')}
                className="rounded-xl px-5 py-3 text-xs font-semibold transition"
                style={{ background: '#5C3419', color: '#FFFFFF' }}
              >
                Marcar status
              </button>
              <button
                type="button"
                onClick={abrirModalRemarcar}
                className="rounded-xl px-5 py-3 text-xs font-medium transition"
                style={{ background: '#EAE1D5', color: '#3B281B' }}
              >
                Remarcar
              </button>
              <button
                type="button"
                onClick={() => setModalAberto('cancelar')}
                className="rounded-xl px-5 py-3 text-xs font-medium transition"
                style={{ background: '#EAE1D5', color: '#3B281B' }}
              >
                Cancelar aula
              </button>
              <button
                type="button"
                onClick={() => abrirEditarAula(aulaDetalhe)}
                className="rounded-xl px-5 py-3 text-xs font-medium transition"
                style={{ background: '#EAE1D5', color: '#3B281B' }}
              >
                Editar
              </button>
            </div>
          </div>

          {/* Cartão lateral Aluno */}
          <div
            className="space-y-5 rounded-3xl border p-6"
            style={{
              background: '#FAF6F0',
              borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
            }}
          >
            <div className="text-xs font-semibold" style={{ color: '#2B1B10' }}>
              Aluno
            </div>

            <div>
              <div className="text-xs font-medium" style={{ color: '#2B1B10' }}>
                {aulaDetalhe.aluno}
              </div>
              <div className="mt-1 text-xs" style={{ color: '#6E5D52' }}>
                {aulaDetalhe.planoAluno}
              </div>
            </div>

            <div className="space-y-1.5 pt-2">
              <div className="text-xs font-medium" style={{ color: '#6E5D52' }}>
                Próxima cobrança
              </div>
              <div className="text-xs" style={{ color: '#2B1B10' }}>
                {aulaDetalhe.proximaCobranca}
              </div>
              <span
                className="mt-2 inline-block rounded-full px-4 py-1 text-xs font-medium"
                style={{ background: badgeCob.bg, color: badgeCob.fg }}
              >
                {aulaDetalhe.statusCobranca}
              </span>
            </div>
          </div>
        </div>

        {/* Modal: Remarcar aula */}
        {modalAberto === 'remarcar' ? (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="titulo-modal-remarcar"
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            style={{ background: 'rgba(43, 27, 16, 0.45)' }}
          >
            <form
              onSubmit={confirmarRemarcacao}
              className="w-full max-w-md space-y-4 rounded-3xl p-7 shadow-xl"
              style={{ background: '#FAF6F0', color: '#2B1B10' }}
            >
              <h2 id="titulo-modal-remarcar" className="text-base font-bold">
                Remarcar aula
              </h2>
              <p className="text-xs" style={{ color: '#6E5D52' }}>
                A aula de {aulaDetalhe.aluno} sai de {formatarDataCurta(aulaDetalhe.data)}, às{' '}
                {aulaDetalhe.horarioInicio}.
              </p>

              <div className="space-y-1.5">
                <label htmlFor="remarcar-data" className="block text-xs font-medium">
                  Nova data
                </label>
                <input
                  id="remarcar-data"
                  type="date"
                  value={novaDataModal}
                  onChange={(e) => setNovaDataModal(e.target.value)}
                  className="w-full rounded-xl border px-4 py-2.5 text-sm outline-none"
                  style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
                />
              </div>

              <div className="space-y-1.5">
                <label htmlFor="remarcar-horario" className="block text-xs font-medium">
                  Novo horário
                </label>
                <input
                  id="remarcar-horario"
                  type="time"
                  value={novoHorarioModal}
                  onChange={(e) => setNovoHorarioModal(e.target.value)}
                  className="w-full rounded-xl border px-4 py-2.5 text-sm outline-none"
                  style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
                />
              </div>

              <div className="space-y-1.5">
                <label htmlFor="remarcar-motivo" className="block text-xs font-medium">
                  Motivo (opcional)
                </label>
                <input
                  id="remarcar-motivo"
                  type="text"
                  value={motivoModal}
                  onChange={(e) => setMotivoModal(e.target.value)}
                  className="w-full rounded-xl border px-4 py-2.5 text-sm outline-none"
                  style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
                />
              </div>

              <div className="space-y-2.5 pt-2">
                <button
                  type="submit"
                  className="w-full rounded-xl py-3 text-xs font-semibold transition"
                  style={{ background: '#5C3419', color: '#FFFFFF' }}
                >
                  Remarcar aula
                </button>
                <button
                  type="button"
                  onClick={() => setModalAberto(null)}
                  className="w-full rounded-xl py-3 text-xs font-medium transition"
                  style={{ background: '#EAE1D5', color: '#3B281B' }}
                >
                  Voltar
                </button>
              </div>
            </form>
          </div>
        ) : null}

        {/* Modal: Cancelar aula? */}
        {modalAberto === 'cancelar' ? (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="titulo-modal-cancelar"
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            style={{ background: 'rgba(43, 27, 16, 0.45)' }}
          >
            <div
              className="w-full max-w-md space-y-5 rounded-3xl p-7 shadow-xl"
              style={{ background: '#FAF6F0', color: '#2B1B10' }}
            >
              <div className="flex items-center justify-between gap-2">
                <h2 id="titulo-modal-cancelar" className="text-base font-bold">
                  Cancelar aula?
                </h2>
                <span
                  className="rounded-full px-2.5 py-0.5 text-[11px] font-medium"
                  style={{ background: '#EAE1D5', color: '#5C3419' }}
                >
                  N1
                </span>
              </div>

              <p className="text-xs leading-relaxed" style={{ color: '#6E5D52' }}>
                A aula de {aulaDetalhe.aluno} em {formatarDataCurta(aulaDetalhe.data)}, às{' '}
                {aulaDetalhe.horarioInicio}, será cancelada em definitivo e fica no histórico.
              </p>

              <div className="space-y-2.5 pt-1">
                <button
                  type="button"
                  onClick={confirmarCancelamentoAula}
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
            </div>
          </div>
        ) : null}

        {/* Modal: Marcar status */}
        {modalAberto === 'status' ? (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="titulo-modal-status"
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            style={{ background: 'rgba(43, 27, 16, 0.45)' }}
          >
            <div
              className="w-full max-w-md space-y-5 rounded-3xl p-7 shadow-xl"
              style={{ background: '#FAF6F0', color: '#2B1B10' }}
            >
              <h2 id="titulo-modal-status" className="text-base font-bold">
                Marcar status
              </h2>
              <p className="text-xs leading-relaxed" style={{ color: '#6E5D52' }}>
                Como foi a aula de {aulaDetalhe.aluno} em {formatarDataCurta(aulaDetalhe.data)}, às{' '}
                {aulaDetalhe.horarioInicio}?
              </p>

              <div className="space-y-2.5 pt-1">
                <button
                  type="button"
                  onClick={() => confirmarStatusAula('Dado')}
                  className="w-full rounded-xl py-3 text-xs font-semibold transition"
                  style={{ background: '#5C3419', color: '#FFFFFF' }}
                >
                  Aula dada
                </button>
                <button
                  type="button"
                  onClick={() => confirmarStatusAula('Falta')}
                  className="w-full rounded-xl py-3 text-xs font-semibold transition"
                  style={{ background: '#9E2A22', color: '#FFFFFF' }}
                >
                  Faltou
                </button>
                <button
                  type="button"
                  onClick={() => setModalAberto(null)}
                  className="w-full rounded-xl py-3 text-xs font-medium transition"
                  style={{ background: '#EAE1D5', color: '#3B281B' }}
                >
                  Voltar
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </section>
    )
  }

  // Tela 1: Agenda principal (Hoje / Mês)
  const celulas: Array<string | null> = [
    ...Array.from({ length: calendario.primeiroDiaSemana }, () => null),
    ...calendario.dias,
    ...Array.from({ length: calendario.vaziosFinais }, () => null),
  ]

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
          Agenda
        </h1>
        <p className="mt-1 text-xs" style={{ color: 'var(--muted)' }}>
          {filtro === 'Hoje'
            ? formatarDataLonga(HOJE_ISO)
            : `${nomeDoMes(mesRef.mes)} de ${mesRef.ano} · ${aulasDoMes.length} ${
                aulasDoMes.length === 1 ? 'aula' : 'aulas'
              }`}
        </p>
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

      {/* Barra de controles: Hoje / Mês + Nova aula */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2.5">
          {(['Hoje', 'Mês'] as FiltroAgenda[]).map((modo) => {
            const ativo = filtro === modo
            return (
              <button
                key={modo}
                type="button"
                onClick={() => setFiltro(modo)}
                aria-pressed={ativo}
                className="rounded-full px-6 py-2.5 text-xs font-medium transition"
                style={{
                  background: ativo ? '#5C3419' : '#EAE1D5',
                  color: ativo ? '#FFFFFF' : '#3B281B',
                }}
              >
                {modo}
              </button>
            )
          })}
        </div>

        <button
          type="button"
          onClick={abrirNovaAula}
          className="rounded-xl px-6 py-3 text-xs font-semibold transition"
          style={{ background: '#5C3419', color: '#FFFFFF' }}
        >
          + Nova aula
        </button>
      </div>

      {filtro === 'Mês' ? (
        <>
          {/* Navegação de mês e ano */}
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => navegarMes(-1)}
              aria-label="Mês anterior"
              className="rounded-xl px-4 py-2.5 text-xs font-medium transition"
              style={{ background: '#EAE1D5', color: '#3B281B' }}
            >
              ‹ Anterior
            </button>

            <label htmlFor="mes-agenda" className="sr-only">
              Mês
            </label>
            <select
              id="mes-agenda"
              value={mesRef.mes}
              onChange={(e) => setMesRef((atual) => ({ ...atual, mes: Number(e.target.value) }))}
              className="rounded-xl border px-3 py-2.5 text-xs outline-none"
              style={{ background: '#FAF6F0', borderColor: '#E5DDD2', color: '#3B281B' }}
            >
              {MESES.map((mes, indice) => (
                <option key={mes} value={indice}>
                  {mes}
                </option>
              ))}
            </select>

            <label htmlFor="ano-agenda" className="sr-only">
              Ano
            </label>
            <select
              id="ano-agenda"
              value={mesRef.ano}
              onChange={(e) => setMesRef((atual) => ({ ...atual, ano: Number(e.target.value) }))}
              className="rounded-xl border px-3 py-2.5 text-xs outline-none"
              style={{ background: '#FAF6F0', borderColor: '#E5DDD2', color: '#3B281B' }}
            >
              {anosDisponiveis.map((ano) => (
                <option key={ano} value={ano}>
                  {ano}
                </option>
              ))}
            </select>

            <button
              type="button"
              onClick={() => navegarMes(1)}
              aria-label="Próximo mês"
              className="rounded-xl px-4 py-2.5 text-xs font-medium transition"
              style={{ background: '#EAE1D5', color: '#3B281B' }}
            >
              Próximo ›
            </button>
          </div>

          {/* Grade do calendário mensal */}
          <div
            className="overflow-hidden rounded-2xl border"
            style={{
              background: '#FAF6F0',
              borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
            }}
          >
            <div className="overflow-x-auto">
              <div className="min-w-[760px]">
                <div
                  className="grid grid-cols-7 border-b text-[11px] font-semibold uppercase tracking-wide"
                  style={{ borderColor: '#E5DDD2', color: '#6E5D52' }}
                >
                  {DIAS_SEMANA_CURTOS.map((dia) => (
                    <div key={dia} className="px-3 py-3 text-center">
                      {dia}
                    </div>
                  ))}
                </div>

                <div className="grid grid-cols-7">
                  {celulas.map((iso, indice) => {
                    const comBordaDireita = (indice + 1) % 7 !== 0
                    const comBordaAbaixo = indice < celulas.length - 7
                    const bordas = {
                      borderRight: comBordaDireita ? '1px solid #EFE7DB' : undefined,
                      borderBottom: comBordaAbaixo ? '1px solid #EFE7DB' : undefined,
                    }

                    if (!iso) {
                      return (
                        <div
                          key={`vazio-${indice}`}
                          className="min-h-[112px]"
                          style={{ ...bordas, background: '#FBF7F1' }}
                        />
                      )
                    }

                    const dia = Number(iso.slice(8, 10))
                    const lista = calendario.porData.get(iso) ?? []
                    const ehHoje = iso === HOJE_ISO

                    return (
                      <div key={iso} className="min-h-[112px] space-y-1 p-2" style={bordas}>
                        <div className="flex items-center justify-between">
                          <span
                            className="grid h-6 w-6 place-items-center rounded-full text-[11px] font-semibold"
                            style={
                              ehHoje
                                ? { background: '#5C3419', color: '#FFFFFF' }
                                : { color: '#2B1B10' }
                            }
                          >
                            {dia}
                          </span>
                          {lista.length > 0 ? (
                            <span className="text-[10px] font-medium" style={{ color: '#6E5D52' }}>
                              {lista.length}
                            </span>
                          ) : null}
                        </div>

                        <div className="space-y-1">
                          {lista.map((aula) => {
                            const st = ESTILO_STATUS[aula.status]
                            return (
                              <button
                                key={aula.id}
                                type="button"
                                onClick={() => abrirDetalheAula(aula)}
                                className="block w-full rounded-lg px-2 py-1 text-left transition hover:brightness-[0.97]"
                                style={{ background: st.bg, color: st.fg }}
                                title={`${aula.horarioInicio} · ${aula.aluno} · ${st.label}`}
                              >
                                <span className="block text-[10px] font-semibold">{aula.horarioInicio}</span>
                                <span className="block truncate text-[10px]">{aula.aluno}</span>
                              </button>
                            )
                          })}
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            </div>
          </div>

          {aulasDoMes.length === 0 ? (
            <p className="text-xs" style={{ color: '#6E5D52' }}>
              Nenhuma aula em {nomeDoMes(mesRef.mes)} de {mesRef.ano}. Use “+ Nova aula” para agendar.
            </p>
          ) : null}
        </>
      ) : (
        /* Modo Hoje: apenas as aulas da data atual */
        <div className="space-y-3">
          {aulasHoje.length === 0 ? (
            <div
              className="rounded-2xl border p-8 text-center"
              style={{
                background: '#FAF6F0',
                borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
              }}
            >
              <p className="text-sm font-semibold" style={{ color: '#2B1B10' }}>
                Nenhuma aula hoje.
              </p>
              <p className="mt-1 text-xs" style={{ color: '#6E5D52' }}>
                Aproveite para organizar a semana ou agende uma nova aula.
              </p>
              <button
                type="button"
                onClick={abrirNovaAula}
                className="mt-5 rounded-xl px-6 py-3 text-xs font-semibold transition"
                style={{ background: '#5C3419', color: '#FFFFFF' }}
              >
                + Nova aula
              </button>
            </div>
          ) : (
            aulasHoje.map((aula) => {
              const st = ESTILO_STATUS[aula.status]
              return (
                <button
                  key={aula.id}
                  type="button"
                  onClick={() => abrirDetalheAula(aula)}
                  className="flex w-full flex-wrap items-center justify-between gap-4 rounded-2xl border p-5 text-left transition hover:brightness-[0.99]"
                  style={{
                    background: '#FAF6F0',
                    borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
                  }}
                >
                  <div className="flex items-center gap-4">
                    <div
                      className="grid h-11 w-11 place-items-center rounded-full text-xs font-medium"
                      style={{ background: '#EAE1D5', color: '#3B281B' }}
                    >
                      {aula.iniciais}
                    </div>
                    <div>
                      <div className="text-sm font-semibold" style={{ color: '#2B1B10' }}>
                        {aula.aluno}
                      </div>
                      <div className="mt-0.5 text-xs" style={{ color: '#6E5D52' }}>
                        {aula.horarioInicio} às {aula.horarioFim} · {aula.modalidade}
                      </div>
                    </div>
                  </div>

                  <span
                    className="rounded-full px-4 py-1 text-[11px] font-medium"
                    style={{ background: st.bg, color: st.fg }}
                  >
                    {st.label}
                  </span>
                </button>
              )
            })
          )}
        </div>
      )}

      {/* Legenda de estados */}
      <div className="flex flex-wrap items-center gap-4 pt-1">
        {(['Agendado', 'Dado', 'Falta', 'Cancelado'] as StatusAula[]).map((stKey) => {
          const st = ESTILO_STATUS[stKey]
          return (
            <span
              key={stKey}
              className="rounded-full px-5 py-1.5 text-xs font-medium"
              style={{ background: st.bg, color: st.fg }}
            >
              {st.label}
            </span>
          )
        })}
      </div>

      {/* Cartões inferiores de resumo */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div
          className="rounded-2xl border p-6"
          style={{
            background: '#FAF6F0',
            borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
          }}
        >
          <div className="text-xs font-medium" style={{ color: '#6E5D52' }}>
            Visitas e cliques
          </div>
          <div className="mt-3 text-xl font-bold" style={{ color: '#2B1B10' }}>
            482 · 37
          </div>
        </div>

        <div
          className="rounded-2xl border p-6"
          style={{
            background: '#FAF6F0',
            borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
          }}
        >
          <div className="text-xs font-medium" style={{ color: '#6E5D52' }}>
            Recebido no mês
          </div>
          <div className="mt-3 text-xl font-bold" style={{ color: '#2B1B10' }}>
            R$ 570,00
          </div>
        </div>

        <div
          className="rounded-2xl border p-6"
          style={{
            background: '#FAF6F0',
            borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
          }}
        >
          <div className="text-xs font-medium" style={{ color: '#6E5D52' }}>
            Cobranças em atraso
          </div>
          <div className="mt-3 text-xl font-bold" style={{ color: '#2B1B10' }}>
            1
          </div>
        </div>
      </div>
    </section>
  )
}
