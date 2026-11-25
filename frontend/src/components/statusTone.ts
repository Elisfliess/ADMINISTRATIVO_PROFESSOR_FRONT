export type StatusTone = 'neutro' | 'sucesso' | 'aviso' | 'erro'

export function toneForStatus(status: string): StatusTone {
  if (['PAGO', 'DADA', 'ATIVO', 'CONFIRMADO', 'PUBLICADO'].includes(status)) return 'sucesso'
  if (['PENDENTE', 'EM_ATRASO', 'AGENDADA'].includes(status)) return 'aviso'
  if (['CANCELADO', 'CANCELADA', 'FALTA', 'ENCERRADO', 'INVALIDO'].includes(status)) return 'erro'
  return 'neutro'
}
