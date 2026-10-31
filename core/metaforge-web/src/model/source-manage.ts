export interface SourceManageContext { source: string; database?: string; table?: string; tab?: string }
const text = (value: unknown) => typeof value === 'string' ? value : ''
export function sourceManageContext(route: { path?: string; params: Record<string, unknown>; query?: Record<string, unknown>; meta?: Record<string, unknown> }) {
  const query = route.query || {}
  if (route.path === '/manage' && query.type === 'source') return { source: text(query.id), database: text(query.database), table: text(query.table), tab: text(query.tab) || 'structure' }
  // The compare page shares the workbench context; older links named the source `source`.
  if (route.path === '/compare') return { source: text(query.id) || text(query.source), database: text(query.database), table: text(query.table), tab: 'compare' }
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

/** Pages showing the source workbench header: the workspace and the compare page of a source. */
export function isSourceWorkbenchRoute(route: { path: string; query: Record<string, unknown> }): boolean {
  if (route.path === '/compare') return !!sourceManageContext({ path: route.path, params: {}, query: route.query }).source
  return isSourceManagementRoute(route)
}

export function isSourceManagementRoute(route: { path: string; query: Record<string, unknown> }): boolean {
  return route.path === '/manage' && route.query.type === 'source' && typeof route.query.id === 'string' && !!route.query.id
}
