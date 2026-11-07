import { computed, onScopeDispose, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import PluginService from '@/services/plugin'
import { formatPackageSize, mergeStorePlugins, normalizePluginType, readStoreCatalogue } from '@/model/store'
import type {
    StoreAction, StoreDrawerTab, StoreInstalledPlugin, StoreLayout, StoreMetadataItem, StoreOperationLog,
    StorePlugin, StorePluginRef, StoreSort, StoreStatusFilter, StoreView,
} from '@/model/store'
import { useUserStore } from '@/stores/user'
import { isAdministrator } from '@/router/navigation'
import { loadingState } from '@/i18n/I18n'

const TYPE_ORDER = ['Connector', 'Convert', 'Executor', 'FileSystem', 'Notify', 'Scheduler', 'Parser']

export function usePluginStore()
{
    const { t, te, locale } = useI18n()
    const userStore = useUserStore()
    const isAdmin = computed(() => isAdministrator(userStore.userInfo))

    const loading = ref(false)
    const ready = ref(false)
    const installedFailed = ref(false)
    const catalogFailed = ref(false)
    const catalogue = ref<StoreMetadataItem[]>([])
    const installed = ref<StoreInstalledPlugin[]>([])
    const references = ref<Record<string, StorePluginRef[]>>({})
    const downloads = ref<Record<string, string>>({})

    const view = ref<StoreView>('market')
    const category = ref('all')
    const query = ref('')
    const status = ref<StoreStatusFilter>('all')
    const sort = ref<StoreSort>('popular')
    const layout = ref<StoreLayout>('grid')
    const drawerKey = ref('')
    const drawerTab = ref<StoreDrawerTab>('overview')
    const confirm = ref<{ action: StoreAction; pluginKey: string; enable: boolean }>()
    const confirmName = ref('')
    const pending = ref<{ action: StoreAction; pluginKey: string; label: string }>()
    const logs = ref<StoreOperationLog[]>([])
    let logId = 0
    let disposed = false

    const plugins = computed(() => mergeStorePlugins(catalogue.value, installed.value, references.value, downloads.value)
        .map(row => ({ ...row, loading: pending.value?.pluginKey === row.key })))
    const hasCatalog = computed(() => ready.value)
    const installedCount = computed(() => plugins.value.filter(item => item.installed).length)
    const runningCount = computed(() => plugins.value.filter(item => item.enabled).length)
    const hasRatings = computed(() => plugins.value.some(item => item.rating != null && Number.isFinite(Number(item.rating))))
    const updatePlugins = computed(() => plugins.value.filter(item => item.hasUpdate))
    const updateCount = computed(() => updatePlugins.value.length)
    const drawerPlugin = computed(() => plugins.value.find(item => item.key === drawerKey.value))
    const confirmPlugin = computed(() => plugins.value.find(item => item.key === confirm.value?.pluginKey))
    const confirmAction = computed(() => confirm.value?.action)
    const confirmEnable = computed(() => confirm.value?.enable ?? false)
    const operating = computed(() => !!pending.value)
    const uninstallBlocked = computed(() => !!confirmPlugin.value?.refs?.length)

    const categories = computed(() => {
        const counts = new Map<string, number>()
        for (const item of plugins.value) {
            const type = normalizePluginType(item.type)
            counts.set(type, (counts.get(type) ?? 0) + 1)
        }
        const known = TYPE_ORDER.filter(type => counts.has(type))
        const rest = [...counts.keys()].filter(type => !TYPE_ORDER.includes(type)).sort()
        return [...known, ...rest].map(id => ({ id, label: typeLabel(id), count: counts.get(id) ?? 0 }))
    })

    function typeLabel(type: string): string
    {
        const normalized = normalizePluginType(type)
        const key = 'store.type.' + normalized
        return te(key) ? t(key) : normalized
    }

    function pluginLabel(plugin: StorePlugin): string
    {
        return plugin.i18nFormat && te(plugin.label) ? t(plugin.label) : plugin.label
    }

    function pluginDescription(plugin: StorePlugin): string
    {
        return plugin.description && plugin.i18nFormat && te(plugin.description)
            ? t(plugin.description) : plugin.description ?? ''
    }

    function sizeLabel(plugin: StorePlugin): string
    {
        return formatPackageSize(plugin.size)
    }

    /** Confirmed downloads with grouping; unknown counts show a dash, never zero. */
    function downloadsLabel(plugin: StorePlugin): string
    {
        const value = plugin.downloads
        if (value == null || value === '') return '—'
        if (typeof value === 'string' && /^\d+$/.test(value)) {
            try {
                return BigInt(value).toLocaleString(locale.value)
            }
            catch {
                return value
            }
        }
        return typeof value === 'number' ? value.toLocaleString(locale.value) : String(value)
    }

    function requiresLabel(plugin: StorePlugin): string
    {
        return plugin.requires ? t('store.field.requiresValue', { version: plugin.requires }) : '--'
    }

    function featureLabel(feature: string): string
    {
        const key = 'store.feature.' + feature
        return /^[A-Za-z]+$/.test(feature) && te(key) ? t(key) : feature
    }

    function authorLabel(plugin: StorePlugin): string
    {
        return plugin.author || t('store.author.unknown')
    }

    function official(plugin: StorePlugin): boolean
    {
        return /官方|official|^devlive$/i.test(plugin.author ?? '')
    }

    function statusKey(plugin: StorePlugin, enabledLabel: 'installed' | 'enabled'): string
    {
        if (plugin.state === 'FAILED') return 'store.status.failed'
        return 'store.status.' + (plugin.enabled ? enabledLabel : 'disabled')
    }

    function referenceKind(kind: string): string
    {
        const key = 'store.reference.' + kind
        return te(key) ? t(key) : kind
    }

    function canInstall(plugin: StorePlugin): boolean
    {
        return isAdmin.value && !installedFailed.value && !operating.value && !!plugin.url
    }

    function canToggle(plugin: StorePlugin): boolean
    {
        return isAdmin.value && !operating.value && plugin.installed && plugin.state !== 'FAILED'
    }

    const keyword = computed(() => query.value.trim().toLocaleLowerCase(locale.value))
    function matches(item: StorePlugin): boolean
    {
        return [pluginLabel(item), item.en, pluginDescription(item), item.key, item.author]
            .join(' ').toLocaleLowerCase(locale.value).includes(keyword.value)
    }

    const marketPlugins = computed(() => {
        const list = plugins.value.filter(item => {
            if (category.value !== 'all' && item.type !== category.value) return false
            if (status.value === 'installed' && !item.installed) return false
            if (status.value === 'uninstalled' && item.installed) return false
            if (status.value === 'updates' && !item.hasUpdate) return false
            return matches(item)
        })
        if (sort.value === 'az') {
            return list.sort((a, b) => pluginLabel(a).localeCompare(pluginLabel(b), locale.value))
        }
        if (sort.value === 'rating') {
            const rating = (item: StorePlugin) => item.rating != null && Number.isFinite(Number(item.rating)) ? Number(item.rating) : -1
            return list.sort((a, b) => rating(b) - rating(a))
        }
        return list
    })

    const installedPlugins = computed(() => plugins.value.filter(item => item.installed && matches(item)))

    function appendLog(action: string, plugin: string, note: string, success = true): void
    {
        logs.value.unshift({ id: logId++, time: new Date().toISOString(), action, plugin, note, success })
    }

    async function loadMetadata(): Promise<void>
    {
        try {
            // The catalogue comes from the server, which fetches the configured remote URL:
            // no cross-origin requests and no per-user catalogue addresses.
            const response = await PluginService.getCatalogue()
            if (disposed) return
            if (!response.status || !response.data) throw new Error('Catalogue unavailable')
            const entries = readStoreCatalogue(response.data)
            if (!disposed) {
                catalogue.value = entries
                catalogFailed.value = false
            }
        }
        catch {
            if (disposed) return
            catalogFailed.value = true
            try {
                const fallback = await import('../../../../configure/metadata.json')
                if (!disposed) catalogue.value = readStoreCatalogue(fallback.default)
            }
            catch { /* Keep the previous catalogue or just the installed plugins. */ }
        }
    }

    async function loadInstalled(): Promise<boolean>
    {
        try {
            const response = await PluginService.getPlugins()
            if (disposed) return false
            if (!response.status || !Array.isArray(response.data)) throw new Error('Installed plugins unavailable')
            installed.value = response.data.filter(item => item && typeof item.name === 'string')
            installedFailed.value = false
            return true
        }
        catch {
            if (!disposed) installedFailed.value = true
            return false
        }
    }

    async function loadDownloads(): Promise<void>
    {
        const keys = catalogue.value.map(item => item.key)
        if (!keys.length) return
        try {
            const response = await PluginService.getDownloads(keys)
            if (!disposed && response.status && response.data && typeof response.data === 'object') {
                downloads.value = response.data
            }
        }
        catch { /* Counts are optional; unknown counts show a dash. */ }
    }

    async function loadReferences(): Promise<void>
    {
        references.value = {}
        if (!isAdmin.value) return
        try {
            const response = await PluginService.getReferences()
            if (!disposed && isAdmin.value && response.status && response.data && typeof response.data === 'object') {
                references.value = response.data
            }
        }
        catch { /* Unknown references stay unknown. The uninstall endpoint checks them again. */ }
    }

    async function refresh(): Promise<void>
    {
        if (loading.value || operating.value) return
        loading.value = true
        try {
            const [loaded] = await Promise.all([loadInstalled(), loadMetadata().then(loadDownloads), loadReferences()])
            if (!disposed) {
                ready.value = true
                appendLog('check', t('store.history.allPlugins'),
                    loaded ? t('store.history.checkNote', { count: updateCount.value }) : t('store.market.installedFailed'), loaded)
            }
        }
        finally {
            loading.value = false
        }
    }

    function openDrawer(item: StorePlugin, tab: StoreDrawerTab = 'overview'): void
    {
        drawerKey.value = item.key
        drawerTab.value = tab
    }

    function closeDrawer(): void
    {
        drawerKey.value = ''
        drawerTab.value = 'overview'
    }

    function openConfirm(action: StoreAction, item: StorePlugin, enable = false): void
    {
        if (!isAdmin.value || operating.value) return
        if ((action === 'install' || action === 'update') && !canInstall(item)) return
        if (action === 'toggle' && !canToggle(item)) return
        confirm.value = { action, pluginKey: item.key, enable }
        confirmName.value = ''
    }

    function closeConfirm(): void
    {
        confirm.value = undefined
        confirmName.value = ''
    }

    async function commit(): Promise<void>
    {
        const operation = confirm.value
        const item = confirmPlugin.value
        if (!operation || !item || !isAdmin.value || operating.value) return
        const { action, enable } = operation
        const label = pluginLabel(item)
        if (action === 'uninstall' && (!item.installed || uninstallBlocked.value || confirmName.value !== label)) return
        if (action === 'toggle' && !canToggle(item)) return
        if ((action === 'update' || action === 'install') && (!canInstall(item) || !item.url)) return

        const actionName = action === 'toggle' ? (enable ? 'enable' : 'disable') : action
        pending.value = { action, pluginKey: item.key, label }
        closeConfirm()
        try {
            const response = action === 'uninstall' ? await PluginService.uninstall(item.registeredName)
                : action === 'toggle' ? await PluginService.changeState(item.registeredName, enable)
                : await PluginService.install({
                    name: item.key,
                    url: item.url!,
                    version: item.version,
                    sha256: item.sha256,
                    size: typeof item.size === 'number' ? item.size : undefined,
                })
            if (!response.status || response.data !== true) {
                throw new Error(response.status ? t('store.toast.failed') : response.message || t('store.toast.failed'))
            }
            const refreshed = await loadInstalled()
            if (disposed) return
            await loadReferences()
            if (disposed) return
            const note = t(refreshed ? 'store.toast.completed' : 'store.toast.completedWithoutRefresh', { name: label })
            if (refreshed) MfMessage.success(note)
            else MfMessage.warning(note)
            appendLog(actionName, label, note)
        }
        catch (error) {
            if (disposed) return
            const message = error instanceof Error ? error.message : t('store.toast.failed')
            MfMessage.error(message)
            appendLog(actionName, label, message, false)
            await loadReferences()
        }
        finally {
            pending.value = undefined
        }
    }

    function switchView(next: StoreView): void
    {
        view.value = next
        category.value = 'all'
        query.value = ''
        closeDrawer()
        closeConfirm()
    }

    function checkUpdates(): void
    {
        switchView('updates')
        void refresh()
    }

    function pickCategory(id: string): void
    {
        if (view.value !== 'market') switchView('market')
        category.value = id
    }

    watch(loadingState, busy => {
        if (!busy && !ready.value && !loading.value) void refresh()
    }, { immediate: true })
    watch(isAdmin, () => { if (ready.value) void loadReferences() })
    watch(hasRatings, available => { if (!available && sort.value === 'rating') sort.value = 'popular' })
    onScopeDispose(() => {
        disposed = true
    })

    return {
        loading, installedFailed, catalogFailed, hasCatalog, view, category, query, status, sort, layout, drawerTab,
        confirmAction, confirmEnable, confirmName, operating, pending, logs, isAdmin, hasRatings,
        categories, marketPlugins, installedPlugins, updatePlugins, installedCount, runningCount, updateCount,
        drawerPlugin, confirmPlugin, uninstallBlocked,
        typeLabel, pluginLabel, pluginDescription, authorLabel, sizeLabel, downloadsLabel, requiresLabel, featureLabel, official, statusKey, referenceKind, canInstall, canToggle,
        refresh, checkUpdates, openDrawer, closeDrawer, openConfirm, closeConfirm, commit, switchView, pickCategory,
        clearFilters: () => { query.value = ''; status.value = 'all' },
        setQuery: (value: string) => { query.value = value },
        setStatus: (value: string | number | undefined) => { if (value === 'all' || value === 'installed' || value === 'uninstalled' || value === 'updates') status.value = value },
        setSort: (value: string | number | undefined) => { if (value === 'popular' || value === 'rating' || value === 'az') sort.value = value },
        setLayout: (value: StoreLayout) => { layout.value = value },
        setDrawerTab: (value: string | number) => { if (value === 'overview' || value === 'versions' || value === 'perms') drawerTab.value = value },
        setConfirmName: (value: string) => { confirmName.value = value },
    }
}

export type PluginStoreContext = ReturnType<typeof usePluginStore>
