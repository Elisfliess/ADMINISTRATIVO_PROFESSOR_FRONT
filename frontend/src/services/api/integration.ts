import type { ApiError } from '../../types/api'
import { formatCurrency, formatDate } from '../../utils/format'
import { endpoints } from './endpoints'
import { backendEnabled } from './mode'

export interface IntegrationResult {
  ok: boolean
  message?: string
  redirect?: string
}

export interface RemoteRow {
  id: string
  cells: Record<string, string>
}

function success(redirect?: string): IntegrationResult {
  return { ok: true, redirect }
}

function failure(error: unknown): IntegrationResult {
  const problem = (error as ApiError | undefined)?.problem
  const message = problem?.detail ?? problem?.title ?? (error as Error | undefined)?.message
  return {
    ok: false,
    message: message?.trim() ? message : 'Não foi possível concluir. Tente novamente.',
  }
}

function downloadCsv(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function optional(value: string | undefined): string | null {
  return value?.trim() ? value : null
}

/** A tela aceita o link completo; a API recebe apenas o token. */
function recoveryToken(value: string | undefined): string {
  const raw = value?.trim() ?? ''
  const query = raw.includes('?') ? new URLSearchParams(raw.slice(raw.indexOf('?') + 1)) : null
  return query?.get('token') ?? raw.split('/').filter(Boolean).pop() ?? ''
}

/** Retorna null quando não há backend configurado; o chamador mantém o fluxo local. */
export async function submitForm(
  screenId: string,
  values: Record<string, string>,
  resourceId?: string | null,
): Promise<IntegrationResult | null> {
  if (!backendEnabled) return null

  try {
    switch (screenId) {
      case 'P01-login': {
        await endpoints.login(values.email ?? '', values.senha ?? '')
        const session = await endpoints.me()
        if (!session.emailVerificado) {
          return { ok: false, message: 'Confirme seu e-mail antes de entrar.' }
        }
        return success(
          session.termosAceitos ? 'P11-lista-de-alunos' : 'P08-aceite-de-termos-e-politica',
        )
      }
      case 'P02-cadastro':
        await endpoints.register({
          email: values.email ?? '',
          senha: values.senha ?? '',
          nome: values.nome ?? '',
          razaoSocial: values.razaoSocial ?? '',
          cnpj: values.cnpj ?? '',
          emailContato: optional(values.emailContato),
          telefoneContato: optional(values.telefoneContato),
        })
        return success('P03-verifique-seu-e-mail')
      case 'P03-verifique-seu-e-mail':
        await endpoints.resendVerification(values.email ?? '')
        return success()
      case 'P05-recuperar-senha':
        await endpoints.requestPasswordRecovery(values.email ?? '')
        return success()
      case 'P07-redefinir-senha':
        await endpoints.confirmPasswordRecovery(recoveryToken(values.token), values.novaSenha ?? '')
        return success('P01-login')
      case 'P12-cadastrar-aluno': {
        const responsavelNome = optional(values.responsavelNome)
        const responsavelContato = optional(values.responsavelContato)
        await endpoints.createAluno({
          nome: values.nome ?? '',
          telefone: values.telefone ?? '',
          dataNascimento: values.dataNascimento ?? '',
          email: optional(values.email),
          observacoes: optional(values.observacoes),
          responsavelLegal:
            responsavelNome && responsavelContato
              ? { nome: responsavelNome, contato: responsavelContato }
              : null,
        })
        return success('P11-lista-de-alunos')
      }
      case 'P16-editar-aluno': {
        if (!resourceId) return success('P13-ficha-do-aluno')
        await endpoints.updateAluno(resourceId, {
          nome: values.nome,
          telefone: values.telefone,
          dataNascimento: values.dataNascimento,
          email: optional(values.email),
          observacoes: optional(values.observacoes),
        })
        return success('P13-ficha-do-aluno')
      }
      case 'P30-dar-baixa-na-cobranca': {
        if (!resourceId) return null
        await endpoints.baixarCobranca(resourceId, {
          recebidoEm: values.recebidoEm ?? '',
          formaPagamento: optional(values.formaPagamento),
        })
        return success('P15-aba-cobrancas')
      }
      case 'P32-exportar-recebimentos': {
        const blob = await endpoints.exportCobrancasCsv(values.mes ?? '', values.senha ?? '')
        downloadCsv(blob, `recebimentos-${values.mes ?? 'exportacao'}.csv`)
        return success()
      }
      default:
        return null
    }
  } catch (error) {
    return failure(error)
  }
}

/** Confirmações que possuem operação no backend. */
export async function confirmIntegration(
  screenId: string,
  resourceId?: string | null,
): Promise<IntegrationResult | null> {
  if (!backendEnabled) return null

  try {
    if (screenId === 'P08-aceite-de-termos-e-politica') {
      await endpoints.acceptLegal('1.0', '1.0')
      return success('P11-lista-de-alunos')
    }
    if (screenId === 'P50-cancelar-cobranca' && resourceId) {
      await endpoints.cancelarCobranca(resourceId)
      return success('P15-aba-cobrancas')
    }
    return null
  } catch (error) {
    return failure(error)
  }
}

export async function logoutIntegration(): Promise<IntegrationResult | null> {
  if (!backendEnabled) return null

  try {
    await endpoints.logout()
    return success('P01-login')
  } catch (error) {
    return failure(error)
  }
}

/** Carrega listas do backend quando disponível; retorna null para manter os dados locais. */
export async function loadRemoteRows(source: 'alunos' | 'cobrancas'): Promise<RemoteRow[] | null> {
  if (!backendEnabled) return null

  try {
    if (source === 'alunos') {
      const page = await endpoints.alunos({ size: 100 })
      return page.content.map((aluno) => ({
        id: aluno.id,
        cells: {
          nome: aluno.nome,
          telefone: aluno.telefone ?? '—',
          dataNascimento: aluno.dataNascimento ? formatDate(aluno.dataNascimento) : '—',
          situacao: aluno.ativo ? 'ATIVO' : 'INATIVADO',
        },
      }))
    }

    const mes = new Date().toISOString().slice(0, 7)
    const [cobrancas, alunos] = await Promise.all([
      endpoints.cobrancas(mes),
      endpoints.alunos({ size: 100 }),
    ])
    const nomes = new Map(alunos.content.map((aluno) => [aluno.id, aluno.nome]))
    return cobrancas.content.map((cobranca) => ({
      id: cobranca.id,
      cells: {
        aluno: nomes.get(cobranca.alunoId) ?? 'Aluno',
        valor: formatCurrency(cobranca.valor),
        vencimento: formatDate(cobranca.vencimentoEm),
        situacao: cobranca.situacao,
      },
    }))
  } catch {
    return null
  }
}
