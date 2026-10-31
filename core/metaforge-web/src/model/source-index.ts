export type IndexMethod = 'BTREE' | 'HASH' | 'FULLTEXT' | 'SPATIAL'
export type IndexOrder = 'ASC' | 'DESC'

export const INDEX_METHODS: IndexMethod[] = ['BTREE', 'HASH', 'FULLTEXT', 'SPATIAL']

export interface IndexColumn {
  name: string
  order: IndexOrder | null
  /** Prefix length for string columns, null for the whole column. */
  subPart: number | null
}

export interface SourceIndex {
  name: string
  primary: boolean
  unique: boolean
  method: IndexMethod
  columns: IndexColumn[]
  cardinality: number | null
  /** Estimated bytes per key, as EXPLAIN reports key_len; null for FULLTEXT and SPATIAL. */
  keyLength: number | null
  comment: string
  usageCount: number | null
  usageRows: number | null
}

export interface IndexForm {
  name: string
  method: IndexMethod
  unique: boolean
  algorithm: '' | 'INPLACE' | 'COPY'
  columns: { name: string; order: IndexOrder; subPart: number | null }[]
  comment: string
}

export type IndexIssue = 'nameRequired' | 'nameInvalid' | 'nameTaken' | 'columnsRequired' | 'columnsRepeated'

const numberOrNull = (value: unknown) => value === null || value === undefined || value === '' || !Number.isFinite(Number(value)) ? null : Number(value)

/** Groups the per-column rows of index-details into indexes, PRIMARY first. */
export function groupIndexDetails(rows: Record<string, unknown>[]): SourceIndex[] {
  const indexes = new Map<string, SourceIndex>()
  for (const row of [...rows].sort((left, right) => Number(left.seq_in_index) - Number(right.seq_in_index))) {
    const name = String(row.index_name ?? '')
    if (!name) continue
    let index = indexes.get(name)
    if (!index) {
      const method = String(row.index_type || 'BTREE').toUpperCase()
      index = {
        name, primary: name === 'PRIMARY', unique: String(row.non_unique) === '0' || row.non_unique === false,
        method: (INDEX_METHODS.includes(method as IndexMethod) ? method : 'BTREE') as IndexMethod, columns: [],
        cardinality: null, keyLength: null, comment: String(row.index_comment ?? ''),
        usageCount: numberOrNull(row.usage_count), usageRows: numberOrNull(row.usage_rows),
      }
      indexes.set(name, index)
    }
    const collation = String(row.collation ?? '').toUpperCase()
    index.columns.push({ name: String(row.column_name ?? ''), order: collation === 'A' ? 'ASC' : collation === 'D' ? 'DESC' : null, subPart: numberOrNull(row.sub_part) })
    // The cardinality of the whole index is the one reported for its last column.
    index.cardinality = numberOrNull(row.cardinality)
    const length = numberOrNull(row.key_length)
    index.keyLength = length === null ? index.keyLength : (index.keyLength ?? 0) + length
  }
  return [...indexes.values()].sort((left, right) => Number(right.primary) - Number(left.primary))
}

export const columnsLabel = (index: SourceIndex) => index.columns.map(column => column.name).join(', ')
export const columnsWithOrder = (index: SourceIndex) => index.columns.map(column => column.order ? `${column.name} (${column.order})` : column.name).join(', ')

export function emptyIndexForm(): IndexForm {
  return { name: '', method: 'BTREE', unique: false, algorithm: '', columns: [{ name: '', order: 'ASC', subPart: null }], comment: '' }
}

export function indexToForm(index: SourceIndex): IndexForm {
  return {
    name: index.name, method: index.method, unique: index.unique || index.primary, algorithm: '', comment: index.comment,
    columns: index.columns.map(column => ({ name: column.name, order: column.order || 'ASC', subPart: column.subPart })),
  }
}

/** FULLTEXT and SPATIAL indexes are never unique and their columns take no order. */
export const orderedMethod = (method: IndexMethod) => method === 'BTREE' || method === 'HASH'

/** The definition the server builds statements from; `originalName` turns it into an edit. */
export function formToDefinition(form: IndexForm, originalName?: string) {
  const columns = form.columns.filter(column => column.name)
  return {
    name: form.name.trim(), type: form.method, unique: orderedMethod(form.method) && form.unique, algorithm: form.algorithm || undefined,
    comment: form.comment.trim() || undefined, columns: columns.map(column => column.name),
    orders: columns.map(column => orderedMethod(form.method) ? column.order : null), lengths: columns.map(column => column.subPart),
    ...(originalName ? { originalName } : {}),
  }
}

export function validateIndexForm(form: IndexForm, existing: string[], originalName?: string): Partial<Record<'name' | 'columns', IndexIssue>> {
  const issues: Partial<Record<'name' | 'columns', IndexIssue>> = {}
  const name = form.name.trim()
  if (!name) issues.name = 'nameRequired'
  else if (name.length > 64 || /[`\r\n\u0000]/.test(name)) issues.name = 'nameInvalid'
  else if (name.toLowerCase() !== (originalName || '').toLowerCase() && existing.some(item => item.toLowerCase() === name.toLowerCase())) issues.name = 'nameTaken'
  const columns = form.columns.map(column => column.name).filter(Boolean)
  if (!columns.length) issues.columns = 'columnsRequired'
  else if (new Set(columns).size !== columns.length) issues.columns = 'columnsRepeated'
  return issues
}
