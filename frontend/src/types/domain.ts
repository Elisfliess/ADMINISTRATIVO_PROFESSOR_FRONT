/** DTOs de leitura alinhados a docs/api/openapi.yaml. */
export type StatusAula = 'AGENDADA' | 'DADA' | 'FALTA' | 'CANCELADA'
export type StatusCobranca = 'PENDENTE' | 'PAGO' | 'CANCELADO'
export type Periodicidade = 'MENSAL' | 'POR_AULA'
export type FormatoModalidade = 'INDIVIDUAL' | 'GRUPO' | 'PERSONALIZADO'

export interface Professor {
  id: string
  nome: string
  razaoSocial: string
  cnpj: string
  emailContato?: string
  telefoneContato?: string
  instagramUrl?: string
}

export interface ResponsavelLegal {
  nome: string
  contato: string
  informadoEm?: string
}

export interface Aluno {
  id: string
  nome: string
  telefone?: string
  email?: string
  dataNascimento?: string
  observacoes?: string
  responsavelLegal?: ResponsavelLegal | null
  ativo: boolean
  inativadoEm?: string
  anonimizadoEm?: string
}

export interface Aula {
  id: string
  alunoId: string
  modalidadeId: string
  inicioEm: string
  fimEm: string
  status: StatusAula
  observacoes?: string
}

export interface Cobranca {
  id: string
  alunoId: string
  planoId: string
  aulaId?: string
  valor: number
  periodoInicio: string
  periodoFim: string
  vencimentoEm: string
  status: StatusCobranca
  situacao: 'PENDENTE' | 'EM_ATRASO' | 'PAGO' | 'CANCELADO'
  recebidoEm?: string
  formaPagamento?: string
}

export interface Plano {
  id: string
  alunoId: string
  periodicidade: Periodicidade
  valor: number
  inicioEm: string
  diaVencimento?: number | null
  prazoVencimentoDias?: number | null
  status: 'ATIVO' | 'ENCERRADO'
}

export interface ModalidadePublica {
  id: string
  nome: string
  formato: FormatoModalidade
  descricao: string
  valor: number | null
  valorAtualizadoEm: string | null
  duracaoMinutos: number
  publicada: boolean
}

export interface PublicProfile {
  id: string
  slug: string
  titulo: string
  apresentacao: string
  whatsappNumero: string
  whatsappMensagem: string
  mostrarValores: boolean
  publicado: boolean
  publicadoEm?: string
  emailContato: string
  telefoneContato: string
  instagramUrl: string
  modalidades: ModalidadePublica[]
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
