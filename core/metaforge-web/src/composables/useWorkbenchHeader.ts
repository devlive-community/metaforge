import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { canOpenWorkbenchPage, compareLocation, erLocation, exportLocation, importLocation, sourceManageLocation } from '@/model/source-manage'
import { useSourceCapabilities } from './useSourceCapabilities'
import { useSourceDatabasesStore } from '@/stores/source-databases'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import { TokenUtils } from '@/utils/token'
import router from '@/router'
import SourceService from '@/services/source'
import { Download, GitCompare, Home, Network, SquareTerminal, Upload } from '@lucide/vue'

export function useWorkbenchHeader() {
  const route = useRoute()

  const { t } = useI18n()

  const userStore = useUserStore()

  // Source workbench navigation: home, then only the pages the source's plugin declares what they need for.
  const context = () => ({ source: routeSource.value, database: String(route.params.database || '') })
  const navItems = computed(() => [
    { path: '/home', label: t('common.home'), icon: Home },
    { path: '/manage', label: t('navigation.console'), icon: SquareTerminal, to: routeSource.value ? sourceManageLocation(context()) : '/source' },
    { path: '/import', label: t('navigation.dataImport'), icon: Upload, to: routeSource.value ? importLocation(context()) : '/import' },
    { path: '/export', label: t('navigation.dataExport'), icon: Download, to: routeSource.value ? exportLocation(context()) : '/export' },
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
  const sourceLoading = ref(false)
  const databases = useSourceDatabasesStore()

  const sourceName = computed(() => String(sourceInfo.value?.name || routeSource.value || 'MetaForge'))

  const pluginLogo = computed(() => {
    const type = String(sourceInfo.value?.type || '')
    return type ? `/static/images/plugin/${ type.toLowerCase() }.svg` : ''
  })

  const databaseOptions = computed(() => databases.entryOf(routeSource.value).options)
  const databasesLoading = computed(() => databases.entryOf(routeSource.value).loading)

  const currentDatabase = computed({
    get: () => String(route.params.database || ''),
    set: (value: string) => {
      if (value && value !== route.params.database) {
        // The compare and ER pages stay where they are, in the chosen database.
        router.push(route.path === '/compare'
            ? compareLocation({ source: routeSource.value, database: value, mode: route.query.mode === 'data' ? 'data' : 'structure' })
            : route.path === '/er' ? erLocation({ source: routeSource.value, database: value })
                : route.path === '/import' ? importLocation({ source: routeSource.value, database: value })
                    : route.path === '/export' ? exportLocation({ source: routeSource.value, database: value }) : sourceManageLocation({ source: routeSource.value, database: value }))
      }
    }
  })

  let sourceRequest = 0
  watch(routeSource, async source => {
    const request = ++sourceRequest
    sourceInfo.value = null
    sourceLoading.value = !!source
    if (!source) return
    SourceService.getByCode(source)
                 .then(response => {
                   if (response.status && request === sourceRequest) {
                     sourceInfo.value = response.data
                   }
                 })
                 .finally(() => { if (request === sourceRequest) sourceLoading.value = false })
    await ensureCapabilities()
    if (request !== sourceRequest || !can('database:list')) return
    // Reloaded on entering a source, so databases created elsewhere show up.
    void databases.load(source, true)
  }, { immediate: true })

  const { userInfo, isLoggedIn } = storeToRefs(userStore)

  onMounted(async () => {
    if (TokenUtils.getAuthUser()) {
      await Promise.all([userStore.fetchUserInfo(), userStore.fetchMenus()])
    }
  })

  onBeforeUnmount(() => { sourceRequest++ })

  const logout = () => {
    userStore.logout()
    router.push('/auth/signin')
  }

  return { can, sourceName, sourceLoading, pluginLogo, currentDatabase, databaseOptions, databasesLoading, navItems, isNavActive, userInfo,
    isLoggedIn, logout }
}
