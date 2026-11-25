import type { ReactNode } from 'react'

export interface Column<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
}

interface DataTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  emptyMessage?: string
}

export function DataTable<T>({
  columns,
  rows,
  emptyMessage = 'Nenhum registro encontrado.',
}: DataTableProps<T>) {
  if (rows.length === 0) {
    return (
      <div className="card text-center" style={{ color: 'var(--muted)' }}>
        {emptyMessage}
      </div>
    )
  }

  return (
    <div className="table-wrap surface rounded-2xl">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col">
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={index}>
              {columns.map((column) => (
                <td key={column.key}>{column.render(row)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
