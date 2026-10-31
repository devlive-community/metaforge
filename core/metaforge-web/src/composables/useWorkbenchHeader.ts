import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { MfMessage, useDismissableLayer } from 'metaforge-ui'
import { sourceManageLocation } from '@/model/source-manage'
import { useSourceCapabilities } from './useSourceCapabilities'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import { TokenUtils } from '@/utils/token'
import router from '@/router'
import NotificationService from '@/services/notification'
import SourceService from '@/services/source'
import MetadataService from '@/services/metadata'
import { FilterModel } from '@/model/filter.ts'
import { Database, Download, GitBranch, GitCompare, History, Home, LayoutDashboard, Presentation, Settings, SquareDashedBottomCode, SquarePlay, Store, Upload } from '@lucide/vue'

export function useWorkbenchHeader() {
  const route = useRoute()

  const { t } = useI18n()

  const userStore = useUserStore()

  const filter: FilterModel = new FilterModel()

  // ===== 原型导航：源选择器 + 固定导航项 + 更多下拉 =====
  const navItems = computed(() => [
    { path: '/home', label: t('common.home'), icon: Home },
    { path: '/source', label: t('source.common.database'), icon: Database },
    { path: '/query', label: 'SQL', icon: SquarePlay },
    { path: '/import', label: t('navigation.dataImport'), icon: Upload },
    { path: '/export', label: t('navigation.dataExport'), icon: Download },
    { path: '/compare', label: t('navigation.dataCompare'), icon: GitCompare }
  ])

  const moreNavItems = computed(() => [
    { path: '/dataset', label: t('common.dataset'), icon: Database },
    { path: '/workflow', label: t('common.workflow'), icon: GitBranch },
    { path: '/snippet', label: t('common.snippet'), icon: SquareDashedBottomCode },
    { path: '/history', label: t('common.history'), icon: History },
    { path: '/report', label: t('common.report'), icon: Presentation },
    { path: '/dashboard', label: t('common.dashboard'), icon: LayoutDashboard },
    { path: '/store', label: t('common.store'), icon: Store },
    ...(userStore.userInfo?.roles?.some((role: any) => String(role.code).includes('ADMIN'))
        ? [{ path: '/admin/user', label: t('navigation.systemManagement'), icon: Settings }]
        : [])
  ])

  const isNavActive = (path: string) => {
    if (path === '/home') return route.path === '/home' || route.path === '/'
    if (path === '/source') return route.path.startsWith('/source') || route.path === '/manage' || route.path.startsWith('/compare')
    if (path === '/query') return route.path.startsWith('/query')
    return route.path.startsWith(path)
  }

  const searchIndex = ref<{ database: string; table: string }[]>([])

  const searchFocused = ref(false)
  const searchRoot = ref<HTMLElement | null>(null)
  useDismissableLayer(searchRoot, searchFocused, () => { searchFocused.value = false })

  let searchIndexSource = ''

  // 工作台头部跟随当前路由的数据源上下文
  const routeSource = computed(() => String(route.params.source || ''))

  const { can, ensureCapabilities } = useSourceCapabilities(routeSource)
  const sourceInfo = ref<any | null>(null)

  const sourceName = computed(() => String(sourceInfo.value?.name || routeSource.value || 'MetaForge'))

  const pluginLogo = computed(() => {
    const type = String(sourceInfo.value?.type || '')
    return type ? `/static/images/plugin/${ type.toLowerCase() }.svg` : ''
  })

  const databaseOptions = ref<{ label: string; value: string }[]>([])

  const currentDatabase = computed({
    get: () => String(route.params.database || ''),
    set: (value: string) => {
      if (value && value !== route.params.database) {
        router.push(sourceManageLocation({ source: routeSource.value, database: value }))
      }
    }
  })

  let sourceRequest = 0
  watch(routeSource, async source => {
    const request = ++sourceRequest
    databaseOptions.value = []
    sourceInfo.value = null
    searchIndex.value = []
    searchIndexSource = ''
    if (!source) return
    SourceService.getByCode(source)
                 .then(response => {
                   if (response.status && request === sourceRequest) {
                     sourceInfo.value = response.data
                   }
                 })
    await ensureCapabilities()
    if (request !== sourceRequest || !can('database:list')) return
    MetadataService.getDatabaseBySource(source)
                   .then(response => {
                     if (response.status && request === sourceRequest) {
                       databaseOptions.value = (response.data?.columns || [])
                           .map((item: any) => ({ label: item.object_name || item.schema_name, value: item.object_name || item.schema_name }))
                           .filter((item: any) => item.value)
                     }
                   })
  }, { immediate: true })

  const buildSearchIndex = () => {
    const source = routeSource.value
    if (!source || !can('table:list') || searchIndexSource === source) return
    searchIndexSource = source
    MetadataService.getDatabaseBySource(source)
                   .then(response => {
                     const databases = (response.data?.columns || [])
                         .map((item: any) => item.object_name || item.schema_name)
                         .filter(Boolean)
                         .slice(0, 20)
                     Promise.all(databases.map((database: string) =>
                         MetadataService.getTablesByDatabase(source, database)
                             .then(res => (res.data?.columns || [])
                                 .filter((item: any) => item.type_name === 'table')
                                 .map((item: any) => ({ database, table: item.object_name })))
                             .catch(() => [] as { database: string; table: string }[])
                     ))
                         .then(groups => {
                           if (routeSource.value === source) {
                             searchIndex.value = groups.flat()
                           }
                         })
                   })
  }

  const searchResults = computed(() => {
    const keyword = searchText.value.trim().toLowerCase()
    if (!keyword) return []
    return searchIndex.value
        .filter(item => item.table.toLowerCase().includes(keyword) || item.database.toLowerCase().includes(keyword))
        .slice(0, 20)
  })

  // 切库后清空搜索索引，重新构建
  watch(currentDatabase, () => {
    searchIndex.value = []
    searchIndexSource = ''
    if (routeSource.value) buildSearchIndex()
  })

  const onSearchBlur = () => {
    window.setTimeout(() => (searchFocused.value = false), 150)
  }

  const onSearchPick = (item: { database: string; table: string }) => {
    searchFocused.value = false
    searchText.value = ''
    router.push(sourceManageLocation({ source: routeSource.value, database: item.database, table: item.table, tab: 'structure' }))
  }

  const messages = ref<any[]>([])

  const pageIndex = ref<number>(1)

  const hasMoreData = ref(true)

  const loading = ref(false)

  const { userInfo, isLoggedIn } = storeToRefs(userStore)

  const fetchMessages = async (value: number = 1, append = false) => {
    filter.page = value
    filter.orders = [{ column: 'createTime', order: 'desc' }]
    loading.value = true
    try {
      const response = await NotificationService.getAll(filter)
      if (response.status && response.data) {
        const items = response.data.content.map((item: any) => {
          item.read = item.isRead
          return item
        })
        messages.value = append ? [...messages.value, ...items] : items
        pageIndex.value = response.data.page
        hasMoreData.value = response.data.page < response.data.totalPage
      }
      else {
        MfMessage.error(response.message)
      }
    }
    finally {
      loading.value = false
    }
  }

  const onNotifyScroll = (event: any) => {
    const target = event.target
    if (loading.value || !hasMoreData.value) {
      return
    }
    if (target.scrollTop + target.offsetHeight >= target.scrollHeight - 10) {
      fetchMessages(pageIndex.value + 1, true)
    }
  }

  onMounted(async () => {
    window.addEventListener('keydown', onGlobalKeydown)
    if (TokenUtils.getAuthUser()) {
      await Promise.all([userStore.fetchUserInfo(), userStore.fetchMenus()])
      await fetchMessages()
    }
  })

  onBeforeUnmount(() => { sourceRequest++; window.removeEventListener('keydown', onGlobalKeydown) })

  const searchText = ref('')
  watch(searchFocused, focused => { if (focused) buildSearchIndex() })

  const searchInputRef = ref<HTMLInputElement | null>(null)

  const focusSearch = () => {
    searchInputRef.value?.focus()
  }

  const onGlobalKeydown = (event: KeyboardEvent) => {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault()
      focusSearch()
    }
  }

  const logout = () => {
    userStore.logout()
    router.push('/auth/signin')
  }

  const handleNotificationClick = (msg: any) => {
    const { id, code } = msg
    const payload = { id, code, isRead: true }
    NotificationService.saveOrUpdate(payload)
                       .then(response => {
                         if (response.status && response.data) {
                           fetchMessages()
                           userStore.fetchUserInfo()
                         }
                         else {
                           MfMessage.error(response.message)
                         }
                       })
  }
  return { can, sourceName, pluginLogo, currentDatabase, databaseOptions, navItems, isNavActive, moreNavItems,
    searchFocused, searchRoot, searchText, searchInputRef, onSearchBlur, searchResults, onSearchPick, userInfo,
    messages, onNotifyScroll, handleNotificationClick, isLoggedIn, logout }
}
