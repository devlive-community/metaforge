/** Kinds returned by GET .../table-dependencies, in the order the drop dialog lists them. */
export const DEPENDENCY_KINDS = ['foreign_key', 'view', 'routine', 'trigger'] as const
export type DependencyKind = typeof DEPENDENCY_KINDS[number]

export interface TableDependency { kind: DependencyKind; schema: string; name: string; detail: string }

export interface DropTableInfo {
  name: string
  database: string
  engine: string
  rows: number | null
  /** Data and index size together, in MB. */
  sizeMb: number | null
  created: string
  updated: string
  comment: string
}

const numberOrNull = (value: unknown) => {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}
/** `2026-08-01T10:24:36.000` and `2026-08-01 10:24:36` both read `2026-08-01 10:24:36`. */
export const formatDateTime = (value: unknown) => value ? String(value).replace('T', ' ').replace(/\.\d+.*$/, '') : '-'

/** The table's facts from GET .../{table} (sizes there are in MB). */
export function mapDropTableInfo(row: any, name: string, database: string): DropTableInfo {
  const data = numberOrNull(row?.object_data_size)
  const index = numberOrNull(row?.object_index_size)
  return {
    name,
    database,
    engine: String(row?.object_engine || '-'),
    rows: numberOrNull(row?.object_rows),
    sizeMb: data === null && index === null ? null : Math.round(((data ?? 0) + (index ?? 0)) * 100) / 100,
    created: formatDateTime(row?.object_create_time),
    updated: formatDateTime(row?.object_update_time),
    comment: String(row?.object_comment || '-'),
  }
}

/** Dependencies grouped by kind; every kind is present, empty when nothing depends on the table that way. */
export function groupDependencies(rows: any[]): Record<DependencyKind, TableDependency[]> {
  const groups = Object.fromEntries(DEPENDENCY_KINDS.map(kind => [kind, [] as TableDependency[]])) as Record<DependencyKind, TableDependency[]>
  for (const row of rows || []) {
    const kind = String(row?.object_type || '') as DependencyKind
    if (!groups[kind]) continue
    groups[kind].push({ kind, schema: String(row?.object_schema || ''), name: String(row?.object_name || ''), detail: String(row?.object_detail || '') })
  }
  return groups
}
