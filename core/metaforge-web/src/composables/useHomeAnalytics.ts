import { onScopeDispose, ref, shallowRef, watch } from 'vue'
import UserService from '@/services/user'
import AuditService from '@/services/audit'
import { FilterModel } from '@/model/filter'

export interface HomeActivity { days: number; total: number; trend: { date: string; count: number }[]; sources: { name: string; count: number }[] }
export interface HomeQuery { code: string; content?: string; state?: string; elapsed?: number; source?: { name?: string; type?: string } }
export function useHomeAnalytics() {
  const period = ref(7), activity = shallowRef<HomeActivity>(), activityLoading = ref(true), activityFailed = ref(false)
  const queries = shallowRef<HomeQuery[]>([]), queriesLoading = ref(true), queriesFailed = ref(false)
  let activityRequest = 0, queryRequest = 0
  async function loadActivity() {
    const request = ++activityRequest, days = period.value
    activityLoading.value = true; activityFailed.value = false
    try {
      const response = await UserService.getHomeActivity(days)
      if (request !== activityRequest || days !== period.value) return
      const value = response.data
      if (!response.status || value?.days !== days || !Array.isArray(value?.trend) || value.trend.length !== days || !Array.isArray(value?.sources)
          || !Number.isSafeInteger(value.total) || value.total < 0
          || !value.trend.every(point => typeof point.date === 'string' && Number.isSafeInteger(point.count) && point.count >= 0)
          || !value.sources.every(source => typeof source.name === 'string' && Number.isSafeInteger(source.count) && source.count >= 0)) throw Error('Invalid query activity')
      activity.value = value
    } catch { if (request === activityRequest) { activity.value = undefined; activityFailed.value = true } }
    finally { if (request === activityRequest) activityLoading.value = false }
  }
  async function loadQueries() {
    const request = ++queryRequest
    queriesLoading.value = true; queriesFailed.value = false
    try {
      const response = await AuditService.getAll({ ...new FilterModel(), size: 4, orders: [{ column: 'createTime', order: 'DESC' }] })
      if (request !== queryRequest) return
      if (!response.status || !Array.isArray(response.data?.content)) throw Error('Invalid recent queries')
      queries.value = response.data.content
    } catch { if (request === queryRequest) { queries.value = []; queriesFailed.value = true } }
    finally { if (request === queryRequest) queriesLoading.value = false }
  }
  watch(period, loadActivity, { immediate: true })
  void loadQueries()
  onScopeDispose(() => { activityRequest++; queryRequest++ })
  return { period, activity, activityLoading, activityFailed, queries, queriesLoading, queriesFailed,
    loadActivity, loadQueries, refresh: () => Promise.allSettled([loadActivity(), loadQueries()]) }
}
