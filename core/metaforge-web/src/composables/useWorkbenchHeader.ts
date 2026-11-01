import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { MfMessage } from 'metaforge-ui'
import { canOpenWorkbenchPage, compareLocation, erLocation, importLocation, sourceManageLocation } from '@/model/source-manage'
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
import { Download, GitCompare, Home, Network, SquareTerminal, Upload } from '@lucide/vue'

export function useWorkbenchHeader() {
  const route = useRoute()

  const { t } = useI18n()

  const userStore = useUserStore()

  const filter: FilterModel = new FilterModel()

  // Source workbench navigation: home, then only the pages the source's plugin declares what they need for.
  const context = () => ({ source: routeSource.value, database: String(route.params.database || '') })
  const navItems = computed(() => [
    { path: '/home', label: t('common.home'), icon: Home },
    { path: '/manage', label: t('navigation.console'), icon: SquareTerminal, to: routeSource.value ? sourceManageLocation(context()) : '/source' },
    { path: '/import', label: t('navigation.dataImport'), icon: Upload, to: routeSource.value ? importLocation(context()) : '/import' },
    { path: '/export', label: t('navigation.dataExport'), icon: Download },
    { path: '/compare', label: t('navigation.dataCompare'), icon: GitCompare, to: routeSource.value ? compareLocation(context()) : '/compare' },
    { path: '/er', label: t('navigation.erDiagram'), icon: Network, to: routeSource.value ? erLocation(context()) : '/er' },
  ].filter(item => item.path === '/home' || canOpenWorkbenchPage(item.path, profile.value?.capabilities || [])))

  const isNavActive = (path: string) => {
    if (path === '/home') return route.path === '/home' || route.path === '/'
    return route.path.startsWith(path)
  }


  // 工作台头部跟随当前路由的数据源上下文
  const routeSource = computed(() => String(route.params.source || ''))

  const { can, profile, ensureCapabilities } = useSourceCapabilities(routeSource)
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
        // The compare and ER pages stay where they are, in the chosen database.
        router.push(route.path === '/compare'
            ? compareLocation({ source: routeSource.value, database: value, mode: route.query.mode === 'data' ? 'data' : 'structure' })
            : route.path === '/er' ? erLocation({ source: routeSource.value, database: value })
                : route.path === '/import' ? importLocation({ source: routeSource.value, database: value }) : sourceManageLocation({ source: routeSource.value, database: value }))
      }
    }
  })

  let sourceRequest = 0
  watch(routeSource, async source => {
    const request = ++sourceRequest
    databaseOptions.value = []
    sourceInfo.value = null
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
    if (TokenUtils.getAuthUser()) {
      await Promise.all([userStore.fetchUserInfo(), userStore.fetchMenus()])
      await fetchMessages()
    }
  })

  onBeforeUnmount(() => { sourceRequest++ })

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
  return { can, sourceName, pluginLogo, currentDatabase, databaseOptions, navItems, isNavActive, userInfo,
    messages, onNotifyScroll, handleNotificationClick, isLoggedIn, logout }
}
