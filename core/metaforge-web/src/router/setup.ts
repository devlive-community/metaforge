import { Router } from 'vue-router'

const createSetupRoute = (router: Router) => {
    router.addRoute({
        path: '/setup',
        name: 'setup',
        children: [
            {
                name: 'setup_home',
                path: '',
                component: () => import('@/views/setup/SetupHome.vue')
            }
        ]
    })
}

export {
    createSetupRoute
}
