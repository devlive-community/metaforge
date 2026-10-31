/** DDL kinds the server generates for a table (GET/POST .../ddl?kind=). */
export type DdlKind = 'create' | 'structure' | 'indexes' | 'constraints' | 'sample-data' | 'alter' | 'alter-table'

/** Entries of the DDL tab's quick menu; `compare` opens the structure compare page instead of a card. */
export type DdlAction = 'create' | 'structure' | 'indexes' | 'constraints' | 'sample-data' | 'alter' | 'compare'

/** One statement card: its title and the kinds it shows, one block each. */
export interface DdlCardSpec {
  action: Exclude<DdlAction, 'compare'>
  kinds: DdlKind[]
}

export const DDL_ACTIONS: DdlAction[] = ['create', 'structure', 'indexes', 'constraints', 'sample-data', 'alter', 'compare']

/** "Generate Alter" shows both templates: one MODIFY per column, and one statement for the whole table. */
export const DDL_CARDS: Record<Exclude<DdlAction, 'compare'>, DdlCardSpec> = {
  create: { action: 'create', kinds: ['create'] },
  structure: { action: 'structure', kinds: ['structure'] },
  indexes: { action: 'indexes', kinds: ['indexes'] },
  constraints: { action: 'constraints', kinds: ['constraints'] },
  'sample-data': { action: 'sample-data', kinds: ['sample-data'] },
  alter: { action: 'alter', kinds: ['alter', 'alter-table'] },
}

/** Cards shown when the tab opens, as in the prototype. */
export const DEFAULT_DDL_CARDS: Exclude<DdlAction, 'compare'>[] = ['create', 'structure']

export interface DdlHistoryEntry {
  id: number
  kind: DdlKind
  content: string
  username: string
  createTime: string
}

export function mapDdlHistory(row: any): DdlHistoryEntry {
  return {
    id: Number(row?.id),
    kind: String(row?.kind || 'create') as DdlKind,
    content: String(row?.content ?? ''),
    username: String(row?.username || '-'),
    createTime: String(row?.createTime || '-'),
  }
}

/** The quick-menu action a recorded kind belongs to. */
export const ddlActionOf = (kind: DdlKind): Exclude<DdlAction, 'compare'> => kind === 'alter-table' ? 'alter' : kind

/** Statements of a card joined for copying and downloading, skipping empty blocks. */
export const joinDdl = (parts: string[]) => parts.map(part => part.trim()).filter(Boolean).join('\n\n')

/** A file name such as `user-create.sql`, keeping only characters safe on every file system. */
export const ddlFileName = (table: string, action: string) => `${table}-${action}.sql`.replace(/[\\/:*?"<>|\s]+/g, '_')
