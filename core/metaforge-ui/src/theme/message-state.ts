import { reactive } from 'vue'

export interface MfMessageItem {
    id: number
    type: 'success' | 'error' | 'info' | 'warning' | 'loading'
    content: string
}

export const messageState = reactive<{ items: MfMessageItem[] }>({ items: [] })
