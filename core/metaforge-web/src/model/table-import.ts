/** A table the import file holds, as GET .../analyze describes it. */
export interface ImportSourceColumn { name: string; type: string; sample: string[] }
export interface ImportSourceTable { name: string; rows: number; hasDefinition: boolean; columns: ImportSourceColumn[] }
export interface ImportAnalysis {
  importId: string
  file: { name: string; size: number; extension: string; parser: string; encoding?: string; dialect?: string; skippedStatements?: number }
  tables: ImportSourceTable[]
  anomalies: string[]
}
export interface TargetColumn { name: string; type: string }

export type ImportAction = 'existing' | 'create' | 'skip'
export type Handling = 'keep' | 'direct' | 'date' | 'ignore'
export interface FieldMapping { source: string; target: string; handling: Handling }
export interface TablePlan { source: string; selected: boolean; action: ImportAction; target: string; mappings: FieldMapping[]; writeMode: string; conflict: string }
export interface ImportSettings {
  writeMode: 'append' | 'truncate' | 'structure'
  conflict: 'skip' | 'upsert' | 'error'
  batchSize: number
  parallel: number
  timeFormat: string
  nullMode: 'keep' | 'emptyToNull' | 'nullToEmpty'
  checkBefore: boolean
  transactional: boolean
  logSkipped: boolean
  continueOnError: boolean
}

export const defaultImportSettings = (): ImportSettings => ({
  writeMode: 'append', conflict: 'skip', batchSize: 1000, parallel: 1, timeFormat: 'yyyy-MM-dd HH:mm:ss', nullMode: 'keep',
  checkBefore: true, transactional: true, logSkipped: true, continueOnError: false,
})

/**
 * The target each source table goes to: an existing table of the same name (or one ending in
 * `_<name>`, as prefixed copies do), otherwise a new table named after the source.
 */
export function matchTables(sources: ImportSourceTable[], targets: string[]): TablePlan[] {
  const lower = targets.map(name => name.toLowerCase())
  return sources.map(source => {
    const name = source.name.toLowerCase()
    const exact = lower.indexOf(name)
    const prefixed = exact >= 0 ? exact : lower.findIndex(target => target.endsWith('_' + name))
    const plan: TablePlan = { source: source.name, selected: true, action: 'existing', target: '', mappings: [], writeMode: '', conflict: '' }
    if (prefixed >= 0) plan.target = targets[prefixed]
    else {
      plan.action = 'create'
      plan.target = source.name
    }
    return plan
  })
}

/** Fields mapped by name (case aside); unmatched source fields are left out. Time columns are parsed as dates. */
export function mapFields(source: ImportSourceTable, targets: TargetColumn[]): FieldMapping[] {
  return source.columns.map(column => {
    const target = targets.find(item => item.name.toLowerCase() === column.name.toLowerCase())
    if (!target) return { source: column.name, target: '', handling: 'ignore' as Handling }
    const time = /^(DATE|DATETIME|TIMESTAMP)/i.test(target.type) && !/^(DATE|DATETIME|TIMESTAMP)/i.test(column.type)
    return { source: column.name, target: target.name, handling: (time ? 'date' : 'direct') as Handling }
  })
}

/** The request for preview and execute; unselected tables are skipped. */
export function importPlan(importId: string, tables: TablePlan[], settings: ImportSettings) {
  return {
    importId,
    settings,
    tables: tables.map(table => ({
      source: table.source,
      action: table.selected ? table.action : 'skip',
      target: table.target,
      mappings: table.mappings.map(mapping => ({ ...mapping, target: mapping.handling === 'ignore' ? '' : mapping.target })),
      writeMode: table.writeMode || null,
      conflict: table.conflict || null,
    })),
  }
}

/** Mapped and needing attention (unmapped or flagged by the server) counts of one table. */
export function mappingCounts(table: TablePlan, flagged: Set<string>) {
  const mapped = table.mappings.filter(mapping => mapping.target && mapping.handling !== 'ignore').length
  const attention = table.mappings.filter(mapping => !mapping.target || mapping.handling === 'ignore' || flagged.has(`${ mapping.source } → ${ mapping.target }`)).length
  return { mapped, total: table.mappings.length, attention }
}

export const formatSize = (bytes: number) => bytes >= 1024 * 1024 ? `${ (bytes / 1024 / 1024).toFixed(1) } MB` : bytes >= 1024 ? `${ (bytes / 1024).toFixed(1) } KB` : `${ bytes } B`
