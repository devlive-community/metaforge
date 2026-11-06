import { computed, inject, onScopeDispose, shallowRef, watch, type InjectionKey, type Ref } from 'vue'
import ReportService from '@/services/report'
import DashboardService from '@/services/dashboard'
import { dashboardSnapshot, type DashboardBoard, type DashboardRefresh } from '@/model/dashboard-layout'
import type { QueryChartRow } from '@/model/query-chart'
import type { ReportModel } from '@/model/report'
import type { ResponseModel } from '@/model/response'

/**
 * The screen a board belongs to. Pages showing a saved screen provide it so charts refresh through the
 * screen, which lets users it is shared with refresh too; without it charts refresh as their owner.
 */
export const dashboardScreenKey: InjectionKey<Ref<string | undefined>> = Symbol('dashboardScreen')

/** A page that is not signed in (a public link) refreshes charts through its own endpoint. */
export const dashboardRefreshKey: InjectionKey<(report: string) => Promise<ResponseModel>> = Symbol('dashboardRefresh')

export function useDashboardReports(board: Ref<DashboardBoard>, reports: Ref<ReportModel[]>, live: Ref<boolean>) {
  const screen = inject(dashboardScreenKey, undefined)
  const refresher = inject(dashboardRefreshKey, undefined)
  const data = shallowRef<Record<string, QueryChartRow[]>>({})
  const failed = shallowRef<ReadonlySet<string>>(new Set())
  const pending = shallowRef<ReadonlySet<string>>(new Set())
  const refreshedAt = shallowRef<number>()
  let generation = 0
  let timer: number | undefined

  const policies = computed(() => {
    const result = new Map<string, number>()
    if (!live.value) return result
    const assets = new Map(reports.value.map(report => [report.code, report]))
    for (const widget of board.value.widgets) {
      const code = widget.reportCode
      const report = assets.get(code)
      if (!code || !report?.realtime) continue
      const snapshot = dashboardSnapshot(report)
      if (!snapshot) continue
      const refreshConfig = widget.refresh === 'saved' ? board.value : widget
      const policy: DashboardRefresh = refreshConfig.refresh
      const seconds = policy === '5m' ? 300 : policy === 'custom' ? refreshConfig.refreshSeconds ?? 300
        : policy === 'off' ? 0 : snapshot.queryChart.options.refreshSeconds ?? 0
      const existing = result.get(code)
      result.set(code, seconds > 0 && existing ? Math.min(seconds, existing) : seconds || existing || 0)
    }
    return result
  })

  async function refresh(codes = [...policies.value.keys()]) {
    const run = generation
    await Promise.all(codes.map(async code => {
      if (pending.value.has(code)) return
      pending.value = new Set([...pending.value, code])
      try {
        const response = refresher ? await refresher(code) : screen?.value ? await DashboardService.refreshChart(screen.value, code) : await ReportService.refreshChart(code)
        if (run !== generation) return
        if (!response.status || !Array.isArray(response.data)) throw new Error('Invalid chart refresh response')
        data.value = { ...data.value, [code]: response.data }
        refreshedAt.value = Date.now()
        failed.value = new Set([...failed.value].filter(item => item !== code))
      }
      catch {
        if (run === generation) failed.value = new Set([...failed.value, code])
      }
      finally {
        if (run === generation) pending.value = new Set([...pending.value].filter(item => item !== code))
      }
    }))
  }

  const schedule = computed(() => JSON.stringify([...policies.value]))
  watch(schedule, () => {
    generation++
    window.clearInterval(timer)
    pending.value = new Set()
    const active = policies.value
    if (!active.size) refreshedAt.value = undefined
    data.value = Object.fromEntries(Object.entries(data.value).filter(([code]) => active.has(code)))
    failed.value = new Set([...failed.value].filter(code => active.has(code)))
    const last = new Map<string, number>()
    const automatic = [...active].filter(([, seconds]) => seconds > 0)
    const initial = [...active.keys()].filter(code => !Object.prototype.hasOwnProperty.call(data.value, code))
    void refresh(initial)
    if (automatic.length) {
      const start = Date.now()
      for (const [code] of automatic) last.set(code, start)
      timer = window.setInterval(() => {
        if (document.hidden) return
        const now = Date.now()
        const due = automatic.filter(([code, seconds]) => now - (last.get(code) ?? 0) >= seconds * 1000)
        for (const [code] of due) last.set(code, now)
        void refresh(due.map(([code]) => code))
      }, 1000)
    }
  }, { immediate: true })

  onScopeDispose(() => {
    generation++
    window.clearInterval(timer)
  })

  return { data, failed, pending, policies, refreshedAt, refresh }
}
