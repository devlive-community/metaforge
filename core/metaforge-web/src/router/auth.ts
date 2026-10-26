import { Router } from 'vue-router'

const createAuthRoute = (router: Router) => {
    router.addRoute({
        path: '/auth',
        name: 'auth',
        children: [
            {
                name: 'signin',
                path: 'signin',
                component: () => import('@/views/auth/signin/AuthSignin.vue')
            },
            {
                name: 'signup',
                path: 'signup',
                component: () => import('@/views/auth/signup/AuthSignup.vue')
            }
        ]
    })
}

export {
    createAuthRoute
}
