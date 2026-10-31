import { computed, ref, watch } from 'vue'

export interface TableCreateIndexColumn {
  name: string
  primaryKey?: boolean
  unique?: boolean
}

export type TableCreateIndexTab = 'primary' | 'normal' | 'unique' | 'foreign' | 'check'
export interface TableCreateIndexRow {
  __key: string
  kind: 'primary' | 'normal' | 'unique'
  name: string
  columns: string[]
  method: string
  comment: string
}

/** Draft indexes share the columns defined in step two; edits are applied without a row-level save. */
export function useTableCreateIndexes(columns: () => TableCreateIndexColumn[], tableName: () => string) {
  const indexRows = ref<TableCreateIndexRow[]>([])
  const indexTab = ref<TableCreateIndexTab>('primary')
  let sequence = 0

  const columnOptions = computed(() => [...new Set(columns().map(column => column.name.trim()).filter(Boolean))]
      .map(name => ({ label: name, value: name })))
  const primaryRows = computed<TableCreateIndexRow[]>(() => {
    const names = columns().filter(column => column.primaryKey && column.name.trim()).map(column => column.name.trim())
    return names.length ? [{ __key: '__primary__', kind: 'primary', name: 'PRIMARY', columns: names, method: 'BTREE', comment: '' }] : []
  })
  const autoUniqueRows = computed<TableCreateIndexRow[]>(() => columns()
      .filter(column => column.unique && column.name.trim())
      .map(column => ({ __key: `__auto_${column.name.trim()}`, kind: 'unique', name: `uk_${column.name.trim()}`,
        columns: [column.name.trim()], method: 'BTREE', comment: '' })))

  const indexCounts = computed(() => ({
    primary: primaryRows.value.length,
    normal: indexRows.value.filter(row => row.kind === 'normal').length,
    unique: indexRows.value.filter(row => row.kind === 'unique').length + autoUniqueRows.value.length,
    foreign: 0,
    check: 0
  }))
  const visibleIndexRows = computed<TableCreateIndexRow[]>(() => {
    if (indexTab.value === 'primary') return primaryRows.value
    if (indexTab.value === 'unique') return [...autoUniqueRows.value, ...indexRows.value.filter(row => row.kind === 'unique')]
    if (indexTab.value === 'normal') return indexRows.value.filter(row => row.kind === 'normal')
    return []
  })
  const canAddIndex = computed(() => (indexTab.value === 'normal' || indexTab.value === 'unique') && columnOptions.value.length > 0)

  function onAddIndex() {
    if (!canAddIndex.value) return
    sequence += 1
    const unique = indexTab.value === 'unique'
    indexRows.value.push({ __key: `idx_${sequence}`, kind: unique ? 'unique' : 'normal',
      name: `${unique ? 'uk' : 'idx'}_${tableName().trim() || 'table'}_${sequence}`,
      columns: [], method: 'BTREE', comment: '' })
  }

  function removeIndex(row: TableCreateIndexRow) {
    if (indexRows.value.some(index => index.__key === row.__key)) {
      indexRows.value = indexRows.value.filter(index => index.__key !== row.__key)
    }
    else if (row.kind === 'primary') {
      columns().forEach(column => { column.primaryKey = false })
    }
    else if (row.kind === 'unique') {
      columns().filter(column => row.columns.includes(column.name.trim())).forEach(column => { column.unique = false })
    }
  }

  function firstInvalidIndex() {
    const available = new Set(columnOptions.value.map(option => option.value))
    return indexRows.value.find(row => !row.name.trim() || !row.columns.length || row.columns.some(name => !available.has(name)))
  }

  const indexDefinitions = computed(() => [...indexRows.value, ...autoUniqueRows.value].map(row => ({
    name: row.name.trim(), columns: [...row.columns], unique: row.kind === 'unique', comment: row.comment || undefined
  })))

  // A field rename follows the same field object; deleting a field removes its stale index references.
  watch(() => columns().map(column => ({ column, name: column.name.trim() })), (current, previous) => {
    const names = new Map(current.map(entry => [entry.column, entry.name]))
    const renamed = new Map(previous.map(entry => [entry.name, names.get(entry.column)]))
    const available = new Set(current.map(entry => entry.name).filter(Boolean))
    indexRows.value.forEach(row => {
      row.columns = [...new Set(row.columns.map(name => renamed.has(name) ? renamed.get(name) || '' : name)
          .filter(name => available.has(name)))]
    })
  })

  return { indexRows, indexTab, indexCounts, visibleIndexRows, canAddIndex, columnOptions,
    onAddIndex, removeIndex, firstInvalidIndex, indexDefinitions }
}
