import type { InjectionKey } from 'vue'
import type { DashboardWidget } from '@/model/dashboard-layout'
import type { DashboardFilterValue } from '@/model/dashboard-filters'

export const dashboardFiltersContext: InjectionKey<{
  read: (widget: DashboardWidget) => DashboardFilterValue | undefined
  write: (widget: DashboardWidget, filter: DashboardFilterValue) => void
  active: (reportCode: string) => boolean
  clear: (reportCode: string) => void
}> = Symbol('dashboardFilters')
