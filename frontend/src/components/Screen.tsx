import type { ReactNode } from 'react'

interface ScreenProps {
  anchorId: string
  title: string
  description?: string
  actions?: ReactNode
  children: ReactNode
}

export function Screen({ anchorId, title, description, actions, children }: ScreenProps) {
  return (
    <section aria-labelledby={`titulo-${anchorId}`} className="mx-auto w-full max-w-6xl">
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 id={`titulo-${anchorId}`} className="text-2xl font-bold sm:text-3xl">
            {title}
          </h1>
          {description ? (
            <p className="mt-2 max-w-3xl leading-relaxed" style={{ color: 'var(--muted)' }}>
              {description}
            </p>
          ) : null}
        </div>
        {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
      </header>
      {children}
    </section>
  )
}
