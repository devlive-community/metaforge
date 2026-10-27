export type SupportedLocale = 'zh-cn' | 'en'
export type LocaleMessages = Record<string, unknown>

interface LocaleResponse {
    status: boolean
    data?: unknown
    message?: unknown
}

export function normalizeLocale(locale: string): SupportedLocale {
    const normalized = locale.toLowerCase().replace(/_/g, '-')
    return normalized === 'zh' || normalized.startsWith('zh-') ? 'zh-cn' : 'en'
}

/** Only successful server responses count as loaded language packs. */
export function createLocaleLoader(
    fetchLocale: (locale: SupportedLocale) => Promise<LocaleResponse>,
    applyMessages: (locale: SupportedLocale, messages: LocaleMessages) => void,
) {
    const loaded = new Set<SupportedLocale>()
    const pending = new Map<SupportedLocale, Promise<void>>()

    return async (locale: string): Promise<SupportedLocale> => {
        const normalized = normalizeLocale(locale)
        if (loaded.has(normalized)) return normalized

        let request = pending.get(normalized)
        if (!request) {
            request = Promise.resolve()
                .then(() => fetchLocale(normalized))
                .then(response => {
                    const messages = response.data
                    if (!response.status || !messages || typeof messages !== 'object'
                        || Array.isArray(messages) || Object.keys(messages).length === 0) {
                        throw new Error(!response.status && typeof response.message === 'string' ? response.message : 'Invalid locale data received')
                    }
                    applyMessages(normalized, messages as LocaleMessages)
                    loaded.add(normalized)
                })
                .finally(() => pending.delete(normalized))
            pending.set(normalized, request)
        }
        await request
        return normalized
    }
}
