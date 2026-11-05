/** One site notification of the signed-in user, as the notification center lists it. */
export interface NotificationItem
{
    code: string
    title: string
    content: string
    scene: string
    category: string
    read: boolean
    createTime: string
    entityType?: string
    entityCode?: string
    entityName?: string
    entityExists: boolean
}

export interface NotificationPage
{
    items: NotificationItem[]
    total: number
    page: number
    size: number
}

export interface NotificationSummary
{
    total: number
    unread: number
    categories: Record<string, number>
}

export interface NotificationQuery
{
    read?: boolean
    category?: string
    keyword?: string
    page?: number
    size?: number
}

/** Categories in the order the center offers them; SYSTEM holds notices that belong to no scene. */
export const NOTIFICATION_CATEGORIES = ['SYSTEM', 'DATASET', 'SOURCE', 'REPORT', 'TRANSFER', 'ACCOUNT', 'PLUGIN']

export type NotificationTone = 'primary' | 'info' | 'success' | 'warning' | 'danger' | 'violet'

const CATEGORY_TONES: Record<string, NotificationTone> = {
    SYSTEM: 'info',
    DATASET: 'primary',
    SOURCE: 'info',
    REPORT: 'violet',
    TRANSFER: 'violet',
    ACCOUNT: 'warning',
    PLUGIN: 'violet'
}

/** Failures stand out whatever their category. */
export const toneOf = (item: Pick<NotificationItem, 'scene' | 'category'>): NotificationTone =>
    (item.scene || '').endsWith('_FAILED') ? 'danger' : CATEGORY_TONES[item.category] || 'info'

const ROUTES: Record<string, (code: string) => string> = {
    DATASET: code => `/dataset/info/${ code }`,
    SOURCE: code => `/source/${ code }`,
    DASHBOARD: code => `/dashboard/preview/${ code }`,
    WORKFLOW: code => `/workflow/monitor/${ code }`,
    REPORT: () => '/report'
}

/** Where the notification's entity lives on the site, when it still exists and has a page. */
export const linkOf = (item: NotificationItem): string => {
    const route = ROUTES[item.entityType || '']
    return item.entityExists && item.entityCode && route ? route(item.entityCode) : ''
}

// Notifications saved before scenes existed carry an operation as their type.
const LEGACY: Record<string, string> = {
    CREATED: 'DATASET_CREATED',
    UPDATED: 'DATASET_UPDATED',
    DELETED: 'DATASET_DELETED',
    DYNAMIC: 'DATASET_UPDATED',
    SYNCDATA: 'DATASET_SYNC_SUCCEEDED'
}

export const sceneKey = (scene: string): string => LEGACY[scene] || scene

/** Splits a newest-first list into today and earlier, keeping the order. */
export const groupByDay = (items: NotificationItem[], now: Date = new Date()): { key: 'today' | 'earlier', items: NotificationItem[] }[] => {
    const pad = (value: number) => String(value).padStart(2, '0')
    const today = `${ now.getFullYear() }-${ pad(now.getMonth() + 1) }-${ pad(now.getDate()) }`
    const groups = [
        { key: 'today' as const, items: items.filter(item => (item.createTime || '').startsWith(today)) },
        { key: 'earlier' as const, items: items.filter(item => !(item.createTime || '').startsWith(today)) }
    ]
    return groups.filter(group => group.items.length > 0)
}
