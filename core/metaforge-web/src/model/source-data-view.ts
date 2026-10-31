import type { DataField } from './source-data'

export type ColumnAlign = 'left' | 'center' | 'right'

export interface ColumnSetting {
  /** Column name, or INDEX_COLUMN for the row number column. */
  name: string
  visible: boolean
  width: number
  align: ColumnAlign
  fixed: boolean
}

export interface ColumnView {
  columns: ColumnSetting[]
  bordered: boolean
  stickyHeader: boolean
  autoWidth: boolean
}

export const INDEX_COLUMN = '__index__'

const defaultWidth = (field: DataField) =>
  field.kind === 'number' || field.kind === 'boolean' ? 100 : field.kind === 'date' || field.kind === 'datetime' ? 170 : field.kind === 'json' || field.kind === 'longtext' ? 200 : 160
const defaultAlign = (field: DataField): ColumnAlign => field.kind === 'text' || field.kind === 'json' || field.kind === 'longtext' ? 'left' : 'center'

export function defaultColumnView(fields: DataField[]): ColumnView {
  return {
    columns: [
      { name: INDEX_COLUMN, visible: true, width: 60, align: 'center', fixed: true },
      ...fields.map(field => ({ name: field.name, visible: true, width: defaultWidth(field), align: defaultAlign(field), fixed: false })),
    ],
    bordered: false,
    stickyHeader: true,
    autoWidth: false,
  }
}

/**
 * Restores a saved view against the current columns: unknown columns are dropped, new ones are
 * appended with defaults, and malformed values fall back to the defaults.
 */
export function readColumnView(raw: string | null, fields: DataField[]): ColumnView {
  const fallback = defaultColumnView(fields)
  let saved: Partial<ColumnView> | null = null
  try { saved = raw ? JSON.parse(raw) : null } catch { saved = null }
  if (!saved || !Array.isArray(saved.columns)) return fallback
  const defaults = new Map(fallback.columns.map(column => [column.name, column]))
  const columns: ColumnSetting[] = []
  for (const item of saved.columns) {
    const base = item && defaults.get(item.name)
    if (!base || columns.some(column => column.name === base.name)) continue
    columns.push({
      name: base.name,
      visible: typeof item.visible === 'boolean' ? item.visible : base.visible,
      width: Number.isFinite(item.width) && item.width >= 40 ? Math.min(800, Math.round(item.width)) : base.width,
      align: ['left', 'center', 'right'].includes(item.align) ? item.align : base.align,
      fixed: typeof item.fixed === 'boolean' ? item.fixed : base.fixed,
    })
  }
  columns.push(...fallback.columns.filter(column => !columns.some(item => item.name === column.name)))
  return {
    columns,
    bordered: typeof saved.bordered === 'boolean' ? saved.bordered : fallback.bordered,
    stickyHeader: typeof saved.stickyHeader === 'boolean' ? saved.stickyHeader : fallback.stickyHeader,
    autoWidth: typeof saved.autoWidth === 'boolean' ? saved.autoWidth : fallback.autoWidth,
  }
}

export const sameColumnView = (left: ColumnView, right: ColumnView) => JSON.stringify(left) === JSON.stringify(right)

export type SortDirection = 'asc' | 'desc'

export interface SortRule {
  name: string
  order: SortDirection
}

export const MAX_SORT_RULES = 5

/**
 * One-click sorts offered for this table: created and updated timestamps, then the primary key,
 * each newest/largest first and then ascending.
 */
export function quickSorts(fields: DataField[], primaryKeys: string[]): SortRule[] {
  const dated = fields.filter(field => field.kind === 'date' || field.kind === 'datetime')
  const created = dated.find(field => /creat/i.test(field.name))
  const updated = dated.find(field => /updat|modif/i.test(field.name))
  const key = fields.find(field => primaryKeys.includes(field.name)) || fields.find(field => field.name.toLowerCase() === 'id')
  return [created, updated, key].filter((field, index, list): field is DataField => !!field && list.indexOf(field) === index)
    .flatMap(field => [{ name: field.name, order: 'desc' as const }, { name: field.name, order: 'asc' as const }])
}

export const sameSort = (left: SortRule[], right: SortRule[]) =>
  left.length === right.length && left.every((rule, index) => rule.name === right[index].name && rule.order === right[index].order)

export const PAGE_SIZES = [10, 20, 50, 100]
export const EXPORT_LIMIT = 10000
export type ExportFormat = 'csv' | 'json'

/** Data table preferences shared by every table the viewer opens. */
export interface TableSettings {
  pageSize: number
  rememberPageSize: boolean
  striped: boolean
  rowBorders: boolean
  hoverable: boolean
  showActions: boolean
  exportFormat: ExportFormat
  exportWithFilters: boolean
}

export const DEFAULT_TABLE_SETTINGS: TableSettings = {
  pageSize: 10, rememberPageSize: false, striped: false, rowBorders: true, hoverable: true, showActions: true, exportFormat: 'csv', exportWithFilters: true,
}

export function readTableSettings(raw: string | null): TableSettings {
  let saved: Record<string, unknown> | null = null
  try { saved = raw ? JSON.parse(raw) : null } catch { saved = null }
  if (!saved || typeof saved !== 'object') return { ...DEFAULT_TABLE_SETTINGS }
  const flag = (key: keyof TableSettings) => typeof saved![key] === 'boolean' ? saved![key] as boolean : DEFAULT_TABLE_SETTINGS[key] as boolean
  return {
    pageSize: PAGE_SIZES.includes(Number(saved.pageSize)) ? Number(saved.pageSize) : DEFAULT_TABLE_SETTINGS.pageSize,
    rememberPageSize: flag('rememberPageSize'), striped: flag('striped'), rowBorders: flag('rowBorders'), hoverable: flag('hoverable'),
    showActions: flag('showActions'), exportWithFilters: flag('exportWithFilters'),
    exportFormat: saved.exportFormat === 'json' ? 'json' : 'csv',
  }
}

/** A per-table default sort is saved as one rule; anything else means no default. */
export function readDefaultSort(raw: string | null): SortRule | null {
  try {
    const rule = raw ? JSON.parse(raw) : null
    return rule && typeof rule.name === 'string' && (rule.order === 'asc' || rule.order === 'desc') ? { name: rule.name, order: rule.order } : null
  } catch { return null }
}
