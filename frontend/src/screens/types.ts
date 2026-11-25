export type FieldType = 'text' | 'email' | 'password' | 'date' | 'number' | 'textarea' | 'select'

export interface FieldDefinition {
  name: string
  label: string
  type?: FieldType
  placeholder?: string
  hint?: string
  required?: boolean
  defaultValue?: string
  options?: { value: string; label: string }[]
}

export interface ActionDefinition {
  label: string
  to?: string
  href?: string
  theme?: 'claro' | 'escuro'
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
}

export type TableSource =
  | 'alunos'
  | 'aulas'
  | 'cobrancas'
  | 'planos'
  | 'modalidades'
  | 'historicoAulas'

export type ScreenBlock =
  | { type: 'paragraph'; text: string }
  | {
      type: 'callout'
      tone: 'info' | 'success' | 'warning' | 'error'
      title?: string
      text: string
    }
  | { type: 'bullets'; items: string[] }
  | {
      type: 'fields'
      fields: FieldDefinition[]
      submitLabel: string
      submitTo?: string
      note?: string
    }
  | { type: 'actions'; items: ActionDefinition[] }
  | {
      type: 'table'
      source: TableSource
      title?: string
      searchable?: boolean
      rowLink?: string
    }
  | { type: 'tabs'; items: { slug: string; label: string }[] }
  | { type: 'stats'; items: { label: string; value: string; hint?: string }[] }
  | { type: 'preview'; audience: 'professor' | 'publico' }
  | {
      type: 'confirm'
      title: string
      text: string
      consequences?: string[]
      checkboxLabel?: string
      confirmLabel: string
      confirmTo: string
      cancelLabel?: string
      cancelTo?: string
    }
  | { type: 'themeChooser' }
  | { type: 'code'; label: string; value: string }

export interface ScreenDefinition {
  id: string
  code: string
  title: string
  description: string
  group: string
  blocks: ScreenBlock[]
}
