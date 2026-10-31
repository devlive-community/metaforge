export type DependencyType = 'index' | 'foreign_key' | 'trigger' | 'view' | 'other'

export const DEPENDENCY_TYPES: DependencyType[] = ['index', 'foreign_key', 'trigger', 'view', 'other']

export interface ColumnDependency {
  type: DependencyType
  name: string
  /** Index kind (PRIMARY, UNIQUE, FULLTEXT, NORMAL), foreign key direction (OUTGOING, INCOMING), trigger timing, or GENERATED. */
  kind: string
  columns: string
  schema: string
  table: string
  detail: string
}

/** Rows from the column-dependencies endpoint; unknown kinds are dropped. */
export function mapDependencies(rows: Record<string, unknown>[]): ColumnDependency[] {
  return rows.map(row => ({
    type: String(row.object_type || '') as DependencyType,
    name: String(row.object_name ?? ''),
    kind: String(row.object_kind ?? ''),
    columns: String(row.object_columns ?? ''),
    schema: String(row.object_schema ?? ''),
    table: String(row.object_table ?? ''),
    detail: String(row.object_detail ?? ''),
  })).filter(dependency => DEPENDENCY_TYPES.includes(dependency.type))
}

export const countByType = (dependencies: ColumnDependency[]) =>
  Object.fromEntries(DEPENDENCY_TYPES.map(type => [type, dependencies.filter(dependency => dependency.type === type).length])) as Record<DependencyType, number>
