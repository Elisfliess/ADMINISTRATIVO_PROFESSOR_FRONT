import type {
  Aluno,
  Aula,
  Cobranca,
  ModalidadePublica,
  Plano,
  Professor,
  PublicProfile,
} from '../types/domain'

/** Dados fictícios para navegação e demonstração das telas; nenhum dado real. */
export const professorMock: Professor = {
  id: 'prof-exemplo-01',
  nome: 'Professor Exemplo',
  razaoSocial: 'Professor Exemplo Música LTDA',
  cnpj: '00000000000191',
  emailContato: 'contato@exemplo.invalid',
  telefoneContato: '11999990000',
  instagramUrl: 'https://instagram.com/exemplo.invalid',
}

export const alunosMock: Aluno[] = [
  {
    id: 'alu-001',
    nome: 'Aluno Exemplo 01',
    telefone: '11900010001',
    email: 'aluno01@exemplo.invalid',
    dataNascimento: '1998-04-12',
    ativo: true,
  },
  {
    id: 'alu-002',
    nome: 'Aluno Exemplo 02',
    telefone: '11900010002',
    dataNascimento: '2012-08-03',
    responsavelLegal: { nome: 'Responsável Exemplo 02', contato: '11900010003' },
    ativo: true,
  },
  {
    id: 'alu-003',
    nome: 'Aluno Exemplo 03',
    telefone: '11900010004',
    email: 'aluno03@exemplo.invalid',
    dataNascimento: '1990-11-27',
    ativo: false,
    inativadoEm: '2026-10-15',
  },
]

export const aulasMock: Aula[] = [
  {
    id: 'aul-001',
    alunoId: 'alu-001',
    modalidadeId: 'mod-001',
    inicioEm: '2026-11-23T18:00:00-03:00',
    fimEm: '2026-11-23T19:00:00-03:00',
    status: 'AGENDADA',
    observacoes: 'Aula de violão individual.',
  },
  {
    id: 'aul-002',
    alunoId: 'alu-002',
    modalidadeId: 'mod-002',
    inicioEm: '2026-11-24T17:00:00-03:00',
    fimEm: '2026-11-24T18:00:00-03:00',
    status: 'AGENDADA',
  },
  {
    id: 'aul-003',
    alunoId: 'alu-001',
    modalidadeId: 'mod-001',
    inicioEm: '2026-11-20T18:00:00-03:00',
    fimEm: '2026-11-20T19:00:00-03:00',
    status: 'FALTA',
  },
  {
    id: 'aul-004',
    alunoId: 'alu-003',
    modalidadeId: 'mod-003',
    inicioEm: '2026-11-19T19:00:00-03:00',
    fimEm: '2026-11-19T20:00:00-03:00',
    status: 'DADA',
  },
]

export const cobrancasMock: Cobranca[] = [
  {
    id: 'cob-001',
    alunoId: 'alu-001',
    planoId: 'pla-001',
    aulaId: 'aul-001',
    valor: 180,
    periodoInicio: '2026-11-01',
    periodoFim: '2026-11-30',
    vencimentoEm: '2026-11-10',
    status: 'PENDENTE',
    situacao: 'EM_ATRASO',
  },
  {
    id: 'cob-002',
    alunoId: 'alu-002',
    planoId: 'pla-002',
    valor: 220,
    periodoInicio: '2026-11-01',
    periodoFim: '2026-11-30',
    vencimentoEm: '2026-11-05',
    status: 'PAGO',
    situacao: 'PAGO',
    recebidoEm: '2026-11-05',
    formaPagamento: 'Pix',
  },
  {
    id: 'cob-003',
    alunoId: 'alu-003',
    planoId: 'pla-003',
    valor: 160,
    periodoInicio: '2026-10-01',
    periodoFim: '2026-10-31',
    vencimentoEm: '2026-10-10',
    status: 'CANCELADO',
    situacao: 'CANCELADO',
  },
]

export const planosMock: Plano[] = [
  {
    id: 'pla-001',
    alunoId: 'alu-001',
    periodicidade: 'MENSAL',
    valor: 180,
    inicioEm: '2026-09-01',
    diaVencimento: 10,
    status: 'ATIVO',
  },
  {
    id: 'pla-002',
    alunoId: 'alu-002',
    periodicidade: 'POR_AULA',
    valor: 55,
    inicioEm: '2026-10-01',
    prazoVencimentoDias: 2,
    status: 'ATIVO',
  },
  {
    id: 'pla-003',
    alunoId: 'alu-003',
    periodicidade: 'MENSAL',
    valor: 160,
    inicioEm: '2026-08-01',
    diaVencimento: 10,
    status: 'ENCERRADO',
  },
]

export const modalidadesMock: ModalidadePublica[] = [
  {
    id: 'mod-001',
    nome: 'Violão individual',
    formato: 'INDIVIDUAL',
    descricao: 'Aulas individuais de violão para iniciantes e intermediários.',
    valor: 180,
    valorAtualizadoEm: '2026-11-01',
    duracaoMinutos: 60,
    publicada: true,
  },
  {
    id: 'mod-002',
    nome: 'Técnica vocal individual',
    formato: 'INDIVIDUAL',
    descricao: 'Treino de respiração, afinação e interpretação.',
    valor: 200,
    valorAtualizadoEm: '2026-11-01',
    duracaoMinutos: 60,
    publicada: true,
  },
  {
    id: 'mod-003',
    nome: 'Aulas em grupo',
    formato: 'GRUPO',
    descricao: 'Turmas reduzidas para prática de conjunto.',
    valor: 120,
    valorAtualizadoEm: '2026-10-15',
    duracaoMinutos: 90,
    publicada: true,
  },
  {
    id: 'mod-004',
    nome: 'Aulas personalizadas',
    formato: 'PERSONALIZADO',
    descricao: 'Conteúdo definido conforme o objetivo do aluno.',
    valor: null,
    valorAtualizadoEm: null,
    duracaoMinutos: 60,
    publicada: false,
  },
]

export const publicProfileMock: PublicProfile = {
  id: 'prof-exemplo-01',
  slug: 'professor-exemplo',
  titulo: 'Música com calma e método',
  apresentacao:
    'Aulas particulares de música para quem quer começar ou retomar o instrumento com consistência.',
  whatsappNumero: '11999990000',
  whatsappMensagem: 'Olá! Gostaria de saber mais sobre as aulas.',
  mostrarValores: false,
  publicado: true,
  publicadoEm: '2026-11-01',
  emailContato: 'contato@exemplo.invalid',
  telefoneContato: '11999990000',
  instagramUrl: 'https://instagram.com/exemplo.invalid',
  modalidades: modalidadesMock,
}

export const indicadoresMock = { totalVisitas: 128, totalCliques: 37 }
