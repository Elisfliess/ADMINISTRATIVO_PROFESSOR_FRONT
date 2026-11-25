export type StatusAula = 'Agendado' | 'Dado' | 'Falta' | 'Cancelado'
export type ModalAula = null | 'remarcar' | 'cancelar' | 'status'

export interface AulaDetalhe {
  id: string
  diaLabel: string
  dataCurta: string
  aluno: string
  iniciais: string
  horarioInicio: string
  horarioFim: string
  status: StatusAula
  modalidade: string
  local: string
  observacao: string
  planoAluno: string
  proximaCobranca: string
  statusCobranca: 'Pago' | 'Pendente'
}

export const AULA_DETALHE: AulaDetalhe = {
  id: 'aula-1',
  diaLabel: 'Segunda, 3 de agosto',
  dataCurta: '03/08',
  aluno: 'Vagner Santos',
  iniciais: 'VS',
  horarioInicio: '19:00',
  horarioFim: '20:00',
  status: 'Agendado',
  modalidade: 'Aula individual',
  local: 'Online',
  observacao: 'Revisar o conteúdo da semana',
  planoAluno: 'Mensal, 2x por semana',
  proximaCobranca: '05/12/2026, R$ 270,00',
  statusCobranca: 'Pago',
}

export const BADGE_AULA_STATUS: Record<StatusAula, { bg: string; fg: string }> = {
  Agendado: { bg: '#E9DFD3', fg: '#5C3419' },
  Dado: { bg: '#DCECE3', fg: '#23533A' },
  Falta: { bg: '#F7E1E4', fg: '#7A2733' },
  Cancelado: { bg: '#F4E6C8', fg: '#6E4F16' },
}

export const ALUNOS_AULA = [
  'Vagner Santos',
  'Letícia Aguiar',
  'André Soares',
  'Tercia Souza',
]
