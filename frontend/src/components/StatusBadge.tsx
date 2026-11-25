import type { ReactNode } from 'react'
import type { StatusTone } from './statusTone'

const toneClass: Record<StatusTone, string> = {
  neutro: 'badge',
  sucesso: 'badge badge-success',
  aviso: 'badge badge-warning',
  erro: 'badge badge-error',
}

export function StatusBadge({
  children,
  tone = 'neutro',
}: {
  children: ReactNode
  tone?: StatusTone
}) {
  return <span className={toneClass[tone]}>{children}</span>
}
