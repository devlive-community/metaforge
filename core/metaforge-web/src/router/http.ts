import { Router } from 'vue-router'

const createHttpRoute = (router: Router) => {
    router.addRoute({
        path: '/common',
        name: 'common',
        children: [
            {
                name: '404',
                path: '404',
                meta: { title: 'navigation.pageNotFound' },
                component: () => import('@/views/common/error/NotFound.vue')
            },
            {
                name: '403',
                path: '403',
                meta: { title: 'navigation.pageNotAuthorized' },
                component: () => import('@/views/common/error/NotAuthorized.vue')
            },
            {
                name: 'not_network',
                path: 'not_network',
                meta: { title: 'common.pageNotNetwork' },
                component: () => import('@/views/common/error/NotNetwork.vue')
            }
        ]
    })
}

export {
    createHttpRoute
}
