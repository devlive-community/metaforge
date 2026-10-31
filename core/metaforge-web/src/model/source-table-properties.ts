export interface TableProperties {
  name: string
  database: string
  type: string
  engine: string
  rowFormat: string
  /** YYYY-MM-DD HH:mm:ss, or empty when unknown. */
  created: string
  charset: string
  collation: string
  autoIncrement: number | null
  dataLength: number | null
  indexLength: number | null
  rows: number | null
  comment: string
  indexCount: number
  foreignKeyCount: number
}

export interface TableOptions {
  engine?: string
  rowFormat?: string
  charset?: string
  collate?: string
  autoIncrement?: number
  comment?: string
}

export interface PropertiesForm {
  engine: string
  rowFormat: string
  charset: string
  collation: string
  /** Text so an empty field means "leave it". */
  autoIncrement: string
  comment: string
}

export interface Collation {
  charset: string
  collation: string
  isDefault: boolean
}

export type PropertiesIssue = 'engineRequired' | 'rowFormatRequired' | 'charsetRequired' | 'collationRequired' | 'collationMismatch' | 'autoIncrementInvalid'

export const ROW_FORMATS = ['DYNAMIC', 'COMPACT', 'REDUNDANT', 'COMPRESSED']

const numberOrNull = (value: unknown) => value === null || value === undefined || value === '' || !Number.isFinite(Number(value)) ? null : Number(value)

export function mapTableProperties(row: Record<string, unknown> | undefined): TableProperties | null {
  if (!row) return null
  return {
    name: String(row.table_name ?? ''), database: String(row.table_schema ?? ''), type: String(row.table_type ?? ''),
    engine: String(row.engine ?? ''), rowFormat: String(row.row_format ?? ''), created: String(row.create_time ?? '').replace('T', ' ').slice(0, 19),
    charset: String(row.charset ?? ''), collation: String(row.collation ?? ''), autoIncrement: numberOrNull(row.auto_increment),
    dataLength: numberOrNull(row.data_length), indexLength: numberOrNull(row.index_length), rows: numberOrNull(row.table_rows),
    comment: String(row.table_comment ?? ''), indexCount: Number(row.index_count) || 0, foreignKeyCount: Number(row.foreign_key_count) || 0,
  }
}

export function mapCollations(rows: Record<string, unknown>[]): Collation[] {
  return rows.map(row => ({ charset: String(row.charset ?? ''), collation: String(row.collation ?? ''), isDefault: ['Yes', 'YES', '1', 'true'].includes(String(row.is_default)) }))
    .filter(item => item.charset && item.collation)
}

/** The default collation of a character set, or its first one. */
export function defaultCollation(collations: Collation[], charset: string): string {
  const list = collations.filter(item => item.charset === charset)
  return (list.find(item => item.isDefault) || list[0])?.collation || ''
}

/** Bytes as KB or MB with two decimals at most, the way the prototype shows sizes. */
export function formatBytes(value: number | null, unit: 'auto' | 'MB' = 'auto'): string {
  if (value === null) return '-'
  if (unit === 'MB' || value >= 1024 * 1024) return `${Number((value / 1024 / 1024).toFixed(2))} MB`
  if (value >= 1024) return `${Number((value / 1024).toFixed(2))} KB`
  return `${value} B`
}

export function propertiesToForm(properties: TableProperties): PropertiesForm {
  return {
    engine: properties.engine, rowFormat: properties.rowFormat.toUpperCase(), charset: properties.charset, collation: properties.collation,
    autoIncrement: properties.autoIncrement === null ? '' : String(properties.autoIncrement), comment: properties.comment,
  }
}

/** Only the options that differ from the table; the server leaves the rest alone. */
export function changedOptions(form: PropertiesForm, properties: TableProperties): TableOptions {
  const options: TableOptions = {}
  if (form.engine && form.engine.toLowerCase() !== properties.engine.toLowerCase()) options.engine = form.engine
  if (form.rowFormat && form.rowFormat.toUpperCase() !== properties.rowFormat.toUpperCase()) options.rowFormat = form.rowFormat
  if (form.charset && form.charset !== properties.charset) options.charset = form.charset
  if (form.collation && form.collation !== properties.collation) options.collate = form.collation
  const next = form.autoIncrement.trim()
  if (next && Number(next) !== properties.autoIncrement) options.autoIncrement = Number(next)
  if (form.comment !== properties.comment) options.comment = form.comment
  return options
}

/** Every option of the table, for the "current table properties" statement. */
export function allOptions(properties: TableProperties): TableOptions {
  return {
    engine: properties.engine || undefined, rowFormat: properties.rowFormat ? properties.rowFormat.toUpperCase() : undefined,
    autoIncrement: properties.autoIncrement ?? undefined, charset: properties.charset || undefined, collate: properties.collation || undefined,
    comment: properties.comment,
  }
}

export function validatePropertiesForm(form: PropertiesForm, collations: Collation[]): Partial<Record<keyof PropertiesForm, PropertiesIssue>> {
  const issues: Partial<Record<keyof PropertiesForm, PropertiesIssue>> = {}
  if (!form.engine) issues.engine = 'engineRequired'
  if (!form.rowFormat) issues.rowFormat = 'rowFormatRequired'
  if (!form.charset) issues.charset = 'charsetRequired'
  if (!form.collation) issues.collation = 'collationRequired'
  else if (collations.length && !collations.some(item => item.charset === form.charset && item.collation === form.collation)) issues.collation = 'collationMismatch'
  const next = form.autoIncrement.trim()
  if (next && !/^[1-9]\d*$/.test(next)) issues.autoIncrement = 'autoIncrementInvalid'
  return issues
}
