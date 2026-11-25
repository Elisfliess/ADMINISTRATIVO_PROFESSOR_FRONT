import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../src/services/api/mode', () => ({ backendEnabled: true }))

vi.mock('../src/services/api/endpoints', () => ({
  endpoints: {
    login: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
    register: vi.fn(),
    resendVerification: vi.fn(),
    requestPasswordRecovery: vi.fn(),
    confirmPasswordRecovery: vi.fn(),
    acceptLegal: vi.fn(),
    createAluno: vi.fn(),
    updateAluno: vi.fn(),
    updateProfessor: vi.fn(),
    baixarCobranca: vi.fn(),
    cancelarCobranca: vi.fn(),
    exportCobrancasCsv: vi.fn(),
    alunos: vi.fn(),
    cobrancas: vi.fn(),
  },
}))

import { endpoints } from '../src/services/api/endpoints'
import {
  confirmIntegration,
  loadRemoteRows,
  logoutIntegration,
  submitForm,
} from '../src/services/api/integration'

const api = vi.mocked(endpoints)

describe('integração com o backend', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('entra e encaminha para o aceite quando os termos ainda não foram aceitos', async () => {
    api.login.mockResolvedValue(undefined)
    api.me.mockResolvedValue({
      id: 'usr-001',
      email: 'professor@exemplo.invalid',
      emailVerificado: true,
      termosAceitos: false,
    })

    const result = await submitForm('P01-login', {
      email: 'professor@exemplo.invalid',
      senha: 'senha1234',
    })

    expect(api.login).toHaveBeenCalledWith('professor@exemplo.invalid', 'senha1234')
    expect(result).toEqual({ ok: true, redirect: 'P08-aceite-de-termos-e-politica' })
  })

  it('devolve a mensagem da API quando o login falha', async () => {
    api.login.mockRejectedValue(
      Object.assign(new Error('E-mail ou senha inválidos.'), {
        status: 401,
        problem: { detail: 'E-mail ou senha inválidos.' },
      }),
    )

    const result = await submitForm('P01-login', { email: 'x@exemplo.invalid', senha: 'senha1234' })

    expect(result?.ok).toBe(false)
    expect(result?.message).toBe('E-mail ou senha inválidos.')
  })

  it('envia o cadastro apenas com os campos do contrato', async () => {
    api.register.mockResolvedValue(undefined)

    await submitForm('P02-cadastro', {
      nome: 'Professor Exemplo',
      razaoSocial: 'Professor Exemplo Música LTDA',
      cnpj: '00000000000191',
      email: 'professor@exemplo.invalid',
      senha: 'senha1234',
      emailContato: '',
      telefoneContato: '11999990000',
    })

    expect(api.register).toHaveBeenCalledWith({
      nome: 'Professor Exemplo',
      razaoSocial: 'Professor Exemplo Música LTDA',
      cnpj: '00000000000191',
      email: 'professor@exemplo.invalid',
      senha: 'senha1234',
      emailContato: null,
      telefoneContato: '11999990000',
    })
  })

  it('extrai o token quando a tela recebe o link completo de recuperação', async () => {
    api.confirmPasswordRecovery.mockResolvedValue(undefined)

    await submitForm('P07-redefinir-senha', {
      token: 'https://app.exemplo.invalid/redefinir?token=abc-123',
      novaSenha: 'senha1234',
    })

    expect(api.confirmPasswordRecovery).toHaveBeenCalledWith('abc-123', 'senha1234')
  })

  it('dá baixa e cancela cobrança quando o identificador está disponível', async () => {
    api.baixarCobranca.mockResolvedValue({} as never)
    api.cancelarCobranca.mockResolvedValue(undefined)

    expect(
      await submitForm(
        'P30-dar-baixa-na-cobranca',
        {
          recebidoEm: '2026-11-21',
          formaPagamento: 'Pix',
        },
        'cob-001',
      ),
    ).toEqual({ ok: true, redirect: 'P15-aba-cobrancas' })
    expect(api.baixarCobranca).toHaveBeenCalledWith('cob-001', {
      recebidoEm: '2026-11-21',
      formaPagamento: 'Pix',
    })

    expect(await confirmIntegration('P50-cancelar-cobranca', 'cob-001')).toEqual({
      ok: true,
      redirect: 'P15-aba-cobrancas',
    })
    expect(api.cancelarCobranca).toHaveBeenCalledWith('cob-001')
  })

  it('mantém o fluxo local quando não há identificador de cobrança', async () => {
    expect(await submitForm('P30-dar-baixa-na-cobranca', { recebidoEm: '2026-11-21' })).toBeNull()
    expect(await confirmIntegration('P50-cancelar-cobranca')).toBeNull()
    expect(api.baixarCobranca).not.toHaveBeenCalled()
    expect(api.cancelarCobranca).not.toHaveBeenCalled()
  })

  it('registra o aceite legal e encerra a sessão pela API', async () => {
    api.acceptLegal.mockResolvedValue(undefined)
    api.logout.mockResolvedValue(undefined)

    expect(await confirmIntegration('P08-aceite-de-termos-e-politica')).toEqual({
      ok: true,
      redirect: 'P11-lista-de-alunos',
    })
    expect(await logoutIntegration()).toEqual({ ok: true, redirect: 'P01-login' })
    expect(api.acceptLegal).toHaveBeenCalledWith('1.0', '1.0')
    expect(api.logout).toHaveBeenCalledOnce()
  })

  it('usa a lista remota de alunos quando o backend responde', async () => {
    api.alunos.mockResolvedValue({
      content: [
        {
          id: 'alu-001',
          nome: 'Aluno Exemplo 01',
          telefone: '11900010001',
          dataNascimento: '1998-04-12',
          ativo: true,
        },
      ],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    })

    const rows = await loadRemoteRows('alunos')

    expect(rows).toEqual([
      {
        id: 'alu-001',
        cells: {
          nome: 'Aluno Exemplo 01',
          telefone: '11900010001',
          dataNascimento: '12/04/1998',
          situacao: 'ATIVO',
        },
      },
    ])
  })
})
