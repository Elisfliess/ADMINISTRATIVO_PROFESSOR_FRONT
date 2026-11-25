export type StatusCobranca = 'Pago' | 'Pendente' | 'Em atraso' | 'Cancelado'
export type FiltroFinanceiro = 'Todos' | 'Pendentes' | 'Em atraso'
export type ModalFinanceiro = null | 'baixa' | 'exportar' | 'cancelar'

export interface Cobranca {
  id: string
  aluno: string
  iniciais: string
  vencimento: string
  valor: string
  status: StatusCobranca
}

export const COBRANCAS_MOCK: Cobranca[] = [
  {
    id: 'cob-1',
    aluno: 'Vagner Santos',
    iniciais: 'VS',
    vencimento: '05/12/2026',
    valor: 'R$ 270,00',
    status: 'Pago',
  },
  {
    id: 'cob-2',
    aluno: 'Letícia Aguiar',
    iniciais: 'LA',
    vencimento: '10/12/2026',
    valor: 'R$ 270,00',
    status: 'Pendente',
  },
  {
    id: 'cob-3',
    aluno: 'André Soares',
    iniciais: 'AS',
    vencimento: '18/12/2026',
    valor: 'R$ 150,00',
    status: 'Em atraso',
  },
  {
    id: 'cob-4',
    aluno: 'Tercia Souza',
    iniciais: 'TS',
    vencimento: '20/12/2026',
    valor: 'R$ 270,00',
    status: 'Pago',
  },
  {
    id: 'cob-5',
    aluno: 'Rafaela Lima',
    iniciais: 'RL',
    vencimento: '25/12/2026',
    valor: 'R$ 150,00',
    status: 'Pendente',
  },
]

export const RESUMO_FINANCEIRO = {
  recebido: 'R$ 570,00',
  pendente: 'R$ 150,00',
  emAtraso: 'R$ 270,00',
}

export const BADGE_COBRANCA_FINANCEIRO: Record<StatusCobranca, { bg: string; fg: string }> = {
  Pago: { bg: '#DCECE3', fg: '#23533A' },
  Pendente: { bg: '#F4E6C8', fg: '#6E4F16' },
  'Em atraso': { bg: '#F7E1E4', fg: '#7A2733' },
  Cancelado: { bg: '#EAE1D5', fg: '#5A4A3F' },
}

export const FILTROS_FINANCEIRO: FiltroFinanceiro[] = ['Todos', 'Pendentes', 'Em atraso']
