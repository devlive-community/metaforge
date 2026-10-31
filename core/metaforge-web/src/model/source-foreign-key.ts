export type ForeignKeyRule = 'RESTRICT' | 'CASCADE' | 'SET NULL' | 'NO ACTION'

export const FOREIGN_KEY_RULES: ForeignKeyRule[] = ['RESTRICT', 'CASCADE', 'SET NULL', 'NO ACTION']

export interface SourceForeignKey {
  name: string
  columns: string[]
  referencedDatabase: string
  referencedTable: string
  referencedColumns: string[]
  onUpdate: string
  onDelete: string
  /** False when the referenced table no longer exists. */
  valid: boolean
}

export interface ForeignKeyForm {
  name: string
  /** Pairs of a column of this table and the referenced column it points to. */
  pairs: { column: string; referenced: string }[]
  referencedDatabase: string
  referencedTable: string
  onUpdate: ForeignKeyRule
  onDelete: ForeignKeyRule
}

export type ForeignKeyIssue = 'nameRequired' | 'nameInvalid' | 'nameTaken' | 'tableRequired' | 'columnsRequired' | 'columnsIncomplete' | 'columnsRepeated'

/** Groups the per-column rows of foreign-key-details into foreign keys. */
export function groupForeignKeys(rows: Record<string, unknown>[]): SourceForeignKey[] {
  const keys = new Map<string, SourceForeignKey>()
  for (const row of [...rows].sort((left, right) => Number(left.ordinal_position) - Number(right.ordinal_position))) {
    const name = String(row.constraint_name ?? '')
    if (!name) continue
    let key = keys.get(name)
    if (!key) {
      key = {
        name, columns: [], referencedDatabase: String(row.referenced_database ?? ''), referencedTable: String(row.referenced_table ?? ''), referencedColumns: [],
        onUpdate: String(row.update_rule ?? ''), onDelete: String(row.delete_rule ?? ''), valid: String(row.referenced_exists) !== '0',
      }
      keys.set(name, key)
    }
    key.columns.push(String(row.column_name ?? ''))
    key.referencedColumns.push(String(row.referenced_column ?? ''))
  }
  return [...keys.values()].sort((left, right) => left.name.localeCompare(right.name))
}

const asRule = (value: string): ForeignKeyRule => (FOREIGN_KEY_RULES.includes(value as ForeignKeyRule) ? value : 'RESTRICT') as ForeignKeyRule

export function emptyForeignKeyForm(database: string): ForeignKeyForm {
  return { name: '', pairs: [{ column: '', referenced: '' }], referencedDatabase: database, referencedTable: '', onUpdate: 'RESTRICT', onDelete: 'RESTRICT' }
}

export function foreignKeyToForm(key: SourceForeignKey): ForeignKeyForm {
  return {
    name: key.name, pairs: key.columns.map((column, index) => ({ column, referenced: key.referencedColumns[index] || '' })),
    referencedDatabase: key.referencedDatabase, referencedTable: key.referencedTable, onUpdate: asRule(key.onUpdate), onDelete: asRule(key.onDelete),
  }
}

/** The definition the server builds statements from; `originalName` turns it into an edit. */
export function formToForeignKey(form: ForeignKeyForm, originalName?: string) {
  const pairs = form.pairs.filter(pair => pair.column && pair.referenced)
  return {
    name: form.name.trim(), columns: pairs.map(pair => pair.column), referencedDatabase: form.referencedDatabase || undefined,
    referencedTable: form.referencedTable, referencedColumns: pairs.map(pair => pair.referenced), onUpdate: form.onUpdate, onDelete: form.onDelete,
    ...(originalName ? { originalName } : {}),
  }
}

export function validateForeignKeyForm(form: ForeignKeyForm, existing: string[], originalName?: string): Partial<Record<'name' | 'table' | 'columns', ForeignKeyIssue>> {
  const issues: Partial<Record<'name' | 'table' | 'columns', ForeignKeyIssue>> = {}
  const name = form.name.trim()
  if (!name) issues.name = 'nameRequired'
  else if (name.length > 64 || /[`\r\n\u0000]/.test(name)) issues.name = 'nameInvalid'
  else if (name.toLowerCase() !== (originalName || '').toLowerCase() && existing.some(item => item.toLowerCase() === name.toLowerCase())) issues.name = 'nameTaken'
  if (!form.referencedTable) issues.table = 'tableRequired'
  const filled = form.pairs.filter(pair => pair.column || pair.referenced)
  if (!filled.length) issues.columns = 'columnsRequired'
  else if (filled.some(pair => !pair.column || !pair.referenced)) issues.columns = 'columnsIncomplete'
  else if (new Set(filled.map(pair => pair.column)).size !== filled.length) issues.columns = 'columnsRepeated'
  return issues
}
