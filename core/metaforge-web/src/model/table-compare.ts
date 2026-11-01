/** A column as the compare API describes one side of it. */
export interface CompareColumn { name: string; type: string; nullable: boolean; defaultValue: string | null; comment: string }
export type StructureStatus = 'same' | 'changed' | 'source_only' | 'target_only'
export interface StructureRow { name: string; status: StructureStatus; source: CompareColumn | null; target: CompareColumn | null; changes: string[] }
export type StructureCounts = Record<StructureStatus, number>

export type DataType = 'same' | 'changed' | 'source_only' | 'target_only'
export interface DataRow { type: DataType; key: (string | null)[]; source: (string | null)[] | null; target: (string | null)[] | null; changed: string[] }
export interface DataResult { columns: string[]; keys: string[]; compared: number; total: number; truncated: boolean; counts: StructureCounts; rows: DataRow[] }

export interface CompareTables { sourceDatabase: string; sourceTable: string; targetDatabase: string; targetTable: string }
export interface DataForm { mode: 'primary' | 'fields'; keys: string[]; scope: 'all' | 'range'; start: number; end: number; onlyChanged: boolean; includeMissing: boolean }

export const emptyDataForm = (): DataForm => ({ mode: 'primary', keys: [], scope: 'range', start: 1, end: 5000, onlyChanged: true, includeMissing: true })
export const emptyCounts = (): StructureCounts => ({ changed: 0, source_only: 0, target_only: 0, same: 0 })

/** One side of the structure comparison: the columns that side has, with the pair's status. */
export interface StructureSideRow { name: string; status: StructureStatus; column: CompareColumn; changes: string[] }

/** The source table lists its columns (same, changed, source only); the target lists its own (same, changed, target only). */
export function structureSides(rows: StructureRow[], onlyDifferent: boolean) {
  const shown = onlyDifferent ? rows.filter(row => row.status !== 'same') : rows
  const side = (key: 'source' | 'target') => shown.filter(row => row[key]).map(row => ({ name: row.name, status: row.status, column: row[key]!, changes: row.changes }))
  return { source: side('source'), target: side('target') }
}

/** The request for compare/data: start and end rows (1-based, inclusive) become an offset and a count. */
export function dataBody(tables: CompareTables, form: DataForm) {
  const start = Math.max(1, Math.floor(form.start))
  const end = Math.max(start, Math.floor(form.end))
  return {
    ...tables,
    mode: form.mode,
    keys: form.mode === 'fields' ? form.keys : [],
    scope: form.scope,
    offset: start - 1,
    limit: end - start + 1,
    onlyChanged: form.onlyChanged,
    includeMissing: form.includeMissing,
  }
}

/** Display of a value from the compare API: NULL stays visible as NULL. */
export const cellText = (value: string | null | undefined) => value === null || value === undefined ? 'NULL' : value
