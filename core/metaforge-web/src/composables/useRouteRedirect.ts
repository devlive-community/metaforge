import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { LocationQuery, RouteLocationNormalized, Router } from 'vue-router'
import { canonicalPagePath } from '@/router/navigation'

/**
 * 中间页回跳：断网、404、403、登录、安装这些页面只是临时落脚点，
 * 跳转时把原地址（完整 fullPath，含 query 与 hash）放进 `redirect`，恢复后 replace 回去。
 */

type LocationLike = Pick<RouteLocationNormalized, 'path' | 'fullPath' | 'query'>

// 中间页自身不能作为回跳目标，否则恢复时会在中间页之间循环。
const INTERIM_PATH = /^\/(?:common\/(?:404|403|not_network)|auth|setup)(?:[/?#]|$)/i

export function isInterimPath(path: string): boolean {
    return INTERIM_PATH.test(path)
}

/** 只接受站内相对路径（`/` 开头、非 `//`、无反斜杠与控制字符），并排除中间页，防止开放重定向。 */
export function resolveRedirect(value: unknown, fallback = '/home'): string {
    const candidate = Array.isArray(value) ? value[0] : value
    if (typeof candidate !== 'string' || !candidate.startsWith('/') || candidate.startsWith('//')
        || candidate.includes('\\') || /[\u0000-\u001f\u007f]/.test(candidate)) {
        return fallback
    }
    const target = canonicalPagePath(candidate)
    return isInterimPath(target) ? fallback : target
}

/** 被打断的地址：普通页面取自身 fullPath；已在中间页时沿用它携带的 redirect，保证多次跳转不丢原地址。 */
export function interruptedTarget(location: LocationLike | undefined): string {
    if (!location) return ''
    if (isInterimPath(location.path)) return resolveRedirect(location.query?.redirect, '')
    return resolveRedirect(location.fullPath, '')
}

export function redirectQuery(target: string): LocationQuery {
    const redirect = resolveRedirect(target, '')
    return redirect ? { redirect } : {}
}

/** 构造前往中间页的地址，原地址作为 `redirect` 查询参数带过去。 */
export function interimLocation(path: string, from: LocationLike | undefined) {
    return { path, query: redirectQuery(interruptedTarget(from)) }
}

// 首次加载时请求在守卫里失败，currentRoute 仍是起始位置，需要记下正在进行的导航才能拿到深链接。
let pendingLocation: RouteLocationNormalized | undefined

export function trackPendingNavigation(router: Router): void {
    router.beforeEach(to => {
        pendingLocation = to
    })
    router.afterEach(to => {
        if (pendingLocation?.fullPath === to.fullPath) pendingLocation = undefined
    })
    router.onError(() => {
        pendingLocation = undefined
    })
}

/** 当前（或正在前往的）位置，供请求层在出错时决定跳转与回跳地址。 */
export function currentLocation(router: Router): LocationLike {
    return pendingLocation ?? router.currentRoute.value
}

/** 中间页使用：读取并校验 `redirect`，恢复时 replace 回原地址，没有原地址时回首页。 */
export function useRouteRedirect(fallback = '/home') {
    const route = useRoute()
    const router = useRouter()

    const original = computed(() => resolveRedirect(route.query.redirect, ''))
    const target = computed(() => original.value || fallback)
    const query = computed(() => redirectQuery(original.value))

    const restore = () => router.replace(target.value)

    return { original, target, query, restore }
}
