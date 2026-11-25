import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { DataTable, type Column } from '../components/DataTable'
import { Screen } from '../components/Screen'
import { ScreenTabs } from '../components/ScreenTabs'
import { SelectField, TextAreaField, TextField } from '../components/Fields'
import { StatusBadge } from '../components/StatusBadge'
import { toneForStatus } from '../components/statusTone'
import {
  aulasMock,
  cobrancasMock,
  modalidadesMock,
  planosMock,
  professorMock,
  publicProfileMock,
  alunosMock,
} from '../data/mock'
import { navigate, useHashRoute } from '../router/hashRouter'
import {
  confirmIntegration,
  loadRemoteRows,
  logoutIntegration,
  submitForm,
} from '../services/api/integration'
import { useTheme } from '../theme/useTheme'
import { formatCurrency, formatDate } from '../utils/format'
import { screenById } from './definitions'
import type { ActionDefinition, FieldDefinition, ScreenBlock, TableSource } from './types'

type FieldsBlock = Extract<ScreenBlock, { type: 'fields' }>
type ActionsBlock = Extract<ScreenBlock, { type: 'actions' }>
type TableBlock = Extract<ScreenBlock, { type: 'table' }>
type ConfirmBlock = Extract<ScreenBlock, { type: 'confirm' }>
type PreviewBlock = Extract<ScreenBlock, { type: 'preview' }>

const variantClass: Record<NonNullable<ActionDefinition['variant']>, string> = {
  primary: 'btn-primary',
  secondary: 'btn-secondary',
  danger: 'btn-danger',
  ghost: 'btn-ghost',
}

interface TableRow {
  id: string
  cells: Record<string, string>
}

function getTableRows(source: TableSource): TableRow[] {
  switch (source) {
    case 'alunos':
      return alunosMock.map((aluno) => ({
        id: aluno.id,
        cells: {
          nome: aluno.nome,
          telefone: aluno.telefone ?? '—',
          dataNascimento: aluno.dataNascimento ? formatDate(aluno.dataNascimento) : '—',
          situacao: aluno.ativo ? 'ATIVO' : 'INATIVADO',
        },
      }))
    case 'aulas':
    case 'historicoAulas':
      return aulasMock
        .filter((aula) => source === 'aulas' || aula.alunoId === 'alu-001')
        .map((aula) => ({
          id: aula.id,
          cells: {
            aluno: alunosMock.find((aluno) => aluno.id === aula.alunoId)?.nome ?? 'Aluno',
            inicio: formatDate(aula.inicioEm),
            modalidade:
              modalidadesMock.find((modalidade) => modalidade.id === aula.modalidadeId)?.nome ??
              '—',
            status: aula.status,
          },
        }))
    case 'cobrancas':
      return cobrancasMock.map((cobranca) => ({
        id: cobranca.id,
        cells: {
          aluno: alunosMock.find((aluno) => aluno.id === cobranca.alunoId)?.nome ?? 'Aluno',
          valor: formatCurrency(cobranca.valor),
          vencimento: formatDate(cobranca.vencimentoEm),
          situacao: cobranca.situacao,
        },
      }))
    case 'planos':
      return planosMock.map((plano) => ({
        id: plano.id,
        cells: {
          aluno: alunosMock.find((aluno) => aluno.id === plano.alunoId)?.nome ?? 'Aluno',
          periodicidade: plano.periodicidade === 'MENSAL' ? 'Mensal' : 'Por aula',
          valor: formatCurrency(plano.valor),
          status: plano.status,
        },
      }))
    case 'modalidades':
      return modalidadesMock.map((modalidade) => ({
        id: modalidade.id,
        cells: {
          nome: modalidade.nome,
          formato: modalidade.formato,
          valor: modalidade.valor === null ? 'Oculto' : formatCurrency(modalidade.valor),
          publicada: modalidade.publicada ? 'PUBLICADO' : 'DESPUBLICADO',
        },
      }))
  }
}

const tableHeadings: Record<TableSource, { key: string; header: string }[]> = {
  alunos: [
    { key: 'nome', header: 'Aluno' },
    { key: 'telefone', header: 'Telefone' },
    { key: 'dataNascimento', header: 'Nascimento' },
    { key: 'situacao', header: 'Situação' },
  ],
  aulas: [
    { key: 'aluno', header: 'Aluno' },
    { key: 'inicio', header: 'Início' },
    { key: 'modalidade', header: 'Modalidade' },
    { key: 'status', header: 'Status' },
  ],
  historicoAulas: [
    { key: 'inicio', header: 'Início' },
    { key: 'modalidade', header: 'Modalidade' },
    { key: 'status', header: 'Status' },
  ],
  cobrancas: [
    { key: 'aluno', header: 'Aluno' },
    { key: 'valor', header: 'Valor' },
    { key: 'vencimento', header: 'Vencimento' },
    { key: 'situacao', header: 'Situação' },
  ],
  planos: [
    { key: 'aluno', header: 'Aluno' },
    { key: 'periodicidade', header: 'Periodicidade' },
    { key: 'valor', header: 'Valor' },
    { key: 'status', header: 'Status' },
  ],
  modalidades: [
    { key: 'nome', header: 'Modalidade' },
    { key: 'formato', header: 'Formato' },
    { key: 'valor', header: 'Valor' },
    { key: 'publicada', header: 'Publicação' },
  ],
}

