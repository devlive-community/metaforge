import type { Router } from 'vue-router'
import LayoutContainer from '@/views/layouts/common/LayoutContainer.vue'
import ProfileContainer from '@/views/layouts/profile/LayoutContainer.vue'
import { sourceManageLocation } from '@/model/source-manage'
import { accountPages, businessPages, canonicalPagePath } from './navigation'

const createDefaultRouter = (router: Router) => {
    router.addRoute({
        name: 'app',
        path: '/',
        redirect: '/home',
        component: LayoutContainer,
        meta: { requiresAuth: true },
        children: [
            { name: 'home', path: 'home', component: () => import('@/views/pages/default/dashboard/DashboardHome.vue') },
            { name: 'store', path: 'store', meta: { title: 'common.store' }, component: () => import('@/views/pages/store/StoreHome.vue') },
            { name: 'workspace', path: 'workspace', redirect: '/query' },
            { name: 'analysis', path: 'analysis', redirect: '/dashboard' },
            {
                name: 'dashboard',
                path: 'dashboard',
                meta: {
                    title: 'common.dashboard',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/dashboard/DashboardHome.vue')
            },
            {
                path: 'dashboard/preview/:code',
                meta: {
                    title: 'common.dashboard',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/dashboard/DashboardPreview.vue')
            },
            {
                path: 'dashboard/info/:code?',
                meta: {
                    title: 'common.dashboard',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/dashboard/DashboardInfo.vue')
            },
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
                    title: 'common.dataset',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/dataset/DatasetAdhoc.vue')
            },
            {
                path: 'dataset/adhoc/:code/:id',
                meta: {
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
                meta: {
                    title: 'common.workflow',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/wofkflow/WorkflowHome.vue')
            },
            {
                path: 'workflow/info/:code?',
                meta: {
                    title: 'common.workflow',
                    isRoot: false
                },
                component: () => import('@/views/pages/admin/wofkflow/WorkflowInfo.vue')
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
                name: 'configure',
                path: 'configure',
                meta: {
                    title: 'configure.runtime.title',
                    isRoot: false
                },
                component: () => import('@/views/pages/system/configure/ConfigureHome.vue')
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
