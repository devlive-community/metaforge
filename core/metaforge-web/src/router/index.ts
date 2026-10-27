import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import { createAuthRoute } from '@/router/auth'
import { createDefaultRouter } from '@/router/default'
import { createHttpRoute } from '@/router/http'
import { createSetupRoute } from '@/router/setup'
import SetupService from '@/services/setup'
import { TokenUtils } from '@/utils/token'
import { pageAccess } from './navigation'

const routes: Array<RouteRecordRaw> = []

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

createHttpRoute(router)
createAuthRoute(router)
createSetupRoute(router)
createDefaultRouter(router)

// 首次导航时检查是否已完成首次安装：未安装跳转 /setup，已安装访问 /setup 回登录页。
// 后端不可达时不拦截，保持原有 not_network 流程。
let setupChecked: Promise<boolean> | undefined

function ensureSetupChecked(): Promise<boolean> {
    if (!setupChecked) {
        setupChecked = SetupService.getStatus()
            .then(response => Boolean(response?.status && response.data?.installed))
            .catch(() => false)
    }
    return setupChecked
}

/** 安装完成后由向导调用，让守卫按最新状态放行登录页。 */
export function invalidateSetupCheck(): void
{
    setupChecked = undefined
}

router.beforeEach(async (_to, _from, _next) => {
    if (_to.matched.length === 0) {
        _next('/common/404')
        return
    }
    const installed = await ensureSetupChecked()
    if (!installed && _to.path !== '/setup') {
        _next('/setup')
    }
    else if (installed && _to.path === '/setup') {
        _next('/auth/signin')
    }
    else {
        const access = pageAccess(_to, TokenUtils.getAuthUser())
        if (access === true) _next()
        else _next(access)
    }
})

export default router
