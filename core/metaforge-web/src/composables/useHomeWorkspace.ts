import { computed, onScopeDispose, ref, shallowRef } from 'vue'
import UserService from '@/services/user'
import DatasetService from '@/services/dataset'
import DashboardService from '@/services/dashboard'
import { FilterModel } from '@/model/filter'
import type { ResponseModel } from '@/model/response'

export interface HomeResource {
  code: string
  name?: string
  updateTime?: string
  createTime?: string
  state?: string[]
  configure?: string
  description?: string
}
interface CountState { value: number | null; loading: boolean; failed: boolean }
interface ResourceState { items: HomeResource[]; total: number | null; loading: boolean; failed: boolean }
export type HomeResourceKind = 'datasets' | 'visualizations'

/** Independent requests keep a failed counter from hiding the rest of the workspace. */
export function useHomeWorkspace() {
  const sources = ref<CountState>({ value: null, loading: true, failed: false })
  const queries = ref<CountState>({ value: null, loading: true, failed: false })
  const datasets = shallowRef<ResourceState>({ items: [], total: null, loading: true, failed: false })
  const visualizations = shallowRef<ResourceState>({ items: [], total: null, loading: true, failed: false })
  const requests = { sources: 0, queries: 0, datasets: 0, visualizations: 0 }
  const loading = computed(() => sources.value.loading || queries.value.loading || datasets.value.loading || visualizations.value.loading)
  const validCount = (value: unknown): value is number => typeof value === 'number' && Number.isSafeInteger(value) && value >= 0
  const counters = { sources, queries }, resources = { datasets, visualizations }

  async function loadCount(key: keyof typeof counters, fetch: () => Promise<ResponseModel>) {
    const request = ++requests[key], target = counters[key]
    target.value = { ...target.value, loading: true, failed: false }
    try {
      const response = await fetch()
      if (request !== requests[key]) return
      if (!response.status || !validCount(response.data)) throw Error('Invalid count')
      target.value = { value: response.data, loading: false, failed: false }
    } catch {
      if (request === requests[key]) target.value = { value: null, loading: false, failed: true }
    }
  }
  async function loadResources(key: HomeResourceKind) {
    const request = ++requests[key], target = resources[key]
    target.value = { ...target.value, loading: true, failed: false }
    const filter = { ...new FilterModel(), size: 4, orders: [{ column: 'updateTime', order: 'DESC' }] }
    try {
      const response = await (key === 'datasets' ? DatasetService : DashboardService).getAll(filter)
      if (request !== requests[key]) return
      if (!response.status || !Array.isArray(response.data?.content) || !validCount(response.data.total)) throw Error('Invalid resources')
      const items = response.data.content
      if (!items.every((item: unknown): item is HomeResource => !!item && typeof item === 'object'
        && typeof (item as HomeResource).code === 'string' && !!(item as HomeResource).code)) throw Error('Invalid resource identity')
      target.value = { items, total: response.data.total, loading: false, failed: false }
    } catch {
      if (request === requests[key]) target.value = { items: [], total: null, loading: false, failed: true }
    }
  }
  function refresh() {
    return Promise.allSettled([
      loadCount('sources', () => UserService.getSourceCount()), loadCount('queries', () => UserService.getQueryCount()),
      loadResources('datasets'), loadResources('visualizations'),
    ])
  }
  onScopeDispose(() => { for (const key of Object.keys(requests) as (keyof typeof requests)[]) requests[key]++ })
  void refresh()
  return { sources, queries, datasets, visualizations, loading, refresh, loadResources }
}
