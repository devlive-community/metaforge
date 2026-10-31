import { formatDateTime } from './source-table-drop'

/** A database as GET .../{database} describes it; sizes in bytes, counts over base tables. */
export interface DatabaseOverview {
  name: string
  charset: string
  collation: string
  created: string
  /** '' when no table has been changed since the server started tracking it. */
  updated: string
  tables: number
  columns: number
  indexes: number
  views: number
  procedures: number
  events: number
  triggers: number
  foreignKeys: number
  dataBytes: number
  indexBytes: number
  freeBytes: number
}

export interface DatabaseTable { name: string; engine: string; rows: number | null; dataBytes: number | null; indexBytes: number | null; updated: string }

export type SpaceKey = 'data' | 'index' | 'other'
export interface SpaceSegment { key: SpaceKey; bytes: number; percent: number }

const count = (value: unknown) => Number.isFinite(Number(value)) ? Number(value) : 0
const orNull = (value: unknown) => value === null || value === undefined || value === '' || !Number.isFinite(Number(value)) ? null : Number(value)

export function mapDatabaseOverview(row: any, name: string): DatabaseOverview {
  return {
    name: String(row?.object_name || name),
    charset: String(row?.object_charset || ''),
    collation: String(row?.object_collation || ''),
    created: formatDateTime(row?.object_create_time),
    updated: row?.object_update_time ? formatDateTime(row.object_update_time) : '',
    tables: count(row?.object_table_count),
    columns: count(row?.object_column_count),
    indexes: count(row?.object_index_count),
    views: count(row?.object_view_count),
    procedures: count(row?.object_procedure_count),
    events: count(row?.object_event_count),
    triggers: count(row?.object_trigger_count),
    foreignKeys: count(row?.object_foreign_key_count),
    dataBytes: count(row?.object_data_length),
    indexBytes: count(row?.object_index_length),
    freeBytes: count(row?.object_data_free),
  }
}

export function mapDatabaseTables(rows: any[]): DatabaseTable[] {
  return (rows || []).map(row => ({
    name: String(row?.object_name || ''),
    engine: String(row?.object_engine || '-'),
    rows: orNull(row?.object_rows),
    dataBytes: orNull(row?.object_data_length),
    indexBytes: orNull(row?.object_index_length),
    updated: formatDateTime(row?.object_update_time || row?.object_create_time),
  }))
}

/** Space used by data, indexes and allocated free space ("other"), with shares of their total. */
export function spaceSegments(overview: DatabaseOverview): SpaceSegment[] {
  const parts: [SpaceKey, number][] = [['data', overview.dataBytes], ['index', overview.indexBytes], ['other', overview.freeBytes]]
  const total = parts.reduce((sum, [, bytes]) => sum + bytes, 0)
  return parts.map(([key, bytes]) => ({ key, bytes, percent: total ? Math.round(bytes / total * 1000) / 10 : 0 }))
}

/**
 * Stroke dash settings drawing each non-empty segment as an arc of a ring with circumference
 * `circumference`, starting at 12 o'clock, separated by `gap` (in the same units).
 */
export function ringArcs(segments: SpaceSegment[], circumference: number, gap: number) {
  const total = segments.reduce((sum, segment) => sum + segment.bytes, 0)
  const visible = segments.filter(segment => segment.bytes > 0)
  const gaps = visible.length > 1 ? gap : 0
  let offset = 0
  return visible.map(segment => {
    const length = segment.bytes / total * circumference
    const arc = { key: segment.key, dash: `${Math.max(length - gaps, 0.5)} ${circumference}`, offset: -offset }
    offset += length
    return arc
  })
}
