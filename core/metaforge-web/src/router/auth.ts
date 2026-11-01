import { Router } from 'vue-router'

const createAuthRoute = (router: Router) => {
    router.addRoute({
        path: '/auth',
        name: 'auth',
        children: [
            {
                name: 'signin',
                path: 'signin',
                meta: { title: 'user.common.signin' },
                component: () => import('@/views/auth/signin/AuthSignin.vue')
            },
            {
                name: 'signup',
                path: 'signup',
                meta: { title: 'user.common.signup' },
                component: () => import('@/views/auth/signup/AuthSignup.vue')
            },
            {
                name: 'reset',
                path: 'reset',
                meta: { title: 'auth.reset.title' },
                component: () => import('@/views/auth/reset/AuthReset.vue')
            },
            {
                // Where a third-party sign-in comes back to, with a ticket for the token or an error.
                name: 'oauth',
                path: 'oauth',
                meta: { title: 'auth.oauth.title' },
                component: () => import('@/views/auth/oauth/AuthOAuth.vue')
            }
        ]
    })
}

export {
    createAuthRoute
}
