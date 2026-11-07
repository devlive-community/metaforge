/** Catalogue metadata describes the package, not its runtime state or business references. */
export interface StoreMetadataItem
{
    key: string
    label: string
    description?: string
    logo?: string
    type?: string
    released?: string
    i18nFormat?: boolean
    version?: string
    supportVersion?: string[]
    author?: string
    url?: string
    en?: string
    /** Package size in bytes (built catalogues) or a ready label (older catalogues). */
    size?: number | string
    /** SHA-256 of the package; the server verifies downloads against it. */
    sha256?: string
    /** Download URL per source (maven, cdn); url stays the Maven Central package. */
    sources?: Record<string, string>
    /** Minimum MetaForge version. */
    requires?: string
    rating?: number | string
    downloads?: string | number
    /** Feature codes (store.feature.<code>) derived from declared capabilities, or plain text. */
    features?: string[]
    permissions?: string[]
    deps?: string[]
    changelog?: string[]
}

export interface StorePluginRef
{
    kind: string
    name: string
}

/** Store details an installed plugin ships in its own plugin.json. */
export interface StorePluginDescriptor
{
    label?: string
    description?: string
    logo?: string
    type?: string
    author?: string
    requires?: string
    i18nFormat?: boolean
    supportVersion?: string[]
    features?: string[]
}

export interface StoreInstalledPlugin
{
    name: string
    key?: string
    version?: string
    state?: string
    loadTime?: string
    type?: string
    store?: StorePluginDescriptor
}

const DESCRIPTOR_FIELDS = ['label', 'description', 'logo', 'type', 'author', 'requires', 'i18nFormat', 'supportVersion', 'features'] as const

/** Catalogue values win; the plugin's own descriptor fills what the catalogue lacks. */
function withDescriptor(item: StoreMetadataItem, descriptor?: StorePluginDescriptor): StoreMetadataItem
{
    if (!descriptor) return item
    const merged: StoreMetadataItem = { ...item }
    for (const field of DESCRIPTOR_FIELDS) {
        const own = descriptor[field]
        const current = merged[field]
        const missing = current == null || current === '' || (Array.isArray(current) && !current.length)
        if (missing && own != null) (merged as unknown as Record<string, unknown>)[field] = own
    }
    return merged
}

export interface StorePlugin extends StoreMetadataItem
{
    installed: boolean
    enabled: boolean
    installVersion: string
    hasUpdate: boolean
    loading: boolean
    state: string
    registeredName: string
    refs?: StorePluginRef[]
}

export type StoreView = 'market' | 'installed' | 'updates' | 'history'
export type StoreStatusFilter = 'all' | 'installed' | 'uninstalled' | 'updates'
export type StoreSort = 'popular' | 'rating' | 'az'
export type StoreLayout = 'grid' | 'list'
export type StoreAction = 'install' | 'update' | 'uninstall' | 'toggle'
export type StoreDrawerTab = 'overview' | 'versions' | 'perms'

export interface StoreOperationLog
{
    id: number
    time: string
    action: string
    plugin: string
    success: boolean
    note: string
}

export function normalizePluginType(type?: string): string
{
    const raw = type?.trim() || 'Other'
    const aliases: Record<string, string> = {
        CONNECTOR: 'Connector', CONVERT: 'Convert', CONVERTER: 'Convert', EXECUTOR: 'Executor',
        FILESYSTEM: 'FileSystem', SCHEDULER: 'Scheduler', NOTIFY: 'Notify', PARSER: 'Parser',
    }
    return aliases[raw.toUpperCase()] ?? raw
}

/** Compare published numeric versions. An unknown version must never trigger a downgrade. */
export function hasNewerPluginVersion(installed?: string, published?: string): boolean
{
    const parse = (value?: string) => value?.trim().match(/^v?(\d+(?:\.\d+)*)(?:-([\w.-]+))?(?:\+[\w.-]+)?$/i)
    const current = parse(installed)
    const latest = parse(published)
    if (!current || !latest) return false
    const a = current[1].split('.').map(Number)
    const b = latest[1].split('.').map(Number)
    for (let i = 0; i < Math.max(a.length, b.length); i++) {
        if ((a[i] ?? 0) !== (b[i] ?? 0)) return (a[i] ?? 0) < (b[i] ?? 0)
    }
    if (!current[2]) return false
    if (!latest[2]) return true
    return current[2].localeCompare(latest[2], 'en', { numeric: true, sensitivity: 'base' }) < 0
}

function identity(value?: string): string
{
    return value?.trim().toLowerCase().replace(/^metaforge-(?:plugin-)?/, '') ?? ''
}

