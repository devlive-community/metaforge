/** One column row of the ER card, with its key roles. */
export interface ErColumn {
  name: string
  /** The declared type as written, upper-cased, e.g. VARCHAR(50) or BIGINT UNSIGNED. */
  type: string
  primary: boolean
  auto: boolean
  /** Covered alone by a unique index other than the primary key. */
  unique: boolean
  /** Referenced table.column for a foreign key column. */
  reference: string | null
  nullable: boolean
  defaultValue: string | null
  comment: string
}

export interface ErSettings { types: boolean; tags: boolean; notNull: boolean; comments: boolean; grid: boolean }

export const defaultErSettings = (): ErSettings => ({ types: true, tags: true, notNull: false, comments: true, grid: true })

export const ER_ZOOM = { min: 0.25, max: 2, step: 0.1 }

/** Keeps the zoom inside the allowed range and on whole percents. */
export const clampZoom = (value: number) => Math.round(Math.min(ER_ZOOM.max, Math.max(ER_ZOOM.min, value)) * 100) / 100

const truthy = (value: unknown) => value === true || value === 1 || value === 'true' || value === '1'

/**
 * Columns of the table from the workspace's shared rows (GET .../columns), with keys taken from
 * the primary rows, the index rows (one per index column) and the foreign key rows.
 */
export function mapErColumns(columnRows: any[], indexRows: any[], foreignKeyRows: any[]): ErColumn[] {
  const primary = new Set(columnRows.filter(row => row?.type_name === 'primary').map(row => String(row.object_name)))
  const indexes = new Map<string, { columns: string[]; unique: boolean }>()
  for (const row of indexRows) {
    const name = String(row?.object_name || '')
    const index = indexes.get(name) || { columns: [], unique: truthy(row?.object_unique) }
    index.columns.push(String(row?.object_column || ''))
    indexes.set(name, index)
  }
  const unique = new Set([...indexes.entries()].filter(([name, index]) => name !== 'PRIMARY' && index.unique && index.columns.length === 1).map(([, index]) => index.columns[0]))
  const references = new Map<string, string>()
  for (const row of foreignKeyRows) {
    const column = String(row?.object_column || '')
    if (column && !references.has(column)) references.set(column, [row?.object_reference_table, row?.object_reference_column].filter(Boolean).join('.'))
  }
  return columnRows
      .filter(row => row?.type_name === 'column')
      .sort((left, right) => (Number(left.object_position) || 0) - (Number(right.object_position) || 0))
      .map(row => {
        const name = String(row.object_name)
        return {
          name,
          type: String(row.object_data_type || '').toUpperCase(),
          primary: primary.has(name),
          auto: String(row.object_autoincrement || '').toUpperCase() === 'YES',
          unique: !primary.has(name) && unique.has(name),
          reference: references.has(name) ? references.get(name) || '' : null,
          nullable: String(row.object_nullable || 'YES').toUpperCase() !== 'NO',
          defaultValue: row.object_default_value == null ? null : String(row.object_default_value),
          comment: String(row.object_comment || ''),
        }
      })
}
