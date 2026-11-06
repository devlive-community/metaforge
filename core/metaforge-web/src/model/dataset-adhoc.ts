import type { QueryChartRow } from './query-chart'

export type AdhocMode = 'METRIC' | 'DIMENSION' | 'FILTER'
export interface AdhocField {
  id: number
  name: string
  aliasName?: string
  type: string
  mode: AdhocMode
  comment?: string
}
export interface AdhocColumn extends AdhocField {
  key: string
  expression?: string
  alias?: string
  order?: string
  function?: string
  value?: string
}
export interface AdhocQuery {
  columns: Pick<AdhocColumn, 'id' | 'mode' | 'expression' | 'alias' | 'order' | 'function' | 'value'>[]
  limit: number
}

export const fieldLabel = (field: AdhocField) => field.aliasName || field.name
export const isAdhocNumeric = (type: string) => type === 'NUMBER' || type === 'NUMBER_SIGNED'
export function adhocOperators(type: string) {
  return isAdhocNumeric(type) || type === 'DATETIME' ? ['EQ', 'NEQ', 'GT', 'GTE', 'LT', 'LTE', 'NULL', 'NNULL']
    : type === 'BOOLEAN' ? ['EQ', 'NEQ', 'NULL', 'NNULL'] : ['EQ', 'NEQ', 'LIKE', 'NLIKE', 'NULL', 'NNULL']
}
export const columnKey = (id: number, mode: AdhocMode) => `${mode}:${id}`
export function createAdhocColumn(field: AdhocField, mode = field.mode): AdhocColumn {
  return { ...field, mode, key: columnKey(field.id, mode),
    expression: mode === 'METRIC' ? isAdhocNumeric(field.type) ? 'SUM' : 'COUNT' : mode === 'FILTER' ? 'EQ' : undefined,
    value: '', order: '' }
}
export function adhocQuery(columns: AdhocColumn[], limit: number): AdhocQuery {
  return { limit, columns: columns.map(({ id, mode, expression, alias, order, function: fn, value }) =>
    ({ id, mode, expression, alias, order, function: fn, value: mode === 'FILTER' && expression !== 'NULL' && expression !== 'NNULL' ? value : undefined })) }
}

/** Restore by identity without mutating the catalog or sharing objects across query roles. */
export function restoreAdhocQuery(fields: AdhocField[], saved: unknown) {
  const query = typeof saved === 'string' ? JSON.parse(saved) : saved
  if (!query || !Array.isArray(query.columns)) throw Error('dataset.adhoc.invalidReport')
  const columns: AdhocColumn[] = []
  let missing = 0
  for (const item of query.columns) {
    const source = fields.find(field => field.id === item.id)
    if (!source || !['METRIC', 'DIMENSION', 'FILTER'].includes(item.mode)) { missing++; continue }
    const base = createAdhocColumn(source, item.mode)
    if (columns.some(column => column.key === base.key)) continue
    columns.push({ ...base, expression: item.expression, alias: item.alias, order: item.order,
      function: item.function, value: item.value == null ? '' : String(item.value) })
  }
  const limit = Number(query.limit)
  return { columns, missing, limit: Number.isInteger(limit) && limit > 0 ? limit : 1000 }
}

export function adhocColumnIssue(column: AdhocColumn): string | undefined {
  if (column.mode === 'METRIC') {
    const choices = isAdhocNumeric(column.type) ? ['SUM', 'COUNT', 'MAX', 'MIN', 'AVG'] : ['COUNT', 'MAX', 'MIN']
    return choices.includes(column.expression || '') ? undefined : 'dataset.tip.selectExpression'
  }
  if (column.mode !== 'FILTER') return
  const operators = adhocOperators(column.type)
  if (!operators.includes(column.expression || '')) return 'dataset.adhoc.invalidOperator'
  if (column.expression === 'NULL' || column.expression === 'NNULL') return
  if (!column.value?.trim()) return 'dataset.adhoc.filterValueRequired'
  if (isAdhocNumeric(column.type) && !Number.isFinite(Number(column.value))) return 'typedFilter.number'
  if (column.type === 'DATETIME' && !Number.isFinite(Date.parse(column.value))) return 'dataset.adhoc.invalidValue'
  if (column.type === 'BOOLEAN' && !['true', 'false'].includes(column.value)) return 'dataset.adhoc.invalidValue'
}

export function adhocRows(headers: string[], rows: unknown[]): QueryChartRow[] {
  return rows.map(row => Object.fromEntries(headers.map((header, index) => [header,
    Array.isArray(row) ? row[index] : row && typeof row === 'object' ? (row as QueryChartRow)[header] : null])))
}

export function adhocCsv(headers: string[], rows: QueryChartRow[]) {
  const escape = (value: unknown) => {
    const text = value == null ? '' : typeof value === 'object' ? JSON.stringify(value) : String(value)
    return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
  }
  return '\uFEFF' + [headers.map(escape).join(','), ...rows.map(row => headers.map(header => escape(row[header])).join(','))].join('\r\n')
}
