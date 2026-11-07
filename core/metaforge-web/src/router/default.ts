import type { Router } from 'vue-router'
import LayoutContainer from '@/views/layouts/common/LayoutContainer.vue'
import ProfileContainer from '@/views/layouts/profile/LayoutContainer.vue'
import { sourceManageLocation } from '@/model/source-manage'
import { accountPages, businessPages, canonicalPagePath } from './navigation'
import { useUserStore } from '@/stores/user'
import { legacyScreenPath, SCREEN_LIST_PATH } from '@/model/screen-route'

const createDefaultRouter = (router: Router) => {
    // A user who chose a data screen as their home page sees it at /home, filling the viewport under the
    // navigation; ?default=1 opens the default home. Runs on every visit, including query-only changes.
    router.beforeEach(async to => {
        if (to.name !== 'home') return true
        const code = to.query.default ? '' : await useUserStore().resolveHomePage()
        to.meta.workspace = !code
        to.meta.workflowWorkspace = !!code
        return true
    })
    router.addRoute({
        name: 'app',
        path: '/',
        redirect: '/home',
        component: LayoutContainer,
        meta: { requiresAuth: true },
        children: [
            {
                // The layout follows the user's home page choice; see the guard below.
                name: 'home', path: 'home', meta: { title: 'common.home', workspace: true }, component: () => import('@/views/pages/default/dashboard/HomePage.vue')
            },
            { path: 'home/screen/:code', redirect: '/home' },
            { name: 'store', path: 'store', meta: { title: 'common.store' }, component: () => import('@/views/pages/store/StoreHome.vue') },
            { name: 'notifications', path: 'notifications', meta: { title: 'notify.text.center' }, component: () => import('@/views/pages/notification/NotificationCenter.vue') },
            { name: 'workspace', path: 'workspace', redirect: '/query' },
            { name: 'analysis', path: 'analysis', redirect: SCREEN_LIST_PATH },
            // Data screens: list, designer (new and existing) and the published view, which has no toolbar.
            { name: 'screen', path: 'screen', meta: { title: 'common.dashboard', isRoot: false, workspace: true }, component: () => import('@/views/pages/admin/dashboard/DashboardHome.vue') },
            { name: 'screen-design-new', path: 'screen/design', meta: { dashboardStudio: true, title: 'common.dashboard', isRoot: false }, component: () => import('@/views/pages/admin/dashboard/DashboardInfo.vue') },
            { name: 'screen-design', path: 'screen/:code/design', meta: { dashboardStudio: true, title: 'common.dashboard', isRoot: false }, component: () => import('@/views/pages/admin/dashboard/DashboardInfo.vue') },
            { name: 'screen-view', path: 'screen/:code', meta: { dashboardStudio: true, title: 'common.dashboard', isRoot: false }, component: () => import('@/views/pages/admin/dashboard/DashboardPreview.vue') },
            // Former /dashboard addresses (list, info, preview) keep working, with their query and hash.
            { path: 'dashboard/:rest(.*)*', redirect: to => legacyScreenPath(to.fullPath) || SCREEN_LIST_PATH },
            {
                name: 'query',
                path: 'query',
                meta: {
                    title: 'common.query',
                    isRoot: false,
                    workspace: true
                },
                component: () => import('@/views/pages/admin/query/QueryHome.vue')
            },
            {
                name: 'queryHistory',
                path: 'query/:type/:code?',
                meta: {
                    title: 'common.query',
                    isRoot: false,
                    workspace: true
                },
                component: () => import('@/views/pages/admin/query/QueryHome.vue')
            },
            {
                name: 'dataset',
                path: 'dataset',
                meta: {
                    title: 'common.dataset',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/dataset/DatasetHome.vue')
            },
            {
                path: 'dataset/detail/:code',
                meta: { title: 'common.dataset', isRoot: false },
                component: () => import('@/views/pages/admin/dataset/DatasetDetail.vue')
            },
            {
                // Sync records share the run page layout: the side menu stays, the content scrolls.
                path: 'dataset/runs/:code',
                meta: { workflowWorkspace: true, title: 'common.dataset', isRoot: false },
                component: () => import('@/views/pages/admin/dataset/DatasetRuns.vue')
            },
            {
                path: 'dataset/info/:code?',
                meta: {
                    title: 'common.dataset',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/dataset/DatasetInfo.vue')
            },
            {
                path: 'dataset/info/source/:sourceCode?',
                meta: {
                    title: 'common.dataset',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/dataset/DatasetInfo.vue')
            },
            {
                path: 'dataset/adhoc/:code',
                meta: {
                    datasetStudio: true,
                    title: 'common.dataset',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/dataset/DatasetAdhoc.vue')
            },
            {
                path: 'dataset/adhoc/:code/:id',
                meta: {
                    datasetStudio: true,
                    title: 'common.dataset',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/dataset/DatasetAdhoc.vue')
            },
            {
                path: 'source',
                meta: {
                    title: 'common.source',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/source/SourceHome.vue')
            },
            {
                name: 'manage', path: 'manage', meta: { title: 'common.source', sourceWorkspace: true },
                component: () => import('@/views/layouts/metadata/ManageWorkspace.vue')
            },
            ...[
                { path: 'source/:source', tab: '' },
                { path: 'source/:source/d/:database/', tab: '' },
                ...['structure', 'data', 'info', 'indexes', 'foreignkeys', 'triggers', 'statement', 'erDiagram'].map(tab => ({ path: 'source/:source/d/:database/t/' + tab + '/:table', tab }))
            ].map(({ path, tab }) => ({
                path, redirect: (to: import('vue-router').RouteLocation) => ({
                    ...sourceManageLocation({ source: String(to.params.source), database: String(to.params.database || ''), table: String(to.params.table || ''), tab }, to.query),
                    hash: to.hash
                })
            })),
            {
                path: 'import',
                meta: {
                    title: 'navigation.dataImport',
                    isRoot: false
                },
                component: () => import('@/views/pages/tools/ImportHome.vue')
            },
            {
                path: 'export',
                meta: {
                    title: 'navigation.dataExport',
                    isRoot: false
                },
                component: () => import('@/views/pages/tools/ExportHome.vue')
            },
            {
                path: 'compare',
                meta: {
                    title: 'navigation.dataCompare',
                    isRoot: false
                },
                component: () => import('@/views/pages/tools/CompareHome.vue')
            },
            {
                path: 'er',
                meta: {
                    title: 'navigation.erDiagram',
                    isRoot: false
                },
                component: () => import('@/views/pages/tools/DatabaseEr.vue')
            },
            {
                path: 'history',
                meta: {
                    title: 'common.history',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/history/HistoryHome.vue')
            },
            {
                path: 'snippet',
                meta: {
                    title: 'common.snippet',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/snippet/SnippetHome.vue')
            },
            {
                path: 'report',
                meta: {
                    title: 'common.report',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/report/ReportHome.vue')
            },
            {
                path: 'chat',
                meta: {
                    title: 'common.chat',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/chat/ChatHome.vue')
            },
            {
                path: 'workflow',
                name: 'workflow',
                meta: {
                    title: 'common.workflow',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/wofkflow/WorkflowHome.vue')
            },
            {
                path: 'workflow/info/:code?',
                name: 'workflow-info',
                meta: {
                    workflowWorkspace: true,
                    workflowStudio: true,
                    title: 'common.workflow',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/wofkflow/WorkflowInfo.vue')
            },
            {
                path: 'workflow/monitor/:code',
                name: 'workflow-monitor',
                // The side menu stays in place while the run details scroll beside it.
                meta: { workflowWorkspace: true, title: 'common.workflow', isRoot: false },
                component: () => import('@/views/pages/admin/wofkflow/WorkflowMonitor.vue')
            }
        ]
    })
    router.addRoute({
        name: 'administration', path: '/admin', redirect: '/admin/user', component: LayoutContainer,
        meta: { requiresAuth: true, requiresAdmin: true, title: 'navigation.systemManagement' },
        children: [
            {
                name: 'user',
                path: 'user',
                meta: {
                    title: 'common.user',
                    isRoot: false
                },
                component: () => import('@/views/pages/system/user/UserHome.vue')
            },
            {
                name: 'role',
                path: 'role',
                meta: {
                    title: 'common.role',
                    isRoot: false
                },
                component: () => import('@/views/pages/system/role/RoleHome.vue')
            },
            {
                name: 'schedule',
                path: 'schedule',
                meta: {
                    title: 'common.schedule',
                    isRoot: false
                },
                component: () => import('@/views/pages/system/schedule/ScheduleHome.vue')
            },
            {
                name: 'function',
                path: 'function',
                meta: {
                    title: 'common.function',
                    isRoot: false
                },
                component: () => import('@/views/pages/system/function/FunctionHome.vue')
            },
            {
                name: 'menu',
                path: 'menu',
                meta: {
                    title: 'common.menu',
                    isRoot: false
                },
                component: () => import('@/views/pages/system/menu/MenuHome.vue')
            },
            {
                // Runtime settings moved into System settings.
                name: 'configure',
                path: 'configure',
                redirect: { path: '/admin/settings', query: { tab: 'runtime' } }
            },
            {
                name: 'settings',
                path: 'settings',
                meta: {
                    title: 'navigation.systemSettings',
                    isRoot: false
                },
                component: () => import('@/views/pages/system/settings/SettingsHome.vue')
            }
        ]
    })
    router.addRoute({
        name: 'account', path: '/account', redirect: '/account/profile', component: ProfileContainer,
        meta: { requiresAuth: true, title: 'navigation.accountSettings' },
        children: [
            {
                name: 'info',
                path: 'info',
                meta: {
                    title: 'user.common.info',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/user/profile/InfoHome.vue')
            },
            {
                name: 'profile',
                path: 'profile',
                meta: {
                    title: 'user.common.profile',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/user/profile/ProfileHome.vue')
            },
            {
                name: 'log',
                path: 'log',
                meta: {
                    title: 'user.common.log',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/user/profile/LogHome.vue')
            },
            {
                name: 'editor',
                path: 'editor',
                meta: {
                    title: 'user.common.editor',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/user/profile/EditorHome.vue')
            },
            {
                name: 'assistant',
                path: 'assistant',
                meta: {
                    title: 'user.common.assistant',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/user/profile/AssistantHome.vue')
            },
            {
                name: 'username',
                path: 'username',
                meta: {
                    title: 'user.common.modifyUsername',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/user/security/UsernameHome.vue')
            },
            {
                name: 'password',
                path: 'password',
                meta: {
                    title: 'user.common.modifyPassword',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/user/security/PasswordHome.vue')
            },
            {
                name: 'notify',
                path: 'notify',
                meta: {
                    title: 'user.common.notify',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/user/profile/NotifyHome.vue')
            }
        ]
    })
    const legacyPaths = ['/system/:legacyPath(.*)*', ...businessPages.map(page => `/admin/${page}/:legacyPath(.*)*`), ...accountPages.map(page => `/admin/user/${page}/:legacyPath(.*)*`)]
    for (const path of legacyPaths) {
        router.addRoute({ path, redirect: to => ({ path: canonicalPagePath(to.path), query: to.query, hash: to.hash, replace: true }) })
    }
}

export { createDefaultRouter }
