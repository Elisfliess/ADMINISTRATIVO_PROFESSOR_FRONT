import type { InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

type BaseFieldProps = {
  label: string
  hint?: string
  error?: string
}

type TextFieldProps = BaseFieldProps & InputHTMLAttributes<HTMLInputElement>

export function TextField({ label, hint, error, id, ...props }: TextFieldProps) {
  const inputId = id ?? `campo-${label.toLowerCase().replace(/\s+/g, '-')}`
  return (
    <div>
      <label className="field-label" htmlFor={inputId}>
        {label}
      </label>
      <input id={inputId} className="input" aria-invalid={Boolean(error)} {...props} />
      {error ? (
        <p className="error-text">{error}</p>
      ) : hint ? (
        <p className="help-text">{hint}</p>
      ) : null}
    </div>
  )
}

type SelectFieldProps = BaseFieldProps &
  SelectHTMLAttributes<HTMLSelectElement> & { options: { value: string; label: string }[] }

export function SelectField({ label, hint, error, id, options, ...props }: SelectFieldProps) {
  const selectId = id ?? `campo-${label.toLowerCase().replace(/\s+/g, '-')}`
  return (
    <div>
      <label className="field-label" htmlFor={selectId}>
        {label}
      </label>
      <select id={selectId} className="input" aria-invalid={Boolean(error)} {...props}>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? (
        <p className="error-text">{error}</p>
      ) : hint ? (
        <p className="help-text">{hint}</p>
      ) : null}
    </div>
  )
}

type TextAreaFieldProps = BaseFieldProps & TextareaHTMLAttributes<HTMLTextAreaElement>

export function TextAreaField({ label, hint, error, id, ...props }: TextAreaFieldProps) {
  const areaId = id ?? `campo-${label.toLowerCase().replace(/\s+/g, '-')}`
  return (
    <div>
      <label className="field-label" htmlFor={areaId}>
        {label}
      </label>
      <textarea id={areaId} className="input min-h-28" aria-invalid={Boolean(error)} {...props} />
      {error ? (
        <p className="error-text">{error}</p>
      ) : hint ? (
        <p className="help-text">{hint}</p>
      ) : null}
    </div>
  )
}
