import { onScopeDispose, ref, shallowRef, watch, type Ref } from 'vue'
import DashboardService from '@/services/dashboard'
import type { DashboardModel } from '@/model/dashboard'
import { FilterModel } from '@/model/filter'

export function useScreenLibrary(view: Ref<string>) {
  const data = shallowRef<DashboardModel[]>([]), loading = ref(true), failed = ref(false)
  const page = ref(1), size = ref(6), total = ref(0), keyword = ref(''), sort = ref('updated')
  const layout = ref<'grid' | 'list'>('grid')
  try { if (localStorage.getItem('MetaForgeScreenLayout') === 'list') layout.value = 'list' } catch { /* Optional preference. */ }
  watch(layout, value => { try { localStorage.setItem('MetaForgeScreenLayout', value) } catch { /* Optional preference. */ } })
  let request = 0, timer: ReturnType<typeof setTimeout> | undefined
  async function load() {
    const ticket = ++request
    loading.value = true; failed.value = false
    const shared = view.value === 'shared'
    try {
      const response = shared ? await DashboardService.shared(keyword.value.trim(), sort.value)
        : await DashboardService.getAll({ ...new FilterModel(), page: page.value, size: size.value, keyword: keyword.value.trim(),
          orders: [{ column: sort.value === 'created' ? 'createTime' : sort.value === 'name' ? 'name' : 'updateTime', order: sort.value === 'name' ? 'ASC' : 'DESC' }] })
      if (ticket !== request) return
      const items = shared ? response.data : response.data?.content
      if (!response.status || !Array.isArray(items) || !items.every(item => typeof item?.code === 'string' && !!item.code)
          || (!shared && (!Number.isSafeInteger(response.data.total) || response.data.total < 0))) throw Error('Invalid screen list')
      data.value = items
      total.value = shared ? items.length : response.data.total
      if (!shared && !items.length && page.value > 1 && total.value > 0) { page.value = Math.max(1, Math.ceil(total.value / size.value)); await load() }
    } catch { if (ticket === request) failed.value = true }
    finally { if (ticket === request) loading.value = false }
  }
  function reset() { page.value = 1; void load() }
  watch(keyword, () => { request++; clearTimeout(timer); timer = setTimeout(reset, 250) })
  watch([view, sort], () => { clearTimeout(timer); reset() })
  function changePage(current: number, count: number) { page.value = size.value === count ? current : 1; size.value = count; void load() }
  onScopeDispose(() => { request++; clearTimeout(timer) })
  void load()
  return { data, loading, failed, page, size, total, keyword, sort, layout, load, changePage }
}
