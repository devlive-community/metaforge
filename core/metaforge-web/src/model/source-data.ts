export type DataOperator = '=' | '!=' | '>' | '>=' | '<' | '<=' | 'LIKE' | 'IN' | 'NOT IN' | 'BETWEEN' | 'IS NULL' | 'IS NOT NULL'

export interface DataCondition {
  id: number
  column: string
  operator: DataOperator
  /** A string for scalar operators, a list for IN / NOT IN, a [start, end] date pair for BETWEEN. */
  value: string | string[]
}

export interface DataFilter {
  name: string
  operator: string
  value?: unknown
}

export type DataKind = 'date' | 'number' | 'text'

/** API operator names understood by Filter.Operator on the server. */
const API_OPERATORS: Record<Exclude<DataOperator, 'BETWEEN'>, string> = {
  '=': 'EQ', '!=': 'NE', '>': 'GT', '>=': 'GE', '<': 'LT', '<=': 'LE',
  LIKE: 'LIKE', IN: 'IN', 'NOT IN': 'NOT_IN', 'IS NULL': 'IS_NULL', 'IS NOT NULL': 'IS_NOT_NULL',
}

export function dataKind(type: string): DataKind {
  const value = String(type || '').toUpperCase()
  if (/DATE|TIME|YEAR/.test(value)) return 'date'
  if (/INT|DECIMAL|NUMERIC|FLOAT|DOUBLE|REAL|BIT/.test(value)) return 'number'
  return 'text'
}

export function operatorsFor(kind: DataKind): DataOperator[] {
  if (kind === 'date') return ['BETWEEN', '=', '!=', '>', '>=', '<', '<=', 'IS NULL', 'IS NOT NULL']
  if (kind === 'number') return ['=', '!=', '>', '>=', '<', '<=', 'IN', 'NOT IN', 'IS NULL', 'IS NOT NULL']
  return ['=', '!=', 'LIKE', 'IN', 'NOT IN', 'IS NULL', 'IS NOT NULL']
}

export const defaultOperator = (kind: DataKind): DataOperator => kind === 'date' ? 'BETWEEN' : kind === 'text' ? 'LIKE' : '='

export const emptyValue = (operator: DataOperator): string | string[] => operator === 'IN' || operator === 'NOT IN' ? [] : operator === 'BETWEEN' ? ['', ''] : ''

export const needsValue = (operator: DataOperator) => operator !== 'IS NULL' && operator !== 'IS NOT NULL'

/**
 * Turns the filter rows into server filters. Incomplete rows are skipped; BETWEEN becomes an
 * inclusive day range and LIKE matches anywhere unless the user typed their own wildcards.
 */
export function buildDataFilters(conditions: DataCondition[]): DataFilter[] {
  return conditions.flatMap<DataFilter>(condition => {
    const { column: name, operator, value } = condition
    if (!name) return []
    if (!needsValue(operator)) return [{ name, operator: API_OPERATORS[operator as Exclude<DataOperator, 'BETWEEN'>] }]
    if (operator === 'BETWEEN') {
      const [start, end] = Array.isArray(value) ? value : []
      return [
        ...(start ? [{ name, operator: 'GE', value: `${start} 00:00:00` }] : []),
        ...(end ? [{ name, operator: 'LE', value: `${end} 23:59:59` }] : []),
      ]
    }
    if (operator === 'IN' || operator === 'NOT IN') {
      const values = (Array.isArray(value) ? value : []).map(String).filter(item => item !== '')
      return values.length ? [{ name, operator: API_OPERATORS[operator], value: values }] : []
    }
    const text = String(Array.isArray(value) ? '' : value)
    if (text === '') return []
    return [{ name, operator: API_OPERATORS[operator], value: operator === 'LIKE' && !/[%_]/.test(text) ? `%${text}%` : text }]
  })
}

