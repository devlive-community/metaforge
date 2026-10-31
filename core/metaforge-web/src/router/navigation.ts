export const businessPages = ['query', 'source', 'dataset', 'dashboard', 'history', 'snippet', 'report', 'chat', 'workflow'] as const
export const accountPages = ['profile', 'info', 'log', 'editor', 'assistant', 'username', 'password', 'notify'] as const

export function canonicalPagePath(path: string): string {
    let result = path.replace(/^\/system(?=[/?#]|$)/i, '/admin')
    result = result.replace(/^\/admin(?=[/?#]|$)/i, '/admin')
    result = result.replace(/^\/admin\/user\/(profile|info|log|editor|assistant|username|password|notify)(?=[/?#]|$)/i, (_match, page) => `/account/${page.toLowerCase()}`)
    result = result.replace(/^\/admin\/(query|source|dataset|dashboard|history|snippet|report|chat|workflow)(?=[/?#]|$)/i, (_match, page) => `/${page.toLowerCase()}`)
    return result.replace(/^\/admin\/(user|role|menu|schedule|function|configure)(?=[/?#]|$)/i, (_match, page) => `/admin/${page.toLowerCase()}`)
}

export function isAdministrator(user: { roles?: unknown } | null | undefined): boolean {
    return Array.isArray(user?.roles) && user.roles.some(role => role === 'ADMIN' || role?.code === 'ADMIN')
}

export function pageAccess(to: { fullPath: string; meta: Record<string, unknown> }, user: { token?: unknown; roles?: unknown } | null | undefined) {
    if (to.meta.requiresAuth && !(typeof user?.token === 'string' && user.token.length > 0)) {
        return { path: '/auth/signin', query: { redirect: to.fullPath } }
    }
    if (to.meta.requiresAdmin && !isAdministrator(user)) return { path: '/common/403' }
    return true
}

/** Normalize stale menu caches until the current catalogue is fetched from the server. */
export function normalizeNavigation(menus: unknown, user: { roles?: unknown } | null | undefined): any[] {
    if (!Array.isArray(menus)) return []
    return menus.flatMap(menu => {
        if (!menu || typeof menu !== 'object') return []
        const original = typeof menu.url === 'string' ? menu.url : ''
        const originalChildren = Array.isArray(menu.children) ? menu.children : []
        const development = (menu.code === 'MANAGEMENT' && (original === '/admin' || menu.i18nKey === 'common.admin')) || (!menu.code && original === '/admin' && originalChildren.some((child: any) => /^\/admin\/(query|source|dataset|workflow|snippet|history|report)(?:\/|$)/i.test(child?.url)))
        const url = development ? '/workspace' : canonicalPagePath(original)
        if (/^\/admin(?:[/?#]|$)/.test(url) && !isAdministrator(user)) return []
        const children = originalChildren.length ? normalizeNavigation(originalChildren, user) : undefined
        if (originalChildren.length && children?.length === 0) return []
        const legacySystem = menu.code === 'SYSTEM' && (original === '/system' || menu.i18nKey === 'common.system')
        const result = { ...menu, url, ...(development ? { i18nKey: 'navigation.dataDevelopment' } : legacySystem ? { i18nKey: 'navigation.systemManagement' } : {}) }
        if (children?.length) result.children = children
        else delete result.children
        return [result]
    })
}
