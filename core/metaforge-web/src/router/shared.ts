import { Router } from 'vue-router'

/** A shared report served on a real path: /share/report/{token} or /embed/report/{token}. */
export function sharedReportPath(pathname: string): { token: string; embed: boolean } | null
{
    const match = /^\/(share|embed)\/report\/([A-Za-z0-9_-]{32})\/?$/.exec(pathname)
    return match ? { token: match[2], embed: match[1] === 'embed' } : null
}

/**
 * The server answers those paths with the app document plus the framing policy of the report.
 * The app uses hash routing, so the path is mapped to the public report route before the router
 * starts; the path stays in the address bar so a reload keeps the same policy.
 */
export function routeSharedReportPath(location: Location, history: History): void
{
    const shared = sharedReportPath(location.pathname)
    if (!shared || location.hash.startsWith('#/public/report/')) return
    history.replaceState(history.state, '', `${ location.pathname }${ location.search }#/public/report/${ shared.token }${ shared.embed ? '?embed=1' : '' }`)
}

/** Public report pages render without the app layout and need no sign-in. */
export function createSharedRoute(router: Router): void
{
    router.addRoute({
        name: 'public-report',
        path: '/public/report/:token',
        meta: { title: 'report.share.publicTitle' },
        component: () => import('@/views/public/PublicReport.vue')
    })
}
