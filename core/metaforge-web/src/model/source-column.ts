export interface SourceColumn extends Record<string, unknown> {
  name: string
  type: string
  nullable: string
  default: string | null
  comment: string
  position: number
  length: string
  autoincrement: string
  charset: string
  collate: string
}

export interface SourceColumnForm extends Record<string, unknown> {
  name: string
  type: string
  length: string | number
  nullable: boolean
  autoIncrement: boolean
  defaultMode: 'none' | 'constant' | 'timestamp'
  defaultValue: string
  comment: string
  charset: string
  collate: string
  createIndex: boolean
  indexType: string
  indexName: string
  compositeIndex: boolean
  compositeColumns: string[]
}

export function sourceColumnForm(): SourceColumnForm {
  return { name: '', type: 'VARCHAR', length: '', nullable: true, autoIncrement: false,
    defaultMode: 'none', defaultValue: '', comment: '', charset: '', collate: '',
    createIndex: false, indexType: 'normal', indexName: '', compositeIndex: false, compositeColumns: [] }
}

/** Keep unsupported type modifiers intact rather than silently dropping them in ALTER COLUMN. */
export function normalizeColumnType(value: unknown): string {
  const raw = String(value || '').toUpperCase().trim()
  const simple = raw.match(/^([A-Z]+)(?:\s*\([\d, ]+\))?$/)
  return simple ? simple[1] : raw
}

export function mapSourceColumns(rows: Record<string, any>[]): SourceColumn[] {
  return rows.filter(row => row.type_name === 'column').map(row => {
    const declaredLength = String(row.object_data_type || '').match(/\(([\d, ]+)\)/)?.[1]?.replace(/\s/g, '')
    return {
      name: String(row.object_name), type: normalizeColumnType(row.object_data_type),
      nullable: String(row.object_nullable || 'YES'),
      default: row.object_default_value == null ? null : String(row.object_default_value),
      comment: String(row.object_comment || ''), position: Number(row.object_position) || 0,
      length: declaredLength || (Number(row.object_length) > 0
          ? String(row.object_length) + (normalizeColumnType(row.object_data_type) === 'DECIMAL' && row.object_scale != null ? ',' + row.object_scale : '') : ''),
      autoincrement: String(row.object_autoincrement || 'NO'),
      charset: String(row.object_charset || ''), collate: String(row.object_collation || ''),
    }
  }).sort((left, right) => left.position - right.position)
}

export function sourceColumnPayload(form: SourceColumnForm): Record<string, unknown> {
  const parts = String(form.length).split(',').map(value => Number(value.trim()))
  return {
    name: form.name.trim(), type: form.type,
    length: String(form.length) && parts[0] > 0 && form.type !== 'DECIMAL' ? parts[0] : undefined,
    precision: form.type === 'DECIMAL' && parts[0] > 0 ? parts[0] : undefined,
    scale: form.type === 'DECIMAL' && parts[0] > 0 ? parts[1] || 0 : undefined,
    nullable: form.nullable, autoIncrement: form.autoIncrement,
    defaultValue: form.defaultMode === 'constant' ? form.defaultValue : form.defaultMode === 'timestamp' ? 'CURRENT_TIMESTAMP' : undefined,
    comment: form.comment || undefined, charset: form.charset || undefined, collate: form.collate || undefined,
  }
}

/**
 * Plans the column moves that turn `current` into `target`. Columns on the longest run already in
 * the right relative order stay put, so the fewest columns are altered; each move places a column
 * right after its target predecessor (or first), applied in target order.
 */
export function planColumnMoves(current: string[], target: string[]): { name: string; previous: string | null }[] {
  const positions = target.map(name => current.indexOf(name))
  const length = positions.map(() => 1)
  const parent = positions.map(() => -1)
  positions.forEach((position, index) => {
    for (let before = 0; before < index; before++) {
      if (positions[before] < position && length[before] + 1 > length[index]) { length[index] = length[before] + 1; parent[index] = before }
    }
  })
  const keep = new Set<number>()
  for (let index = length.indexOf(Math.max(...length)); index >= 0; index = parent[index]) keep.add(index)
  return target.flatMap((name, index) => keep.has(index) ? [] : [{ name, previous: index ? target[index - 1] : null }])
}