const csvCell = (value: unknown) => {
  if (value === null || value === undefined) return ''
  const text = typeof value === 'object' ? JSON.stringify(value) : String(value)
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

export function toCsv(columns: string[], rows: Record<string, unknown>[]): string {
  return [columns.map(csvCell).join(','), ...rows.map(row => columns.map(column => csvCell(row[column])).join(','))].join('\r\n')
}

export function toJson(columns: string[], rows: Record<string, unknown>[]): string {
  return JSON.stringify(rows.map(row => Object.fromEntries(columns.map(column => [column, row[column] ?? null]))), null, 2)
}

export type FieldKind = 'enum' | 'set' | 'boolean' | 'json' | 'date' | 'datetime' | 'time' | 'number' | 'longtext' | 'text'

export interface DataField {
  name: string
  /** Declared column type as the database reports it, e.g. `enum('a','b')` or `tinyint(1)`. */
  type: string
  kind: FieldKind
  options: string[]
  nullable: boolean
  hasDefault: boolean
  autoIncrement: boolean
  /** NOT NULL without a default or generated value: the user has to provide it. */
  required: boolean
}

export type FormValues = Record<string, string | null>
export type FieldIssue = 'required' | 'number' | 'json' | 'date' | 'datetime'

export function fieldKind(type: string): FieldKind {
  const raw = String(type || '').toLowerCase().trim()
  if (/^enum\(/.test(raw)) return 'enum'
  if (/^set\(/.test(raw)) return 'set'
  if (/^(tinyint\(1\)|bit\(1\)|bool)/.test(raw)) return 'boolean'
  if (/^json/.test(raw)) return 'json'
  if (/^date$/.test(raw)) return 'date'
  if (/^(datetime|timestamp)/.test(raw)) return 'datetime'
  if (/^time/.test(raw)) return 'time'
  if (/^(tinyint|smallint|mediumint|int|integer|bigint|decimal|numeric|float|double|real|year|bit)/.test(raw)) return 'number'
  if (/(text|blob)$/.test(raw)) return 'longtext'
  return 'text'
}

export function enumOptions(type: string): string[] {
  return [...String(type || '').matchAll(/'((?:[^']|'')*)'/g)].map(match => match[1].replace(/''/g, "'"))
}

/** Editable fields from the workspace column metadata rows. */
export function dataFields(rows: Record<string, any>[]): DataField[] {
  return rows.filter(row => row.type_name === 'column')
    .sort((left, right) => Number(left.object_position) - Number(right.object_position))
    .map(row => {
      const type = String(row.object_data_type || '')
      const nullable = String(row.object_nullable || 'YES') !== 'NO'
      const hasDefault = row.object_default_value != null
      const autoIncrement = String(row.object_autoincrement || 'NO') === 'YES'
      return { name: String(row.object_name), type, kind: fieldKind(type), options: enumOptions(type), nullable, hasDefault, autoIncrement,
        required: !nullable && !hasDefault && !autoIncrement }
    })
}

/** Turns a stored value into the text a form control edits; NULL stays null. */
export function toFormValue(field: DataField, value: unknown): string | null {
  if (value === null || value === undefined) return null
  if (typeof value === 'boolean') return value ? '1' : '0'
  if (typeof value === 'object') return JSON.stringify(value, null, field.kind === 'json' ? 2 : 0)
  if (field.kind === 'json') {
    try { return JSON.stringify(JSON.parse(String(value)), null, 2) } catch { return String(value) }
  }
  return String(value)
}

export const rowToForm = (fields: DataField[], row: Record<string, unknown> | null): FormValues =>
  Object.fromEntries(fields.map(field => [field.name, row ? toFormValue(field, row[field.name]) : null]))

function typedValue(field: DataField, text: string): unknown {
  if (field.kind === 'number' || field.kind === 'boolean') {
    const number = Number(text)
    return text.trim() !== '' && Number.isFinite(number) ? number : text
  }
  if (field.kind === 'json') {
    // JSON columns travel as text so the statement builder quotes and escapes them.
    try { return JSON.stringify(JSON.parse(text)) } catch { return text }
  }
  return text
}

/**
 * Builds the row sent to the server. Empty fields become NULL, except that generated values and
 * column defaults are left to the database by omitting the field when `omitDefaults` is set.
 */
export function formToRow(fields: DataField[], values: FormValues, omitDefaults = true): Record<string, unknown> {
  const row: Record<string, unknown> = {}
  for (const field of fields) {
    const value = values[field.name]
    if (value === null || value === '') {
      if (field.autoIncrement) row[field.name] = null
      else if (field.hasDefault && omitDefaults) continue
      else row[field.name] = field.nullable || value === null ? null : ''
      continue
    }
    row[field.name] = typedValue(field, value)
  }
  return row
}

export function validateForm(fields: DataField[], values: FormValues): Record<string, FieldIssue> {
  const issues: Record<string, FieldIssue> = {}
  for (const field of fields) {
    const value = values[field.name]
    if (value === null || value === '') {
      if (field.required) issues[field.name] = 'required'
      continue
    }
    if (field.kind === 'number' && !Number.isFinite(Number(value))) issues[field.name] = 'number'
    else if (field.kind === 'json') { try { JSON.parse(value) } catch { issues[field.name] = 'json' } }
    else if (field.kind === 'date' && !/^\d{4}-\d{2}-\d{2}$/.test(value)) issues[field.name] = 'date'
    else if (field.kind === 'datetime' && !/^\d{4}-\d{2}-\d{2}( \d{2}:\d{2}(:\d{2}(\.\d+)?)?)?$/.test(value)) issues[field.name] = 'datetime'
  }
  return issues
}

/** JSON editing shows every field, NULL included, with typed values. */
export function formToJson(fields: DataField[], values: FormValues): Record<string, unknown> {
  return Object.fromEntries(fields.map(field => {
    const value = values[field.name]
    if (value === null) return [field.name, null]
    if (field.kind === 'json') { try { return [field.name, JSON.parse(value)] } catch { return [field.name, value] } }
    return [field.name, typedValue(field, value)]
  }))
}

/** Normalizes a row typed in the JSON editor: nested objects in JSON columns become text. */
export function jsonToRow(fields: DataField[], row: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(row).map(([name, value]) => {
    const field = fields.find(item => item.name === name)
    return [name, value !== null && typeof value === 'object' && (!field || field.kind === 'json' || field.kind === 'longtext' || field.kind === 'text') ? JSON.stringify(value) : value]
  }))
}