function FieldsBlockView({ block }: { block: FieldsBlock }) {
  const { slug, search } = useHashRoute()
  const [values, setValues] = useState<Record<string, string>>(() =>
    Object.fromEntries(
      block.fields.map((definition) => [definition.name, definition.defaultValue ?? '']),
    ),
  )
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saved, setSaved] = useState(false)
  const [busy, setBusy] = useState(false)
  const [apiError, setApiError] = useState('')

  function validate(definition: FieldDefinition, value: string) {
    if (definition.required && !value.trim()) return 'Campo obrigatório.'
    if (definition.type === 'email' && value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
      return 'Informe um e-mail válido.'
    }
    if (
      (definition.name === 'senha' || definition.name === 'novaSenha') &&
      value &&
      (value.length < 8 || value.length > 72 || !/[A-Za-z]/.test(value) || !/\d/.test(value))
    ) {
      return 'Use de 8 a 72 caracteres, com letra e número.'
    }
    return ''
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextErrors: Record<string, string> = {}
    for (const definition of block.fields) {
      const error = validate(definition, values[definition.name] ?? '')
      if (error) nextErrors[definition.name] = error
    }
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setApiError('')
    setBusy(true)
    const result = await submitForm(slug, values, search.get('id'))
    setBusy(false)

    if (result && !result.ok) {
      setApiError(result.message ?? 'Não foi possível concluir. Tente novamente.')
      return
    }

    setSaved(true)
    navigate(result?.redirect ?? block.submitTo ?? slug)
  }

  return (
    <form className="card space-y-4" aria-label={block.submitLabel} onSubmit={onSubmit} noValidate>
      <div className="grid gap-4 sm:grid-cols-2">
        {block.fields.map((definition) => {
          const value = values[definition.name] ?? ''
          const onChange = (next: string) =>
            setValues((current) => ({ ...current, [definition.name]: next }))
          const error = errors[definition.name]
          const common = {
            label: definition.label,
            value,
            hint: definition.hint,
            error,
            required: definition.required,
            onChange: (event: { target: { value: string } }) => onChange(event.target.value),
          }

          if (definition.type === 'select') {
            return (
              <SelectField
                key={definition.name}
                {...common}
                options={definition.options ?? [{ value: '', label: 'Selecione' }]}
              />
            )
          }
          if (definition.type === 'textarea') {
            return <TextAreaField key={definition.name} {...common} />
          }
          return (
            <TextField
              key={definition.name}
              {...common}
              type={definition.type ?? 'text'}
              placeholder={definition.placeholder}
            />
          )
        })}
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <button className="btn btn-primary" type="submit" disabled={busy}>
          {busy ? 'Enviando…' : block.submitLabel}
        </button>
        {saved && !apiError ? (
          <span role="status" style={{ color: 'var(--success)' }}>
            Dados conferidos.
          </span>
        ) : null}
      </div>
      {apiError ? (
        <p role="alert" style={{ color: 'var(--error)' }}>
          {apiError}
        </p>
      ) : null}
      {block.note ? <p className="help-text">{block.note}</p> : null}
    </form>
  )
}

function ActionsBlockView({ block }: { block: ActionsBlock }) {
  const { setTheme } = useTheme()
  const { slug } = useHashRoute()
  const [apiError, setApiError] = useState('')

  async function runAction(item: ActionDefinition) {
    if (slug === 'P10-sair' && item.to === 'P01-login') {
      const result = await logoutIntegration()
      if (result && !result.ok) {
        setApiError(result.message ?? 'Não foi possível encerrar a sessão.')
        return
      }
    }
    navigate(item.to ?? '/P01-login')
  }

  return (
    <div className="flex flex-wrap gap-2">
      {block.items.map((item) => {
        const className = `btn ${variantClass[item.variant ?? 'secondary']}`
        if (item.href) {
          return (
            <a
              key={item.label}
              className={className}
              href={item.href}
              target="_blank"
              rel="noreferrer"
            >
              {item.label}
            </a>
          )
        }
        const theme = item.theme
        if (theme) {
          return (
            <button key={item.label} className={className} onClick={() => setTheme(theme)}>
              {item.label}
            </button>
          )
        }
        return (
          <button key={item.label} className={className} onClick={() => void runAction(item)}>
            {item.label}
          </button>
        )
      })}
      {apiError ? (
        <p role="alert" className="w-full" style={{ color: 'var(--error)' }}>
          {apiError}
        </p>
      ) : null}
    </div>
  )
}

