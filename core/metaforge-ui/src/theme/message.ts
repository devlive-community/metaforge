/**
 * Imperative toast API, drop-in replacement for ant-design-vue's `message`:
 *
 *   import { MfMessage } from 'metaforge-ui'
 *   MfMessage.success('Saved')
 *   const hide = MfMessage.loading('Working…'); hide()
 */
import { createApp, type App } from 'vue'
import MfMessageHost from '../components/MfMessageHost.vue'
import { messageState, type MfMessageItem } from './message-state'

let hostApp: App | undefined
let counter = 0

function ensureHost(): void {
    if (hostApp || typeof document === 'undefined') return
    const container = document.createElement('div')
    document.body.appendChild(container)
    hostApp = createApp(MfMessageHost)
    hostApp.mount(container)
}

type MessageType = MfMessageItem['type']

function open(content: string, type: MessageType, duration = 3000): () => void {
    ensureHost()
    const id = ++counter
    messageState.items.push({ id, type, content })
    const dismiss = (): void => {
        const index = messageState.items.findIndex(item => item.id === id)
        if (index >= 0) {
            messageState.items.splice(index, 1)
        }
    }
    if (duration > 0) {
        setTimeout(dismiss, duration)
    }
    return dismiss
}

export const MfMessage = {
    success: (content: string, duration?: number) => open(content, 'success', duration),
    error: (content: string, duration?: number) => open(content, 'error', duration),
    info: (content: string, duration?: number) => open(content, 'info', duration),
    warning: (content: string, duration?: number) => open(content, 'warning', duration),
    warn: (content: string, duration?: number) => open(content, 'warning', duration),
    loading: (content: string, duration = 0) => open(content, 'loading', duration),
    destroy: (): void => {
        messageState.items.splice(0, messageState.items.length)
    },
}
