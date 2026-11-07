/** One table of the database as the export lists it. */
export interface ExportObject { name: string; type: 'table' | 'view'; rows: number | null; bytes: number | null; comment: string }
export interface ExportColumn { name: string; type: string }
export interface ExportFilter { column: string; operator: string; value: string }
export interface TableSetting { fields: string[]; filters: ExportFilter[]; scope: 'all' | 'filtered' }
export interface FormatChoice { value: string; labels: Record<string, string> }
export interface FormatOption { name: string; labels: Record<string, string>; defaultValue: string; choices: FormatChoice[] }
export interface ExportFormat { name: string; version: string; extension: string; merge: boolean; options: FormatOption[] }

export interface ExportState {
  objects: 'tables' | 'database'
  selected: string[]
  content: 'all' | 'data'
  structure: { create: boolean; indexes: boolean; drop: boolean }
  settings: Record<string, TableSetting>
  format: string
  options: Record<string, string>
  fileName: string
  organize: 'zip' | 'merge'
  compress: boolean
  batchSize: number
  concurrency: number
  reading: 'batch' | 'single'
  failure: 'stop' | 'skip'
  autoDownload: boolean
  keepRecord: boolean
}

export const EXPORT_STEPS = ['objects', 'content', 'format', 'output', 'preview'] as const
export const OPERATORS = ['=', '!=', '>', '>=', '<', '<=', 'LIKE', 'NOT LIKE', 'IS NULL', 'IS NOT NULL'] as const
export const needsValue = (operator: string) => !operator.startsWith('IS ')

const pad = (value: number) => String(value).padStart(2, '0')
/** database_export_yyyyMMdd */
export const defaultFileName = (database: string, today = new Date()) => `${ database || 'export' }_export_${ today.getFullYear() }${ pad(today.getMonth() + 1) }${ pad(today.getDate()) }`

export function defaultExportState(database: string): ExportState {
  return {
    objects: 'tables', selected: [], content: 'all', structure: { create: true, indexes: true, drop: false }, settings: {},
    format: '', options: {}, fileName: defaultFileName(database), organize: 'zip', compress: true, batchSize: 1000, concurrency: 1,
    reading: 'batch', failure: 'stop', autoDownload: true, keepRecord: true,
  }
}

/** The tables the export writes: every listed object for a whole-database export, the checked ones otherwise. */
export const exportedTables = (state: ExportState, objects: ExportObject[]) => state.objects === 'database' ? objects.map(object => object.name) : state.selected

export const setting = (state: ExportState, table: string): TableSetting => state.settings[table] || { fields: [], filters: [], scope: 'all' }

/** A table counts as configured once its fields or rows differ from "every field, every row". */
export const configured = (value: TableSetting | undefined) => !!value && (value.fields.length > 0 || (value.scope === 'filtered' && value.filters.length > 0))

/** The format's declared options with their defaults, keeping values already chosen. */
export function formatOptions(format: ExportFormat | undefined, current: Record<string, string>): Record<string, string> {
  const values: Record<string, string> = {}
  for (const option of format?.options || []) values[option.name] = current[option.name] ?? option.defaultValue
  return values
}

/** The plan the server checks and runs. Fields equal to every column are sent as "all" (empty). */
export function exportPlan(state: ExportState, objects: ExportObject[], columns: Record<string, ExportColumn[]>) {
  return {
    tables: exportedTables(state, objects).map(name => {
      const value = setting(state, name)
      const all = columns[name]?.length ?? -1
      return {
        name,
        fields: value.fields.length === all ? [] : [...value.fields],
        filters: value.scope === 'filtered' ? value.filters.filter(filter => filter.column && (!needsValue(filter.operator) || filter.value !== '')) : [],
      }
    }),
    content: state.content,
    structure: { ...state.structure },
    format: state.format,
    options: { ...state.options },
    fileName: state.fileName,
    organize: state.organize,
    compress: state.compress,
    batchSize: state.batchSize,
    concurrency: state.concurrency,
    reading: state.reading,
    failure: state.failure,
    keepRecord: state.keepRecord,
  }
}

/** The download: one file as written when it is the only one and not compressed, a ZIP otherwise (as the server names it). */
export function outputName(state: ExportState, tables: string[], extension: string) {
  const files = tables.length + (state.content === 'all' && (state.structure.create || state.structure.drop) ? 1 : 0)
  const base = (state.fileName || '').trim().replace(/[\\/:*?"<>|\s]+/g, '_') || 'export'
  return files === 1 && !state.compress ? `${ tables[0] }.${ extension }` : `${ base }.zip`
}

/** A label of a plugin option or choice in the page's language. */
export const label = (labels: Record<string, string>, locale: string) => labels[locale.startsWith('zh') ? 'zh' : 'en'] || Object.values(labels)[0] || ''

export function formatBytes(bytes: number | null) {
  if (bytes === null || bytes === undefined) return '-'
  return `${ (bytes / 1024 / 1024).toFixed(2) } MB`
}