function TableBlockView({ block }: { block: TableBlock }) {
  const [search, setSearch] = useState('')
  const [remote, setRemote] = useState<{ source: string; rows: TableRow[] } | null>(null)

  useEffect(() => {
    let active = true
    if (block.source === 'alunos' || block.source === 'cobrancas') {
      void loadRemoteRows(block.source).then((rows) => {
        if (active && rows) setRemote({ source: block.source, rows })
      })
    }
    return () => {
      active = false
    }
  }, [block.source])

  const rows = useMemo(() => {
    const all = remote?.source === block.source ? remote.rows : getTableRows(block.source)
    const term = search.trim().toLowerCase()
    return term
      ? all.filter((row) => Object.values(row.cells).join(' ').toLowerCase().includes(term))
      : all
  }, [block.source, remote, search])

  const columns: Column<TableRow>[] = tableHeadings[block.source].map((heading) => ({
    key: heading.key,
    header: heading.header,
    render: (row) => {
      const value = row.cells[heading.key] ?? '—'
      const isStatus = ['situacao', 'status', 'publicada'].includes(heading.key)
      const cell = isStatus ? <StatusBadge tone={toneForStatus(value)}>{value}</StatusBadge> : value
      if (block.rowLink && heading.key === 'nome') {
        return (
          <button className="link" onClick={() => navigate(`${block.rowLink}?id=${row.id}`)}>
            {cell}
          </button>
        )
      }
      return cell
    },
  }))

  return (
    <div className="space-y-3">
      {block.title ? <h2 className="text-lg font-bold">{block.title}</h2> : null}
      {block.searchable ? (
        <TextField
          label="Buscar"
          placeholder="Busque por nome, telefone ou situação"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      ) : null}
      <DataTable columns={columns} rows={rows} />
    </div>
  )
}

