import { Router } from 'vue-router'

const createHttpRoute = (router: Router) => {
    router.addRoute({
        path: '/common',
        name: 'common',
        children: [
            {
                name: '404',
                path: '404',
                component: () => import('@/views/common/error/NotFound.vue')
            },
            {
                name: '403',
                path: '403',
                component: () => import('@/views/common/error/NotAuthorized.vue')
            },
            {
                name: 'not_network',
                path: 'not_network',
                component: () => import('@/views/common/error/NotNetwork.vue')
            }
        ]
    })
}

export {
    createHttpRoute
}
