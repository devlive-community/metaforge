export interface SourceManageContext { source: string; database?: string; table?: string; tab?: string }
const text = (value: unknown) => typeof value === 'string' ? value : ''
export function sourceManageContext(route: { path?: string; params: Record<string, unknown>; query?: Record<string, unknown>; meta?: Record<string, unknown> }) {
  const query = route.query || {}
  if (route.path === '/manage' && query.type === 'source') return { source: text(query.id), database: text(query.database), table: text(query.table), tab: text(query.tab) || 'structure' }
  // The compare page shares the workbench context; older links named the source `source`.
  if (route.path === '/compare') return { source: text(query.id) || text(query.source), database: text(query.database), table: text(query.table), tab: 'compare' }
  if (route.path === '/er' || route.path === '/import') return { source: text(query.id), database: text(query.database), table: '', tab: route.path.slice(1) }
  return { source: text(route.params.source), database: text(route.params.database), table: text(route.params.table), tab: text(route.meta?.type) || 'structure' }
}
export function sourceManageLocation(context: SourceManageContext, extra: Record<string, string | null | undefined | (string | null)[]> = {}) {
  const query: Record<string, string | null | undefined | (string | null)[]> = { ...extra, type: 'source', id: context.source }
  if (context.database) query.database = context.database
  if (context.table) query.table = context.table
  if (context.tab) query.tab = context.tab
  return { path: '/manage', query }
}

export const sourceTabCapability = (tab: string) => 'tab:' + (tab === 'erDiagram' ? 'er-diagram' : tab)

export type CompareMode = 'structure' | 'data'

/** The compare page for a source's database, optionally a source table and a mode. */
export function compareLocation(context: { source: string; database?: string; table?: string; mode?: CompareMode }) {
  const query: Record<string, string> = { id: context.source }
  if (context.database) query.database = context.database
  if (context.table) query.table = context.table
  if (context.mode) query.mode = context.mode
  return { path: '/compare', query }
}

/** A workbench page of a source's database: the ER diagram or the import. */
function databasePage(path: '/er' | '/import', context: { source: string; database?: string }) {
  const query: Record<string, string> = { id: context.source }
  if (context.database) query.database = context.database
  return { path, query }
}
export const erLocation = (context: { source: string; database?: string }) => databasePage('/er', context)
export const importLocation = (context: { source: string; database?: string }) => databasePage('/import', context)

/** Pages showing the source workbench header: the workspace, and the compare and ER pages of a source. */
export function isSourceWorkbenchRoute(route: { path: string; query: Record<string, unknown> }): boolean {
  if (route.path === '/compare' || route.path === '/er' || route.path === '/import') return !!sourceManageContext({ path: route.path, params: {}, query: route.query }).source
  return isSourceManagementRoute(route)
}

export function isSourceManagementRoute(route: { path: string; query: Record<string, unknown> }): boolean {
  return route.path === '/manage' && route.query.type === 'source' && typeof route.query.id === 'string' && !!route.query.id
}

/**
 * Capabilities a workbench page needs before it opens for a source, matching what its server
 * calls require. The console opens for any source whose plugin declares a capability; a source
 * declaring none has no management at all.
 */
export const WORKBENCH_PAGE_CAPABILITIES: Record<string, readonly string[]> = {
  '/manage': [],
  '/import': ['data:insert', 'table:list', 'column:read'],
  '/export': ['table:export'],
  '/compare': ['table:list', 'column:read'],
  '/er': ['database:info', 'tab:er-diagram'],
}
export const canManageSource = (capabilities: Iterable<string>) => new Set(capabilities).size > 0
export function canOpenWorkbenchPage(path: string, capabilities: Iterable<string>): boolean {
  const owned = new Set(capabilities)
  return owned.size > 0 && (WORKBENCH_PAGE_CAPABILITIES[path] || []).every(capability => owned.has(capability))
}
/** Inside a managed source's workspace: whether `can` grants what a workbench page needs. */
export const pageCapabilitiesMet = (path: string, can: (capability: string) => boolean) => (WORKBENCH_PAGE_CAPABILITIES[path] || []).every(can)
/** Compare modes and export formats a source can use: data needs its rows, sync SQL its statements. */
export const compareModeCapabilities = (mode: CompareMode) => mode === 'data' ? ['table:read'] : []
export const compareExportCapabilities = (kind: CompareMode, format: 'sql' | 'csv') => kind === 'data' ? ['table:read'] : format === 'sql' ? ['table:statement'] : []
