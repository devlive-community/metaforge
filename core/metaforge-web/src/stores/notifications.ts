import { defineStore } from 'pinia'
import { ref } from 'vue'
import { MfMessage } from 'metaforge-ui'
import NotificationService from '@/services/notification'
import { NotificationItem, NotificationSummary } from '@/model/notification'

const POLL_INTERVAL = 60_000

/**
 * The signed-in user's notification state shared by the header bell, the notification center page
 * and the detail drawer. `version` changes whenever notifications change, so every open list reloads.
 */
export const useNotificationStore = defineStore('notifications', () => {
    const summary = ref<NotificationSummary>({ total: 0, unread: 0, categories: {} })
    const version = ref(0)
    const detail = ref<NotificationItem | null>(null)
    const detailOpen = ref(false)
    /** A notification waiting for the user to confirm its removal. */
    const pendingRemoval = ref<NotificationItem | null>(null)
    let timer: ReturnType<typeof setInterval> | undefined
    let watchers = 0

    const refresh = async () => {
        const response = await NotificationService.summary()
        if (response.status && response.data) {
            const previous = summary.value.total
            summary.value = response.data
            // New notifications arrived in the background: lists showing the old state reload.
            if (response.data.total !== previous) {
                version.value++
            }
        }
    }

    /** Something changed notifications (a test send, a read or a removal): reload counts and lists. */
    const changed = async () => {
        version.value++
        await refresh()
    }

    const mark = async (item: NotificationItem, read: boolean) => {
        const response = await NotificationService.mark(item.code, read)
        if (!response.status) {
            MfMessage.error(response.message)
            return false
        }
        item.read = read
        if (detail.value?.code === item.code) {
            detail.value.read = read
        }
        await changed()
        return true
    }

    const markAllRead = async (category?: string) => {
        const response = await NotificationService.markAllRead(category)
        if (!response.status) {
            MfMessage.error(response.message)
            return 0
        }
        await changed()
        return Number(response.data?.updated || 0)
    }

    const remove = async (item: NotificationItem) => {
        const response = await NotificationService.remove(item.code)
        if (!response.status) {
            MfMessage.error(response.message)
            return false
        }
        if (detail.value?.code === item.code) {
            detailOpen.value = false
        }
        await changed()
        return true
    }

    const confirmRemove = (item: NotificationItem) => {
        pendingRemoval.value = item
    }

    /** Shows a notification in the drawer; opening an unread one reads it. */
    const open = async (item: NotificationItem) => {
        detail.value = item
        detailOpen.value = true
        if (!item.read) {
            await mark(item, true)
        }
    }

    /** Keeps the unread count current while any header is mounted; checks only while the tab is visible. */
    const watch = () => {
        watchers++
        if (!timer) {
            refresh()
            timer = setInterval(() => {
                if (document.visibilityState === 'visible') {
                    refresh()
                }
            }, POLL_INTERVAL)
        }
        return () => {
            watchers--
            if (watchers <= 0 && timer) {
                clearInterval(timer)
                timer = undefined
                watchers = 0
            }
        }
    }

    return { summary, version, detail, detailOpen, pendingRemoval, confirmRemove, refresh, changed, mark, markAllRead, remove, open, watch }
})
