export type StatusAluno = 'Ativo' | 'Inativo'
export type AbaFicha = 'Resumo' | 'Histórico de aulas' | 'Cobranças'
export type ModalAluno = null | 'inativar' | 'exportar' | 'anonimizar' | 'encerrar-plano'
export type StatusCobrancaAluno = 'Pago' | 'Pendente' | 'Em atraso'

export interface AulaHistorico {
  id: string
  data: string
  horario: string
  tipo: string
  status: 'Dado' | 'Agendado' | 'Falta' | 'Cancelado'
}

export interface CobrancaAluno {
  id: string
  vencimento: string
  valor: string
  status: StatusCobrancaAluno
}

export interface AlunoFicha {
  id: string
  nome: string
  iniciais: string
  status: StatusAluno
  email: string
  telefone: string
  responsavel: string
  dataNascimento: string
  planoTitulo: string
  planoDetalhe: string
  valorMensalidade: string
  diaVencimento: string
  lgpdAutorizado: boolean
  proximaAulaDataHora: string
  proximaAulaTipo: string
  cobrancaStatus: StatusCobrancaAluno
  historico: AulaHistorico[]
  cobrancas: CobrancaAluno[]
}

export const ALUNOS_MOCK: AlunoFicha[] = [
  {
    id: 'vagner-santos',
    nome: 'Vagner Santos',
    iniciais: 'VS',
    status: 'Ativo',
    email: 'vagner@email.com',
    telefone: '(11) 90000-0000',
    responsavel: '—',
    dataNascimento: '12/05/1994',
    planoTitulo: 'Mensal, 2x por semana',
    planoDetalhe: 'R$ 270,00, vence dia 5',
    valorMensalidade: 'R$ 270,00',
    diaVencimento: '5',
    lgpdAutorizado: true,
    proximaAulaDataHora: '03/08 às 19:00',
    proximaAulaTipo: 'Aula individual',
    cobrancaStatus: 'Pago',
    historico: [
      { id: 'a1', data: '03/08/2026', horario: '19:00', tipo: 'Individual', status: 'Agendado' },
      { id: 'a2', data: '27/07/2026', horario: '19:00', tipo: 'Individual', status: 'Dado' },
      { id: 'a3', data: '20/07/2026', horario: '19:00', tipo: 'Individual', status: 'Dado' },
      { id: 'a4', data: '13/07/2026', horario: '19:00', tipo: 'Individual', status: 'Falta' },
      { id: 'a5', data: '06/07/2026', horario: '19:00', tipo: 'Individual', status: 'Dado' },
    ],
    cobrancas: [
      { id: 'c1', vencimento: '05/12/2026', valor: 'R$ 270,00', status: 'Pago' },
      { id: 'c2', vencimento: '05/11/2026', valor: 'R$ 270,00', status: 'Pago' },
      { id: 'c3', vencimento: '05/10/2026', valor: 'R$ 270,00', status: 'Pago' },
      { id: 'c4', vencimento: '05/09/2026', valor: 'R$ 270,00', status: 'Pendente' },
      { id: 'c5', vencimento: '05/08/2026', valor: 'R$ 270,00', status: 'Pago' },
    ],
  },
  {
    id: 'leticia-aguiar',
    nome: 'Letícia Aguiar',
    iniciais: 'LA',
    status: 'Ativo',
    email: 'leticia.aguiar@email.com',
    telefone: '(11) 98888-1111',
    responsavel: '—',
    dataNascimento: '24/09/1998',
    planoTitulo: 'Mensal, 1x por semana',
    planoDetalhe: 'R$ 180,00, vence dia 10',
    valorMensalidade: 'R$ 180,00',
    diaVencimento: '10',
    lgpdAutorizado: true,
    proximaAulaDataHora: '04/08 às 10:00',
    proximaAulaTipo: 'Aula individual',
    cobrancaStatus: 'Em atraso',
    historico: [
      { id: 'a6', data: '04/08/2026', horario: '10:00', tipo: 'Individual', status: 'Dado' },
    ],
    cobrancas: [
      { id: 'c6', vencimento: '10/12/2026', valor: 'R$ 270,00', status: 'Em atraso' },
      { id: 'c7', vencimento: '10/08/2026', valor: 'R$ 180,00', status: 'Pago' },
    ],
  },
  {
    id: 'andre-soares',
    nome: 'André Soares',
    iniciais: 'AS',
    status: 'Ativo',
    email: 'andre.soares@email.com',
    telefone: '(11) 97777-2222',
    responsavel: '—',
    dataNascimento: '03/02/1990',
    planoTitulo: 'Por aula',
    planoDetalhe: 'R$ 150,00 por aula',
    valorMensalidade: 'R$ 150,00',
    diaVencimento: '—',
    lgpdAutorizado: true,
    proximaAulaDataHora: '06/08 às 16:00',
    proximaAulaTipo: 'Aula individual',
    cobrancaStatus: 'Pendente',
    historico: [
      { id: 'a7', data: '06/08/2026', horario: '16:00', tipo: 'Individual', status: 'Agendado' },
    ],
    cobrancas: [
      { id: 'c8', vencimento: '18/12/2026', valor: 'R$ 150,00', status: 'Pendente' },
    ],
  },
  {
    id: 'tercia-souza',
    nome: 'Tercia Souza',
    iniciais: 'TS',
    status: 'Ativo',
    email: 'tercia.souza@email.com',
    telefone: '(11) 96666-3333',
    responsavel: '—',
    dataNascimento: '19/11/1987',
    planoTitulo: 'Mensal, 1x por semana',
    planoDetalhe: 'R$ 270,00, vence dia 20',
    valorMensalidade: 'R$ 270,00',
    diaVencimento: '20',
    lgpdAutorizado: true,
    proximaAulaDataHora: '05/08 às 09:00',
    proximaAulaTipo: 'Aula individual',
    cobrancaStatus: 'Pago',
    historico: [
      { id: 'a8', data: '05/08/2026', horario: '09:00', tipo: 'Individual', status: 'Falta' },
    ],
    cobrancas: [
      { id: 'c9', vencimento: '20/12/2026', valor: 'R$ 270,00', status: 'Pago' },
    ],
  },
  {
    id: 'rafaela-lima',
    nome: 'Rafaela Lima',
    iniciais: 'RL',
    status: 'Inativo',
    email: 'rafaela.lima@email.com',
    telefone: '(11) 95555-4444',
    responsavel: '—',
    dataNascimento: '07/06/2001',
    planoTitulo: 'Mensal',
    planoDetalhe: 'Plano encerrado',
    valorMensalidade: 'R$ 150,00',
    diaVencimento: '25',
    lgpdAutorizado: true,
    proximaAulaDataHora: 'Sem aula marcada',
    proximaAulaTipo: '—',
    cobrancaStatus: 'Pendente',
    historico: [],
    cobrancas: [
      { id: 'c10', vencimento: '25/12/2026', valor: 'R$ 150,00', status: 'Pendente' },
    ],
  },
]