function ConfirmBlockView({ block }: { block: ConfirmBlock }) {
  const { slug, search } = useHashRoute()
  const [checked, setChecked] = useState(false)
  const [busy, setBusy] = useState(false)
  const [apiError, setApiError] = useState('')
  const cancelLabel = block.cancelLabel
  const cancelTo = block.cancelTo

  async function onConfirm() {
    setApiError('')
    setBusy(true)
    const result = await confirmIntegration(slug, search.get('id'))
    setBusy(false)
    if (result && !result.ok) {
      setApiError(result.message ?? 'Não foi possível concluir. Tente novamente.')
      return
    }
    navigate(result?.redirect ?? block.confirmTo)
  }

  return (
    <div className="card space-y-4">
      <h2 className="text-xl font-bold">{block.title}</h2>
      <p style={{ color: 'var(--muted)' }}>{block.text}</p>
      {block.consequences ? (
        <ul className="list-disc space-y-1 pl-5" style={{ color: 'var(--muted)' }}>
          {block.consequences.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      ) : null}
      {block.checkboxLabel ? (
        <label className="flex min-h-11 items-start gap-3">
          <input
            type="checkbox"
            className="mt-1 h-5 w-5"
            checked={checked}
            onChange={(event) => setChecked(event.target.checked)}
          />
          <span>{block.checkboxLabel}</span>
        </label>
      ) : null}
      <div className="flex flex-wrap gap-2">
        <button
          className="btn btn-primary"
          disabled={(Boolean(block.checkboxLabel) && !checked) || busy}
          onClick={() => void onConfirm()}
        >
          {busy ? 'Enviando…' : block.confirmLabel}
        </button>
        {cancelLabel && cancelTo ? (
          <button className="btn btn-secondary" onClick={() => navigate(cancelTo)}>
            {cancelLabel}
          </button>
        ) : null}
      </div>
      {apiError ? (
        <p role="alert" style={{ color: 'var(--error)' }}>
          {apiError}
        </p>
      ) : null}
    </div>
  )
}

function PreviewBlockView({ block }: { block: PreviewBlock }) {
  const publicProfile = publicProfileMock
  const modalidades = publicProfile.modalidades.filter((modalidade) => modalidade.publicada)

  return (
    <div className="surface overflow-hidden rounded-2xl">
      <div className="p-6 sm:p-8">
        <div className="flex flex-wrap items-center gap-3">
          <span
            className="grid h-14 w-14 place-items-center rounded-full text-xl font-bold"
            style={{ background: 'var(--link)', color: 'var(--bg)' }}
            aria-hidden="true"
          >
            PE
          </span>
          <div>
            <p className="text-xs font-bold tracking-widest" style={{ color: 'var(--muted)' }}>
              {block.audience === 'publico' ? 'PÁGINA PÚBLICA' : 'PRÉVIA INTERNA'}
            </p>
            <h2 className="text-2xl font-bold">{professorMock.nome}</h2>
          </div>
        </div>
        <h3 className="mt-8 text-xl font-bold">{publicProfile.titulo}</h3>
        <p className="mt-2 max-w-2xl leading-relaxed">{publicProfile.apresentacao}</p>
        <h4 className="mt-8 font-bold">Modalidades</h4>
        <ul className="mt-3 grid gap-3 sm:grid-cols-2">
          {modalidades.map((modalidade) => (
            <li key={modalidade.id} className="card">
              <strong className="block">{modalidade.nome}</strong>
              <span className="text-sm" style={{ color: 'var(--muted)' }}>
                {modalidade.formato} · {modalidade.duracaoMinutos} min
              </span>
              {publicProfile.mostrarValores && modalidade.valor !== null ? (
                <p className="mt-2 font-bold">{formatCurrency(modalidade.valor)}</p>
              ) : null}
            </li>
          ))}
        </ul>
        <div className="divider mt-8 pt-4 text-sm" style={{ color: 'var(--muted)' }}>
          {professorMock.emailContato} · {professorMock.telefoneContato}
        </div>
      </div>
    </div>
  )
}

function ThemeChooserBlock() {
  const { theme, setTheme } = useTheme()
  return (
    <div className="flex flex-wrap gap-2">
      <button
        className={`btn ${theme === 'claro' ? 'btn-primary' : 'btn-secondary'}`}
        onClick={() => setTheme('claro')}
        aria-pressed={theme === 'claro'}
      >
        Tema claro
      </button>
      <button
        className={`btn ${theme === 'escuro' ? 'btn-primary' : 'btn-secondary'}`}
        onClick={() => setTheme('escuro')}
        aria-pressed={theme === 'escuro'}
      >
        Tema escuro
      </button>
    </div>
  )
}

function BlockView({ block }: { block: ScreenBlock }) {
  switch (block.type) {
    case 'paragraph':
      return <p className="leading-relaxed">{block.text}</p>
    case 'callout': {
      const toneClass = {
        info: 'notice',
        success: 'notice',
        warning: 'notice',
        error: 'notice',
      }[block.tone]
      return (
        <div className={toneClass} role="status">
          {block.title ? <strong className="block">{block.title}</strong> : null}
          <span>{block.text}</span>
        </div>
      )
    }
    case 'bullets':
      return (
        <ul className="list-disc space-y-2 pl-5" style={{ color: 'var(--muted)' }}>
          {block.items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      )
    case 'fields':
      return <FieldsBlockView block={block} />
    case 'actions':
      return <ActionsBlockView block={block} />
    case 'table':
      return <TableBlockView block={block} />
    case 'tabs':
      return <ScreenTabs items={block.items} />
    case 'stats':
      return (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {block.items.map((item) => (
            <div key={item.label} className="card">
              <p
                className="text-xs font-bold uppercase tracking-wide"
                style={{ color: 'var(--muted)' }}
              >
                {item.label}
              </p>
              <p className="mt-2 text-2xl font-bold">{item.value}</p>
              {item.hint ? (
                <p className="mt-1 text-sm" style={{ color: 'var(--muted)' }}>
                  {item.hint}
                </p>
              ) : null}
            </div>
          ))}
        </div>
      )
    case 'preview':
      return <PreviewBlockView block={block} />
    case 'confirm':
      return <ConfirmBlockView block={block} />
    case 'themeChooser':
      return <ThemeChooserBlock />
    case 'code':
      return (
        <div>
          <p className="field-label">{block.label}</p>
          <pre className="surface overflow-x-auto rounded-xl p-4 text-xs">{block.value}</pre>
        </div>
      )
  }
}

export function DynamicScreen({ id }: { id: string }) {
  const definition = screenById.get(id)

  if (!definition) {
    return (
      <Screen
        anchorId="V03"
        title="Página não encontrada"
        description="O endereço acessado não existe."
      >
        <div className="flex flex-wrap gap-2">
          <button className="btn btn-primary" onClick={() => navigate('/P01-login')}>
            Ir para o início
          </button>
        </div>
      </Screen>
    )
  }

  return (
    <Screen
      anchorId={definition.code}
      title={definition.title}
      description={definition.description}
    >
      <div className="space-y-5">
        {definition.blocks.map((block, index) => (
          <BlockView key={index} block={block} />
        ))}
      </div>
    </Screen>
  )
}
