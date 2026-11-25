import { apiDownload, apiRequest } from './client'
import type { Aula, Cobranca, Page, Professor, PublicProfile } from '../../types/domain'

const MERGE_PATCH = { 'Content-Type': 'application/merge-patch+json' }

/** Chamadas restritas aos caminhos oficiais de docs/api/openapi.yaml. */
export const endpoints = {
  login: (email: string, senha: string) =>
    apiRequest<void>('/auth/login', { method: 'POST', body: JSON.stringify({ email, senha }) }),

  logout: () => apiRequest<void>('/auth/logout', { method: 'POST' }),

  me: () =>
    apiRequest<{
      id: string
      email: string
      emailVerificado: boolean
      termosAceitos: boolean
    }>('/auth/me'),

  register: (payload: {
    email: string
    senha: string
    nome: string
    razaoSocial: string
    cnpj: string
    emailContato?: string | null
    telefoneContato?: string | null
  }) => apiRequest<void>('/auth/register', { method: 'POST', body: JSON.stringify(payload) }),

  verifyEmail: (token: string) =>
    apiRequest<void>('/auth/verify-email', { method: 'POST', body: JSON.stringify({ token }) }),

  resendVerification: (email: string) =>
    apiRequest<void>('/auth/verification/resend', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  requestPasswordRecovery: (email: string) =>
    apiRequest<void>('/auth/password-recovery', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  confirmPasswordRecovery: (token: string, novaSenha: string) =>
    apiRequest<void>('/auth/password-recovery/confirm', {
      method: 'POST',
      body: JSON.stringify({ token, novaSenha }),
    }),

  acceptLegal: (termosVersao: string, politicaVersao: string) =>
    apiRequest<void>('/legal/acceptances', {
      method: 'POST',
      body: JSON.stringify({ termosVersao, politicaVersao }),
    }),

  professor: () => apiRequest<Professor>('/professor'),

  updateProfessor: (payload: { nome?: string; razaoSocial?: string }) =>
    apiRequest<Professor>('/professor', {
      method: 'PATCH',
      headers: MERGE_PATCH,
      body: JSON.stringify(payload),
    }),

  alunos: (params: { page?: number; size?: number; nome?: string; ativo?: boolean } = {}) => {
    const query = new URLSearchParams()
    if (params.page !== undefined) query.set('page', String(params.page))
    if (params.size !== undefined) query.set('size', String(params.size))
    if (params.nome) query.set('nome', params.nome)
    if (params.ativo !== undefined) query.set('ativo', String(params.ativo))
    const suffix = query.size ? `?${query}` : ''
    return apiRequest<Page<import('../../types/domain').Aluno>>(`/alunos${suffix}`)
  },

  createAluno: (payload: {
    nome: string
    telefone: string
    dataNascimento: string
    email?: string | null
    observacoes?: string | null
    responsavelLegal?: { nome: string; contato: string } | null
  }) =>
    apiRequest<import('../../types/domain').Aluno>('/alunos', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  getAluno: (alunoId: string) =>
    apiRequest<import('../../types/domain').Aluno>(`/alunos/${encodeURIComponent(alunoId)}`),

  updateAluno: (
    alunoId: string,
    payload: {
      nome?: string
      telefone?: string
      dataNascimento?: string
      email?: string | null
      observacoes?: string | null
      responsavelLegal?: { nome?: string | null; contato?: string | null } | null
    },
  ) =>
    apiRequest<import('../../types/domain').Aluno>(`/alunos/${encodeURIComponent(alunoId)}`, {
      method: 'PATCH',
      headers: MERGE_PATCH,
      body: JSON.stringify(payload),
    }),

  reativarAluno: (alunoId: string) =>
    apiRequest<void>(`/alunos/${encodeURIComponent(alunoId)}/reativacao`, { method: 'POST' }),

  aulas: (inicio: string, fim: string) =>
    apiRequest<Page<Aula>>(`/aulas?${new URLSearchParams({ inicio, fim })}`),

  planosDoAluno: (alunoId: string) =>
    apiRequest<import('../../types/domain').Plano[]>(
      `/alunos/${encodeURIComponent(alunoId)}/planos`,
    ),

  cobrancas: (mes: string) =>
    apiRequest<Page<Cobranca>>(`/cobrancas?${new URLSearchParams({ mes })}`),

  baixarCobranca: (
    cobrancaId: string,
    payload: { recebidoEm: string; formaPagamento?: string | null },
  ) =>
    apiRequest<Cobranca>(`/cobrancas/${encodeURIComponent(cobrancaId)}/baixa`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  cancelarCobranca: (cobrancaId: string) =>
    apiRequest<void>(`/cobrancas/${encodeURIComponent(cobrancaId)}/cancelamento`, {
      method: 'POST',
    }),

  publicProfile: () => apiRequest<PublicProfile>('/publico/perfil'),

  publicIndicators: () =>
    apiRequest<{ totalVisitas: number; totalCliques: number }>('/publico/indicadores'),

  exportAccountCsv: (senha: string) =>
    apiDownload('/account/exportacao', { method: 'POST', body: JSON.stringify({ senha }) }),

  exportCobrancasCsv: (mes: string, senha: string) =>
    apiDownload('/cobrancas/exportacao', {
      method: 'POST',
      body: JSON.stringify({ mes, senha }),
    }),
}