export const PLANOS_OPCOES = [
  'Mensal, 2x por semana',
  'Mensal, 1x por semana',
  'Mensal',
  'Por aula',
  'Sem plano ativo',
]

export const DIAS_VENCIMENTO = ['5', '10', '15', '20', '25']

export const BADGE_AULA: Record<AulaHistorico['status'], { bg: string; fg: string }> = {
  Agendado: { bg: '#E9DFD3', fg: '#5C3419' },
  Dado: { bg: '#DCECE3', fg: '#23533A' },
  Falta: { bg: '#F7E1E4', fg: '#7A2733' },
  Cancelado: { bg: '#F4E6C8', fg: '#6E4F16' },
}

export const BADGE_COBRANCA: Record<StatusCobrancaAluno, { bg: string; fg: string }> = {
  Pago: { bg: '#DCECE3', fg: '#23533A' },
  Pendente: { bg: '#F4E6C8', fg: '#6E4F16' },
  'Em atraso': { bg: '#F7E1E4', fg: '#7A2733' },
}

export function iniciaisDe(nome: string): string {
  const partes = nome.trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return 'AL'
  if (partes.length === 1) return partes[0]!.slice(0, 2).toUpperCase()
  return `${partes[0]![0] ?? ''}${partes[partes.length - 1]![0] ?? ''}`.toUpperCase()
}
