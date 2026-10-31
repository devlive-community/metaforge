import { ref, type Ref } from 'vue'
import { FilterModel, emptyTableFilters, type TableFilters } from '@/model/filter'
import type { ResponseModel } from '@/model/response'
import { MfMessage } from 'metaforge-ui'

/**
 * 分页列表通用状态与动作（Vue3 Composable）。
 *
 * 覆盖所有列表页的固定套路：
 * - loading / data / pageIndex / pageSize / dataCount
 * - 竞态保护（仅采纳最后一次请求的响应）
 * - 查询（回到第一页）、翻页、改每页条数、刷新
 *
 * @param fetcher 传入内部 filter 的请求函数，如 `filter => UserService.getAll(filter)`。
 * @param options.onData 每次数据返回后的回调（行后处理/联动刷新）。
 * @param options.onFailure 请求失败回调（默认 MfMessage.error）。
 */
export function usePagedList<T = any>(
    fetcher: (filter: FilterModel) => Promise<ResponseModel>,
    options: {
      defaultPageSize?: number
      onData?: (content: T[]) => void
      onFailure?: (message: string) => void
    } = {},
)
{
  const filter = new FilterModel()
  if (options.defaultPageSize) {
    filter.size = options.defaultPageSize
  }

  const loading = ref(false)
  const data: Ref<T[]> = ref([])
  const pageIndex = ref(1)
  const pageSize = ref(options.defaultPageSize ?? 10)
  const dataCount = ref(0)
  let latestRequest = 0

  const initialize = () => {
    const request = ++latestRequest
    loading.value = true
    fetcher(filter)
        .then(response => {
          if (request !== latestRequest) return
          if (response.status) {
            data.value = response.data?.content ?? []
            dataCount.value = response.data?.total ?? 0
            pageSize.value = response.data?.size ?? pageSize.value
            pageIndex.value = response.data?.page ?? pageIndex.value
            options.onData?.(data.value)
          }
          else {
            (options.onFailure ?? ((message: string) => MfMessage.error(message)))(response.message)
          }
        })
        .finally(() => {
          if (request === latestRequest) {
            loading.value = false
          }
        })
  }

  /** 查询/重置：应用筛选条件并回到第一页。 */
  const onSearch = (values: TableFilters) => {
    Object.assign(filter, emptyTableFilters(), values, { page: 1 })
    pageIndex.value = 1
    initialize()
  }

  const onPageChange = (page: number) => {
    filter.page = page
    initialize()
  }

  const onSizeChange = (_current: number, size: number) => {
    pageSize.value = size
    filter.size = size
    filter.page = pageIndex.value
    initialize()
  }

  return {
    filter,
    loading,
    data,
    pageIndex,
    pageSize,
    dataCount,
    initialize,
    onSearch,
    onPageChange,
    onSizeChange
  }
}
