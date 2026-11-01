import type { RouteLocationNormalized, RouteLocationRaw } from 'vue-router'
import { canManageSource, canOpenWorkbenchPage, compareLocation, compareModeCapabilities, sourceManageLocation, WORKBENCH_PAGE_CAPABILITIES } from '@/model/source-manage'
import { useSourceCapabilitiesStore } from '@/stores/source-capabilities'

const text = (value: unknown) => typeof value === 'string' ? value : ''

/**
 * Opens a workbench page of a source only when its plugin declares what the page needs: a page
 * the source can not use falls back to its console, and a source without management to the
 * source list. A page without a source, or whose capabilities failed to load, opens and shows
 * its own empty or error state. It runs on every navigation, so switching the source of an open
 * page is checked too.
 */
export async function workbenchGuard(to: RouteLocationNormalized): Promise<true | RouteLocationRaw> {
  if (!(to.path in WORKBENCH_PAGE_CAPABILITIES) || (to.path === '/manage' && to.query.type !== 'source')) return true
  const source = text(to.query.id) || text(to.query.source)
  if (!source) return true
  const store = useSourceCapabilitiesStore()
  await store.load(source)
  const profile = store.entries[source]?.profile
  if (!profile) return true
  const database = text(to.query.database)
  if (!canOpenWorkbenchPage(to.path, profile.capabilities)) {
    return to.path !== '/manage' && canManageSource(profile.capabilities) ? sourceManageLocation({ source, database }) : '/source'
  }
  if (to.path === '/compare' && to.query.mode === 'data' && !compareModeCapabilities('data').every(capability => profile.capabilities.includes(capability))) {
    return compareLocation({ source, database, table: text(to.query.table), mode: 'structure' })
  }
  return true
}