/** Bytes as a short label; labels from older catalogues pass through unchanged. */
export function formatPackageSize(size?: number | string): string
{
    if (typeof size === 'string') return size.trim() || '--'
    if (typeof size !== 'number' || !Number.isFinite(size) || size <= 0) return '--'
    const units = ['B', 'KB', 'MB', 'GB']
    let value = size
    let unit = 0
    while (value >= 1024 && unit < units.length - 1) {
        value /= 1024
        unit++
    }
    return `${unit === 0 ? value : value.toFixed(value >= 100 ? 0 : 1)} ${units[unit]}`
}

/**
 * Merges the catalogue with the runtime plugins. downloads holds confirmed counts from the
 * statistics service; plugins it does not know keep the catalogue value (usually none).
 */
export function mergeStorePlugins(catalogue: StoreMetadataItem[], installed: StoreInstalledPlugin[], references: Record<string, StorePluginRef[]>,
                                  downloads: Record<string, string> = {}): StorePlugin[]
{
    const runtime = new Map<string, StoreInstalledPlugin>()
    for (const plugin of installed) {
        for (const value of [plugin.key, plugin.name]) {
            const id = identity(value)
            if (id && !runtime.has(id)) runtime.set(id, plugin)
        }
    }

    const used = new Set<StoreInstalledPlugin>()
    const rows: StorePlugin[] = []
    function row(item: StoreMetadataItem, hit?: StoreInstalledPlugin): StorePlugin {
        const state = hit?.state || (hit ? 'STARTED' : '')
        return {
            ...item,
            downloads: Object.prototype.hasOwnProperty.call(downloads, item.key) ? downloads[item.key] : item.downloads,
            type: normalizePluginType(item.type || hit?.type),
            installed: !!hit,
            enabled: !!hit && !['STOPPED', 'FAILED', 'UNLOADED'].includes(state),
            installVersion: hit?.version || '',
            hasUpdate: !!hit && !!item.url && hasNewerPluginVersion(hit.version, item.version),
            loading: false,
            state,
            registeredName: hit?.name || '',
            refs: hit && Object.prototype.hasOwnProperty.call(references, hit.name) ? references[hit.name] : undefined,
        }
    }

    for (const item of catalogue) {
        const hit = [item.key, item.label, item.en].map(value => runtime.get(identity(value))).find(Boolean)
        if (hit && used.has(hit)) continue
        if (hit) used.add(hit)
        rows.push(row(withDescriptor(item, hit?.store), hit))
    }
    for (const hit of installed) {
        if (!used.has(hit)) {
            // Not in the catalogue (self-built or offline): describe it from its own plugin.json.
            rows.push(row(withDescriptor({ key: hit.key || hit.name, label: hit.store?.label || hit.name, type: hit.store?.type || hit.type, version: hit.version }, hit.store), hit))
        }
    }
    return rows
}

export function readStoreCatalogue(value: unknown): StoreMetadataItem[]
{
    if (value === null || typeof value !== 'object' || !('children' in value) || !Array.isArray(value.children)) {
        throw new Error('Invalid plugin catalogue')
    }
    const text = (value: unknown) => typeof value === 'string' ? value : undefined
    const list = (value: unknown) => Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : undefined
    const metric = (value: unknown) => typeof value === 'number' || typeof value === 'string' ? value : undefined
    const links = (value: unknown) => {
        if (!value || typeof value !== 'object' || Array.isArray(value)) return undefined
        const entries = Object.entries(value).filter((entry): entry is [string, string] => typeof entry[1] === 'string' && /^https?:\/\//.test(entry[1]))
        return entries.length ? Object.fromEntries(entries) : undefined
    }
    const items = new Map<string, StoreMetadataItem>()
    for (const item of value.children) {
        if (!item || typeof item.key !== 'string' || !item.key.trim() || typeof item.label !== 'string') continue
        items.set(item.key, {
            key: item.key,
            label: item.label,
            description: text(item.description),
            logo: text(item.logo),
            type: text(item.type),
            released: text(item.released),
            i18nFormat: item.i18nFormat === true,
            version: text(item.version),
            supportVersion: list(item.supportVersion),
            author: text(item.author),
            url: text(item.url),
            en: text(item.en),
            size: metric(item.size),
            sha256: typeof item.sha256 === 'string' && /^[0-9a-f]{64}$/.test(item.sha256) ? item.sha256 : undefined,
            sources: links(item.sources),
            requires: text(item.requires),
            rating: metric(item.rating),
            downloads: metric(item.downloads),
            features: list(item.features),
            permissions: list(item.permissions),
            deps: list(item.deps),
            changelog: list(item.changelog),
        })
    }
    if (!items.size) throw new Error('Empty plugin catalogue')
    return [...items.values()]
}
